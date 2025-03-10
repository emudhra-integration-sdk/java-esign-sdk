package esign.text.pdf;

import esign.text.ExceptionConverter;
import java.util.Iterator;
import java.util.LinkedList;

public class CFFFont {

    static final String[] operatorNames = new String[]{"version", "Notice", "FullName", "FamilyName", "Weight", "FontBBox", "BlueValues", "OtherBlues", "FamilyBlues", "FamilyOtherBlues", "StdHW", "StdVW", "UNKNOWN_12", "UniqueID", "XUID", "charset", "Encoding", "CharStrings", "Private", "Subrs", "defaultWidthX", "nominalWidthX", "UNKNOWN_22", "UNKNOWN_23", "UNKNOWN_24", "UNKNOWN_25", "UNKNOWN_26", "UNKNOWN_27", "UNKNOWN_28", "UNKNOWN_29", "UNKNOWN_30", "UNKNOWN_31", "Copyright", "isFixedPitch", "ItalicAngle", "UnderlinePosition", "UnderlineThickness", "PaintType", "CharstringType", "FontMatrix", "StrokeWidth", "BlueScale", "BlueShift", "BlueFuzz", "StemSnapH", "StemSnapV", "ForceBold", "UNKNOWN_12_15", "UNKNOWN_12_16", "LanguageGroup", "ExpansionFactor", "initialRandomSeed", "SyntheticBase", "PostScript", "BaseFontName", "BaseFontBlend", "UNKNOWN_12_24", "UNKNOWN_12_25", "UNKNOWN_12_26", "UNKNOWN_12_27", "UNKNOWN_12_28", "UNKNOWN_12_29", "ROS", "CIDFontVersion", "CIDFontRevision", "CIDFontType", "CIDCount", "UIDBase", "FDArray", "FDSelect", "FontName"};

