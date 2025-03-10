package esign.text.pdf;

import esign.text.Chunk;
import esign.text.Image;
import esign.text.TabStop;
import esign.text.Utilities;
import esign.text.pdf.draw.DrawInterface;
import esign.text.pdf.draw.LineSeparator;
import esign.text.pdf.languages.ArabicLigaturizer;
import java.util.ArrayList;
import java.util.List;

public class BidiLine {

    protected int runDirection;
    protected int pieceSize = 256;
    protected char[] text = new char[this.pieceSize];
    protected PdfChunk[] detailChunks = new PdfChunk[this.pieceSize];
    protected int totalTextLength = 0;

    protected byte[] orderLevels = new byte[this.pieceSize];
    protected int[] indexChars = new int[this.pieceSize];

    protected ArrayList<PdfChunk> chunks = new ArrayList<PdfChunk>();
    protected int indexChunk = 0;
    protected int indexChunkChar = 0;
    protected int currentChar = 0;

    protected int storedRunDirection;
    protected char[] storedText = new char[0];
    protected PdfChunk[] storedDetailChunks = new PdfChunk[0];
    protected int storedTotalTextLength = 0;

    protected byte[] storedOrderLevels = new byte[0];
    protected int[] storedIndexChars = new int[0];

    protected int storedIndexChunk = 0;
    protected int storedIndexChunkChar = 0;
    protected int storedCurrentChar = 0;

    protected boolean isWordSplit = false;

    protected boolean shortStore;

    protected static final IntHashtable mirrorChars = new IntHashtable();

    protected int arabicOptions;

    public BidiLine(BidiLine org) {
        this.runDirection = org.runDirection;
        this.pieceSize = org.pieceSize;
        this.text = (char[]) org.text.clone();
        this.detailChunks = (PdfChunk[]) org.detailChunks.clone();
        this.totalTextLength = org.totalTextLength;

        this.orderLevels = (byte[]) org.orderLevels.clone();
        this.indexChars = (int[]) org.indexChars.clone();

        this.chunks = new ArrayList<PdfChunk>(org.chunks);
        this.indexChunk = org.indexChunk;
        this.indexChunkChar = org.indexChunkChar;
        this.currentChar = org.currentChar;

        this.storedRunDirection = org.storedRunDirection;
        this.storedText = (char[]) org.storedText.clone();
        this.storedDetailChunks = (PdfChunk[]) org.storedDetailChunks.clone();
        this.storedTotalTextLength = org.storedTotalTextLength;

        this.storedOrderLevels = (byte[]) org.storedOrderLevels.clone();
        this.storedIndexChars = (int[]) org.storedIndexChars.clone();

        this.storedIndexChunk = org.storedIndexChunk;
        this.storedIndexChunkChar = org.storedIndexChunkChar;
        this.storedCurrentChar = org.storedCurrentChar;

        this.shortStore = org.shortStore;
        this.arabicOptions = org.arabicOptions;
    }

    public boolean isEmpty() {
        return (this.currentChar >= this.totalTextLength && this.indexChunk >= this.chunks.size());
    }

    public void clearChunks() {
        this.chunks.clear();
        this.totalTextLength = 0;
        this.currentChar = 0;
    }

    public boolean getParagraph(int runDirection) {
        this.runDirection = runDirection;
        this.currentChar = 0;
        this.totalTextLength = 0;

        boolean hasText = false;

        for (; this.indexChunk < this.chunks.size(); this.indexChunk++) {
            PdfChunk ck = this.chunks.get(this.indexChunk);
            BaseFont bf = ck.font().getFont();
            String s = ck.toString();
            int len = s.length();
            for (; this.indexChunkChar < len; this.indexChunkChar++) {
                char c = s.charAt(this.indexChunkChar);
                char uniC = (char) bf.getUnicodeEquivalent(c);
                if (uniC == '\r' || uniC == '\n') {

                    if (uniC == '\r' && this.indexChunkChar + 1 < len && s.charAt(this.indexChunkChar + 1) == '\n') {
                        this.indexChunkChar++;
                    }
                    this.indexChunkChar++;
                    if (this.indexChunkChar >= len) {
                        this.indexChunkChar = 0;
                        this.indexChunk++;
                    }
                    hasText = true;
                    if (this.totalTextLength == 0) {
                        this.detailChunks[0] = ck;
                    }
                    break;
                }
                addPiece(c, ck);
            }
            if (hasText) {
                break;
            }
            this.indexChunkChar = 0;
        }
        if (this.totalTextLength == 0) {
            return hasText;
        }

        this.totalTextLength = trimRight(0, this.totalTextLength - 1) + 1;
        if (this.totalTextLength == 0) {
            return true;
        }

        if (runDirection != 1) {
            byte paragraphEmbeddingLevel;
            if (this.orderLevels.length < this.totalTextLength) {
                this.orderLevels = new byte[this.pieceSize];
                this.indexChars = new int[this.pieceSize];
            }
            ArabicLigaturizer.processNumbers(this.text, 0, this.totalTextLength, this.arabicOptions);

            switch (runDirection) {
                case 2:
                    paragraphEmbeddingLevel = 0;
                    break;
                case 3:
                    paragraphEmbeddingLevel = 1;
                    break;

                default:
                    paragraphEmbeddingLevel = -1;
                    break;
            }
            BidiOrder order = new BidiOrder(this.text, 0, this.totalTextLength, paragraphEmbeddingLevel);
            byte[] od = order.getLevels();
            for (int k = 0; k < this.totalTextLength; k++) {
                this.orderLevels[k] = od[k];
                this.indexChars[k] = k;
            }
            doArabicShapping();
            mirrorGlyphs();
        }
        this.totalTextLength = trimRightEx(0, this.totalTextLength - 1) + 1;
        return true;
    }

