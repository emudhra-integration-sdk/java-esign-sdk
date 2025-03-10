package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;

public final class BidiOrder {

    private byte[] initialTypes;
    private byte[] embeddings;
    private byte paragraphEmbeddingLevel = -1;

    private int textLength;

    private byte[] resultTypes;

    private byte[] resultLevels;

    public static final byte L = 0;

    public static final byte LRE = 1;

    public static final byte LRO = 2;

    public static final byte R = 3;

    public static final byte AL = 4;

    public static final byte RLE = 5;

    public static final byte RLO = 6;

    public static final byte PDF = 7;

    public static final byte EN = 8;

    public static final byte ES = 9;

    public static final byte ET = 10;

    public static final byte AN = 11;

    public static final byte CS = 12;

    public static final byte NSM = 13;

    public static final byte BN = 14;

    public static final byte B = 15;

    public static final byte S = 16;

    public static final byte WS = 17;

    public static final byte ON = 18;

    public static final byte TYPE_MIN = 0;

    public static final byte TYPE_MAX = 18;

    public BidiOrder(byte[] types) {
        validateTypes(types);

        this.initialTypes = (byte[]) types.clone();

        runAlgorithm();
    }

    public BidiOrder(byte[] types, byte paragraphEmbeddingLevel) {
        validateTypes(types);
        validateParagraphEmbeddingLevel(paragraphEmbeddingLevel);

        this.initialTypes = (byte[]) types.clone();
        this.paragraphEmbeddingLevel = paragraphEmbeddingLevel;

        runAlgorithm();
    }

    public BidiOrder(char[] text, int offset, int length, byte paragraphEmbeddingLevel) {
        this.initialTypes = new byte[length];
        for (int k = 0; k < length; k++) {
            this.initialTypes[k] = rtypes[text[offset + k]];
        }
        validateParagraphEmbeddingLevel(paragraphEmbeddingLevel);

        this.paragraphEmbeddingLevel = paragraphEmbeddingLevel;

        runAlgorithm();
    }

    public static final byte getDirection(char c) {
        return rtypes[c];
    }

    private void runAlgorithm() {
        this.textLength = this.initialTypes.length;

        this.resultTypes = (byte[]) this.initialTypes.clone();

        if (this.paragraphEmbeddingLevel == -1) {
            determineParagraphEmbeddingLevel();
        }

        this.resultLevels = new byte[this.textLength];
        setLevels(0, this.textLength, this.paragraphEmbeddingLevel);

        determineExplicitEmbeddingLevels();

        this.textLength = removeExplicitCodes();

        byte prevLevel = this.paragraphEmbeddingLevel;
        int start = 0;
        while (start < this.textLength) {
            byte level = this.resultLevels[start];
            byte prevType = typeForLevel(Math.max(prevLevel, level));

            int limit = start + 1;
            while (limit < this.textLength && this.resultLevels[limit] == level) {
                limit++;
            }

            byte succLevel = (limit < this.textLength) ? this.resultLevels[limit] : this.paragraphEmbeddingLevel;
            byte succType = typeForLevel(Math.max(succLevel, level));

            resolveWeakTypes(start, limit, level, prevType, succType);

            resolveNeutralTypes(start, limit, level, prevType, succType);

            resolveImplicitLevels(start, limit, level, prevType, succType);

            prevLevel = level;
            start = limit;
        }

        this.textLength = reinsertExplicitCodes(this.textLength);
    }

    private void determineParagraphEmbeddingLevel() {
        byte strongType = -1;

        for (int i = 0; i < this.textLength; i++) {
            byte t = this.resultTypes[i];
            if (t == 0 || t == 4 || t == 3) {
                strongType = t;

                break;
            }
        }

        if (strongType == -1) {

            this.paragraphEmbeddingLevel = 0;
        } else if (strongType == 0) {
            this.paragraphEmbeddingLevel = 0;
        } else {
            this.paragraphEmbeddingLevel = 1;
        }
    }

    private void determineExplicitEmbeddingLevels() {
        this.embeddings = processEmbeddings(this.resultTypes, this.paragraphEmbeddingLevel);

        for (int i = 0; i < this.textLength; i++) {
            byte level = this.embeddings[i];
            if ((level & 0x80) != 0) {
                level = (byte) (level & Byte.MAX_VALUE);
                this.resultTypes[i] = typeForLevel(level);
            }
            this.resultLevels[i] = level;
        }
    }

    private int removeExplicitCodes() {
        int w = 0;
        for (int i = 0; i < this.textLength; i++) {
            byte t = this.initialTypes[i];
            if (t != 1 && t != 5 && t != 2 && t != 6 && t != 7 && t != 14) {
                this.embeddings[w] = this.embeddings[i];
                this.resultTypes[w] = this.resultTypes[i];
                this.resultLevels[w] = this.resultLevels[i];
                w++;
            }
        }
        return w;
    }

    private int reinsertExplicitCodes(int textLength) {
        int i;
        for (i = this.initialTypes.length; --i >= 0;) {
            byte t = this.initialTypes[i];
            if (t == 1 || t == 5 || t == 2 || t == 6 || t == 7 || t == 14) {
                this.embeddings[i] = 0;
                this.resultTypes[i] = t;
                this.resultLevels[i] = -1;
                continue;
            }
            textLength--;
            this.embeddings[i] = this.embeddings[textLength];
            this.resultTypes[i] = this.resultTypes[textLength];
            this.resultLevels[i] = this.resultLevels[textLength];
        }

        if (this.resultLevels[0] == -1) {
            this.resultLevels[0] = this.paragraphEmbeddingLevel;
        }
        for (i = 1; i < this.initialTypes.length; i++) {
            if (this.resultLevels[i] == -1) {
                this.resultLevels[i] = this.resultLevels[i - 1];
            }
        }

        return this.initialTypes.length;
    }