    static final String[] standardStrings = new String[]{".notdef", "space", "exclam", "quotedbl", "numbersign", "dollar", "percent", "ampersand", "quoteright", "parenleft", "parenright", "asterisk", "plus", "comma", "hyphen", "period", "slash", "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "colon", "semicolon", "less", "equal", "greater", "question", "at", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "bracketleft", "backslash", "bracketright", "asciicircum", "underscore", "quoteleft", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "braceleft", "bar", "braceright", "asciitilde", "exclamdown", "cent", "sterling", "fraction", "yen", "florin", "section", "currency", "quotesingle", "quotedblleft", "guillemotleft", "guilsinglleft", "guilsinglright", "fi", "fl", "endash", "dagger", "daggerdbl", "periodcentered", "paragraph", "bullet", "quotesinglbase", "quotedblbase", "quotedblright", "guillemotright", "ellipsis", "perthousand", "questiondown", "grave", "acute", "circumflex", "tilde", "macron", "breve", "dotaccent", "dieresis", "ring", "cedilla", "hungarumlaut", "ogonek", "caron", "emdash", "AE", "ordfeminine", "Lslash", "Oslash", "OE", "ordmasculine", "ae", "dotlessi", "lslash", "oslash", "oe", "germandbls", "onesuperior", "logicalnot", "mu", "trademark", "Eth", "onehalf", "plusminus", "Thorn", "onequarter", "divide", "brokenbar", "degree", "thorn", "threequarters", "twosuperior", "registered", "minus", "eth", "multiply", "threesuperior", "copyright", "Aacute", "Acircumflex", "Adieresis", "Agrave", "Aring", "Atilde", "Ccedilla", "Eacute", "Ecircumflex", "Edieresis", "Egrave", "Iacute", "Icircumflex", "Idieresis", "Igrave", "Ntilde", "Oacute", "Ocircumflex", "Odieresis", "Ograve", "Otilde", "Scaron", "Uacute", "Ucircumflex", "Udieresis", "Ugrave", "Yacute", "Ydieresis", "Zcaron", "aacute", "acircumflex", "adieresis", "agrave", "aring", "atilde", "ccedilla", "eacute", "ecircumflex", "edieresis", "egrave", "iacute", "icircumflex", "idieresis", "igrave", "ntilde", "oacute", "ocircumflex", "odieresis", "ograve", "otilde", "scaron", "uacute", "ucircumflex", "udieresis", "ugrave", "yacute", "ydieresis", "zcaron", "exclamsmall", "Hungarumlautsmall", "dollaroldstyle", "dollarsuperior", "ampersandsmall", "Acutesmall", "parenleftsuperior", "parenrightsuperior", "twodotenleader", "onedotenleader", "zerooldstyle", "oneoldstyle", "twooldstyle", "threeoldstyle", "fouroldstyle", "fiveoldstyle", "sixoldstyle", "sevenoldstyle", "eightoldstyle", "nineoldstyle", "commasuperior", "threequartersemdash", "periodsuperior", "questionsmall", "asuperior", "bsuperior", "centsuperior", "dsuperior", "esuperior", "isuperior", "lsuperior", "msuperior", "nsuperior", "osuperior", "rsuperior", "ssuperior", "tsuperior", "ff", "ffi", "ffl", "parenleftinferior", "parenrightinferior", "Circumflexsmall", "hyphensuperior", "Gravesmall", "Asmall", "Bsmall", "Csmall", "Dsmall", "Esmall", "Fsmall", "Gsmall", "Hsmall", "Ismall", "Jsmall", "Ksmall", "Lsmall", "Msmall", "Nsmall", "Osmall", "Psmall", "Qsmall", "Rsmall", "Ssmall", "Tsmall", "Usmall", "Vsmall", "Wsmall", "Xsmall", "Ysmall", "Zsmall", "colonmonetary", "onefitted", "rupiah", "Tildesmall", "exclamdownsmall", "centoldstyle", "Lslashsmall", "Scaronsmall", "Zcaronsmall", "Dieresissmall", "Brevesmall", "Caronsmall", "Dotaccentsmall", "Macronsmall", "figuredash", "hypheninferior", "Ogoneksmall", "Ringsmall", "Cedillasmall", "questiondownsmall", "oneeighth", "threeeighths", "fiveeighths", "seveneighths", "onethird", "twothirds", "zerosuperior", "foursuperior", "fivesuperior", "sixsuperior", "sevensuperior", "eightsuperior", "ninesuperior", "zeroinferior", "oneinferior", "twoinferior", "threeinferior", "fourinferior", "fiveinferior", "sixinferior", "seveninferior", "eightinferior", "nineinferior", "centinferior", "dollarinferior", "periodinferior", "commainferior", "Agravesmall", "Aacutesmall", "Acircumflexsmall", "Atildesmall", "Adieresissmall", "Aringsmall", "AEsmall", "Ccedillasmall", "Egravesmall", "Eacutesmall", "Ecircumflexsmall", "Edieresissmall", "Igravesmall", "Iacutesmall", "Icircumflexsmall", "Idieresissmall", "Ethsmall", "Ntildesmall", "Ogravesmall", "Oacutesmall", "Ocircumflexsmall", "Otildesmall", "Odieresissmall", "OEsmall", "Oslashsmall", "Ugravesmall", "Uacutesmall", "Ucircumflexsmall", "Udieresissmall", "Yacutesmall", "Thornsmall", "Ydieresissmall", "001.000", "001.001", "001.002", "001.003", "Black", "Bold", "Book", "Light", "Medium", "Regular", "Roman", "Semibold"};

    int nextIndexOffset;

    protected String key;

    public String getString(char sid) {
        if (sid < standardStrings.length) {
            return standardStrings[sid];
        }
        if (sid >= standardStrings.length + this.stringOffsets.length - 1) {
            return null;
        }
        int j = sid - standardStrings.length;

        int p = getPosition();
        seek(this.stringOffsets[j]);
        StringBuffer s = new StringBuffer();
        for (int k = this.stringOffsets[j]; k < this.stringOffsets[j + 1]; k++) {
            s.append(getCard8());
        }
        seek(p);
        return s.toString();
    }

    char getCard8() {
        try {
            byte i = this.buf.readByte();
            return (char) (i & 0xFF);
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    char getCard16() {
        try {
            return this.buf.readChar();
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    int getOffset(int offSize) {
        int offset = 0;
        for (int i = 0; i < offSize; i++) {
            offset *= 256;
            offset += getCard8();
        }
        return offset;
    }

    void seek(int offset) {
        try {
            this.buf.seek(offset);
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    short getShort() {
        try {
            return this.buf.readShort();
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    int getInt() {
        try {
            return this.buf.readInt();
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    int getPosition() {
        try {
            return (int) this.buf.getFilePointer();
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    int[] getIndex(int nextIndexOffset) {
        seek(nextIndexOffset);
        int count = getCard16();
        int[] offsets = new int[count + 1];

        if (count == 0) {
            offsets[0] = -1;
            nextIndexOffset += 2;
            return offsets;
        }

        int indexOffSize = getCard8();

        for (int j = 0; j <= count; j++) {
            offsets[j] = nextIndexOffset + 2 + 1 + (count + 1) * indexOffSize - 1
                    + getOffset(indexOffSize);
        }

        return offsets;
    }

    protected Object[] args = new Object[48];
    protected int arg_count = 0;
    protected RandomAccessFileOrArray buf;
    private int offSize;

    protected void getDictItem() {
        for (int i = 0; i < this.arg_count;) {
            this.args[i] = null;
            i++;
        }
        this.arg_count = 0;
        this.key = null;
        boolean gotKey = false;

        while (!gotKey) {
            char b0 = getCard8();
            if (b0 == '\035') {
                int item = getInt();
                this.args[this.arg_count] = Integer.valueOf(item);
                this.arg_count++;

                continue;
            }
            if (b0 == '\034') {
                short item = getShort();
                this.args[this.arg_count] = Integer.valueOf(item);
                this.arg_count++;

                continue;
            }
            if (b0 >= ' ' && b0 <= 'ö') {
                byte item = (byte) (b0 - 139);
                this.args[this.arg_count] = Integer.valueOf(item);
                this.arg_count++;

                continue;
            }
            if (b0 >= '÷' && b0 <= 'ú') {
                char b1 = getCard8();
                short item = (short) ((b0 - 247) * 256 + b1 + 108);
                this.args[this.arg_count] = Integer.valueOf(item);
                this.arg_count++;

                continue;
            }
            if (b0 >= 'û' && b0 <= 'þ') {
                char b1 = getCard8();
                short item = (short) (-(b0 - 251) * 256 - b1 - 108);
                this.args[this.arg_count] = Integer.valueOf(item);
                this.arg_count++;

                continue;
            }
            if (b0 == '\036') {
                StringBuilder item = new StringBuilder("");
                boolean done = false;
                char buffer = Character.MIN_VALUE;
                byte avail = 0;
                int nibble = 0;
                while (!done) {

                    if (avail == 0) {
                        buffer = getCard8();
                        avail = 2;
                    }
                    if (avail == 1) {
                        nibble = buffer / 16;
                        avail = (byte) (avail - 1);
                    }
                    if (avail == 2) {
                        nibble = buffer % 16;
                        avail = (byte) (avail - 1);
                    }
                    switch (nibble) {
                        case 10:
                            item.append(".");
                            continue;
                        case 11:
                            item.append("E");
                            continue;
                        case 12:
                            item.append("E-");
                            continue;
                        case 14:
                            item.append("-");
                            continue;
                        case 15:
                            done = true;
                            continue;
                    }

                    if (nibble >= 0 && nibble <= 9) {
                        item.append(String.valueOf(nibble));
                        continue;
                    }
                    item.append("<NIBBLE ERROR: ").append(nibble).append('>');
                    done = true;
                }

                this.args[this.arg_count] = item.toString();
                this.arg_count++;

                continue;
            }
            if (b0 <= '\025') {
                gotKey = true;
                if (b0 != '\f') {
                    this.key = operatorNames[b0];
                    continue;
                }
                this.key = operatorNames[32 + getCard8()];
            }
        }
    }
    protected int nameIndexOffset;
    protected int topdictIndexOffset;
    protected int stringIndexOffset;
    protected int gsubrIndexOffset;
    protected int[] nameOffsets;
    protected int[] topdictOffsets;
    protected int[] stringOffsets;
    protected int[] gsubrOffsets;
    protected Font[] fonts;

    protected static abstract class Item {

        protected int myOffset = -1;

        public void increment(int[] currentOffset) {
            this.myOffset = currentOffset[0];
        }

        public void emit(byte[] buffer) {
        }

        public void xref() {
        }
    }

    protected static abstract class OffsetItem
            extends Item {

        public int value;

        public void set(int offset) {
            this.value = offset;
        }
    }

    protected static final class RangeItem
            extends Item {

        public int offset;
        public int length;
        private RandomAccessFileOrArray buf;

        public RangeItem(RandomAccessFileOrArray buf, int offset, int length) {
            this.offset = offset;
            this.length = length;
            this.buf = buf;
        }

        public void increment(int[] currentOffset) {
            super.increment(currentOffset);
            currentOffset[0] = currentOffset[0] + this.length;
        }

        public void emit(byte[] buffer) {
            try {
                this.buf.seek(this.offset);
                for (int i = this.myOffset; i < this.myOffset + this.length; i++) {
                    buffer[i] = this.buf.readByte();
                }
            } catch (Exception e) {
                throw new ExceptionConverter(e);
            }
        }
    }

    protected static final class IndexOffsetItem
            extends OffsetItem {

        public final int size;

        public IndexOffsetItem(int size, int value) {
            this.size = size;
            this.value = value;
        }

        public IndexOffsetItem(int size) {
            this.size = size;
        }

        public void increment(int[] currentOffset) {
            super.increment(currentOffset);
            currentOffset[0] = currentOffset[0] + this.size;
        }

        public void emit(byte[] buffer) {
            int i = 0;
            switch (this.size) {
                case 4:
                    buffer[this.myOffset + i] = (byte) (this.value >>> 24 & 0xFF);
                    i++;
                case 3:
                    buffer[this.myOffset + i] = (byte) (this.value >>> 16 & 0xFF);
                    i++;
                case 2:
                    buffer[this.myOffset + i] = (byte) (this.value >>> 8 & 0xFF);
                    i++;
                case 1:
                    buffer[this.myOffset + i] = (byte) (this.value >>> 0 & 0xFF);
                    i++;
                    break;
            }
        }
    }

    protected static final class IndexBaseItem
            extends Item {
    }

    protected static final class IndexMarkerItem
            extends Item {

        private CFFFont.OffsetItem offItem;

        private CFFFont.IndexBaseItem indexBase;

        public IndexMarkerItem(CFFFont.OffsetItem offItem, CFFFont.IndexBaseItem indexBase) {
            this.offItem = offItem;
            this.indexBase = indexBase;
        }

        public void xref() {
            this.offItem.set(this.myOffset - this.indexBase.myOffset + 1);
        }
    }

    protected static final class SubrMarkerItem
            extends Item {

        private CFFFont.OffsetItem offItem;
        private CFFFont.IndexBaseItem indexBase;

        public SubrMarkerItem(CFFFont.OffsetItem offItem, CFFFont.IndexBaseItem indexBase) {
            this.offItem = offItem;
            this.indexBase = indexBase;
        }

        public void xref() {
            this.offItem.set(this.myOffset - this.indexBase.myOffset);
        }
    }

    protected static final class DictOffsetItem
            extends OffsetItem {

        public final int size = 5;

        public void increment(int[] currentOffset) {
            super.increment(currentOffset);
            currentOffset[0] = currentOffset[0] + this.size;
        }

        public void emit(byte[] buffer) {
            if (this.size == 5) {
                buffer[this.myOffset] = 29;
                buffer[this.myOffset + 1] = (byte) (this.value >>> 24 & 0xFF);
                buffer[this.myOffset + 2] = (byte) (this.value >>> 16 & 0xFF);
                buffer[this.myOffset + 3] = (byte) (this.value >>> 8 & 0xFF);
                buffer[this.myOffset + 4] = (byte) (this.value >>> 0 & 0xFF);
            }
        }
    }

    protected static final class UInt24Item
            extends Item {

        public int value;

        public UInt24Item(int value) {
            this.value = value;
        }

        public void increment(int[] currentOffset) {
            super.increment(currentOffset);
            currentOffset[0] = currentOffset[0] + 3;
        }

        public void emit(byte[] buffer) {
            buffer[this.myOffset + 0] = (byte) (this.value >>> 16 & 0xFF);
            buffer[this.myOffset + 1] = (byte) (this.value >>> 8 & 0xFF);
            buffer[this.myOffset + 2] = (byte) (this.value >>> 0 & 0xFF);
        }
    }

    protected static final class UInt32Item
            extends Item {

        public int value;

        public UInt32Item(int value) {
            this.value = value;
        }

        public void increment(int[] currentOffset) {
            super.increment(currentOffset);
            currentOffset[0] = currentOffset[0] + 4;
        }

        public void emit(byte[] buffer) {
            buffer[this.myOffset + 0] = (byte) (this.value >>> 24 & 0xFF);
            buffer[this.myOffset + 1] = (byte) (this.value >>> 16 & 0xFF);
            buffer[this.myOffset + 2] = (byte) (this.value >>> 8 & 0xFF);
            buffer[this.myOffset + 3] = (byte) (this.value >>> 0 & 0xFF);
        }
    }

    protected static final class UInt16Item
            extends Item {

        public char value;

        public UInt16Item(char value) {
            this.value = value;
        }

        public void increment(int[] currentOffset) {
            super.increment(currentOffset);
            currentOffset[0] = currentOffset[0] + 2;
        }

        public void emit(byte[] buffer) {
            buffer[this.myOffset + 0] = (byte) (this.value >>> 8 & 0xFF);
            buffer[this.myOffset + 1] = (byte) (this.value >>> 0 & 0xFF);
        }
    }

    protected static final class UInt8Item
            extends Item {

        public char value;

        public UInt8Item(char value) {
            this.value = value;
        }

        public void increment(int[] currentOffset) {
            super.increment(currentOffset);
            currentOffset[0] = currentOffset[0] + 1;
        }

        public void emit(byte[] buffer) {
            buffer[this.myOffset + 0] = (byte) (this.value >>> 0 & 0xFF);
        }
    }

    protected static final class StringItem extends Item {

        public String s;

        public StringItem(String s) {
            this.s = s;
        }

        public void increment(int[] currentOffset) {
            super.increment(currentOffset);
            currentOffset[0] = currentOffset[0] + this.s.length();
        }

        public void emit(byte[] buffer) {
            for (int i = 0; i < this.s.length(); i++) {
                buffer[this.myOffset + i] = (byte) (this.s.charAt(i) & 0xFF);
            }
        }
    }

    protected static final class DictNumberItem
            extends Item {

        public final int value;

        public int size = 5;

        public DictNumberItem(int value) {
            this.value = value;
        }

        public void increment(int[] currentOffset) {
            super.increment(currentOffset);
            currentOffset[0] = currentOffset[0] + this.size;
        }

        public void emit(byte[] buffer) {
            if (this.size == 5) {
                buffer[this.myOffset] = 29;
                buffer[this.myOffset + 1] = (byte) (this.value >>> 24 & 0xFF);
                buffer[this.myOffset + 2] = (byte) (this.value >>> 16 & 0xFF);
                buffer[this.myOffset + 3] = (byte) (this.value >>> 8 & 0xFF);
                buffer[this.myOffset + 4] = (byte) (this.value >>> 0 & 0xFF);
            }
        }
    }

    protected static final class MarkerItem
            extends Item {

        CFFFont.OffsetItem p;

        public MarkerItem(CFFFont.OffsetItem pointerToMarker) {
            this.p = pointerToMarker;
        }

        public void xref() {
            this.p.set(this.myOffset);
        }
    }

    protected RangeItem getEntireIndexRange(int indexOffset) {
        seek(indexOffset);
        int count = getCard16();
        if (count == 0) {
            return new RangeItem(this.buf, indexOffset, 2);
        }
        int indexOffSize = getCard8();
        seek(indexOffset + 2 + 1 + count * indexOffSize);
        int size = getOffset(indexOffSize) - 1;
        return new RangeItem(this.buf, indexOffset, 3 + (count + 1) * indexOffSize + size);
    }

    public byte[] getCID(String fontName) {
        int j;
        for (j = 0; j < this.fonts.length
                && !fontName.equals((this.fonts[j]).name); j++);
        if (j == this.fonts.length) {
            return null;
        }

        LinkedList<Item> l = new LinkedList<Item>();

        seek(0);

        int major = getCard8();
        int minor = getCard8();
        int hdrSize = getCard8();
        int offSize = getCard8();
        this.nextIndexOffset = hdrSize;

        l.addLast(new RangeItem(this.buf, 0, hdrSize));

        int nglyphs = -1, nstrings = -1;
        if (!(this.fonts[j]).isCID) {

            seek((this.fonts[j]).charstringsOffset);
            nglyphs = getCard16();
            seek(this.stringIndexOffset);
            nstrings = getCard16() + standardStrings.length;
        }

        l.addLast(new UInt16Item('\001'));
        l.addLast(new UInt8Item('\001'));
        l.addLast(new UInt8Item('\001'));
        l.addLast(new UInt8Item((char) (1 + (this.fonts[j]).name.length())));
        l.addLast(new StringItem((this.fonts[j]).name));

        l.addLast(new UInt16Item('\001'));
        l.addLast(new UInt8Item('\002'));
        l.addLast(new UInt16Item('\001'));
        OffsetItem topdictIndex1Ref = new IndexOffsetItem(2);
        l.addLast(topdictIndex1Ref);
        IndexBaseItem topdictBase = new IndexBaseItem();
        l.addLast(topdictBase);

        OffsetItem charsetRef = new DictOffsetItem();
        OffsetItem charstringsRef = new DictOffsetItem();
        OffsetItem fdarrayRef = new DictOffsetItem();
        OffsetItem fdselectRef = new DictOffsetItem();

        if (!(this.fonts[j]).isCID) {

            l.addLast(new DictNumberItem(nstrings));
            l.addLast(new DictNumberItem(nstrings + 1));
            l.addLast(new DictNumberItem(0));
            l.addLast(new UInt8Item('\f'));
            l.addLast(new UInt8Item('\036'));

            l.addLast(new DictNumberItem(nglyphs));
            l.addLast(new UInt8Item('\f'));
            l.addLast(new UInt8Item('"'));
        }

        l.addLast(fdarrayRef);
        l.addLast(new UInt8Item('\f'));
        l.addLast(new UInt8Item('$'));

        l.addLast(fdselectRef);
        l.addLast(new UInt8Item('\f'));
        l.addLast(new UInt8Item('%'));

        l.addLast(charsetRef);
        l.addLast(new UInt8Item('\017'));

        l.addLast(charstringsRef);
        l.addLast(new UInt8Item('\021'));

        seek(this.topdictOffsets[j]);
        while (getPosition() < this.topdictOffsets[j + 1]) {
            int p1 = getPosition();
            getDictItem();
            int p2 = getPosition();
            if (this.key == "Encoding" || this.key == "Private" || this.key == "FDSelect" || this.key == "FDArray" || this.key == "charset" || this.key == "CharStrings") {
                continue;
            }

            l.add(new RangeItem(this.buf, p1, p2 - p1));
        }

        l.addLast(new IndexMarkerItem(topdictIndex1Ref, topdictBase));

        if ((this.fonts[j]).isCID) {
            l.addLast(getEntireIndexRange(this.stringIndexOffset));
        } else {
            byte stringsIndexOffSize;
            String fdFontName = (this.fonts[j]).name + "-OneRange";
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

            l.addLast(new UInt16Item((char) (this.stringOffsets.length - 1 + 3)));
            l.addLast(new UInt8Item((char) stringsIndexOffSize));
            for (int stringOffset : this.stringOffsets) {
                l.addLast(new IndexOffsetItem(stringsIndexOffSize, stringOffset - stringsBaseOffset));
            }
            int currentStringsOffset = this.stringOffsets[this.stringOffsets.length - 1] - stringsBaseOffset;

            currentStringsOffset += "Adobe".length();
            l.addLast(new IndexOffsetItem(stringsIndexOffSize, currentStringsOffset));
            currentStringsOffset += "Identity".length();
            l.addLast(new IndexOffsetItem(stringsIndexOffSize, currentStringsOffset));
            currentStringsOffset += fdFontName.length();
            l.addLast(new IndexOffsetItem(stringsIndexOffSize, currentStringsOffset));

            l.addLast(new RangeItem(this.buf, this.stringOffsets[0], origStringsLen));
            l.addLast(new StringItem(extraStrings));
        }

        l.addLast(getEntireIndexRange(this.gsubrIndexOffset));

        if (!(this.fonts[j]).isCID) {

            l.addLast(new MarkerItem(fdselectRef));
            l.addLast(new UInt8Item('\003'));
            l.addLast(new UInt16Item('\001'));

            l.addLast(new UInt16Item((char) 0));
            l.addLast(new UInt8Item((char) 0));

            l.addLast(new UInt16Item((char) nglyphs));

            l.addLast(new MarkerItem(charsetRef));
            l.addLast(new UInt8Item('\002'));

            l.addLast(new UInt16Item('\001'));
            l.addLast(new UInt16Item((char) (nglyphs - 1)));

            l.addLast(new MarkerItem(fdarrayRef));
            l.addLast(new UInt16Item('\001'));
            l.addLast(new UInt8Item('\001'));
            l.addLast(new UInt8Item('\001'));

            OffsetItem privateIndex1Ref = new IndexOffsetItem(1);
            l.addLast(privateIndex1Ref);
            IndexBaseItem privateBase = new IndexBaseItem();
            l.addLast(privateBase);

            l.addLast(new DictNumberItem((this.fonts[j]).privateLength));
            OffsetItem privateRef = new DictOffsetItem();
            l.addLast(privateRef);
            l.addLast(new UInt8Item('\022'));

            l.addLast(new IndexMarkerItem(privateIndex1Ref, privateBase));

            l.addLast(new MarkerItem(privateRef));

            l.addLast(new RangeItem(this.buf, (this.fonts[j]).privateOffset, (this.fonts[j]).privateLength));
            if ((this.fonts[j]).privateSubrs >= 0) {
                l.addLast(getEntireIndexRange((this.fonts[j]).privateSubrs));
            }
        }

        l.addLast(new MarkerItem(charstringsRef));
        l.addLast(getEntireIndexRange((this.fonts[j]).charstringsOffset));

        int[] currentOffset = new int[1];
        currentOffset[0] = 0;

        Iterator<Item> listIter = l.iterator();
        while (listIter.hasNext()) {
            Item item = listIter.next();
            item.increment(currentOffset);
        }

        listIter = l.iterator();
        while (listIter.hasNext()) {
            Item item = listIter.next();
            item.xref();
        }

        int size = currentOffset[0];
        byte[] b = new byte[size];

        listIter = l.iterator();
        while (listIter.hasNext()) {
            Item item = listIter.next();
            item.emit(b);
        }

        return b;
    }

    public boolean isCID(String fontName) {
        for (int j = 0; j < this.fonts.length; j++) {
            if (fontName.equals((this.fonts[j]).name)) {
                return (this.fonts[j]).isCID;
            }
        }
        return false;
    }

    public boolean exists(String fontName) {
        for (int j = 0; j < this.fonts.length; j++) {
            if (fontName.equals((this.fonts[j]).name)) {
                return true;
            }
        }
        return false;
    }

    public String[] getNames() {
        String[] names = new String[this.fonts.length];
        for (int i = 0; i < this.fonts.length; i++) {
            names[i] = (this.fonts[i]).name;
        }
        return names;
    }

    protected final class Font {

        public String name;

        public String fullName;

        public boolean isCID = false;

        public int privateOffset = -1;
        public int privateLength = -1;
        public int privateSubrs = -1;
        public int charstringsOffset = -1;
        public int encodingOffset = -1;
        public int charsetOffset = -1;
        public int fdarrayOffset = -1;
        public int fdselectOffset = -1;

        public int[] fdprivateOffsets;

        public int[] fdprivateLengths;
        public int[] fdprivateSubrs;
        public int nglyphs;
        public int nstrings;
        public int CharsetLength;
        public int[] charstringsOffsets;
        public int[] charset;
        public int[] FDSelect;
        public int FDSelectLength;
        public int FDSelectFormat;
        public int CharstringType = 2;

        public int FDArrayCount;

        public int FDArrayOffsize;

        public int[] FDArrayOffsets;

        public int[] PrivateSubrsOffset;
        public int[][] PrivateSubrsOffsetsArray;
        public int[] SubrsOffsets;
    }

    public CFFFont(RandomAccessFileOrArray inputbuffer) {
        this.buf = inputbuffer;
        seek(0);

        int major = getCard8();
        int minor = getCard8();

        int hdrSize = getCard8();

        this.offSize = getCard8();

        this.nameIndexOffset = hdrSize;
        this.nameOffsets = getIndex(this.nameIndexOffset);
        this.topdictIndexOffset = this.nameOffsets[this.nameOffsets.length - 1];
        this.topdictOffsets = getIndex(this.topdictIndexOffset);
        this.stringIndexOffset = this.topdictOffsets[this.topdictOffsets.length - 1];
        this.stringOffsets = getIndex(this.stringIndexOffset);
        this.gsubrIndexOffset = this.stringOffsets[this.stringOffsets.length - 1];
        this.gsubrOffsets = getIndex(this.gsubrIndexOffset);

        this.fonts = new Font[this.nameOffsets.length - 1];

        int j;

        for (j = 0; j < this.nameOffsets.length - 1; j++) {
            this.fonts[j] = new Font();
            seek(this.nameOffsets[j]);
            (this.fonts[j]).name = "";
            for (int k = this.nameOffsets[j]; k < this.nameOffsets[j + 1]; k++) {
                (this.fonts[j]).name += getCard8();
            }
        }

        for (j = 0; j < this.topdictOffsets.length - 1; j++) {
            seek(this.topdictOffsets[j]);
            while (getPosition() < this.topdictOffsets[j + 1]) {
                getDictItem();
                if (this.key == "FullName") {

                    (this.fonts[j]).fullName = getString((char) ((Integer) this.args[0]).intValue());
                    continue;
                }
                if (this.key == "ROS") {
                    (this.fonts[j]).isCID = true;
                    continue;
                }
                if (this.key == "Private") {
                    (this.fonts[j]).privateLength = ((Integer) this.args[0]).intValue();
                    (this.fonts[j]).privateOffset = ((Integer) this.args[1]).intValue();
                    continue;
                }
                if (this.key == "charset") {
                    (this.fonts[j]).charsetOffset = ((Integer) this.args[0]).intValue();

                    continue;
                }

                if (this.key == "CharStrings") {
                    (this.fonts[j]).charstringsOffset = ((Integer) this.args[0]).intValue();

                    int p = getPosition();
                    (this.fonts[j]).charstringsOffsets = getIndex((this.fonts[j]).charstringsOffset);
                    seek(p);
                    continue;
                }
                if (this.key == "FDArray") {
                    (this.fonts[j]).fdarrayOffset = ((Integer) this.args[0]).intValue();
                    continue;
                }
                if (this.key == "FDSelect") {
                    (this.fonts[j]).fdselectOffset = ((Integer) this.args[0]).intValue();
                    continue;
                }
                if (this.key == "CharstringType") {
                    (this.fonts[j]).CharstringType = ((Integer) this.args[0]).intValue();
                }
            }

            if ((this.fonts[j]).privateOffset >= 0) {

                seek((this.fonts[j]).privateOffset);
                while (getPosition() < (this.fonts[j]).privateOffset + (this.fonts[j]).privateLength) {
                    getDictItem();
                    if (this.key == "Subrs") {

                        (this.fonts[j]).privateSubrs = ((Integer) this.args[0]).intValue() + (this.fonts[j]).privateOffset;
                    }
                }
            }

            if ((this.fonts[j]).fdarrayOffset >= 0) {
                int[] fdarrayOffsets = getIndex((this.fonts[j]).fdarrayOffset);

                (this.fonts[j]).fdprivateOffsets = new int[fdarrayOffsets.length - 1];
                (this.fonts[j]).fdprivateLengths = new int[fdarrayOffsets.length - 1];

                for (int k = 0; k < fdarrayOffsets.length - 1; k++) {
                    seek(fdarrayOffsets[k]);
                    while (getPosition() < fdarrayOffsets[k + 1]) {
                        getDictItem();
                        if (this.key == "Private") {
                            (this.fonts[j]).fdprivateLengths[k] = ((Integer) this.args[0]).intValue();
                            (this.fonts[j]).fdprivateOffsets[k] = ((Integer) this.args[1]).intValue();
                        }
                    }
                }
            }
        }
    }

    void ReadEncoding(int nextIndexOffset) {
        seek(nextIndexOffset);
        int format = getCard8();
    }
}