    public void addChunk(PdfChunk chunk) {
        this.chunks.add(chunk);
    }

    public void addChunks(ArrayList<PdfChunk> chunks) {
        this.chunks.addAll(chunks);
    }

    public void addPiece(char c, PdfChunk chunk) {
        if (this.totalTextLength >= this.pieceSize) {
            char[] tempText = this.text;
            PdfChunk[] tempDetailChunks = this.detailChunks;
            this.pieceSize *= 2;
            this.text = new char[this.pieceSize];
            this.detailChunks = new PdfChunk[this.pieceSize];
            System.arraycopy(tempText, 0, this.text, 0, this.totalTextLength);
            System.arraycopy(tempDetailChunks, 0, this.detailChunks, 0, this.totalTextLength);
        }
        this.text[this.totalTextLength] = c;
        this.detailChunks[this.totalTextLength++] = chunk;
    }

    public void save() {
        if (this.indexChunk > 0) {
            if (this.indexChunk >= this.chunks.size()) {
                this.chunks.clear();
            } else {
                this.indexChunk--;
                for (; this.indexChunk >= 0; this.indexChunk--) {
                    this.chunks.remove(this.indexChunk);
                }
            }
            this.indexChunk = 0;
        }
        this.storedRunDirection = this.runDirection;
        this.storedTotalTextLength = this.totalTextLength;
        this.storedIndexChunk = this.indexChunk;
        this.storedIndexChunkChar = this.indexChunkChar;
        this.storedCurrentChar = this.currentChar;
        this.shortStore = (this.currentChar < this.totalTextLength);
        if (!this.shortStore) {

            if (this.storedText.length < this.totalTextLength) {
                this.storedText = new char[this.totalTextLength];
                this.storedDetailChunks = new PdfChunk[this.totalTextLength];
            }
            System.arraycopy(this.text, 0, this.storedText, 0, this.totalTextLength);
            System.arraycopy(this.detailChunks, 0, this.storedDetailChunks, 0, this.totalTextLength);
        }
        if (this.runDirection != 1) {
            if (this.storedOrderLevels.length < this.totalTextLength) {
                this.storedOrderLevels = new byte[this.totalTextLength];
                this.storedIndexChars = new int[this.totalTextLength];
            }
            System.arraycopy(this.orderLevels, this.currentChar, this.storedOrderLevels, this.currentChar, this.totalTextLength - this.currentChar);
            System.arraycopy(this.indexChars, this.currentChar, this.storedIndexChars, this.currentChar, this.totalTextLength - this.currentChar);
        }
    }

    public void restore() {
        this.runDirection = this.storedRunDirection;
        this.totalTextLength = this.storedTotalTextLength;
        this.indexChunk = this.storedIndexChunk;
        this.indexChunkChar = this.storedIndexChunkChar;
        this.currentChar = this.storedCurrentChar;
        if (!this.shortStore) {

            System.arraycopy(this.storedText, 0, this.text, 0, this.totalTextLength);
            System.arraycopy(this.storedDetailChunks, 0, this.detailChunks, 0, this.totalTextLength);
        }
        if (this.runDirection != 1) {
            System.arraycopy(this.storedOrderLevels, this.currentChar, this.orderLevels, this.currentChar, this.totalTextLength - this.currentChar);
            System.arraycopy(this.storedIndexChars, this.currentChar, this.indexChars, this.currentChar, this.totalTextLength - this.currentChar);
        }
    }

    public void mirrorGlyphs() {
        for (int k = 0; k < this.totalTextLength; k++) {
            if ((this.orderLevels[k] & 0x1) == 1) {
                int mirror = mirrorChars.get(this.text[k]);
                if (mirror != 0) {
                    this.text[k] = (char) mirror;
                }
            }
        }
    }

    public void doArabicShapping() {
        int src = 0;
        int dest = 0;
        while (true) {
            if (src < this.totalTextLength) {
                char c = this.text[src];
                if (c < '؀' || c > 'ۿ') {

                    if (src != dest) {
                        this.text[dest] = this.text[src];
                        this.detailChunks[dest] = this.detailChunks[src];
                        this.orderLevels[dest] = this.orderLevels[src];
                    }
                    src++;
                    dest++;
                    continue;
                }
            }
            if (src >= this.totalTextLength) {
                this.totalTextLength = dest;
                return;
            }
            int startArabicIdx = src;
            src++;
            while (src < this.totalTextLength) {
                char c = this.text[src];
                if (c < '؀' || c > 'ۿ') {
                    break;
                }
                src++;
            }
            int arabicWordSize = src - startArabicIdx;
            int size = ArabicLigaturizer.arabic_shape(this.text, startArabicIdx, arabicWordSize, this.text, dest, arabicWordSize, this.arabicOptions);
            if (startArabicIdx != dest) {
                for (int k = 0; k < size; k++) {
                    this.detailChunks[dest] = this.detailChunks[startArabicIdx];
                    this.orderLevels[dest++] = this.orderLevels[startArabicIdx++];
                }
                continue;
            }
            dest += size;
        }
    }