    private static byte[] processEmbeddings(byte[] resultTypes, byte paragraphEmbeddingLevel) {
        int EXPLICIT_LEVEL_LIMIT = 62;

        int textLength = resultTypes.length;
        byte[] embeddings = new byte[textLength];

        byte[] embeddingValueStack = new byte[62];
        int stackCounter = 0;

        int overflowAlmostCounter = 0;

        int overflowCounter = 0;

        byte currentEmbeddingLevel = paragraphEmbeddingLevel;
        byte currentEmbeddingValue = paragraphEmbeddingLevel;

        for (int i = 0; i < textLength; i++) {

            embeddings[i] = currentEmbeddingValue;

            byte t = resultTypes[i];

            switch (t) {

                case 1:
                case 2:
                case 5:
                case 6:
                    if (overflowCounter == 0) {
                        byte newLevel;
                        if (t == 5 || t == 6) {
                            newLevel = (byte) (currentEmbeddingLevel + 1 | 0x1);
                        } else {
                            newLevel = (byte) (currentEmbeddingLevel + 2 & 0xFFFFFFFE);
                        }

                        if (newLevel < 62) {
                            embeddingValueStack[stackCounter] = currentEmbeddingValue;
                            stackCounter++;

                            currentEmbeddingLevel = newLevel;
                            if (t == 2 || t == 6) {
                                currentEmbeddingValue = (byte) (newLevel | 0x80);
                            } else {
                                currentEmbeddingValue = newLevel;
                            }

                            embeddings[i] = currentEmbeddingValue;

                            break;
                        }

                        if (currentEmbeddingLevel == 60) {
                            overflowAlmostCounter++;

                            break;
                        }
                    }

                    overflowCounter++;
                    break;

                case 7:
                    if (overflowCounter > 0) {
                        overflowCounter--;
                        break;
                    }
                    if (overflowAlmostCounter > 0 && currentEmbeddingLevel != 61) {
                        overflowAlmostCounter--;
                        break;
                    }
                    if (stackCounter > 0) {
                        stackCounter--;
                        currentEmbeddingValue = embeddingValueStack[stackCounter];
                        currentEmbeddingLevel = (byte) (currentEmbeddingValue & Byte.MAX_VALUE);
                    }
                    break;

                case 15:
                    stackCounter = 0;
                    overflowCounter = 0;
                    overflowAlmostCounter = 0;
                    currentEmbeddingLevel = paragraphEmbeddingLevel;
                    currentEmbeddingValue = paragraphEmbeddingLevel;

                    embeddings[i] = paragraphEmbeddingLevel;
                    break;
            }

        }
        return embeddings;
    }

    private void resolveWeakTypes(int start, int limit, byte level, byte sor, byte eor) {
        byte preceedingCharacterType = sor;
        int i;
        for (i = start; i < limit; i++) {
            byte t = this.resultTypes[i];
            if (t == 13) {
                this.resultTypes[i] = preceedingCharacterType;
            } else {
                preceedingCharacterType = t;
            }
        }

        for (i = start; i < limit; i++) {
            if (this.resultTypes[i] == 8) {
                for (int j = i - 1; j >= start; j--) {
                    byte t = this.resultTypes[j];
                    if (t == 0 || t == 3 || t == 4) {
                        if (t == 4) {
                            this.resultTypes[i] = 11;
                        }

                        break;
                    }
                }
            }
        }

        for (i = start; i < limit; i++) {
            if (this.resultTypes[i] == 4) {
                this.resultTypes[i] = 3;
            }
        }

        for (i = start + 1; i < limit - 1; i++) {
            if (this.resultTypes[i] == 9 || this.resultTypes[i] == 12) {
                byte prevSepType = this.resultTypes[i - 1];
                byte succSepType = this.resultTypes[i + 1];
                if (prevSepType == 8 && succSepType == 8) {
                    this.resultTypes[i] = 8;
                } else if (this.resultTypes[i] == 12 && prevSepType == 11 && succSepType == 11) {
                    this.resultTypes[i] = 11;
                }
            }
        }

        for (i = start; i < limit; i++) {
            if (this.resultTypes[i] == 10) {

                int runstart = i;
                int runlimit = findRunLimit(runstart, limit, new byte[]{10});

                byte t = (runstart == start) ? sor : this.resultTypes[runstart - 1];

                if (t != 8) {
                    t = (runlimit == limit) ? eor : this.resultTypes[runlimit];
                }

                if (t == 8) {
                    setTypes(runstart, runlimit, (byte) 8);
                }

                i = runlimit;
            }
        }

        for (i = start; i < limit; i++) {
            byte t = this.resultTypes[i];
            if (t == 9 || t == 10 || t == 12) {
                this.resultTypes[i] = 18;
            }
        }

        for (i = start; i < limit; i++) {
            if (this.resultTypes[i] == 8) {

                byte prevStrongType = sor;
                for (int j = i - 1; j >= start; j--) {
                    byte t = this.resultTypes[j];
                    if (t == 0 || t == 3) {
                        prevStrongType = t;
                        break;
                    }
                }
                if (prevStrongType == 0) {
                    this.resultTypes[i] = 0;
                }
            }
        }
    }

