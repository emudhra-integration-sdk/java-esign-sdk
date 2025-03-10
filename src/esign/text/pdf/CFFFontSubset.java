package esign.text.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class CFFFontSubset
        extends CFFFont {

    static final String[] SubrsFunctions = new String[]{"RESERVED_0", "hstem", "RESERVED_2", "vstem", "vmoveto", "rlineto", "hlineto", "vlineto", "rrcurveto", "RESERVED_9", "callsubr", "return", "escape", "RESERVED_13", "endchar", "RESERVED_15", "RESERVED_16", "RESERVED_17", "hstemhm", "hintmask", "cntrmask", "rmoveto", "hmoveto", "vstemhm", "rcurveline", "rlinecurve", "vvcurveto", "hhcurveto", "shortint", "callgsubr", "vhcurveto", "hvcurveto"};

    static final String[] SubrsEscapeFuncs = new String[]{"RESERVED_0", "RESERVED_1", "RESERVED_2", "and", "or", "not", "RESERVED_6", "RESERVED_7", "RESERVED_8", "abs", "add", "sub", "div", "RESERVED_13", "neg", "eq", "RESERVED_16", "RESERVED_17", "drop", "RESERVED_19", "put", "get", "ifelse", "random", "mul", "RESERVED_25", "sqrt", "dup", "exch", "index", "roll", "RESERVED_31", "RESERVED_32", "RESERVED_33", "hflex", "flex", "hflex1", "flex1", "RESERVED_REST"};

    static final byte ENDCHAR_OP = 14;

    static final byte RETURN_OP = 11;

    HashMap<Integer, int[]> GlyphsUsed;

    ArrayList<Integer> glyphsInList;

    HashSet<Integer> FDArrayUsed = new HashSet<Integer>();

    HashMap<Integer, int[]>[] hSubrsUsed;

    ArrayList<Integer>[] lSubrsUsed;

    HashMap<Integer, int[]> hGSubrsUsed = (HashMap) new HashMap<Integer, int[]>();

    ArrayList<Integer> lGSubrsUsed = new ArrayList<Integer>();

    HashMap<Integer, int[]> hSubrsUsedNonCID = (HashMap) new HashMap<Integer, int[]>();

    ArrayList<Integer> lSubrsUsedNonCID = new ArrayList<Integer>();

    byte[][] NewLSubrsIndex;

    byte[] NewSubrsIndexNonCID;

    byte[] NewGSubrsIndex;

    byte[] NewCharStringsIndex;

    int GBias = 0;

    LinkedList<CFFFont.Item> OutputList;

    int NumOfHints = 0;

    public CFFFontSubset(RandomAccessFileOrArray rf, HashMap<Integer, int[]> GlyphsUsed) {
        super(rf);
        this.GlyphsUsed = GlyphsUsed;

        this.glyphsInList = new ArrayList<Integer>(GlyphsUsed.keySet());

        for (int i = 0; i < this.fonts.length; i++) {

            seek((this.fonts[i]).charstringsOffset);
            (this.fonts[i]).nglyphs = getCard16();

            seek(this.stringIndexOffset);
            (this.fonts[i]).nstrings = getCard16() + standardStrings.length;

            (this.fonts[i]).charstringsOffsets = getIndex((this.fonts[i]).charstringsOffset);

            if ((this.fonts[i]).fdselectOffset >= 0) {

                readFDSelect(i);

                BuildFDArrayUsed(i);
            }
            if ((this.fonts[i]).isCID) {
                ReadFDArray(i);
            }
            (this.fonts[i]).CharsetLength = CountCharset((this.fonts[i]).charsetOffset, (this.fonts[i]).nglyphs);
        }
    }

    int CountCharset(int Offset, int NumofGlyphs) {
        int Length = 0;
        seek(Offset);

        int format = getCard8();

        switch (format) {
            case 0:
                Length = 1 + 2 * NumofGlyphs;
                break;
            case 1:
                Length = 1 + 3 * CountRange(NumofGlyphs, 1);
                break;
            case 2:
                Length = 1 + 4 * CountRange(NumofGlyphs, 2);
                break;
        }

        return Length;
    }

    int CountRange(int NumofGlyphs, int Type) {
        int num = 0;

        int i = 1;
        while (i < NumofGlyphs) {
            int nLeft;
            num++;
            char Sid = getCard16();
            if (Type == 1) {
                nLeft = getCard8();
            } else {
                nLeft = getCard16();
            }
            i += nLeft + 1;
        }
        return num;
    }

    protected void readFDSelect(int Font) {
        int i, nRanges, l, first, j, NumOfGlyphs = (this.fonts[Font]).nglyphs;
        int[] FDSelect = new int[NumOfGlyphs];

        seek((this.fonts[Font]).fdselectOffset);

        (this.fonts[Font]).FDSelectFormat = getCard8();

        switch ((this.fonts[Font]).FDSelectFormat) {

            case 0:
                for (i = 0; i < NumOfGlyphs; i++) {
                    FDSelect[i] = getCard8();
                }

                (this.fonts[Font]).FDSelectLength = (this.fonts[Font]).nglyphs + 1;
                break;

            case 3:
                nRanges = getCard16();
                l = 0;

                first = getCard16();
                for (j = 0; j < nRanges; j++) {

                    int fd = getCard8();

                    int last = getCard16();

                    int steps = last - first;
                    for (int k = 0; k < steps; k++) {

                        FDSelect[l] = fd;
                        l++;
                    }

                    first = last;
                }

                (this.fonts[Font]).FDSelectLength = 3 + nRanges * 3 + 2;
                break;
        }

        (this.fonts[Font]).FDSelect = FDSelect;
    }

    protected void BuildFDArrayUsed(int Font) {
        int[] FDSelect = (this.fonts[Font]).FDSelect;

        for (int i = 0; i < this.glyphsInList.size(); i++) {

            int glyph = ((Integer) this.glyphsInList.get(i)).intValue();

            int FD = FDSelect[glyph];

            this.FDArrayUsed.add(Integer.valueOf(FD));
        }
    }

    protected void ReadFDArray(int Font) {
        seek((this.fonts[Font]).fdarrayOffset);
        (this.fonts[Font]).FDArrayCount = getCard16();
        (this.fonts[Font]).FDArrayOffsize = getCard8();

        if ((this.fonts[Font]).FDArrayOffsize < 4) {
            (this.fonts[Font]).FDArrayOffsize++;
        }
        (this.fonts[Font]).FDArrayOffsets = getIndex((this.fonts[Font]).fdarrayOffset);
    }

    public byte[] Process(String fontName) throws IOException {
        try {
            this.buf.reOpen();

            int j;
            for (j = 0; j < this.fonts.length
                    && !fontName.equals((this.fonts[j]).name); j++);
            if (j == this.fonts.length) {
                return null;
            }

            if (this.gsubrIndexOffset >= 0) {
                this.GBias = CalcBias(this.gsubrIndexOffset, j);
            }

            BuildNewCharString(j);

            BuildNewLGSubrs(j);

            byte[] Ret = BuildNewFile(j);
            return Ret;
        } finally {

            try {
                this.buf.close();
            } catch (Exception exception) {
            }
        }
    }

    protected int CalcBias(int Offset, int Font) {
        seek(Offset);
        int nSubrs = getCard16();

        if ((this.fonts[Font]).CharstringType == 1) {
            return 0;
        }
        if (nSubrs < 1240) {
            return 107;
        }
        if (nSubrs < 33900) {
            return 1131;
        }
        return 32768;
    }

    protected void BuildNewCharString(int FontIndex) throws IOException {
        this.NewCharStringsIndex = BuildNewIndex((this.fonts[FontIndex]).charstringsOffsets, this.GlyphsUsed, (byte) 14);
    }

    protected void BuildNewLGSubrs(int Font) throws IOException {
        if ((this.fonts[Font]).isCID) {

            this.hSubrsUsed = (HashMap<Integer, int[]>[]) new HashMap[(this.fonts[Font]).fdprivateOffsets.length];
            this.lSubrsUsed = (ArrayList<Integer>[]) new ArrayList[(this.fonts[Font]).fdprivateOffsets.length];

            this.NewLSubrsIndex = new byte[(this.fonts[Font]).fdprivateOffsets.length][];

            (this.fonts[Font]).PrivateSubrsOffset = new int[(this.fonts[Font]).fdprivateOffsets.length];

            (this.fonts[Font]).PrivateSubrsOffsetsArray = new int[(this.fonts[Font]).fdprivateOffsets.length][];

            ArrayList<Integer> FDInList = new ArrayList<Integer>(this.FDArrayUsed);

            for (int j = 0; j < FDInList.size(); j++) {

                int FD = ((Integer) FDInList.get(j)).intValue();
                this.hSubrsUsed[FD] = (HashMap) new HashMap<Integer, int[]>();
                this.lSubrsUsed[FD] = new ArrayList<Integer>();

                BuildFDSubrsOffsets(Font, FD);

                if ((this.fonts[Font]).PrivateSubrsOffset[FD] >= 0) {

                    BuildSubrUsed(Font, FD, (this.fonts[Font]).PrivateSubrsOffset[FD], (this.fonts[Font]).PrivateSubrsOffsetsArray[FD], this.hSubrsUsed[FD], this.lSubrsUsed[FD]);

                    this.NewLSubrsIndex[FD] = BuildNewIndex((this.fonts[Font]).PrivateSubrsOffsetsArray[FD], this.hSubrsUsed[FD], (byte) 11);
                }

            }

        } else if ((this.fonts[Font]).privateSubrs >= 0) {

            (this.fonts[Font]).SubrsOffsets = getIndex((this.fonts[Font]).privateSubrs);

            BuildSubrUsed(Font, -1, (this.fonts[Font]).privateSubrs, (this.fonts[Font]).SubrsOffsets, this.hSubrsUsedNonCID, this.lSubrsUsedNonCID);
        }

        BuildGSubrsUsed(Font);
        if ((this.fonts[Font]).privateSubrs >= 0) {
            this.NewSubrsIndexNonCID = BuildNewIndex((this.fonts[Font]).SubrsOffsets, this.hSubrsUsedNonCID, (byte) 11);
        }
        this.NewGSubrsIndex = BuildNewIndex(this.gsubrOffsets, this.hGSubrsUsed, (byte) 11);
    }

    protected void BuildFDSubrsOffsets(int Font, int FD) {
        (this.fonts[Font]).PrivateSubrsOffset[FD] = -1;

        seek((this.fonts[Font]).fdprivateOffsets[FD]);

        while (getPosition() < (this.fonts[Font]).fdprivateOffsets[FD] + (this.fonts[Font]).fdprivateLengths[FD]) {

            getDictItem();

            if (this.key == "Subrs") {
                (this.fonts[Font]).PrivateSubrsOffset[FD] = ((Integer) this.args[0]).intValue() + (this.fonts[Font]).fdprivateOffsets[FD];
            }
        }
        if ((this.fonts[Font]).PrivateSubrsOffset[FD] >= 0) {
            (this.fonts[Font]).PrivateSubrsOffsetsArray[FD] = getIndex((this.fonts[Font]).PrivateSubrsOffset[FD]);
        }
    }

    protected void BuildSubrUsed(int Font, int FD, int SubrOffset, int[] SubrsOffsets, HashMap<Integer, int[]> hSubr, ArrayList<Integer> lSubr) {
        int LBias = CalcBias(SubrOffset, Font);

        int i;
        for (i = 0; i < this.glyphsInList.size(); i++) {

            int glyph = ((Integer) this.glyphsInList.get(i)).intValue();
            int Start = (this.fonts[Font]).charstringsOffsets[glyph];
            int End = (this.fonts[Font]).charstringsOffsets[glyph + 1];

            if (FD >= 0) {

                EmptyStack();
                this.NumOfHints = 0;

                int GlyphFD = (this.fonts[Font]).FDSelect[glyph];

                if (GlyphFD == FD) {
                    ReadASubr(Start, End, this.GBias, LBias, hSubr, lSubr, SubrsOffsets);
                }
            } else {

                ReadASubr(Start, End, this.GBias, LBias, hSubr, lSubr, SubrsOffsets);
            }
        }
        for (i = 0; i < lSubr.size(); i++) {

            int Subr = ((Integer) lSubr.get(i)).intValue();

            if (Subr < SubrsOffsets.length - 1 && Subr >= 0) {

                int Start = SubrsOffsets[Subr];
                int End = SubrsOffsets[Subr + 1];
                ReadASubr(Start, End, this.GBias, LBias, hSubr, lSubr, SubrsOffsets);
            }
        }
    }

    protected void BuildGSubrsUsed(int Font) {
        int LBias = 0;
        int SizeOfNonCIDSubrsUsed = 0;
        if ((this.fonts[Font]).privateSubrs >= 0) {

            LBias = CalcBias((this.fonts[Font]).privateSubrs, Font);
            SizeOfNonCIDSubrsUsed = this.lSubrsUsedNonCID.size();
        }

        for (int i = 0; i < this.lGSubrsUsed.size(); i++) {

            int Subr = ((Integer) this.lGSubrsUsed.get(i)).intValue();
            if (Subr < this.gsubrOffsets.length - 1 && Subr >= 0) {

                int Start = this.gsubrOffsets[Subr];
                int End = this.gsubrOffsets[Subr + 1];

                if ((this.fonts[Font]).isCID) {
                    ReadASubr(Start, End, this.GBias, 0, this.hGSubrsUsed, this.lGSubrsUsed, (int[]) null);
                } else {

                    ReadASubr(Start, End, this.GBias, LBias, this.hSubrsUsedNonCID, this.lSubrsUsedNonCID, (this.fonts[Font]).SubrsOffsets);
                    if (SizeOfNonCIDSubrsUsed < this.lSubrsUsedNonCID.size()) {

                        for (int j = SizeOfNonCIDSubrsUsed; j < this.lSubrsUsedNonCID.size(); j++) {

                            int LSubr = ((Integer) this.lSubrsUsedNonCID.get(j)).intValue();
                            if (LSubr < (this.fonts[Font]).SubrsOffsets.length - 1 && LSubr >= 0) {

                                int LStart = (this.fonts[Font]).SubrsOffsets[LSubr];
                                int LEnd = (this.fonts[Font]).SubrsOffsets[LSubr + 1];
                                ReadASubr(LStart, LEnd, this.GBias, LBias, this.hSubrsUsedNonCID, this.lSubrsUsedNonCID, (this.fonts[Font]).SubrsOffsets);
                            }
                        }
                        SizeOfNonCIDSubrsUsed = this.lSubrsUsedNonCID.size();
                    }
                }
            }
        }
    }

    protected void ReadASubr(int begin, int end, int GBias, int LBias, HashMap<Integer, int[]> hSubr, ArrayList<Integer> lSubr, int[] LSubrsOffsets) {
        EmptyStack();
        this.NumOfHints = 0;

        seek(begin);
        while (getPosition() < end) {

            ReadCommand();
            int pos = getPosition();
            Object TopElement = null;
            if (this.arg_count > 0) {
                TopElement = this.args[this.arg_count - 1];
            }
            int NumOfArgs = this.arg_count;

            HandelStack();

            if (this.key == "callsubr") {

                if (NumOfArgs > 0) {

                    int Subr = ((Integer) TopElement).intValue() + LBias;

                    if (!hSubr.containsKey(Integer.valueOf(Subr))) {

                        hSubr.put(Integer.valueOf(Subr), null);
                        lSubr.add(Integer.valueOf(Subr));
                    }
                    CalcHints(LSubrsOffsets[Subr], LSubrsOffsets[Subr + 1], LBias, GBias, LSubrsOffsets);
                    seek(pos);
                }
                continue;
            }
            if (this.key == "callgsubr") {

                if (NumOfArgs > 0) {

                    int Subr = ((Integer) TopElement).intValue() + GBias;

                    if (!this.hGSubrsUsed.containsKey(Integer.valueOf(Subr))) {

                        this.hGSubrsUsed.put(Integer.valueOf(Subr), null);
                        this.lGSubrsUsed.add(Integer.valueOf(Subr));
                    }
                    CalcHints(this.gsubrOffsets[Subr], this.gsubrOffsets[Subr + 1], LBias, GBias, LSubrsOffsets);
                    seek(pos);
                }
                continue;
            }
            if (this.key == "hstem" || this.key == "vstem" || this.key == "hstemhm" || this.key == "vstemhm") {

                this.NumOfHints += NumOfArgs / 2;
                continue;
            }
            if (this.key == "hintmask" || this.key == "cntrmask") {

                this.NumOfHints += NumOfArgs / 2;

                int SizeOfMask = this.NumOfHints / 8;
                if (this.NumOfHints % 8 != 0 || SizeOfMask == 0) {
                    SizeOfMask++;
                }
                for (int i = 0; i < SizeOfMask; i++) {
                    getCard8();
                }
            }
        }
    }

    protected void HandelStack() {
        int StackHandel = StackOpp();
        if (StackHandel < 2) {

            if (StackHandel == 1) {
                PushStack();

            } else {

                StackHandel *= -1;
                for (int i = 0; i < StackHandel; i++) {
                    PopStack();
                }
            }

        } else {

            EmptyStack();
        }
    }

    protected int StackOpp() {
        if (this.key == "ifelse") {
            return -3;
        }
        if (this.key == "roll" || this.key == "put") {
            return -2;
        }
        if (this.key == "callsubr" || this.key == "callgsubr" || this.key == "add" || this.key == "sub" || this.key == "div" || this.key == "mul" || this.key == "drop" || this.key == "and" || this.key == "or" || this.key == "eq") {

            return -1;
        }
        if (this.key == "abs" || this.key == "neg" || this.key == "sqrt" || this.key == "exch" || this.key == "index" || this.key == "get" || this.key == "not" || this.key == "return") {
            return 0;
        }
        if (this.key == "random" || this.key == "dup") {
            return 1;
        }
        return 2;
    }

    protected void EmptyStack() {
        for (int i = 0; i < this.arg_count;) {
            this.args[i] = null;
            i++;
        }
        this.arg_count = 0;
    }

    protected void PopStack() {
        if (this.arg_count > 0) {

            this.args[this.arg_count - 1] = null;
            this.arg_count--;
        }
    }

    protected void PushStack() {
        this.arg_count++;
    }

    protected void ReadCommand() {
        this.key = null;
        boolean gotKey = false;

        while (!gotKey) {

            char b0 = getCard8();

            if (b0 == '\034') {

                int first = getCard8();
                int second = getCard8();
                this.args[this.arg_count] = Integer.valueOf(first << 8 | second);
                this.arg_count++;
                continue;
            }
            if (b0 >= ' ' && b0 <= 'ö') {

                this.args[this.arg_count] = Integer.valueOf(b0 - 139);
                this.arg_count++;
                continue;
            }
            if (b0 >= '÷' && b0 <= 'ú') {

                int w = getCard8();
                this.args[this.arg_count] = Integer.valueOf((b0 - 247) * 256 + w + 108);
                this.arg_count++;
                continue;
            }
            if (b0 >= 'û' && b0 <= 'þ') {

                int w = getCard8();
                this.args[this.arg_count] = Integer.valueOf(-(b0 - 251) * 256 - w - 108);
                this.arg_count++;
                continue;
            }
            if (b0 == 'ÿ') {

                int first = getCard8();
                int second = getCard8();
                int third = getCard8();
                int fourth = getCard8();
                this.args[this.arg_count] = Integer.valueOf(first << 24 | second << 16 | third << 8 | fourth);
                this.arg_count++;
                continue;
            }
            if (b0 <= '\037' && b0 != '\034') {

                gotKey = true;

                if (b0 == '\f') {

                    int b1 = getCard8();
                    if (b1 > SubrsEscapeFuncs.length - 1) {
                        b1 = SubrsEscapeFuncs.length - 1;
                    }
                    this.key = SubrsEscapeFuncs[b1];
                    continue;
                }
                this.key = SubrsFunctions[b0];
            }
        }
    }

    protected int CalcHints(int begin, int end, int LBias, int GBias, int[] LSubrsOffsets) {
        seek(begin);
        while (getPosition() < end) {

            ReadCommand();
            int pos = getPosition();
            Object TopElement = null;
            if (this.arg_count > 0) {
                TopElement = this.args[this.arg_count - 1];
            }
            int NumOfArgs = this.arg_count;

            HandelStack();

            if (this.key == "callsubr") {

                if (NumOfArgs > 0) {

                    int Subr = ((Integer) TopElement).intValue() + LBias;
                    CalcHints(LSubrsOffsets[Subr], LSubrsOffsets[Subr + 1], LBias, GBias, LSubrsOffsets);
                    seek(pos);
                }
                continue;
            }
            if (this.key == "callgsubr") {

                if (NumOfArgs > 0) {

                    int Subr = ((Integer) TopElement).intValue() + GBias;
                    CalcHints(this.gsubrOffsets[Subr], this.gsubrOffsets[Subr + 1], LBias, GBias, LSubrsOffsets);
                    seek(pos);
                }
                continue;
            }
            if (this.key == "hstem" || this.key == "vstem" || this.key == "hstemhm" || this.key == "vstemhm") {

                this.NumOfHints += NumOfArgs / 2;
                continue;
            }
            if (this.key == "hintmask" || this.key == "cntrmask") {

                int SizeOfMask = this.NumOfHints / 8;
                if (this.NumOfHints % 8 != 0 || SizeOfMask == 0) {
                    SizeOfMask++;
                }
                for (int i = 0; i < SizeOfMask; i++) {
                    getCard8();
                }
            }
        }
        return this.NumOfHints;
    }

    protected byte[] BuildNewIndex(int[] Offsets, HashMap<Integer, int[]> Used, byte OperatorForUnusedEntries) throws IOException {
        int unusedCount = 0;
        int Offset = 0;
        int[] NewOffsets = new int[Offsets.length];

        for (int i = 0; i < Offsets.length; i++) {

            NewOffsets[i] = Offset;

            if (Used.containsKey(Integer.valueOf(i))) {
                Offset += Offsets[i + 1] - Offsets[i];
            } else {

                unusedCount++;
            }
        }

        byte[] NewObjects = new byte[Offset + unusedCount];

        int unusedOffset = 0;
        for (int j = 0; j < Offsets.length - 1; j++) {

            int start = NewOffsets[j];
            int end = NewOffsets[j + 1];
            NewOffsets[j] = start + unusedOffset;

            if (start != end) {

                this.buf.seek(Offsets[j]);

                this.buf.readFully(NewObjects, start + unusedOffset, end - start);
            } else {
                NewObjects[start + unusedOffset] = OperatorForUnusedEntries;
                unusedOffset++;
            }
        }
        NewOffsets[Offsets.length - 1] = NewOffsets[Offsets.length - 1] + unusedOffset;

        return AssembleIndex(NewOffsets, NewObjects);
    }

    protected byte[] AssembleIndex(int[] NewOffsets, byte[] NewObjects) {
        byte Offsize;
        char Count = (char) (NewOffsets.length - 1);

        int Size = NewOffsets[NewOffsets.length - 1];

        if (Size <= 255) {
            Offsize = 1;
        } else if (Size <= 65535) {
            Offsize = 2;
        } else if (Size <= 16777215) {
            Offsize = 3;
        } else {
            Offsize = 4;
        }

        byte[] NewIndex = new byte[3 + Offsize * (Count + 1) + NewObjects.length];

        int Place = 0;

        NewIndex[Place++] = (byte) (Count >>> 8 & 0xFF);
        NewIndex[Place++] = (byte) (Count >>> 0 & 0xFF);

        NewIndex[Place++] = Offsize;

        for (int newOffset : NewOffsets) {

            int Num = newOffset - NewOffsets[0] + 1;

            switch (Offsize) {
                case 4:
                    NewIndex[Place++] = (byte) (Num >>> 24 & 0xFF);
                case 3:
                    NewIndex[Place++] = (byte) (Num >>> 16 & 0xFF);
                case 2:
                    NewIndex[Place++] = (byte) (Num >>> 8 & 0xFF);
                case 1:
                    NewIndex[Place++] = (byte) (Num >>> 0 & 0xFF);
                    break;
            }
        }
        for (byte newObject : NewObjects) {
            NewIndex[Place++] = newObject;
        }

        return NewIndex;
    }

    protected byte[] BuildNewFile(int Font) {
        this.OutputList = new LinkedList<CFFFont.Item>();

        CopyHeader();

        BuildIndexHeader(1, 1, 1);
        this.OutputList.addLast(new CFFFont.UInt8Item((char) (1 + (this.fonts[Font]).name.length())));
        this.OutputList.addLast(new CFFFont.StringItem((this.fonts[Font]).name));

        BuildIndexHeader(1, 2, 1);
        CFFFont.OffsetItem topdictIndex1Ref = new CFFFont.IndexOffsetItem(2);
        this.OutputList.addLast(topdictIndex1Ref);
        CFFFont.IndexBaseItem topdictBase = new CFFFont.IndexBaseItem();
        this.OutputList.addLast(topdictBase);

        CFFFont.OffsetItem charsetRef = new CFFFont.DictOffsetItem();
        CFFFont.OffsetItem charstringsRef = new CFFFont.DictOffsetItem();
        CFFFont.OffsetItem fdarrayRef = new CFFFont.DictOffsetItem();
        CFFFont.OffsetItem fdselectRef = new CFFFont.DictOffsetItem();
        CFFFont.OffsetItem privateRef = new CFFFont.DictOffsetItem();

        if (!(this.fonts[Font]).isCID) {

            this.OutputList.addLast(new CFFFont.DictNumberItem((this.fonts[Font]).nstrings));
            this.OutputList.addLast(new CFFFont.DictNumberItem((this.fonts[Font]).nstrings + 1));
            this.OutputList.addLast(new CFFFont.DictNumberItem(0));
            this.OutputList.addLast(new CFFFont.UInt8Item('\f'));
            this.OutputList.addLast(new CFFFont.UInt8Item('\036'));

            this.OutputList.addLast(new CFFFont.DictNumberItem((this.fonts[Font]).nglyphs));
            this.OutputList.addLast(new CFFFont.UInt8Item('\f'));
            this.OutputList.addLast(new CFFFont.UInt8Item('"'));
        }

        seek(this.topdictOffsets[Font]);

        while (getPosition() < this.topdictOffsets[Font + 1]) {
            int p1 = getPosition();
            getDictItem();
            int p2 = getPosition();

            if (this.key == "Encoding" || this.key == "Private" || this.key == "FDSelect" || this.key == "FDArray" || this.key == "charset" || this.key == "CharStrings") {
                continue;
            }

            this.OutputList.add(new CFFFont.RangeItem(this.buf, p1, p2 - p1));
        }

        CreateKeys(fdarrayRef, fdselectRef, charsetRef, charstringsRef);

        this.OutputList.addLast(new CFFFont.IndexMarkerItem(topdictIndex1Ref, topdictBase));

        if ((this.fonts[Font]).isCID) {
            this.OutputList.addLast(getEntireIndexRange(this.stringIndexOffset));

        } else {

            CreateNewStringIndex(Font);
        }

        this.OutputList.addLast(new CFFFont.RangeItem(new RandomAccessFileOrArray(this.NewGSubrsIndex), 0, this.NewGSubrsIndex.length));

        if ((this.fonts[Font]).isCID) {

            this.OutputList.addLast(new CFFFont.MarkerItem(fdselectRef));

            if ((this.fonts[Font]).fdselectOffset >= 0) {
                this.OutputList.addLast(new CFFFont.RangeItem(this.buf, (this.fonts[Font]).fdselectOffset, (this.fonts[Font]).FDSelectLength));
            } else {

                CreateFDSelect(fdselectRef, (this.fonts[Font]).nglyphs);
            }

            this.OutputList.addLast(new CFFFont.MarkerItem(charsetRef));
            this.OutputList.addLast(new CFFFont.RangeItem(this.buf, (this.fonts[Font]).charsetOffset, (this.fonts[Font]).CharsetLength));

            if ((this.fonts[Font]).fdarrayOffset >= 0) {

                this.OutputList.addLast(new CFFFont.MarkerItem(fdarrayRef));

                Reconstruct(Font);
            } else {
                CreateFDArray(fdarrayRef, privateRef, Font);

            }

        } else {

            CreateFDSelect(fdselectRef, (this.fonts[Font]).nglyphs);

            CreateCharset(charsetRef, (this.fonts[Font]).nglyphs);

            CreateFDArray(fdarrayRef, privateRef, Font);
        }

        if ((this.fonts[Font]).privateOffset >= 0) {

            CFFFont.IndexBaseItem PrivateBase = new CFFFont.IndexBaseItem();
            this.OutputList.addLast(PrivateBase);
            this.OutputList.addLast(new CFFFont.MarkerItem(privateRef));

            CFFFont.OffsetItem Subr = new CFFFont.DictOffsetItem();

            CreateNonCIDPrivate(Font, Subr);

            CreateNonCIDSubrs(Font, PrivateBase, Subr);
        }

        this.OutputList.addLast(new CFFFont.MarkerItem(charstringsRef));

        this.OutputList.addLast(new CFFFont.RangeItem(new RandomAccessFileOrArray(this.NewCharStringsIndex), 0, this.NewCharStringsIndex.length));

        int[] currentOffset = new int[1];
        currentOffset[0] = 0;

        Iterator<CFFFont.Item> listIter = this.OutputList.iterator();
        while (listIter.hasNext()) {
            CFFFont.Item item = listIter.next();
            item.increment(currentOffset);
        }

        listIter = this.OutputList.iterator();
        while (listIter.hasNext()) {
            CFFFont.Item item = listIter.next();
            item.xref();
        }

        int size = currentOffset[0];
        byte[] b = new byte[size];

        listIter = this.OutputList.iterator();
        while (listIter.hasNext()) {
            CFFFont.Item item = listIter.next();
            item.emit(b);
        }

        return b;
    }

    protected void CopyHeader() {
        seek(0);
        int major = getCard8();
        int minor = getCard8();
        int hdrSize = getCard8();
        int offSize = getCard8();
        this.nextIndexOffset = hdrSize;
        this.OutputList.addLast(new CFFFont.RangeItem(this.buf, 0, hdrSize));
    }

    protected void BuildIndexHeader(int Count, int Offsize, int First) {
        this.OutputList.addLast(new CFFFont.UInt16Item((char) Count));

        this.OutputList.addLast(new CFFFont.UInt8Item((char) Offsize));

        switch (Offsize) {
            case 1:
                this.OutputList.addLast(new CFFFont.UInt8Item((char) First));
                break;
            case 2:
                this.OutputList.addLast(new CFFFont.UInt16Item((char) First));
                break;
            case 3:
                this.OutputList.addLast(new CFFFont.UInt24Item((char) First));
                break;
            case 4:
                this.OutputList.addLast(new CFFFont.UInt32Item((char) First));
                break;
        }
    }

    protected void CreateKeys(CFFFont.OffsetItem fdarrayRef, CFFFont.OffsetItem fdselectRef, CFFFont.OffsetItem charsetRef, CFFFont.OffsetItem charstringsRef) {
        this.OutputList.addLast(fdarrayRef);
        this.OutputList.addLast(new CFFFont.UInt8Item('\f'));
        this.OutputList.addLast(new CFFFont.UInt8Item('$'));

        this.OutputList.addLast(fdselectRef);
        this.OutputList.addLast(new CFFFont.UInt8Item('\f'));
        this.OutputList.addLast(new CFFFont.UInt8Item('%'));

        this.OutputList.addLast(charsetRef);
        this.OutputList.addLast(new CFFFont.UInt8Item('\017'));

        this.OutputList.addLast(charstringsRef);
        this.OutputList.addLast(new CFFFont.UInt8Item('\021'));
    }

    protected void CreateNewStringIndex(int Font) {
        byte stringsIndexOffSize;
        String fdFontName = (this.fonts[Font]).name + "-OneRange";
        if (fdFontName.length() > 127) {
            fdFontName = fdFontName.substring(0, 127);
        }
        String extraStrings = "AdobeIdentity" + fdFontName;

        int origStringsLen = this.stringOffsets[this.stringOffsets.length - 1] - this.stringOffsets[0];

        int stringsBaseOffset = this.stringOffsets[0] - 1;

        if (origStringsLen + extraStrings.length() <= 255) {
            stringsIndexOffSize = 1;
        } else if (origStringsLen + extraStrings.length() <= 65535) {
            stringsIndexOffSize = 2;
        } else if (origStringsLen + extraStrings.length() <= 16777215) {
            stringsIndexOffSize = 3;
        } else {
            stringsIndexOffSize = 4;
        }

        this.OutputList.addLast(new CFFFont.UInt16Item((char) (this.stringOffsets.length - 1 + 3)));
        this.OutputList.addLast(new CFFFont.UInt8Item((char) stringsIndexOffSize));
        for (int stringOffset : this.stringOffsets) {
            this.OutputList.addLast(new CFFFont.IndexOffsetItem(stringsIndexOffSize, stringOffset - stringsBaseOffset));
        }
        int currentStringsOffset = this.stringOffsets[this.stringOffsets.length - 1] - stringsBaseOffset;

        currentStringsOffset += "Adobe".length();
        this.OutputList.addLast(new CFFFont.IndexOffsetItem(stringsIndexOffSize, currentStringsOffset));
        currentStringsOffset += "Identity".length();
        this.OutputList.addLast(new CFFFont.IndexOffsetItem(stringsIndexOffSize, currentStringsOffset));
        currentStringsOffset += fdFontName.length();
        this.OutputList.addLast(new CFFFont.IndexOffsetItem(stringsIndexOffSize, currentStringsOffset));

        this.OutputList.addLast(new CFFFont.RangeItem(this.buf, this.stringOffsets[0], origStringsLen));
        this.OutputList.addLast(new CFFFont.StringItem(extraStrings));
    }

    protected void CreateFDSelect(CFFFont.OffsetItem fdselectRef, int nglyphs) {
        this.OutputList.addLast(new CFFFont.MarkerItem(fdselectRef));
        this.OutputList.addLast(new CFFFont.UInt8Item('\003'));
        this.OutputList.addLast(new CFFFont.UInt16Item('\001'));

        this.OutputList.addLast(new CFFFont.UInt16Item((char) 0));
        this.OutputList.addLast(new CFFFont.UInt8Item((char) 0));

        this.OutputList.addLast(new CFFFont.UInt16Item((char) nglyphs));
    }

    protected void CreateCharset(CFFFont.OffsetItem charsetRef, int nglyphs) {
        this.OutputList.addLast(new CFFFont.MarkerItem(charsetRef));
        this.OutputList.addLast(new CFFFont.UInt8Item('\002'));
        this.OutputList.addLast(new CFFFont.UInt16Item('\001'));
        this.OutputList.addLast(new CFFFont.UInt16Item((char) (nglyphs - 1)));
    }

    protected void CreateFDArray(CFFFont.OffsetItem fdarrayRef, CFFFont.OffsetItem privateRef, int Font) {
        this.OutputList.addLast(new CFFFont.MarkerItem(fdarrayRef));

        BuildIndexHeader(1, 1, 1);

        CFFFont.OffsetItem privateIndex1Ref = new CFFFont.IndexOffsetItem(1);
        this.OutputList.addLast(privateIndex1Ref);
        CFFFont.IndexBaseItem privateBase = new CFFFont.IndexBaseItem();

        this.OutputList.addLast(privateBase);

        int NewSize = (this.fonts[Font]).privateLength;

        int OrgSubrsOffsetSize = CalcSubrOffsetSize((this.fonts[Font]).privateOffset, (this.fonts[Font]).privateLength);

        if (OrgSubrsOffsetSize != 0) {
            NewSize += 5 - OrgSubrsOffsetSize;
        }
        this.OutputList.addLast(new CFFFont.DictNumberItem(NewSize));
        this.OutputList.addLast(privateRef);
        this.OutputList.addLast(new CFFFont.UInt8Item('\022'));

        this.OutputList.addLast(new CFFFont.IndexMarkerItem(privateIndex1Ref, privateBase));
    }

    void Reconstruct(int Font) {
        CFFFont.DictOffsetItem[] arrayOfDictOffsetItem1 = new CFFFont.DictOffsetItem[(this.fonts[Font]).FDArrayOffsets.length - 1];
        CFFFont.IndexBaseItem[] fdPrivateBase = new CFFFont.IndexBaseItem[(this.fonts[Font]).fdprivateOffsets.length];
        CFFFont.DictOffsetItem[] arrayOfDictOffsetItem2 = new CFFFont.DictOffsetItem[(this.fonts[Font]).fdprivateOffsets.length];

        ReconstructFDArray(Font, (CFFFont.OffsetItem[]) arrayOfDictOffsetItem1);
        ReconstructPrivateDict(Font, (CFFFont.OffsetItem[]) arrayOfDictOffsetItem1, fdPrivateBase, (CFFFont.OffsetItem[]) arrayOfDictOffsetItem2);
        ReconstructPrivateSubrs(Font, fdPrivateBase, (CFFFont.OffsetItem[]) arrayOfDictOffsetItem2);
    }

    void ReconstructFDArray(int Font, CFFFont.OffsetItem[] fdPrivate) {
        BuildIndexHeader((this.fonts[Font]).FDArrayCount, (this.fonts[Font]).FDArrayOffsize, 1);

        CFFFont.IndexOffsetItem[] arrayOfIndexOffsetItem = new CFFFont.IndexOffsetItem[(this.fonts[Font]).FDArrayOffsets.length - 1];
        for (int i = 0; i < (this.fonts[Font]).FDArrayOffsets.length - 1; i++) {

            arrayOfIndexOffsetItem[i] = new CFFFont.IndexOffsetItem((this.fonts[Font]).FDArrayOffsize);
            this.OutputList.addLast(arrayOfIndexOffsetItem[i]);
        }

        CFFFont.IndexBaseItem fdArrayBase = new CFFFont.IndexBaseItem();
        this.OutputList.addLast(fdArrayBase);

        for (int k = 0; k < (this.fonts[Font]).FDArrayOffsets.length - 1; k++) {

            seek((this.fonts[Font]).FDArrayOffsets[k]);
            while (getPosition() < (this.fonts[Font]).FDArrayOffsets[k + 1]) {

                int p1 = getPosition();
                getDictItem();
                int p2 = getPosition();

                if (this.key == "Private") {

                    int NewSize = ((Integer) this.args[0]).intValue();

                    int OrgSubrsOffsetSize = CalcSubrOffsetSize((this.fonts[Font]).fdprivateOffsets[k], (this.fonts[Font]).fdprivateLengths[k]);

                    if (OrgSubrsOffsetSize != 0) {
                        NewSize += 5 - OrgSubrsOffsetSize;
                    }
                    this.OutputList.addLast(new CFFFont.DictNumberItem(NewSize));
                    fdPrivate[k] = new CFFFont.DictOffsetItem();
                    this.OutputList.addLast(fdPrivate[k]);
                    this.OutputList.addLast(new CFFFont.UInt8Item('\022'));

                    seek(p2);

                    continue;
                }
                this.OutputList.addLast(new CFFFont.RangeItem(this.buf, p1, p2 - p1));
            }

            this.OutputList.addLast(new CFFFont.IndexMarkerItem(arrayOfIndexOffsetItem[k], fdArrayBase));
        }
    }

    void ReconstructPrivateDict(int Font, CFFFont.OffsetItem[] fdPrivate, CFFFont.IndexBaseItem[] fdPrivateBase, CFFFont.OffsetItem[] fdSubrs) {
        for (int i = 0; i < (this.fonts[Font]).fdprivateOffsets.length; i++) {

            this.OutputList.addLast(new CFFFont.MarkerItem(fdPrivate[i]));
            fdPrivateBase[i] = new CFFFont.IndexBaseItem();
            this.OutputList.addLast(fdPrivateBase[i]);

            seek((this.fonts[Font]).fdprivateOffsets[i]);
            while (getPosition() < (this.fonts[Font]).fdprivateOffsets[i] + (this.fonts[Font]).fdprivateLengths[i]) {

                int p1 = getPosition();
                getDictItem();
                int p2 = getPosition();

                if (this.key == "Subrs") {
                    fdSubrs[i] = new CFFFont.DictOffsetItem();
                    this.OutputList.addLast(fdSubrs[i]);
                    this.OutputList.addLast(new CFFFont.UInt8Item('\023'));

                    continue;
                }
                this.OutputList.addLast(new CFFFont.RangeItem(this.buf, p1, p2 - p1));
            }
        }
    }

    void ReconstructPrivateSubrs(int Font, CFFFont.IndexBaseItem[] fdPrivateBase, CFFFont.OffsetItem[] fdSubrs) {
        for (int i = 0; i < (this.fonts[Font]).fdprivateLengths.length; i++) {

            if (fdSubrs[i] != null && (this.fonts[Font]).PrivateSubrsOffset[i] >= 0) {

                this.OutputList.addLast(new CFFFont.SubrMarkerItem(fdSubrs[i], fdPrivateBase[i]));
                if (this.NewLSubrsIndex[i] != null) {
                    this.OutputList.addLast(new CFFFont.RangeItem(new RandomAccessFileOrArray(this.NewLSubrsIndex[i]), 0, (this.NewLSubrsIndex[i]).length));
                }
            }
        }
    }

    int CalcSubrOffsetSize(int Offset, int Size) {
        int OffsetSize = 0;

        seek(Offset);

        while (getPosition() < Offset + Size) {

            int p1 = getPosition();
            getDictItem();
            int p2 = getPosition();

            if (this.key == "Subrs") {
                OffsetSize = p2 - p1 - 1;
            }
        }

        return OffsetSize;
    }

    protected int countEntireIndexRange(int indexOffset) {
        seek(indexOffset);

        int count = getCard16();

        if (count == 0) {
            return 2;
        }

        int indexOffSize = getCard8();

        seek(indexOffset + 2 + 1 + count * indexOffSize);

        int size = getOffset(indexOffSize) - 1;

        return 3 + (count + 1) * indexOffSize + size;
    }

    void CreateNonCIDPrivate(int Font, CFFFont.OffsetItem Subr) {
        seek((this.fonts[Font]).privateOffset);
        while (getPosition() < (this.fonts[Font]).privateOffset + (this.fonts[Font]).privateLength) {

            int p1 = getPosition();
            getDictItem();
            int p2 = getPosition();

            if (this.key == "Subrs") {
                this.OutputList.addLast(Subr);
                this.OutputList.addLast(new CFFFont.UInt8Item('\023'));

                continue;
            }
            this.OutputList.addLast(new CFFFont.RangeItem(this.buf, p1, p2 - p1));
        }
    }

    void CreateNonCIDSubrs(int Font, CFFFont.IndexBaseItem PrivateBase, CFFFont.OffsetItem Subrs) {
        this.OutputList.addLast(new CFFFont.SubrMarkerItem(Subrs, PrivateBase));

        if (this.NewSubrsIndexNonCID != null) {
            this.OutputList.addLast(new CFFFont.RangeItem(new RandomAccessFileOrArray(this.NewSubrsIndexNonCID), 0, this.NewSubrsIndexNonCID.length));
        }
    }
}