    public PdfLine processLine(float leftX, float width, int alignment, int runDirection, int arabicOptions, float minY, float yLine, float descender) {
        this.isWordSplit = false;
        this.arabicOptions = arabicOptions;
        save();
        boolean isRTL = (runDirection == 3);
        if (this.currentChar >= this.totalTextLength) {
            boolean hasText = getParagraph(runDirection);
            if (!hasText) {
                return null;
            }
            if (this.totalTextLength == 0) {
                ArrayList<PdfChunk> ar = new ArrayList<PdfChunk>();
                PdfChunk pdfChunk = new PdfChunk("", this.detailChunks[0]);
                ar.add(pdfChunk);
                return new PdfLine(0.0F, 0.0F, width, alignment, true, ar, isRTL);
            }
        }
        float originalWidth = width;
        int lastSplit = -1;
        if (this.currentChar != 0) {
            this.currentChar = trimLeftEx(this.currentChar, this.totalTextLength - 1);
        }
        int oldCurrentChar = this.currentChar;
        int uniC = 0;
        PdfChunk ck = null;
        float charWidth = 0.0F;
        PdfChunk lastValidChunk = null;
        TabStop tabStop = null;
        List<TabStop> rtlTabsToBeAligned = new ArrayList<TabStop>();
        float tabStopAnchorPosition = Float.NaN;
        float tabPosition = Float.NaN;
        boolean surrogate = false;
        for (; this.currentChar < this.totalTextLength; this.currentChar++) {
            ck = this.detailChunks[this.currentChar];
            if (ck.isImage() && minY < yLine) {
                Image img = ck.getImage();
                if (img.isScaleToFitHeight() && yLine + 2.0F * descender - img.getScaledHeight() - ck.getImageOffsetY() - img.getSpacingBefore() < minY) {
                    float scalePercent = (yLine + 2.0F * descender - ck.getImageOffsetY() - img.getSpacingBefore() - minY) / img.getScaledHeight();
                    ck.setImageScalePercentage(scalePercent);
                }
            }
            surrogate = Utilities.isSurrogatePair(this.text, this.currentChar);
            if (surrogate) {
                uniC = ck.getUnicodeEquivalent(Utilities.convertToUtf32(this.text, this.currentChar));
            } else {
                uniC = ck.getUnicodeEquivalent(this.text[this.currentChar]);
            }
            if (!PdfChunk.noPrint(uniC)) {

                if (surrogate) {
                    charWidth = ck.getCharWidth(uniC);
                } else if (ck.isImage()) {
                    charWidth = ck.getImageWidth();
                } else {
                    charWidth = ck.getCharWidth(this.text[this.currentChar]);
                }

                if (width - charWidth < 0.0F) {

                    if (lastValidChunk == null && ck.isImage()) {
                        Image img = ck.getImage();
                        if (img.isScaleToFitLineWhenOverflow()) {

                            float scalePercent = width / img.getWidth();
                            ck.setImageScalePercentage(scalePercent);
                            charWidth = width;
                        }
                    }
                }
                if (ck.isTab()) {
                    if (ck.isAttribute("TABSETTINGS")) {
                        lastSplit = this.currentChar;
                        if (tabStop != null) {
                            width = processTabStop(tabStop, tabPosition, originalWidth, width, tabStopAnchorPosition, isRTL, rtlTabsToBeAligned);
                        }

                        tabStop = PdfChunk.getTabStop(ck, originalWidth - width);
                        if (tabStop.getPosition() > originalWidth) {
                            tabStop = null;
                            break;
                        }
                        ck.setTabStop(tabStop);
                        if (!isRTL && tabStop.getAlignment() == TabStop.Alignment.LEFT) {
                            width = originalWidth - tabStop.getPosition();
                            tabStop = null;
                            tabPosition = Float.NaN;
                            tabStopAnchorPosition = Float.NaN;
                        } else {
                            tabPosition = originalWidth - width;
                            tabStopAnchorPosition = Float.NaN;
                        }
                    } else {
                        Object[] tab = (Object[]) ck.getAttribute("TAB");

                        float tabStopPosition = ((Float) tab[1]).floatValue();
                        boolean newLine = ((Boolean) tab[2]).booleanValue();
                        if (newLine && tabStopPosition < originalWidth - width) {
                            return new PdfLine(0.0F, originalWidth, width, alignment, true, createArrayOfPdfChunks(oldCurrentChar, this.currentChar - 1), isRTL);
                        }
                        this.detailChunks[this.currentChar].adjustLeft(leftX);
                        width = originalWidth - tabStopPosition;
                    }

                } else if (ck.isSeparator()) {
                    Object[] sep = (Object[]) ck.getAttribute("SEPARATOR");
                    DrawInterface di = (DrawInterface) sep[0];
                    Boolean vertical = (Boolean) sep[1];
                    if (vertical.booleanValue() && di instanceof LineSeparator) {
                        float separatorWidth = originalWidth * ((LineSeparator) di).getPercentage() / 100.0F;
                        width -= separatorWidth;
                        if (width < 0.0F) {
                            width = 0.0F;
                        }
                    }
                } else {
                    boolean splitChar = ck.isExtSplitCharacter(oldCurrentChar, this.currentChar, this.totalTextLength, this.text, this.detailChunks);
                    if (splitChar && Character.isWhitespace((char) uniC)) {
                        lastSplit = this.currentChar;
                    }
                    if (width - charWidth < 0.0F) {
                        break;
                    }
                    if (tabStop != null && tabStop.getAlignment() == TabStop.Alignment.ANCHOR && Float.isNaN(tabStopAnchorPosition) && tabStop.getAnchorChar() == (char) uniC) {
                        tabStopAnchorPosition = originalWidth - width;
                    }
                    width -= charWidth;
                    if (splitChar) {
                        lastSplit = this.currentChar;
                    }
                }
                lastValidChunk = ck;
                if (surrogate) {
                    this.currentChar++;
                }
            }
        }
        if (lastValidChunk == null) {

            this.currentChar++;
            if (surrogate) {
                this.currentChar++;
            }
            return new PdfLine(0.0F, originalWidth, 0.0F, alignment, false, createArrayOfPdfChunks(this.currentChar - 1, this.currentChar - 1), isRTL);
        }

        if (tabStop != null) {
            width = processTabStop(tabStop, tabPosition, originalWidth, width, tabStopAnchorPosition, isRTL, rtlTabsToBeAligned);
        }
        if (rtlTabsToBeAligned != null) {
            for (TabStop rtlTabStop : rtlTabsToBeAligned) {
                rtlTabStop.setPosition(originalWidth - width - rtlTabStop.getPosition());
            }
        }

        if (this.currentChar >= this.totalTextLength) {
            return new PdfLine(0.0F, originalWidth, width, alignment, true, createArrayOfPdfChunks(oldCurrentChar, this.totalTextLength - 1), isRTL);
        }
        int newCurrentChar = trimRightEx(oldCurrentChar, this.currentChar - 1);
        if (newCurrentChar < oldCurrentChar) {
            return new PdfLine(0.0F, originalWidth, width, alignment, false, createArrayOfPdfChunks(oldCurrentChar, this.currentChar - 1), isRTL);
        }
        if (newCurrentChar == this.currentChar - 1) {
            HyphenationEvent he = (HyphenationEvent) lastValidChunk.getAttribute("HYPHENATION");
            if (he != null) {
                int[] word = getWord(oldCurrentChar, newCurrentChar);
                if (word != null) {
                    float testWidth = width + getWidth(word[0], this.currentChar - 1);
                    String pre = he.getHyphenatedWordPre(new String(this.text, word[0], word[1] - word[0]), lastValidChunk.font().getFont(), lastValidChunk.font().size(), testWidth);
                    String post = he.getHyphenatedWordPost();
                    if (pre.length() > 0) {
                        PdfChunk extra = new PdfChunk(pre, lastValidChunk);
                        this.currentChar = word[1] - post.length();
                        return new PdfLine(0.0F, originalWidth, testWidth - lastValidChunk.width(pre), alignment, false, createArrayOfPdfChunks(oldCurrentChar, word[0] - 1, extra), isRTL);
                    }
                }
            }
        }
        if (lastSplit == -1) {
            this.isWordSplit = true;
        }
        if (lastSplit == -1 || lastSplit >= newCurrentChar) {
            return new PdfLine(0.0F, originalWidth, width + getWidth(newCurrentChar + 1, this.currentChar - 1, originalWidth), alignment, false, createArrayOfPdfChunks(oldCurrentChar, newCurrentChar), isRTL);
        }

        this.currentChar = lastSplit + 1;
        newCurrentChar = trimRightEx(oldCurrentChar, lastSplit);
        if (newCurrentChar < oldCurrentChar) {
            newCurrentChar = this.currentChar - 1;
        }
        return new PdfLine(0.0F, originalWidth, originalWidth - getWidth(oldCurrentChar, newCurrentChar, originalWidth), alignment, false, createArrayOfPdfChunks(oldCurrentChar, newCurrentChar), isRTL);
    }