    private void resolveNeutralTypes(int start, int limit, byte level, byte sor, byte eor) {
        for (int i = start; i < limit; i++) {
            byte t = this.resultTypes[i];
            if (t == 17 || t == 18 || t == 15 || t == 16) {
                byte leadingType, trailingType, resolvedType;
                int runstart = i;
                int runlimit = findRunLimit(runstart, limit, new byte[]{15, 16, 17, 18});

                if (runstart == start) {
                    leadingType = sor;
                } else {
                    leadingType = this.resultTypes[runstart - 1];
                    if (leadingType != 0 && leadingType != 3) {
                        if (leadingType == 11) {
                            leadingType = 3;
                        } else if (leadingType == 8) {

                            leadingType = 3;
                        }
                    }
                }
                if (runlimit == limit) {
                    trailingType = eor;
                } else {
                    trailingType = this.resultTypes[runlimit];
                    if (trailingType != 0 && trailingType != 3) {
                        if (trailingType == 11) {
                            trailingType = 3;
                        } else if (trailingType == 8) {
                            trailingType = 3;
                        }
                    }
                }

                if (leadingType == trailingType) {

                    resolvedType = leadingType;

                } else {

                    resolvedType = typeForLevel(level);
                }

                setTypes(runstart, runlimit, resolvedType);

                i = runlimit;
            }
        }
    }

    private void resolveImplicitLevels(int start, int limit, byte level, byte sor, byte eor) {
        if ((level & 0x1) == 0) {
            for (int i = start; i < limit; i++) {
                byte t = this.resultTypes[i];

                if (t != 0) {
                    if (t == 3) {
                        this.resultLevels[i] = (byte) (this.resultLevels[i] + 1);
                    } else {
                        this.resultLevels[i] = (byte) (this.resultLevels[i] + 2);
                    }
                }
            }
        } else {
            for (int i = start; i < limit; i++) {
                byte t = this.resultTypes[i];

                if (t != 3) {

                    this.resultLevels[i] = (byte) (this.resultLevels[i] + 1);
                }
            }
        }
    }

    public byte[] getLevels() {
        return getLevels(new int[]{this.textLength});
    }

    public byte[] getLevels(int[] linebreaks) {
        validateLineBreaks(linebreaks, this.textLength);

        byte[] result = (byte[]) this.resultLevels.clone();

        for (int i = 0; i < result.length; i++) {
            byte t = this.initialTypes[i];
            if (t == 15 || t == 16) {

                result[i] = this.paragraphEmbeddingLevel;

                for (int k = i - 1; k >= 0
                        && isWhitespace(this.initialTypes[k]); k--) {
                    result[k] = this.paragraphEmbeddingLevel;
                }
            }
        }

        int start = 0;
        for (int j = 0; j < linebreaks.length; j++) {
            int limit = linebreaks[j];
            for (int k = limit - 1; k >= start
                    && isWhitespace(this.initialTypes[k]); k--) {
                result[k] = this.paragraphEmbeddingLevel;
            }

            start = limit;
        }

        return result;
    }

    public int[] getReordering(int[] linebreaks) {
        validateLineBreaks(linebreaks, this.textLength);

        byte[] levels = getLevels(linebreaks);

        return computeMultilineReordering(levels, linebreaks);
    }

    private static int[] computeMultilineReordering(byte[] levels, int[] linebreaks) {
        int[] result = new int[levels.length];

        int start = 0;
        for (int i = 0; i < linebreaks.length; i++) {
            int limit = linebreaks[i];

            byte[] templevels = new byte[limit - start];
            System.arraycopy(levels, start, templevels, 0, templevels.length);

            int[] temporder = computeReordering(templevels);
            for (int j = 0; j < temporder.length; j++) {
                result[start + j] = temporder[j] + start;
            }

            start = limit;
        }

        return result;
    }

    private static int[] computeReordering(byte[] levels) {
        int lineLength = levels.length;

        int[] result = new int[lineLength];

        for (int i = 0; i < lineLength; i++) {
            result[i] = i;
        }

        byte highestLevel = 0;
        byte lowestOddLevel = 63;
        for (int j = 0; j < lineLength; j++) {
            byte b = levels[j];
            if (b > highestLevel) {
                highestLevel = b;
            }
            if ((b & 0x1) != 0 && b < lowestOddLevel) {
                lowestOddLevel = b;
            }
        }

        for (int level = highestLevel; level >= lowestOddLevel; level--) {
            for (int k = 0; k < lineLength; k++) {
                if (levels[k] >= level) {

                    int start = k;
                    int limit = k + 1;
                    while (limit < lineLength && levels[limit] >= level) {
                        limit++;
                    }

                    for (int m = start, n = limit - 1; m < n; m++, n--) {
                        int temp = result[m];
                        result[m] = result[n];
                        result[n] = temp;
                    }

                    k = limit;
                }
            }
        }

        return result;
    }

    public byte getBaseLevel() {
        return this.paragraphEmbeddingLevel;
    }

    private static boolean isWhitespace(byte biditype) {
        switch (biditype) {
            case 1:
            case 2:
            case 5:
            case 6:
            case 7:
            case 14:
            case 17:
                return true;
        }
        return false;
    }

    private static byte typeForLevel(int level) {
        return ((level & 0x1) == 0) ? (byte) 0 : (byte) 3;
    }