    private float processTabStop(TabStop tabStop, float tabPosition, float originalWidth, float width, float tabStopAnchorPosition, boolean isRTL, List<TabStop> rtlTabsToBeAligned) {
        float tabStopPosition = tabStop.getPosition(tabPosition, originalWidth - width, tabStopAnchorPosition);
        width -= tabStopPosition - tabPosition;
        if (width < 0.0F) {
            tabStopPosition += width;
            width = 0.0F;
        }
        if (!isRTL) {
            tabStop.setPosition(tabStopPosition);
        } else {
            tabStop.setPosition(tabPosition);
            rtlTabsToBeAligned.add(tabStop);
        }
        return width;
    }

    public boolean isWordSplit() {
        return this.isWordSplit;
    }

    public float getWidth(int startIdx, int lastIdx) {
        return getWidth(startIdx, lastIdx, 0.0F);
    }

    public float getWidth(int startIdx, int lastIdx, float originalWidth) {
        char c = Character.MIN_VALUE;
        PdfChunk ck = null;
        float width = 0.0F;
        TabStop tabStop = null;
        float tabStopAnchorPosition = Float.NaN;
        float tabPosition = Float.NaN;
        for (; startIdx <= lastIdx; startIdx++) {
            boolean surrogate = Utilities.isSurrogatePair(this.text, startIdx);
            if (this.detailChunks[startIdx].isTab() && this.detailChunks[startIdx]
                    .isAttribute("TABSETTINGS")) {
                if (tabStop != null) {
                    float tabStopPosition = tabStop.getPosition(tabPosition, width, tabStopAnchorPosition);
                    width = tabStopPosition + width - tabPosition;
                    tabStop.setPosition(tabStopPosition);
                }
                tabStop = this.detailChunks[startIdx].getTabStop();
                if (tabStop == null) {
                    tabStop = PdfChunk.getTabStop(this.detailChunks[startIdx], width);
                    tabPosition = width;
                    tabStopAnchorPosition = Float.NaN;
                } else {
                    if (this.runDirection == 3) {
                        width = originalWidth - tabStop.getPosition();
                    } else {
                        width = tabStop.getPosition();
                    }
                    tabStop = null;
                    tabPosition = Float.NaN;
                    tabStopAnchorPosition = Float.NaN;
                }
            } else if (surrogate) {
                width += this.detailChunks[startIdx].getCharWidth(Utilities.convertToUtf32(this.text, startIdx));
                startIdx++;
            } else {

                c = this.text[startIdx];
                ck = this.detailChunks[startIdx];
                if (!PdfChunk.noPrint(ck.getUnicodeEquivalent(c))) {

                    if (tabStop != null && tabStop.getAlignment() != TabStop.Alignment.ANCHOR && Float.isNaN(tabStopAnchorPosition) && tabStop.getAnchorChar() == (char) ck.getUnicodeEquivalent(c)) {
                        tabStopAnchorPosition = width;
                    }
                    width += this.detailChunks[startIdx].getCharWidth(c);
                }
            }
        }
        if (tabStop != null) {
            float tabStopPosition = tabStop.getPosition(tabPosition, width, tabStopAnchorPosition);
            width = tabStopPosition + width - tabPosition;
            tabStop.setPosition(tabStopPosition);
        }
        return width;
    }

    public ArrayList<PdfChunk> createArrayOfPdfChunks(int startIdx, int endIdx) {
        return createArrayOfPdfChunks(startIdx, endIdx, null);
    }

    public ArrayList<PdfChunk> createArrayOfPdfChunks(int startIdx, int endIdx, PdfChunk extraPdfChunk) {
        boolean bidi = (this.runDirection != 1);
        if (bidi) {
            reorder(startIdx, endIdx);
        }
        ArrayList<PdfChunk> ar = new ArrayList<PdfChunk>();
        PdfChunk refCk = this.detailChunks[startIdx];
        PdfChunk ck = null;
        StringBuffer buf = new StringBuffer();

        int idx = 0;
        for (; startIdx <= endIdx; startIdx++) {
            idx = bidi ? this.indexChars[startIdx] : startIdx;
            char c = this.text[idx];
            ck = this.detailChunks[idx];
            if (!PdfChunk.noPrint(ck.getUnicodeEquivalent(c))) {
                if (ck.isImage() || ck.isSeparator() || ck.isTab()) {
                    if (buf.length() > 0) {
                        ar.add(new PdfChunk(buf.toString(), refCk));
                        buf = new StringBuffer();
                    }
                    ar.add(ck);
                } else if (ck == refCk) {
                    buf.append(c);
                } else {

                    if (buf.length() > 0) {
                        ar.add(new PdfChunk(buf.toString(), refCk));
                        buf = new StringBuffer();
                    }
                    if (!ck.isImage() && !ck.isSeparator() && !ck.isTab()) {
                        buf.append(c);
                    }
                    refCk = ck;
                }
            }
        }
        if (buf.length() > 0) {
            ar.add(new PdfChunk(buf.toString(), refCk));
        }
        if (extraPdfChunk != null) {
            ar.add(extraPdfChunk);
        }
        return ar;
    }

    public int[] getWord(int startIdx, int idx) {
        int last = idx;
        int first = idx;

        for (; last < this.totalTextLength && (Character.isLetter(this.text[last]) || Character.isDigit(this.text[last]) || this.text[last] == '­'); last++);

        if (last == idx) {
            return null;
        }
        for (; first >= startIdx && (Character.isLetter(this.text[first]) || Character.isDigit(this.text[first]) || this.text[first] == '­'); first--);

        first++;
        return new int[]{first, last};
    }

    public int trimRight(int startIdx, int endIdx) {
        int idx = endIdx;

        for (; idx >= startIdx; idx--) {
            char c = (char) this.detailChunks[idx].getUnicodeEquivalent(this.text[idx]);
            if (!isWS(c)) {
                break;
            }
        }
        return idx;
    }

    public int trimLeft(int startIdx, int endIdx) {
        int idx = startIdx;

        for (; idx <= endIdx; idx++) {
            char c = (char) this.detailChunks[idx].getUnicodeEquivalent(this.text[idx]);
            if (!isWS(c)) {
                break;
            }
        }
        return idx;
    }

    public int trimRightEx(int startIdx, int endIdx) {
        int idx = endIdx;
        char c = Character.MIN_VALUE;
        for (; idx >= startIdx; idx--) {
            c = (char) this.detailChunks[idx].getUnicodeEquivalent(this.text[idx]);
            if (!isWS(c) && !PdfChunk.noPrint(c)) {
                if (this.detailChunks[idx].isTab() && this.detailChunks[idx]
                        .isAttribute("TABSETTINGS")) {
                    Object[] tab = (Object[]) this.detailChunks[idx].getAttribute("TAB");
                    boolean isWhitespace = ((Boolean) tab[1]).booleanValue();
                    if (isWhitespace) {
                        continue;
                    }
                }
                break;
            }
            continue;
        }
        return idx;
    }

    public int trimLeftEx(int startIdx, int endIdx) {
        int idx = startIdx;
        char c = Character.MIN_VALUE;
        for (; idx <= endIdx; idx++) {
            c = (char) this.detailChunks[idx].getUnicodeEquivalent(this.text[idx]);
            if (!isWS(c) && !PdfChunk.noPrint(c)) {
                if (this.detailChunks[idx].isTab() && this.detailChunks[idx]
                        .isAttribute("TABSETTINGS")) {
                    Object[] tab = (Object[]) this.detailChunks[idx].getAttribute("TAB");
                    boolean isWhitespace = ((Boolean) tab[1]).booleanValue();
                    if (isWhitespace) {
                        continue;
                    }
                }
                break;
            }
            continue;
        }
        return idx;
    }