    private int findRunLimit(int index, int limit, byte[] validSet) {
        index--;

        label13:
        while (++index < limit) {
            byte t = this.resultTypes[index];
            for (int i = 0; i < validSet.length; i++) {
                if (t == validSet[i]) {
                    continue label13;
                }
            }

            return index;
        }
        return limit;
    }

    private int findRunStart(int index, byte[] validSet) {
        label12:
        while (--index >= 0) {
            byte t = this.resultTypes[index];
            for (int i = 0; i < validSet.length; i++) {
                if (t == validSet[i]) {
                    continue label12;
                }
            }
            return index + 1;
        }
        return 0;
    }

    private void setTypes(int start, int limit, byte newType) {
        for (int i = start; i < limit; i++) {
            this.resultTypes[i] = newType;
        }
    }

    private void setLevels(int start, int limit, byte newLevel) {
        for (int i = start; i < limit; i++) {
            this.resultLevels[i] = newLevel;
        }
    }

    private static void validateTypes(byte[] types) {
        if (types == null) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("types.is.null", new Object[0]));
        }
        int i;
        for (i = 0; i < types.length; i++) {
            if (types[i] < 0 || types[i] > 18) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("illegal.type.value.at.1.2", new Object[]{String.valueOf(i), String.valueOf(types[i])}));
            }
        }
        for (i = 0; i < types.length - 1; i++) {
            if (types[i] == 15) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("b.type.before.end.of.paragraph.at.index.1", i));
            }
        }
    }

    private static void validateParagraphEmbeddingLevel(byte paragraphEmbeddingLevel) {
        if (paragraphEmbeddingLevel != -1 && paragraphEmbeddingLevel != 0 && paragraphEmbeddingLevel != 1) {

            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("illegal.paragraph.embedding.level.1", paragraphEmbeddingLevel));
        }
    }

    private static void validateLineBreaks(int[] linebreaks, int textLength) {
        int prev = 0;
        for (int i = 0; i < linebreaks.length; i++) {
            int next = linebreaks[i];
            if (next <= prev) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("bad.linebreak.1.at.index.2", new Object[]{String.valueOf(next), String.valueOf(i)}));
            }
            prev = next;
        }
        if (prev != textLength) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("last.linebreak.must.be.at.1", textLength));
        }
    }

    private static final byte[] rtypes = new byte[65536];

    private static char[] baseTypes = new char[]{Character.MIN_VALUE, '\b', '\016', '\t', '\t', '\020', '\n', '\n', '\017', '\013', '\013', '\020', '\f', '\f', '\021', '\r', '\r', '\017', '\016', '\033', '\016', '\034', '\036', '\017', '\037', '\037', '\020', ' ', ' ', '\021', '!', '"', '\022', '#', '%', '\n', '&', '*', '\022', '+', '+', '\n', ',', ',', '\f', '-', '-', '\n', '.', '.', '\f', '/', '/', '\t', '0', '9', '\b', ':', ':', '\f', ';', '@', '\022', 'A', 'Z', Character.MIN_VALUE, '[', '`', '\022', 'a', 'z', Character.MIN_VALUE, '{', '~', '\022', '', '', '\016', '', '', '\017', '', '', '\016', ' ', ' ', '\f', '¡', '¡', '\022', '¢', '¥', '\n', '¦', '©', '\022', 'ª', 'ª', Character.MIN_VALUE, '«', '¯', '\022', '°', '±', '\n', '²', '³', '\b', '´', '´', '\022', 'µ', 'µ', Character.MIN_VALUE, '¶', '¸', '\022', '¹', '¹', '\b', 'º', 'º', Character.MIN_VALUE, '»', '¿', '\022', 'À', 'Ö', Character.MIN_VALUE, '×', '×', '\022', 'Ø', 'ö', Character.MIN_VALUE, '÷', '÷', '\022', 'ø', 'ʸ', Character.MIN_VALUE, 'ʹ', 'ʺ', '\022', 'ʻ', 'ˁ', Character.MIN_VALUE, '˂', 'ˏ', '\022', 'ː', 'ˑ', Character.MIN_VALUE, '˒', '˟', '\022', 'ˠ', 'ˤ', Character.MIN_VALUE, '˥', '˭', '\022', 'ˮ', 'ˮ', Character.MIN_VALUE, '˯', '˿', '\022', '̀', '͗', '\r', '͘', '͜', Character.MIN_VALUE, '͝', 'ͯ', '\r', 'Ͱ', 'ͳ', Character.MIN_VALUE, 'ʹ', '͵', '\022', 'Ͷ', 'ͽ', Character.MIN_VALUE, ';', ';', '\022', 'Ϳ', '΃', Character.MIN_VALUE, '΄', '΅', '\022', 'Ά', 'Ά', Character.MIN_VALUE, '·', '·', '\022', 'Έ', 'ϵ', Character.MIN_VALUE, '϶', '϶', '\022', 'Ϸ', '҂', Character.MIN_VALUE, '҃', '҆', '\r', '҇', '҇', Character.MIN_VALUE, '҈', '҉', '\r', 'Ҋ', '։', Character.MIN_VALUE, '֊', '֊', '\022', '֋', '֐', Character.MIN_VALUE, '֑', '֡', '\r', '֢', '֢', Character.MIN_VALUE, '֣', 'ֹ', '\r', 'ֺ', 'ֺ', Character.MIN_VALUE, 'ֻ', 'ֽ', '\r', '־', '־', '\003', 'ֿ', 'ֿ', '\r', '׀', '׀', '\003', 'ׁ', 'ׂ', '\r', '׃', '׃', '\003', 'ׄ', 'ׄ', '\r', 'ׅ', '׏', Character.MIN_VALUE, 'א', 'ת', '\003', '׫', 'ׯ', Character.MIN_VALUE, 'װ', '״', '\003', '׵', '׿', Character.MIN_VALUE, '؀', '؃', '\004', '؄', '؋', Character.MIN_VALUE, '،', '،', '\f', '؍', '؍', '\004', '؎', '؏', '\022', 'ؐ', 'ؕ', '\r', 'ؖ', 'ؚ', Character.MIN_VALUE, '؛', '؛', '\004', '؜', '؞', Character.MIN_VALUE, '؟', '؟', '\004', 'ؠ', 'ؠ', Character.MIN_VALUE, 'ء', 'غ', '\004', 'ػ', 'ؿ', Character.MIN_VALUE, 'ـ', 'ي', '\004', 'ً', '٘', '\r', 'ٙ', 'ٟ', Character.MIN_VALUE, '٠', '٩', '\013', '٪', '٪', '\n', '٫', '٬', '\013', '٭', 'ٯ', '\004', 'ٰ', 'ٰ', '\r', 'ٱ', 'ە', '\004', 'ۖ', 'ۜ', '\r', '۝', '۝', '\004', '۞', 'ۤ', '\r', 'ۥ', 'ۦ', '\004', 'ۧ', 'ۨ', '\r', '۩', '۩', '\022', '۪', 'ۭ', '\r', 'ۮ', 'ۯ', '\004', '۰', '۹', '\b', 'ۺ', '܍', '\004', '܎', '܎', Character.MIN_VALUE, '܏', '܏', '\016', 'ܐ', 'ܐ', '\004', 'ܑ', 'ܑ', '\r', 'ܒ', 'ܯ', '\004', 'ܰ', '݊', '\r', '݋', '݌', Character.MIN_VALUE, 'ݍ', 'ݏ', '\004', 'ݐ', 'ݿ', Character.MIN_VALUE, 'ހ', 'ޥ', '\004', 'ަ', 'ް', '\r', 'ޱ', 'ޱ', '\004', '޲', 'ऀ', Character.MIN_VALUE, 'ँ', 'ं', '\r', 'ः', 'ऻ', Character.MIN_VALUE, '़', '़', '\r', 'ऽ', 'ी', Character.MIN_VALUE, 'ु', 'ै', '\r', 'ॉ', 'ौ', Character.MIN_VALUE, '्', '्', '\r', 'ॎ', 'ॐ', Character.MIN_VALUE, '॑', '॔', '\r', 'ॕ', 'ॡ', Character.MIN_VALUE, 'ॢ', 'ॣ', '\r', '।', 'ঀ', Character.MIN_VALUE, 'ঁ', 'ঁ', '\r', 'ং', '঻', Character.MIN_VALUE, '়', '়', '\r', 'ঽ', 'ী', Character.MIN_VALUE, 'ু', 'ৄ', '\r', '৅', 'ৌ', Character.MIN_VALUE, '্', '্', '\r', 'ৎ', 'ৡ', Character.MIN_VALUE, 'ৢ', 'ৣ', '\r', '৤', 'ৱ', Character.MIN_VALUE, '৲', '৳', '\n', '৴', '਀', Character.MIN_VALUE, 'ਁ', 'ਂ', '\r', 'ਃ', '਻', Character.MIN_VALUE, '਼', '਼', '\r', '਽', 'ੀ', Character.MIN_VALUE, 'ੁ', 'ੂ', '\r', '੃', '੆', Character.MIN_VALUE, 'ੇ', 'ੈ', '\r', '੉', '੊', Character.MIN_VALUE, 'ੋ', '੍', '\r', '੎', '੯', Character.MIN_VALUE, 'ੰ', 'ੱ', '\r', 'ੲ', '઀', Character.MIN_VALUE, 'ઁ', 'ં', '\r', 'ઃ', '઻', Character.MIN_VALUE, '઼', '઼', '\r', 'ઽ', 'ી', Character.MIN_VALUE, 'ુ', 'ૅ', '\r', '૆', '૆', Character.MIN_VALUE, 'ે', 'ૈ', '\r', 'ૉ', 'ૌ', Character.MIN_VALUE, '્', '્', '\r', '૎', 'ૡ', Character.MIN_VALUE, 'ૢ', 'ૣ', '\r', '૤', '૰', Character.MIN_VALUE, '૱', '૱', '\n', '૲', '଀', Character.MIN_VALUE, 'ଁ', 'ଁ', '\r', 'ଂ', '଻', Character.MIN_VALUE, '଼', '଼', '\r', 'ଽ', 'ା', Character.MIN_VALUE, 'ି', 'ି', '\r', 'ୀ', 'ୀ', Character.MIN_VALUE, 'ୁ', 'ୃ', '\r', 'ୄ', 'ୌ', Character.MIN_VALUE, '୍', '୍', '\r', '୎', '୕', Character.MIN_VALUE, 'ୖ', 'ୖ', '\r', 'ୗ', '஁', Character.MIN_VALUE, 'ஂ', 'ஂ', '\r', 'ஃ', 'ி', Character.MIN_VALUE, 'ீ', 'ீ', '\r', 'ு', 'ௌ', Character.MIN_VALUE, '்', '்', '\r', '௎', '௲', Character.MIN_VALUE, '௳', '௸', '\022', '௹', '௹', '\n', '௺', '௺', '\022', '௻', 'ఽ', Character.MIN_VALUE, 'ా', 'ీ', '\r', 'ు', '౅', Character.MIN_VALUE, 'ె', 'ై', '\r', '౉', '౉', Character.MIN_VALUE, 'ొ', '్', '\r', '౎', '౔', Character.MIN_VALUE, 'ౕ', 'ౖ', '\r', '౗', '಻', Character.MIN_VALUE, '಼', '಼', '\r', 'ಽ', 'ೋ', Character.MIN_VALUE, 'ೌ', '್', '\r', '೎', 'ീ', Character.MIN_VALUE, 'ു', 'ൃ', '\r', 'ൄ', 'ൌ', Character.MIN_VALUE, '്', '്', '\r', 'ൎ', '෉', Character.MIN_VALUE, '්', '්', '\r', '෋', 'ෑ', Character.MIN_VALUE, 'ි', 'ු', '\r', '෕', '෕', Character.MIN_VALUE, 'ූ', 'ූ', '\r', '෗', 'ะ', Character.MIN_VALUE, 'ั', 'ั', '\r', 'า', 'ำ', Character.MIN_VALUE, 'ิ', 'ฺ', '\r', '฻', '฾', Character.MIN_VALUE, '฿', '฿', '\n', 'เ', 'ๆ', Character.MIN_VALUE, '็', '๎', '\r', '๏', 'ະ', Character.MIN_VALUE, 'ັ', 'ັ', '\r', 'າ', 'ຳ', Character.MIN_VALUE, 'ິ', 'ູ', '\r', '຺', '຺', Character.MIN_VALUE, 'ົ', 'ຼ', '\r', 'ຽ', '໇', Character.MIN_VALUE, '່', 'ໍ', '\r', '໎', '༗', Character.MIN_VALUE, '༘', '༙', '\r', '༚', '༴', Character.MIN_VALUE, '༵', '༵', '\r', '༶', '༶', Character.MIN_VALUE, '༷', '༷', '\r', '༸', '༸', Character.MIN_VALUE, '༹', '༹', '\r', '༺', '༽', '\022', '༾', '཰', Character.MIN_VALUE, 'ཱ', 'ཾ', '\r', 'ཿ', 'ཿ', Character.MIN_VALUE, 'ྀ', '྄', '\r', '྅', '྅', Character.MIN_VALUE, '྆', '྇', '\r', 'ྈ', 'ྏ', Character.MIN_VALUE, 'ྐ', 'ྗ', '\r', '྘', '྘', Character.MIN_VALUE, 'ྙ', 'ྼ', '\r', '྽', '࿅', Character.MIN_VALUE, '࿆', '࿆', '\r', '࿇', 'ာ', Character.MIN_VALUE, 'ိ', 'ူ', '\r', 'ေ', 'ေ', Character.MIN_VALUE, 'ဲ', 'ဲ', '\r', 'ဳ', 'ဵ', Character.MIN_VALUE, 'ံ', '့', '\r', 'း', 'း', Character.MIN_VALUE, '္', '္', '\r', '်', 'ၗ', Character.MIN_VALUE, 'ၘ', 'ၙ', '\r', 'ၚ', 'ᙿ', Character.MIN_VALUE, ' ', ' ', '\021', 'ᚁ', 'ᚚ', Character.MIN_VALUE, '᚛', '᚜', '\022', '᚝', 'ᜑ', Character.MIN_VALUE, 'ᜒ', '᜔', '\r', '᜕', 'ᜱ', Character.MIN_VALUE, 'ᜲ', '᜴', '\r', '᜵', 'ᝑ', Character.MIN_VALUE, 'ᝒ', 'ᝓ', '\r', '᝔', '᝱', Character.MIN_VALUE, 'ᝲ', 'ᝳ', '\r', '᝴', 'ា', Character.MIN_VALUE, 'ិ', 'ួ', '\r', 'ើ', 'ៅ', Character.MIN_VALUE, 'ំ', 'ំ', '\r', 'ះ', 'ៈ', Character.MIN_VALUE, '៉', '៓', '\r', '។', '៚', Character.MIN_VALUE, '៛', '៛', '\n', 'ៜ', 'ៜ', Character.MIN_VALUE, '៝', '៝', '\r', '៞', '៯', Character.MIN_VALUE, '៰', '៹', '\022', '៺', '៿', Character.MIN_VALUE, '᠀', '᠊', '\022', '᠋', '᠍', '\r', '᠎', '᠎', '\021', '᠏', 'ᢨ', Character.MIN_VALUE, 'ᢩ', 'ᢩ', '\r', 'ᢪ', '᤟', Character.MIN_VALUE, 'ᤠ', 'ᤢ', '\r', 'ᤣ', 'ᤦ', Character.MIN_VALUE, 'ᤧ', 'ᤫ', '\r', '᤬', 'ᤱ', Character.MIN_VALUE, 'ᤲ', 'ᤲ', '\r', 'ᤳ', 'ᤸ', Character.MIN_VALUE, '᤹', '᤻', '\r', '᤼', '᤿', Character.MIN_VALUE, '᥀', '᥀', '\022', '᥁', '᥃', Character.MIN_VALUE, '᥄', '᥅', '\022', '᥆', '᧟', Character.MIN_VALUE, '᧠', '᧿', '\022', 'ᨀ', 'ᾼ', Character.MIN_VALUE, '᾽', '᾽', '\022', 'ι', 'ι', Character.MIN_VALUE, '᾿', '῁', '\022', 'ῂ', 'ῌ', Character.MIN_VALUE, '῍', '῏', '\022', 'ῐ', '῜', Character.MIN_VALUE, '῝', '῟', '\022', 'ῠ', 'Ῥ', Character.MIN_VALUE, '῭', '`', '\022', '῰', 'ῼ', Character.MIN_VALUE, '´', '῾', '\022', '῿', '῿', Character.MIN_VALUE, ' ', ' ', '\021', '​', '‍', '\016', '‎', '‎', Character.MIN_VALUE, '‏', '‏', '\003', '‐', '‧', '\022', ' ', ' ', '\021', ' ', ' ', '\017', '‪', '‪', '\001', '‫', '‫', '\005', '‬', '‬', '\007', '‭', '‭', '\002', '‮', '‮', '\006', ' ', ' ', '\021', '‰', '‴', '\n', '‵', '⁔', '\022', '⁕', '⁖', Character.MIN_VALUE, '⁗', '⁗', '\022', '⁘', '⁞', Character.MIN_VALUE, ' ', ' ', '\021', '⁠', '⁣', '\016', '⁤', '⁩', Character.MIN_VALUE, '⁪', '⁯', '\016', '⁰', '⁰', '\b', 'ⁱ', '⁳', Character.MIN_VALUE, '⁴', '⁹', '\b', '⁺', '⁻', '\n', '⁼', '⁾', '\022', 'ⁿ', 'ⁿ', Character.MIN_VALUE, '₀', '₉', '\b', '₊', '₋', '\n', '₌', '₎', '\022', '₏', '₟', Character.MIN_VALUE, '₠', '₱', '\n', '₲', '⃏', Character.MIN_VALUE, '⃐', '⃪', '\r', '⃫', '⃿', Character.MIN_VALUE, '℀', '℁', '\022', 'ℂ', 'ℂ', Character.MIN_VALUE, '℃', '℆', '\022', 'ℇ', 'ℇ', Character.MIN_VALUE, '℈', '℉', '\022', 'ℊ', 'ℓ', Character.MIN_VALUE, '℔', '℔', '\022', 'ℕ', 'ℕ', Character.MIN_VALUE, '№', '℘', '\022', 'ℙ', 'ℝ', Character.MIN_VALUE, '℞', '℣', '\022', 'ℤ', 'ℤ', Character.MIN_VALUE, '℥', '℥', '\022', 'Ω', 'Ω', Character.MIN_VALUE, '℧', '℧', '\022', 'ℨ', 'ℨ', Character.MIN_VALUE, '℩', '℩', '\022', 'K', 'ℭ', Character.MIN_VALUE, '℮', '℮', '\n', 'ℯ', 'ℱ', Character.MIN_VALUE, 'Ⅎ', 'Ⅎ', '\022', 'ℳ', 'ℹ', Character.MIN_VALUE, '℺', '℻', '\022', 'ℼ', 'ℿ', Character.MIN_VALUE, '⅀', '⅄', '\022', 'ⅅ', 'ⅉ', Character.MIN_VALUE, '⅊', '⅋', '\022', '⅌', '⅒', Character.MIN_VALUE, '⅓', '⅟', '\022', 'Ⅰ', '↏', Character.MIN_VALUE, '←', '∑', '\022', '−', '∓', '\n', '∔', '⌵', '\022', '⌶', '⍺', Character.MIN_VALUE, '⍻', '⎔', '\022', '⎕', '⎕', Character.MIN_VALUE, '⎖', '⏐', '\022', '⏑', '⏿', Character.MIN_VALUE, '␀', '␦', '\022', '␧', '␿', Character.MIN_VALUE, '⑀', '⑊', '\022', '⑋', '⑟', Character.MIN_VALUE, '①', '⒛', '\b', '⒜', 'ⓩ', Character.MIN_VALUE, '⓪', '⓪', '\b', '⓫', '☗', '\022', '☘', '☘', Character.MIN_VALUE, '☙', '♽', '\022', '♾', '♿', Character.MIN_VALUE, '⚀', '⚑', '\022', '⚒', '⚟', Character.MIN_VALUE, '⚠', '⚡', '\022', '⚢', '✀', Character.MIN_VALUE, '✁', '✄', '\022', '✅', '✅', Character.MIN_VALUE, '✆', '✉', '\022', '✊', '✋', Character.MIN_VALUE, '✌', '✧', '\022', '✨', '✨', Character.MIN_VALUE, '✩', '❋', '\022', '❌', '❌', Character.MIN_VALUE, '❍', '❍', '\022', '❎', '❎', Character.MIN_VALUE, '❏', '❒', '\022', '❓', '❕', Character.MIN_VALUE, '❖', '❖', '\022', '❗', '❗', Character.MIN_VALUE, '❘', '❞', '\022', '❟', '❠', Character.MIN_VALUE, '❡', '➔', '\022', '➕', '➗', Character.MIN_VALUE, '➘', '➯', '\022', '➰', '➰', Character.MIN_VALUE, '➱', '➾', '\022', '➿', '⟏', Character.MIN_VALUE, '⟐', '⟫', '\022', '⟬', '⟯', Character.MIN_VALUE, '⟰', '⬍', '\022', '⬎', '⹿', Character.MIN_VALUE, '⺀', '⺙', '\022', '⺚', '⺚', Character.MIN_VALUE, '⺛', '⻳', '\022', '⻴', '⻿', Character.MIN_VALUE, '⼀', '⿕', '\022', '⿖', '⿯', Character.MIN_VALUE, '⿰', '⿻', '\022', '⿼', '⿿', Character.MIN_VALUE, '　', '　', '\021', '、', '〄', '\022', '々', '〇', Character.MIN_VALUE, '〈', '〠', '\022', '〡', '〩', Character.MIN_VALUE, '〪', '〯', '\r', '〰', '〰', '\022', '〱', '〵', Character.MIN_VALUE, '〶', '〷', '\022', '〸', '〼', Character.MIN_VALUE, '〽', '〿', '\022', '぀', '゘', Character.MIN_VALUE, '゙', '゚', '\r', '゛', '゜', '\022', 'ゝ', 'ゟ', Character.MIN_VALUE, '゠', '゠', '\022', 'ァ', 'ヺ', Character.MIN_VALUE, '・', '・', '\022', 'ー', '㈜', Character.MIN_VALUE, '㈝', '㈞', '\022', '㈟', '㉏', Character.MIN_VALUE, '㉐', '㉟', '\022', '㉠', '㉻', Character.MIN_VALUE, '㉼', '㉽', '\022', '㉾', '㊰', Character.MIN_VALUE, '㊱', '㊿', '\022', '㋀', '㋋', Character.MIN_VALUE, '㋌', '㋏', '\022', '㋐', '㍶', Character.MIN_VALUE, '㍷', '㍺', '\022', '㍻', '㏝', Character.MIN_VALUE, '㏞', '㏟', '\022', '㏠', '㏾', Character.MIN_VALUE, '㏿', '㏿', '\022', '㐀', '䶿', Character.MIN_VALUE, '䷀', '䷿', '\022', '一', '꒏', Character.MIN_VALUE, '꒐', '꓆', '\022', '꓇', '﬜', Character.MIN_VALUE, 'יִ', 'יִ', '\003', 'ﬞ', 'ﬞ', '\r', 'ײַ', 'ﬨ', '\003', '﬩', '﬩', '\n', 'שׁ', 'זּ', '\003', '﬷', '﬷', Character.MIN_VALUE, 'טּ', 'לּ', '\003', '﬽', '﬽', Character.MIN_VALUE, 'מּ', 'מּ', '\003', '﬿', '﬿', Character.MIN_VALUE, 'נּ', 'סּ', '\003', '﭂', '﭂', Character.MIN_VALUE, 'ףּ', 'פּ', '\003', '﭅', '﭅', Character.MIN_VALUE, 'צּ', 'ﭏ', '\003', 'ﭐ', 'ﮱ', '\004', '﮲', '﯒', Character.MIN_VALUE, 'ﯓ', 'ﴽ', '\004', '﴾', '﴿', '\022', '﵀', '﵏', Character.MIN_VALUE, 'ﵐ', 'ﶏ', '\004', '﶐', '﶑', Character.MIN_VALUE, 'ﶒ', 'ﷇ', '\004', '﷈', '﷯', Character.MIN_VALUE, 'ﷰ', '﷼', '\004', '﷽', '﷽', '\022', '﷾', '﷿', Character.MIN_VALUE, '︀', '️', '\r', '︐', '︟', Character.MIN_VALUE, '︠', '︣', '\r', '︤', '︯', Character.MIN_VALUE, '︰', '﹏', '\022', '﹐', '﹐', '\f', '﹑', '﹑', '\022', '﹒', '﹒', '\f', '﹓', '﹓', Character.MIN_VALUE, '﹔', '﹔', '\022', '﹕', '﹕', '\f', '﹖', '﹞', '\022', '﹟', '﹟', '\n', '﹠', '﹡', '\022', '﹢', '﹣', '\n', '﹤', '﹦', '\022', '﹧', '﹧', Character.MIN_VALUE, '﹨', '﹨', '\022', '﹩', '﹪', '\n', '﹫', '﹫', '\022', '﹬', '﹯', Character.MIN_VALUE, 'ﹰ', 'ﹴ', '\004', '﹵', '﹵', Character.MIN_VALUE, 'ﹶ', 'ﻼ', '\004', '﻽', '﻾', Character.MIN_VALUE, '﻿', '﻿', '\016', '＀', '＀', Character.MIN_VALUE, '！', '＂', '\022', '＃', '％', '\n', '＆', '＊', '\022', '＋', '＋', '\n', '，', '，', '\f', '－', '－', '\n', '．', '．', '\f', '／', '／', '\t', '０', '９', '\b', '：', '：', '\f', '；', '＠', '\022', 'Ａ', 'Ｚ', Character.MIN_VALUE, '［', '｀', '\022', 'ａ', 'ｚ', Character.MIN_VALUE, '｛', '･', '\022', 'ｦ', '￟', Character.MIN_VALUE, '￠', '￡', '\n', '￢', '￤', '\022', '￥', '￦', '\n', '￧', '￧', Character.MIN_VALUE, '￨', '￮', '\022', '￯', '￸', Character.MIN_VALUE, '￹', '￻', '\016', '￼', '�', '\022', '￾', Character.MAX_VALUE, Character.MIN_VALUE};

    static {
        for (int k = 0; k < baseTypes.length; k++) {
            int start = baseTypes[k];
            int end = baseTypes[++k];
            byte b = (byte) baseTypes[++k];
            while (start <= end) {
                rtypes[start++] = b;
            }
        }
    }
}