    public void reorder(int start, int end) {
        byte maxLevel = this.orderLevels[start];
        byte minLevel = maxLevel;
        byte onlyOddLevels = maxLevel;
        byte onlyEvenLevels = maxLevel;
        for (int k = start + 1; k <= end; k++) {
            byte b = this.orderLevels[k];
            if (b > maxLevel) {
                maxLevel = b;
            } else if (b < minLevel) {
                minLevel = b;
            }
            onlyOddLevels = (byte) (onlyOddLevels & b);
            onlyEvenLevels = (byte) (onlyEvenLevels | b);
        }
        if ((onlyEvenLevels & 0x1) == 0) {
            return;
        }
        if ((onlyOddLevels & 0x1) == 1) {
            flip(start, end + 1);
            return;
        }
        minLevel = (byte) (minLevel | 0x1);
        while (maxLevel >= minLevel) {
            int pstart = start;
            while (true) {
                if (pstart > end
                        || this.orderLevels[pstart] >= maxLevel) {

                    if (pstart > end) {
                        maxLevel = (byte) (maxLevel - 1);
                        continue;
                    }
                    int pend = pstart + 1;
                    for (; pend <= end
                            && this.orderLevels[pend] >= maxLevel; pend++);

                    flip(pstart, pend);
                    pstart = pend + 1;
                    continue;
                }
                pstart++;
            }
        }
    }

    public void flip(int start, int end) {
        int mid = (start + end) / 2;
        end--;
        for (; start < mid; start++, end--) {
            int temp = this.indexChars[start];
            this.indexChars[start] = this.indexChars[end];
            this.indexChars[end] = temp;
        }
    }

    public static boolean isWS(char c) {
        return (c <= ' ');
    }

    static {
        mirrorChars.put(40, 41);
        mirrorChars.put(41, 40);
        mirrorChars.put(60, 62);
        mirrorChars.put(62, 60);
        mirrorChars.put(91, 93);
        mirrorChars.put(93, 91);
        mirrorChars.put(123, 125);
        mirrorChars.put(125, 123);
        mirrorChars.put(171, 187);
        mirrorChars.put(187, 171);
        mirrorChars.put(8249, 8250);
        mirrorChars.put(8250, 8249);
        mirrorChars.put(8261, 8262);
        mirrorChars.put(8262, 8261);
        mirrorChars.put(8317, 8318);
        mirrorChars.put(8318, 8317);
        mirrorChars.put(8333, 8334);
        mirrorChars.put(8334, 8333);
        mirrorChars.put(8712, 8715);
        mirrorChars.put(8713, 8716);
        mirrorChars.put(8714, 8717);
        mirrorChars.put(8715, 8712);
        mirrorChars.put(8716, 8713);
        mirrorChars.put(8717, 8714);
        mirrorChars.put(8725, 10741);
        mirrorChars.put(8764, 8765);
        mirrorChars.put(8765, 8764);
        mirrorChars.put(8771, 8909);
        mirrorChars.put(8786, 8787);
        mirrorChars.put(8787, 8786);
        mirrorChars.put(8788, 8789);
        mirrorChars.put(8789, 8788);
        mirrorChars.put(8804, 8805);
        mirrorChars.put(8805, 8804);
        mirrorChars.put(8806, 8807);
        mirrorChars.put(8807, 8806);
        mirrorChars.put(8808, 8809);
        mirrorChars.put(8809, 8808);
        mirrorChars.put(8810, 8811);
        mirrorChars.put(8811, 8810);
        mirrorChars.put(8814, 8815);
        mirrorChars.put(8815, 8814);
        mirrorChars.put(8816, 8817);
        mirrorChars.put(8817, 8816);
        mirrorChars.put(8818, 8819);
        mirrorChars.put(8819, 8818);
        mirrorChars.put(8820, 8821);
        mirrorChars.put(8821, 8820);
        mirrorChars.put(8822, 8823);
        mirrorChars.put(8823, 8822);
        mirrorChars.put(8824, 8825);
        mirrorChars.put(8825, 8824);
        mirrorChars.put(8826, 8827);
        mirrorChars.put(8827, 8826);
        mirrorChars.put(8828, 8829);
        mirrorChars.put(8829, 8828);
        mirrorChars.put(8830, 8831);
        mirrorChars.put(8831, 8830);
        mirrorChars.put(8832, 8833);
        mirrorChars.put(8833, 8832);
        mirrorChars.put(8834, 8835);
        mirrorChars.put(8835, 8834);
        mirrorChars.put(8836, 8837);
        mirrorChars.put(8837, 8836);
        mirrorChars.put(8838, 8839);
        mirrorChars.put(8839, 8838);
        mirrorChars.put(8840, 8841);
        mirrorChars.put(8841, 8840);
        mirrorChars.put(8842, 8843);
        mirrorChars.put(8843, 8842);
        mirrorChars.put(8847, 8848);
        mirrorChars.put(8848, 8847);
        mirrorChars.put(8849, 8850);
        mirrorChars.put(8850, 8849);
        mirrorChars.put(8856, 10680);
        mirrorChars.put(8866, 8867);
        mirrorChars.put(8867, 8866);
        mirrorChars.put(8870, 10974);
        mirrorChars.put(8872, 10980);
        mirrorChars.put(8873, 10979);
        mirrorChars.put(8875, 10981);
        mirrorChars.put(8880, 8881);
        mirrorChars.put(8881, 8880);
        mirrorChars.put(8882, 8883);
        mirrorChars.put(8883, 8882);
        mirrorChars.put(8884, 8885);
        mirrorChars.put(8885, 8884);
        mirrorChars.put(8886, 8887);
        mirrorChars.put(8887, 8886);
        mirrorChars.put(8905, 8906);
        mirrorChars.put(8906, 8905);
        mirrorChars.put(8907, 8908);
        mirrorChars.put(8908, 8907);
        mirrorChars.put(8909, 8771);
        mirrorChars.put(8912, 8913);
        mirrorChars.put(8913, 8912);
        mirrorChars.put(8918, 8919);
        mirrorChars.put(8919, 8918);
        mirrorChars.put(8920, 8921);
        mirrorChars.put(8921, 8920);
        mirrorChars.put(8922, 8923);
        mirrorChars.put(8923, 8922);
        mirrorChars.put(8924, 8925);
        mirrorChars.put(8925, 8924);
        mirrorChars.put(8926, 8927);
        mirrorChars.put(8927, 8926);
        mirrorChars.put(8928, 8929);
        mirrorChars.put(8929, 8928);
        mirrorChars.put(8930, 8931);
        mirrorChars.put(8931, 8930);
        mirrorChars.put(8932, 8933);
        mirrorChars.put(8933, 8932);
        mirrorChars.put(8934, 8935);
        mirrorChars.put(8935, 8934);
        mirrorChars.put(8936, 8937);
        mirrorChars.put(8937, 8936);
        mirrorChars.put(8938, 8939);
        mirrorChars.put(8939, 8938);
        mirrorChars.put(8940, 8941);
        mirrorChars.put(8941, 8940);
        mirrorChars.put(8944, 8945);
        mirrorChars.put(8945, 8944);
        mirrorChars.put(8946, 8954);
        mirrorChars.put(8947, 8955);
        mirrorChars.put(8948, 8956);
        mirrorChars.put(8950, 8957);
        mirrorChars.put(8951, 8958);
        mirrorChars.put(8954, 8946);
        mirrorChars.put(8955, 8947);
        mirrorChars.put(8956, 8948);
        mirrorChars.put(8957, 8950);
        mirrorChars.put(8958, 8951);
        mirrorChars.put(8968, 8969);
        mirrorChars.put(8969, 8968);
        mirrorChars.put(8970, 8971);
        mirrorChars.put(8971, 8970);
        mirrorChars.put(9001, 9002);
        mirrorChars.put(9002, 9001);
        mirrorChars.put(10088, 10089);
        mirrorChars.put(10089, 10088);
        mirrorChars.put(10090, 10091);
        mirrorChars.put(10091, 10090);
        mirrorChars.put(10092, 10093);
        mirrorChars.put(10093, 10092);
        mirrorChars.put(10094, 10095);
        mirrorChars.put(10095, 10094);
        mirrorChars.put(10096, 10097);
        mirrorChars.put(10097, 10096);
        mirrorChars.put(10098, 10099);
        mirrorChars.put(10099, 10098);
        mirrorChars.put(10100, 10101);
        mirrorChars.put(10101, 10100);
        mirrorChars.put(10197, 10198);
        mirrorChars.put(10198, 10197);
        mirrorChars.put(10205, 10206);
        mirrorChars.put(10206, 10205);
        mirrorChars.put(10210, 10211);
        mirrorChars.put(10211, 10210);
        mirrorChars.put(10212, 10213);
        mirrorChars.put(10213, 10212);
        mirrorChars.put(10214, 10215);
        mirrorChars.put(10215, 10214);
        mirrorChars.put(10216, 10217);
        mirrorChars.put(10217, 10216);
        mirrorChars.put(10218, 10219);
        mirrorChars.put(10219, 10218);
        mirrorChars.put(10627, 10628);
        mirrorChars.put(10628, 10627);
        mirrorChars.put(10629, 10630);
        mirrorChars.put(10630, 10629);
        mirrorChars.put(10631, 10632);
        mirrorChars.put(10632, 10631);
        mirrorChars.put(10633, 10634);
        mirrorChars.put(10634, 10633);
        mirrorChars.put(10635, 10636);
        mirrorChars.put(10636, 10635);
        mirrorChars.put(10637, 10640);
        mirrorChars.put(10638, 10639);
        mirrorChars.put(10639, 10638);
        mirrorChars.put(10640, 10637);
        mirrorChars.put(10641, 10642);
        mirrorChars.put(10642, 10641);
        mirrorChars.put(10643, 10644);
        mirrorChars.put(10644, 10643);
        mirrorChars.put(10645, 10646);
        mirrorChars.put(10646, 10645);
        mirrorChars.put(10647, 10648);
        mirrorChars.put(10648, 10647);
        mirrorChars.put(10680, 8856);
        mirrorChars.put(10688, 10689);
        mirrorChars.put(10689, 10688);
        mirrorChars.put(10692, 10693);
        mirrorChars.put(10693, 10692);
        mirrorChars.put(10703, 10704);
        mirrorChars.put(10704, 10703);
        mirrorChars.put(10705, 10706);
        mirrorChars.put(10706, 10705);
        mirrorChars.put(10708, 10709);
        mirrorChars.put(10709, 10708);
        mirrorChars.put(10712, 10713);
        mirrorChars.put(10713, 10712);
        mirrorChars.put(10714, 10715);
        mirrorChars.put(10715, 10714);
        mirrorChars.put(10741, 8725);
        mirrorChars.put(10744, 10745);
        mirrorChars.put(10745, 10744);
        mirrorChars.put(10748, 10749);
        mirrorChars.put(10749, 10748);
        mirrorChars.put(10795, 10796);
        mirrorChars.put(10796, 10795);
        mirrorChars.put(10797, 10796);
        mirrorChars.put(10798, 10797);
        mirrorChars.put(10804, 10805);
        mirrorChars.put(10805, 10804);
        mirrorChars.put(10812, 10813);
        mirrorChars.put(10813, 10812);
        mirrorChars.put(10852, 10853);
        mirrorChars.put(10853, 10852);
        mirrorChars.put(10873, 10874);
        mirrorChars.put(10874, 10873);
        mirrorChars.put(10877, 10878);
        mirrorChars.put(10878, 10877);
        mirrorChars.put(10879, 10880);
        mirrorChars.put(10880, 10879);
        mirrorChars.put(10881, 10882);
        mirrorChars.put(10882, 10881);
        mirrorChars.put(10883, 10884);
        mirrorChars.put(10884, 10883);
        mirrorChars.put(10891, 10892);
        mirrorChars.put(10892, 10891);
        mirrorChars.put(10897, 10898);
        mirrorChars.put(10898, 10897);
        mirrorChars.put(10899, 10900);
        mirrorChars.put(10900, 10899);
        mirrorChars.put(10901, 10902);
        mirrorChars.put(10902, 10901);
        mirrorChars.put(10903, 10904);
        mirrorChars.put(10904, 10903);
        mirrorChars.put(10905, 10906);
        mirrorChars.put(10906, 10905);
        mirrorChars.put(10907, 10908);
        mirrorChars.put(10908, 10907);
        mirrorChars.put(10913, 10914);
        mirrorChars.put(10914, 10913);
        mirrorChars.put(10918, 10919);
        mirrorChars.put(10919, 10918);
        mirrorChars.put(10920, 10921);
        mirrorChars.put(10921, 10920);
        mirrorChars.put(10922, 10923);
        mirrorChars.put(10923, 10922);
        mirrorChars.put(10924, 10925);
        mirrorChars.put(10925, 10924);
        mirrorChars.put(10927, 10928);
        mirrorChars.put(10928, 10927);
        mirrorChars.put(10931, 10932);
        mirrorChars.put(10932, 10931);
        mirrorChars.put(10939, 10940);
        mirrorChars.put(10940, 10939);
        mirrorChars.put(10941, 10942);
        mirrorChars.put(10942, 10941);
        mirrorChars.put(10943, 10944);
        mirrorChars.put(10944, 10943);
        mirrorChars.put(10945, 10946);
        mirrorChars.put(10946, 10945);
        mirrorChars.put(10947, 10948);
        mirrorChars.put(10948, 10947);
        mirrorChars.put(10949, 10950);
        mirrorChars.put(10950, 10949);
        mirrorChars.put(10957, 10958);
        mirrorChars.put(10958, 10957);
        mirrorChars.put(10959, 10960);
        mirrorChars.put(10960, 10959);
        mirrorChars.put(10961, 10962);
        mirrorChars.put(10962, 10961);
        mirrorChars.put(10963, 10964);
        mirrorChars.put(10964, 10963);
        mirrorChars.put(10965, 10966);
        mirrorChars.put(10966, 10965);
        mirrorChars.put(10974, 8870);
        mirrorChars.put(10979, 8873);
        mirrorChars.put(10980, 8872);
        mirrorChars.put(10981, 8875);
        mirrorChars.put(10988, 10989);
        mirrorChars.put(10989, 10988);
        mirrorChars.put(10999, 11000);
        mirrorChars.put(11000, 10999);
        mirrorChars.put(11001, 11002);
        mirrorChars.put(11002, 11001);
        mirrorChars.put(12296, 12297);
        mirrorChars.put(12297, 12296);
        mirrorChars.put(12298, 12299);
        mirrorChars.put(12299, 12298);
        mirrorChars.put(12300, 12301);
        mirrorChars.put(12301, 12300);
        mirrorChars.put(12302, 12303);
        mirrorChars.put(12303, 12302);
        mirrorChars.put(12304, 12305);
        mirrorChars.put(12305, 12304);
        mirrorChars.put(12308, 12309);
        mirrorChars.put(12309, 12308);
        mirrorChars.put(12310, 12311);
        mirrorChars.put(12311, 12310);
        mirrorChars.put(12312, 12313);
        mirrorChars.put(12313, 12312);
        mirrorChars.put(12314, 12315);
        mirrorChars.put(12315, 12314);
        mirrorChars.put(65288, 65289);
        mirrorChars.put(65289, 65288);
        mirrorChars.put(65308, 65310);
        mirrorChars.put(65310, 65308);
        mirrorChars.put(65339, 65341);
        mirrorChars.put(65341, 65339);
        mirrorChars.put(65371, 65373);
        mirrorChars.put(65373, 65371);
        mirrorChars.put(65375, 65376);
        mirrorChars.put(65376, 65375);
        mirrorChars.put(65378, 65379);
        mirrorChars.put(65379, 65378);
    }

    public static String processLTR(String s, int runDirection, int arabicOptions) {
        BidiLine bidi = new BidiLine();
        bidi.addChunk(new PdfChunk(new Chunk(s), null));
        bidi.arabicOptions = arabicOptions;
        bidi.getParagraph(runDirection);
        ArrayList<PdfChunk> arr = bidi.createArrayOfPdfChunks(0, bidi.totalTextLength - 1);
        StringBuilder sb = new StringBuilder();
        for (PdfChunk ck : arr) {
            sb.append(ck.toString());
        }
        return sb.toString();
    }

    public BidiLine() {
    }
}
