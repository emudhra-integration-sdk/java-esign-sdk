package esign.text.pdf;

import esign.text.Document;
import esign.text.DocumentException;
import esign.text.error_messages.MessageLocalization;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

class TrueTypeFont
        extends BaseFont {

    static final String[] codePages = new String[]{"1252 Latin 1", "1250 Latin 2: Eastern Europe", "1251 Cyrillic", "1253 Greek", "1254 Turkish", "1255 Hebrew", "1256 Arabic", "1257 Windows Baltic", "1258 Vietnamese", null, null, null, null, null, null, null, "874 Thai", "932 JIS/Japan", "936 Chinese: Simplified chars--PRC and Singapore", "949 Korean Wansung", "950 Chinese: Traditional chars--Taiwan and Hong Kong", "1361 Korean Johab", null, null, null, null, null, null, null, "Macintosh Character Set (US Roman)", "OEM Character Set", "Symbol Character Set", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "869 IBM Greek", "866 MS-DOS Russian", "865 MS-DOS Nordic", "864 Arabic", "863 MS-DOS Canadian French", "862 Hebrew", "861 MS-DOS Icelandic", "860 MS-DOS Portuguese", "857 IBM Turkish", "855 IBM Cyrillic; primarily Russian", "852 Latin 2", "775 MS-DOS Baltic", "737 Greek; former 437 G", "708 Arabic; ASMO 708", "850 WE/Latin 1", "437 US"};

    protected boolean justNames = false;

    protected HashMap<String, int[]> tables;

    protected RandomAccessFileOrArray rf;

    protected String fileName;

    protected boolean cff = false;

    protected int cffOffset;

    protected int cffLength;

    protected int directoryOffset;

    protected String ttcIndex;

    protected String style = "";

    protected FontHeader head = new FontHeader();

    protected HorizontalHeader hhea = new HorizontalHeader();

    protected WindowsMetrics os_2 = new WindowsMetrics();

    protected int[] glyphWidthsByIndex;

    protected int[][] bboxes;

    protected HashMap<Integer, int[]> cmap10;

    protected HashMap<Integer, int[]> cmap31;

    protected HashMap<Integer, int[]> cmapExt;

    protected int[] glyphIdToChar;

    protected int maxGlyphId;

    protected IntHashtable kerning = new IntHashtable();

    protected String fontName;

    protected String[][] subFamily;

    protected String[][] fullName;

    protected String[][] allNameEntries;

    protected String[][] familyName;

    protected double italicAngle;

    protected boolean isFixedPitch = false;

    protected int underlinePosition;

    protected int underlineThickness;

    protected TrueTypeFont() {
    }

    protected static class FontHeader {

        int flags;

        int unitsPerEm;

        short xMin;

        short yMin;

        short xMax;

        short yMax;

        int macStyle;
    }

    protected static class HorizontalHeader {

        short Ascender;

        short Descender;

        short LineGap;

        int advanceWidthMax;

        short minLeftSideBearing;

        short minRightSideBearing;

        short xMaxExtent;

        short caretSlopeRise;

        short caretSlopeRun;

        int numberOfHMetrics;
    }

    protected static class WindowsMetrics {

        short xAvgCharWidth;

        int usWeightClass;

        int usWidthClass;

        short fsType;

        short ySubscriptXSize;

        short ySubscriptYSize;

        short ySubscriptXOffset;

        short ySubscriptYOffset;

        short ySuperscriptXSize;

        short ySuperscriptYSize;

        short ySuperscriptXOffset;

        short ySuperscriptYOffset;

        short yStrikeoutSize;

        short yStrikeoutPosition;

        short sFamilyClass;

        byte[] panose = new byte[10];

        byte[] achVendID = new byte[4];

        int fsSelection;

        int usFirstCharIndex;

        int usLastCharIndex;

        short sTypoAscender;

        short sTypoDescender;

        short sTypoLineGap;

        int usWinAscent;

        int usWinDescent;

        int ulCodePageRange1;

        int ulCodePageRange2;

        int sCapHeight;
    }

    TrueTypeFont(String ttFile, String enc, boolean emb, byte[] ttfAfm, boolean justNames, boolean forceRead) throws DocumentException, IOException {
        this.justNames = justNames;
        String nameBase = getBaseName(ttFile);
        String ttcName = getTTCName(nameBase);
        if (nameBase.length() < ttFile.length()) {
            this.style = ttFile.substring(nameBase.length());
        }
        this.encoding = enc;
        this.embedded = emb;
        this.fileName = ttcName;
        this.fontType = 1;
        this.ttcIndex = "";
        if (ttcName.length() < nameBase.length()) {
            this.ttcIndex = nameBase.substring(ttcName.length() + 1);
        }
        if (this.fileName.toLowerCase().endsWith(".ttf") || this.fileName.toLowerCase().endsWith(".otf") || this.fileName.toLowerCase().endsWith(".ttc")) {
            process(ttfAfm, forceRead);
            if (!justNames && this.embedded && this.os_2.fsType == 2) {
                throw new DocumentException(MessageLocalization.getComposedMessage("1.cannot.be.embedded.due.to.licensing.restrictions", new Object[]{this.fileName + this.style}));
            }
        } else {
            throw new DocumentException(MessageLocalization.getComposedMessage("1.is.not.a.ttf.otf.or.ttc.font.file", new Object[]{this.fileName + this.style}));
        }
        if (!this.encoding.startsWith("#")) {
            PdfEncodings.convertToBytes(" ", enc);
        }
        createEncoding();
    }

    protected static String getTTCName(String name) {
        int idx = name.toLowerCase().indexOf(".ttc,");
        if (idx < 0) {
            return name;
        }
        return name.substring(0, idx + 4);
    }

    void fillTables() throws DocumentException, IOException {
        int[] table_location = this.tables.get("head");
        if (table_location == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"head", this.fileName + this.style}));
        }
        this.rf.seek((table_location[0] + 16));
        this.head.flags = this.rf.readUnsignedShort();
        this.head.unitsPerEm = this.rf.readUnsignedShort();
        this.rf.skipBytes(16);
        this.head.xMin = this.rf.readShort();
        this.head.yMin = this.rf.readShort();
        this.head.xMax = this.rf.readShort();
        this.head.yMax = this.rf.readShort();
        this.head.macStyle = this.rf.readUnsignedShort();

        table_location = this.tables.get("hhea");
        if (table_location == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"hhea", this.fileName + this.style}));
        }
        this.rf.seek((table_location[0] + 4));
        this.hhea.Ascender = this.rf.readShort();
        this.hhea.Descender = this.rf.readShort();
        this.hhea.LineGap = this.rf.readShort();
        this.hhea.advanceWidthMax = this.rf.readUnsignedShort();
        this.hhea.minLeftSideBearing = this.rf.readShort();
        this.hhea.minRightSideBearing = this.rf.readShort();
        this.hhea.xMaxExtent = this.rf.readShort();
        this.hhea.caretSlopeRise = this.rf.readShort();
        this.hhea.caretSlopeRun = this.rf.readShort();
        this.rf.skipBytes(12);
        this.hhea.numberOfHMetrics = this.rf.readUnsignedShort();

        table_location = this.tables.get("OS/2");
        if (table_location != null) {
            this.rf.seek(table_location[0]);
            int version = this.rf.readUnsignedShort();
            this.os_2.xAvgCharWidth = this.rf.readShort();
            this.os_2.usWeightClass = this.rf.readUnsignedShort();
            this.os_2.usWidthClass = this.rf.readUnsignedShort();
            this.os_2.fsType = this.rf.readShort();
            this.os_2.ySubscriptXSize = this.rf.readShort();
            this.os_2.ySubscriptYSize = this.rf.readShort();
            this.os_2.ySubscriptXOffset = this.rf.readShort();
            this.os_2.ySubscriptYOffset = this.rf.readShort();
            this.os_2.ySuperscriptXSize = this.rf.readShort();
            this.os_2.ySuperscriptYSize = this.rf.readShort();
            this.os_2.ySuperscriptXOffset = this.rf.readShort();
            this.os_2.ySuperscriptYOffset = this.rf.readShort();
            this.os_2.yStrikeoutSize = this.rf.readShort();
            this.os_2.yStrikeoutPosition = this.rf.readShort();
            this.os_2.sFamilyClass = this.rf.readShort();
            this.rf.readFully(this.os_2.panose);
            this.rf.skipBytes(16);
            this.rf.readFully(this.os_2.achVendID);
            this.os_2.fsSelection = this.rf.readUnsignedShort();
            this.os_2.usFirstCharIndex = this.rf.readUnsignedShort();
            this.os_2.usLastCharIndex = this.rf.readUnsignedShort();
            this.os_2.sTypoAscender = this.rf.readShort();
            this.os_2.sTypoDescender = this.rf.readShort();
            if (this.os_2.sTypoDescender > 0) {
                this.os_2.sTypoDescender = (short) -this.os_2.sTypoDescender;
            }
            this.os_2.sTypoLineGap = this.rf.readShort();
            this.os_2.usWinAscent = this.rf.readUnsignedShort();
            this.os_2.usWinDescent = this.rf.readUnsignedShort();
            this.os_2.ulCodePageRange1 = 0;
            this.os_2.ulCodePageRange2 = 0;
            if (version > 0) {
                this.os_2.ulCodePageRange1 = this.rf.readInt();
                this.os_2.ulCodePageRange2 = this.rf.readInt();
            }
            if (version > 1) {
                this.rf.skipBytes(2);
                this.os_2.sCapHeight = this.rf.readShort();
            } else {
                this.os_2.sCapHeight = (int) (0.7D * this.head.unitsPerEm);
            }
        } else if (this.tables.get("hhea") != null && this.tables.get("head") != null) {

            if (this.head.macStyle == 0) {
                this.os_2.usWeightClass = 700;
                this.os_2.usWidthClass = 5;
            } else if (this.head.macStyle == 5) {
                this.os_2.usWeightClass = 400;
                this.os_2.usWidthClass = 3;
            } else if (this.head.macStyle == 6) {
                this.os_2.usWeightClass = 400;
                this.os_2.usWidthClass = 7;
            } else {
                this.os_2.usWeightClass = 400;
                this.os_2.usWidthClass = 5;
            }
            this.os_2.fsType = 0;

            this.os_2.ySubscriptYSize = 0;
            this.os_2.ySubscriptYOffset = 0;
            this.os_2.ySuperscriptYSize = 0;
            this.os_2.ySuperscriptYOffset = 0;
            this.os_2.yStrikeoutSize = 0;
            this.os_2.yStrikeoutPosition = 0;
            this.os_2.sTypoAscender = (short) (int) (this.hhea.Ascender - 0.21D * this.hhea.Ascender);
            this.os_2.sTypoDescender = (short) (int) -(Math.abs(this.hhea.Descender) - Math.abs(this.hhea.Descender) * 0.07D);
            this.os_2.sTypoLineGap = (short) (this.hhea.LineGap * 2);
            this.os_2.usWinAscent = this.hhea.Ascender;
            this.os_2.usWinDescent = this.hhea.Descender;
            this.os_2.ulCodePageRange1 = 0;
            this.os_2.ulCodePageRange2 = 0;
            this.os_2.sCapHeight = (int) (0.7D * this.head.unitsPerEm);
        }

        table_location = this.tables.get("post");
        if (table_location == null) {
            this.italicAngle = -Math.atan2(this.hhea.caretSlopeRun, this.hhea.caretSlopeRise) * 180.0D / Math.PI;
        } else {
            this.rf.seek((table_location[0] + 4));
            short mantissa = this.rf.readShort();
            int fraction = this.rf.readUnsignedShort();
            this.italicAngle = mantissa + fraction / 16384.0D;
            this.underlinePosition = this.rf.readShort();
            this.underlineThickness = this.rf.readShort();
            this.isFixedPitch = (this.rf.readInt() != 0);
        }

        table_location = this.tables.get("maxp");
        if (table_location == null) {
            this.maxGlyphId = 65536;
        } else {
            this.rf.seek((table_location[0] + 4));
            this.maxGlyphId = this.rf.readUnsignedShort();
        }
    }

    String getBaseFont() throws DocumentException, IOException {
        int[] table_location = this.tables.get("name");
        if (table_location == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"name", this.fileName + this.style}));
        }
        this.rf.seek((table_location[0] + 2));
        int numRecords = this.rf.readUnsignedShort();
        int startOfStorage = this.rf.readUnsignedShort();
        for (int k = 0; k < numRecords; k++) {
            int platformID = this.rf.readUnsignedShort();
            int platformEncodingID = this.rf.readUnsignedShort();
            int languageID = this.rf.readUnsignedShort();
            int nameID = this.rf.readUnsignedShort();
            int length = this.rf.readUnsignedShort();
            int offset = this.rf.readUnsignedShort();
            if (nameID == 6) {
                this.rf.seek((table_location[0] + startOfStorage + offset));
                if (platformID == 0 || platformID == 3) {
                    return readUnicodeString(length);
                }
                return readStandardString(length);
            }
        }
        File file = new File(this.fileName);
        return file.getName().replace(' ', '-');
    }

    String[][] getNames(int id) throws DocumentException, IOException {
        int[] table_location = this.tables.get("name");
        if (table_location == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"name", this.fileName + this.style}));
        }
        this.rf.seek((table_location[0] + 2));
        int numRecords = this.rf.readUnsignedShort();
        int startOfStorage = this.rf.readUnsignedShort();
        ArrayList<String[]> names = (ArrayList) new ArrayList<String>();
        for (int k = 0; k < numRecords; k++) {
            int platformID = this.rf.readUnsignedShort();
            int platformEncodingID = this.rf.readUnsignedShort();
            int languageID = this.rf.readUnsignedShort();
            int nameID = this.rf.readUnsignedShort();
            int length = this.rf.readUnsignedShort();
            int offset = this.rf.readUnsignedShort();
            if (nameID == id) {
                String name;
                int pos = (int) this.rf.getFilePointer();
                this.rf.seek((table_location[0] + startOfStorage + offset));

                if (platformID == 0 || platformID == 3 || (platformID == 2 && platformEncodingID == 1)) {
                    name = readUnicodeString(length);
                } else {
                    name = readStandardString(length);
                }
                names.add(new String[]{String.valueOf(platformID),
                    String.valueOf(platformEncodingID), String.valueOf(languageID), name});
                this.rf.seek(pos);
            }
        }
        String[][] thisName = new String[names.size()][];
        for (int i = 0; i < names.size(); i++) {
            thisName[i] = names.get(i);
        }
        return thisName;
    }

    String[][] getAllNames() throws DocumentException, IOException {
        int[] table_location = this.tables.get("name");
        if (table_location == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"name", this.fileName + this.style}));
        }
        this.rf.seek((table_location[0] + 2));
        int numRecords = this.rf.readUnsignedShort();
        int startOfStorage = this.rf.readUnsignedShort();
        ArrayList<String[]> names = (ArrayList) new ArrayList<String>();
        for (int k = 0; k < numRecords; k++) {
            String name;
            int platformID = this.rf.readUnsignedShort();
            int platformEncodingID = this.rf.readUnsignedShort();
            int languageID = this.rf.readUnsignedShort();
            int nameID = this.rf.readUnsignedShort();
            int length = this.rf.readUnsignedShort();
            int offset = this.rf.readUnsignedShort();
            int pos = (int) this.rf.getFilePointer();
            this.rf.seek((table_location[0] + startOfStorage + offset));

            if (platformID == 0 || platformID == 3 || (platformID == 2 && platformEncodingID == 1)) {
                name = readUnicodeString(length);
            } else {
                name = readStandardString(length);
            }
            names.add(new String[]{String.valueOf(nameID), String.valueOf(platformID),
                String.valueOf(platformEncodingID), String.valueOf(languageID), name});
            this.rf.seek(pos);
        }
        String[][] thisName = new String[names.size()][];
        for (int i = 0; i < names.size(); i++) {
            thisName[i] = names.get(i);
        }
        return thisName;
    }

    void checkCff() {
        int[] table_location = this.tables.get("CFF ");
        if (table_location != null) {
            this.cff = true;
            this.cffOffset = table_location[0];
            this.cffLength = table_location[1];
        }
    }

    void process(byte[] ttfAfm, boolean preload) throws DocumentException, IOException {
        this.tables = (HashMap) new HashMap<String, int[]>();

        if (ttfAfm == null) {
            this.rf = new RandomAccessFileOrArray(this.fileName, preload, Document.plainRandomAccess);
        } else {
            this.rf = new RandomAccessFileOrArray(ttfAfm);
        }
        try {
            if (this.ttcIndex.length() > 0) {
                int dirIdx = Integer.parseInt(this.ttcIndex);
                if (dirIdx < 0) {
                    throw new DocumentException(MessageLocalization.getComposedMessage("the.font.index.for.1.must.be.positive", new Object[]{this.fileName}));
                }
                String mainTag = readStandardString(4);
                if (!mainTag.equals("ttcf")) {
                    throw new DocumentException(MessageLocalization.getComposedMessage("1.is.not.a.valid.ttc.file", new Object[]{this.fileName}));
                }
                this.rf.skipBytes(4);
                int dirCount = this.rf.readInt();
                if (dirIdx >= dirCount) {
                    throw new DocumentException(MessageLocalization.getComposedMessage("the.font.index.for.1.must.be.between.0.and.2.it.was.3", new Object[]{this.fileName, String.valueOf(dirCount - 1), String.valueOf(dirIdx)}));
                }
                this.rf.skipBytes(dirIdx * 4);
                this.directoryOffset = this.rf.readInt();
            }
            this.rf.seek(this.directoryOffset);
            int ttId = this.rf.readInt();
            if (ttId != 65536 && ttId != 1330926671) {
                throw new DocumentException(MessageLocalization.getComposedMessage("1.is.not.a.valid.ttf.or.otf.file", new Object[]{this.fileName}));
            }
            int num_tables = this.rf.readUnsignedShort();
            this.rf.skipBytes(6);
            for (int k = 0; k < num_tables; k++) {
                String tag = readStandardString(4);
                this.rf.skipBytes(4);
                int[] table_location = new int[2];
                table_location[0] = this.rf.readInt();
                table_location[1] = this.rf.readInt();
                this.tables.put(tag, table_location);
            }
            checkCff();
            this.fontName = getBaseFont();
            this.fullName = getNames(4);

            String[][] otfFamilyName = getNames(16);
            if (otfFamilyName.length > 0) {
                this.familyName = otfFamilyName;
            } else {
                this.familyName = getNames(1);
            }
            String[][] otfSubFamily = getNames(17);
            if (otfFamilyName.length > 0) {
                this.subFamily = otfSubFamily;
            } else {
                this.subFamily = getNames(2);
            }
            this.allNameEntries = getAllNames();
            if (!this.justNames) {
                fillTables();
                readGlyphWidths();
                readCMaps();
                readKerning();
                readBbox();
            }
        } finally {

            if (!this.embedded) {
                this.rf.close();
                this.rf = null;
            }
        }
    }

    protected String readStandardString(int length) throws IOException {
        return this.rf.readString(length, "Cp1252");
    }

    protected String readUnicodeString(int length) throws IOException {
        StringBuffer buf = new StringBuffer();
        length /= 2;
        for (int k = 0; k < length; k++) {
            buf.append(this.rf.readChar());
        }
        return buf.toString();
    }

    protected void readGlyphWidths() throws DocumentException, IOException {
        int[] table_location = this.tables.get("hmtx");
        if (table_location == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"hmtx", this.fileName + this.style}));
        }
        this.rf.seek(table_location[0]);
        this.glyphWidthsByIndex = new int[this.hhea.numberOfHMetrics];
        for (int k = 0; k < this.hhea.numberOfHMetrics; k++) {
            this.glyphWidthsByIndex[k] = this.rf.readUnsignedShort() * 1000 / this.head.unitsPerEm;

            int i = this.rf.readShort() * 1000 / this.head.unitsPerEm;
        }
    }

    protected int getGlyphWidth(int glyph) {
        if (glyph >= this.glyphWidthsByIndex.length) {
            glyph = this.glyphWidthsByIndex.length - 1;
        }
        return this.glyphWidthsByIndex[glyph];
    }

    private void readBbox() throws DocumentException, IOException {
        int[] locaTable, tableLocation = this.tables.get("head");
        if (tableLocation == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"head", this.fileName + this.style}));
        }
        this.rf.seek((tableLocation[0] + 51));
        boolean locaShortTable = (this.rf.readUnsignedShort() == 0);
        tableLocation = this.tables.get("loca");
        if (tableLocation == null) {
            return;
        }
        this.rf.seek(tableLocation[0]);

        if (locaShortTable) {
            int entries = tableLocation[1] / 2;
            locaTable = new int[entries];
            for (int k = 0; k < entries; k++) {
                locaTable[k] = this.rf.readUnsignedShort() * 2;
            }
        } else {
            int entries = tableLocation[1] / 4;
            locaTable = new int[entries];
            for (int k = 0; k < entries; k++) {
                locaTable[k] = this.rf.readInt();
            }
        }
        tableLocation = this.tables.get("glyf");
        if (tableLocation == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"glyf", this.fileName + this.style}));
        }
        int tableGlyphOffset = tableLocation[0];
        this.bboxes = new int[locaTable.length - 1][];
        for (int glyph = 0; glyph < locaTable.length - 1; glyph++) {
            int start = locaTable[glyph];
            if (start != locaTable[glyph + 1]) {
                this.rf.seek((tableGlyphOffset + start + 2));
                (new int[4])[0] = this.rf
                        .readShort() * 1000 / this.head.unitsPerEm;
                (new int[4])[1] = this.rf
                        .readShort() * 1000 / this.head.unitsPerEm;
                (new int[4])[2] = this.rf
                        .readShort() * 1000 / this.head.unitsPerEm;
                (new int[4])[3] = this.rf
                        .readShort() * 1000 / this.head.unitsPerEm;
                this.bboxes[glyph] = new int[4];
            }
        }
    }

    void readCMaps() throws DocumentException, IOException {
        int[] table_location = this.tables.get("cmap");
        if (table_location == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"cmap", this.fileName + this.style}));
        }
        this.rf.seek(table_location[0]);
        this.rf.skipBytes(2);
        int num_tables = this.rf.readUnsignedShort();
        this.fontSpecific = false;
        int map10 = 0;
        int map31 = 0;
        int map30 = 0;
        int mapExt = 0;
        for (int k = 0; k < num_tables; k++) {
            int platId = this.rf.readUnsignedShort();
            int platSpecId = this.rf.readUnsignedShort();
            int offset = this.rf.readInt();
            if (platId == 3 && platSpecId == 0) {
                this.fontSpecific = true;
                map30 = offset;
            } else if (platId == 3 && platSpecId == 1) {
                map31 = offset;
            } else if (platId == 3 && platSpecId == 10) {
                mapExt = offset;
            }
            if (platId == 1 && platSpecId == 0) {
                map10 = offset;
            }
        }
        if (map10 > 0) {
            this.rf.seek((table_location[0] + map10));
            int format = this.rf.readUnsignedShort();
            switch (format) {
                case 0:
                    this.cmap10 = readFormat0();
                    break;
                case 4:
                    this.cmap10 = readFormat4();
                    break;
                case 6:
                    this.cmap10 = readFormat6();
                    break;
            }
        }
        if (map31 > 0) {
            this.rf.seek((table_location[0] + map31));
            int format = this.rf.readUnsignedShort();
            if (format == 4) {
                this.cmap31 = readFormat4();
            }
        }
        if (map30 > 0) {
            this.rf.seek((table_location[0] + map30));
            int format = this.rf.readUnsignedShort();
            if (format == 4) {
                this.cmap10 = readFormat4();
            }
        }
        if (mapExt > 0) {
            this.rf.seek((table_location[0] + mapExt));
            int format = this.rf.readUnsignedShort();
            switch (format) {
                case 0:
                    this.cmapExt = readFormat0();
                    break;
                case 4:
                    this.cmapExt = readFormat4();
                    break;
                case 6:
                    this.cmapExt = readFormat6();
                    break;
                case 12:
                    this.cmapExt = readFormat12();
                    break;
            }
        }
    }

    HashMap<Integer, int[]> readFormat12() throws IOException {
        HashMap<Integer, int[]> h = (HashMap) new HashMap<Integer, int[]>();
        this.rf.skipBytes(2);
        int table_lenght = this.rf.readInt();
        this.rf.skipBytes(4);
        int nGroups = this.rf.readInt();
        for (int k = 0; k < nGroups; k++) {
            int startCharCode = this.rf.readInt();
            int endCharCode = this.rf.readInt();
            int startGlyphID = this.rf.readInt();
            for (int i = startCharCode; i <= endCharCode; i++) {
                int[] r = new int[2];
                r[0] = startGlyphID;
                r[1] = getGlyphWidth(r[0]);
                h.put(Integer.valueOf(i), r);
                startGlyphID++;
            }
        }
        return h;
    }

    HashMap<Integer, int[]> readFormat0() throws IOException {
        HashMap<Integer, int[]> h = (HashMap) new HashMap<Integer, int[]>();
        this.rf.skipBytes(4);
        for (int k = 0; k < 256; k++) {
            int[] r = new int[2];
            r[0] = this.rf.readUnsignedByte();
            r[1] = getGlyphWidth(r[0]);
            h.put(Integer.valueOf(k), r);
        }
        return h;
    }

    HashMap<Integer, int[]> readFormat4() throws IOException {
        HashMap<Integer, int[]> h = (HashMap) new HashMap<Integer, int[]>();
        int table_lenght = this.rf.readUnsignedShort();
        this.rf.skipBytes(2);
        int segCount = this.rf.readUnsignedShort() / 2;
        this.rf.skipBytes(6);
        int[] endCount = new int[segCount];
        for (int k = 0; k < segCount; k++) {
            endCount[k] = this.rf.readUnsignedShort();
        }
        this.rf.skipBytes(2);
        int[] startCount = new int[segCount];
        for (int i = 0; i < segCount; i++) {
            startCount[i] = this.rf.readUnsignedShort();
        }
        int[] idDelta = new int[segCount];
        for (int j = 0; j < segCount; j++) {
            idDelta[j] = this.rf.readUnsignedShort();
        }
        int[] idRO = new int[segCount];
        for (int m = 0; m < segCount; m++) {
            idRO[m] = this.rf.readUnsignedShort();
        }
        int[] glyphId = new int[table_lenght / 2 - 8 - segCount * 4];
        int n;
        for (n = 0; n < glyphId.length; n++) {
            glyphId[n] = this.rf.readUnsignedShort();
        }
        for (n = 0; n < segCount; n++) {

            for (int i1 = startCount[n]; i1 <= endCount[n] && i1 != 65535; i1++) {
                int glyph;
                if (idRO[n] == 0) {
                    glyph = i1 + idDelta[n] & 0xFFFF;
                } else {
                    int idx = n + idRO[n] / 2 - segCount + i1 - startCount[n];
                    if (idx >= glyphId.length) {
                        continue;
                    }
                    glyph = glyphId[idx] + idDelta[n] & 0xFFFF;
                }
                int[] r = new int[2];
                r[0] = glyph;
                r[1] = getGlyphWidth(r[0]);
                h.put(Integer.valueOf(this.fontSpecific ? (((i1 & 0xFF00) == 61440) ? (i1 & 0xFF) : i1) : i1), r);
                continue;
            }
        }
        return h;
    }

    HashMap<Integer, int[]> readFormat6() throws IOException {
        HashMap<Integer, int[]> h = (HashMap) new HashMap<Integer, int[]>();
        this.rf.skipBytes(4);
        int start_code = this.rf.readUnsignedShort();
        int code_count = this.rf.readUnsignedShort();
        for (int k = 0; k < code_count; k++) {
            int[] r = new int[2];
            r[0] = this.rf.readUnsignedShort();
            r[1] = getGlyphWidth(r[0]);
            h.put(Integer.valueOf(k + start_code), r);
        }
        return h;
    }

    void readKerning() throws IOException {
        int[] table_location = this.tables.get("kern");
        if (table_location == null) {
            return;
        }
        this.rf.seek((table_location[0] + 2));
        int nTables = this.rf.readUnsignedShort();
        int checkpoint = table_location[0] + 4;
        int length = 0;
        for (int k = 0; k < nTables; k++) {
            checkpoint += length;
            this.rf.seek(checkpoint);
            this.rf.skipBytes(2);
            length = this.rf.readUnsignedShort();
            int coverage = this.rf.readUnsignedShort();
            if ((coverage & 0xFFF7) == 1) {
                int nPairs = this.rf.readUnsignedShort();
                this.rf.skipBytes(6);
                for (int j = 0; j < nPairs; j++) {
                    int pair = this.rf.readInt();
                    int value = this.rf.readShort() * 1000 / this.head.unitsPerEm;
                    this.kerning.put(pair, value);
                }
            }
        }
    }

    public int getKerning(int char1, int char2) {
        int[] metrics = getMetricsTT(char1);
        if (metrics == null) {
            return 0;
        }
        int c1 = metrics[0];
        metrics = getMetricsTT(char2);
        if (metrics == null) {
            return 0;
        }
        int c2 = metrics[0];
        return this.kerning.get((c1 << 16) + c2);
    }

    int getRawWidth(int c, String name) {
        int[] metric = getMetricsTT(c);
        if (metric == null) {
            return 0;
        }
        return metric[1];
    }

    protected PdfDictionary getFontDescriptor(PdfIndirectReference fontStream, String subsetPrefix, PdfIndirectReference cidset) {
        PdfDictionary dic = new PdfDictionary(PdfName.FONTDESCRIPTOR);
        dic.put(PdfName.ASCENT, new PdfNumber(this.os_2.sTypoAscender * 1000 / this.head.unitsPerEm));
        dic.put(PdfName.CAPHEIGHT, new PdfNumber(this.os_2.sCapHeight * 1000 / this.head.unitsPerEm));
        dic.put(PdfName.DESCENT, new PdfNumber(this.os_2.sTypoDescender * 1000 / this.head.unitsPerEm));
        dic.put(PdfName.FONTBBOX, new PdfRectangle((this.head.xMin * 1000 / this.head.unitsPerEm), (this.head.yMin * 1000 / this.head.unitsPerEm), (this.head.xMax * 1000 / this.head.unitsPerEm), (this.head.yMax * 1000 / this.head.unitsPerEm)));

        if (cidset != null) {
            dic.put(PdfName.CIDSET, cidset);
        }
        if (this.cff) {
            if (this.encoding.startsWith("Identity-")) {
                dic.put(PdfName.FONTNAME, new PdfName(subsetPrefix + this.fontName + "-" + this.encoding));
            } else {
                dic.put(PdfName.FONTNAME, new PdfName(subsetPrefix + this.fontName + this.style));
            }
        } else {
            dic.put(PdfName.FONTNAME, new PdfName(subsetPrefix + this.fontName + this.style));
        }
        dic.put(PdfName.ITALICANGLE, new PdfNumber(this.italicAngle));
        dic.put(PdfName.STEMV, new PdfNumber(80));
        if (fontStream != null) {
            if (this.cff) {
                dic.put(PdfName.FONTFILE3, fontStream);
            } else {
                dic.put(PdfName.FONTFILE2, fontStream);
            }
        }
        int flags = 0;
        if (this.isFixedPitch) {
            flags |= 0x1;
        }
        flags |= this.fontSpecific ? 4 : 32;
        if ((this.head.macStyle & 0x2) != 0) {
            flags |= 0x40;
        }
        if ((this.head.macStyle & 0x1) != 0) {
            flags |= 0x40000;
        }
        dic.put(PdfName.FLAGS, new PdfNumber(flags));

        return dic;
    }

    protected PdfDictionary getFontBaseType(PdfIndirectReference fontDescriptor, String subsetPrefix, int firstChar, int lastChar, byte[] shortTag) {
        PdfDictionary dic = new PdfDictionary(PdfName.FONT);
        if (this.cff) {
            dic.put(PdfName.SUBTYPE, PdfName.TYPE1);
            dic.put(PdfName.BASEFONT, new PdfName(this.fontName + this.style));
        } else {
            dic.put(PdfName.SUBTYPE, PdfName.TRUETYPE);
            dic.put(PdfName.BASEFONT, new PdfName(subsetPrefix + this.fontName + this.style));
        }
        if (!this.fontSpecific) {
            for (int i = firstChar; i <= lastChar; i++) {
                if (!this.differences[i].equals(".notdef")) {
                    firstChar = i;
                    break;
                }
            }
            if (this.encoding.equals("Cp1252") || this.encoding.equals("MacRoman")) {
                dic.put(PdfName.ENCODING, this.encoding.equals("Cp1252") ? PdfName.WIN_ANSI_ENCODING : PdfName.MAC_ROMAN_ENCODING);
            } else {
                PdfDictionary enc = new PdfDictionary(PdfName.ENCODING);
                PdfArray dif = new PdfArray();
                boolean gap = true;
                for (int j = firstChar; j <= lastChar; j++) {
                    if (shortTag[j] != 0) {
                        if (gap) {
                            dif.add(new PdfNumber(j));
                            gap = false;
                        }
                        dif.add(new PdfName(this.differences[j]));
                    } else {
                        gap = true;
                    }
                }
                enc.put(PdfName.DIFFERENCES, dif);
                dic.put(PdfName.ENCODING, enc);
            }
        }
        dic.put(PdfName.FIRSTCHAR, new PdfNumber(firstChar));
        dic.put(PdfName.LASTCHAR, new PdfNumber(lastChar));
        PdfArray wd = new PdfArray();
        for (int k = firstChar; k <= lastChar; k++) {
            if (shortTag[k] == 0) {
                wd.add(new PdfNumber(0));
            } else {
                wd.add(new PdfNumber(this.widths[k]));
            }
        }
        dic.put(PdfName.WIDTHS, wd);
        if (fontDescriptor != null) {
            dic.put(PdfName.FONTDESCRIPTOR, fontDescriptor);
        }
        return dic;
    }

    protected byte[] getFullFont() throws IOException {
        RandomAccessFileOrArray rf2 = null;
        try {
            rf2 = new RandomAccessFileOrArray(this.rf);
            rf2.reOpen();
            byte[] b = new byte[(int) rf2.length()];
            rf2.readFully(b);
            return b;
        } finally {
            try {
                if (rf2 != null) {
                    rf2.close();
                }
            } catch (Exception exception) {
            }
        }
    }

    protected synchronized byte[] getSubSet(HashSet<Integer> glyphs, boolean subsetp) throws IOException, DocumentException {
        TrueTypeFontSubSet sb = new TrueTypeFontSubSet(this.fileName, new RandomAccessFileOrArray(this.rf), glyphs, this.directoryOffset, true, !subsetp);
        return sb.process();
    }

    protected static int[] compactRanges(ArrayList<int[]> ranges) {
        ArrayList<int[]> simp = (ArrayList) new ArrayList<int[]>();
        for (int k = 0; k < ranges.size(); k++) {
            int[] r = ranges.get(k);
            for (int j = 0; j < r.length; j += 2) {
                simp.add(new int[]{Math.max(0, Math.min(r[j], r[j + 1])), Math.min(65535, Math.max(r[j], r[j + 1]))});
            }
        }
        for (int k1 = 0; k1 < simp.size() - 1; k1++) {
            for (int k2 = k1 + 1; k2 < simp.size(); k2++) {
                int[] r1 = simp.get(k1);
                int[] r2 = simp.get(k2);
                if ((r1[0] >= r2[0] && r1[0] <= r2[1]) || (r1[1] >= r2[0] && r1[0] <= r2[1])) {
                    r1[0] = Math.min(r1[0], r2[0]);
                    r1[1] = Math.max(r1[1], r2[1]);
                    simp.remove(k2);
                    k2--;
                }
            }
        }
        int[] s = new int[simp.size() * 2];
        for (int i = 0; i < simp.size(); i++) {
            int[] r = simp.get(i);
            s[i * 2] = r[0];
            s[i * 2 + 1] = r[1];
        }
        return s;
    }

    protected void addRangeUni(HashMap<Integer, int[]> longTag, boolean includeMetrics, boolean subsetp) {
        if (!subsetp && (this.subsetRanges != null || this.directoryOffset > 0)) {
            HashMap<Integer, int[]> usemap;
            (new int[2])[0] = 0;
            (new int[2])[1] = 65535;
            int[] rg = (this.subsetRanges == null && this.directoryOffset > 0) ? new int[2] : compactRanges(this.subsetRanges);

            if (!this.fontSpecific && this.cmap31 != null) {
                usemap = this.cmap31;
            } else if (this.fontSpecific && this.cmap10 != null) {
                usemap = this.cmap10;
            } else if (this.cmap31 != null) {
                usemap = this.cmap31;
            } else {
                usemap = this.cmap10;
            }
            for (Map.Entry<Integer, int[]> e : usemap.entrySet()) {
                int[] v = e.getValue();
                Integer gi = Integer.valueOf(v[0]);
                if (longTag.containsKey(gi)) {
                    continue;
                }
                int c = ((Integer) e.getKey()).intValue();
                boolean skip = true;
                for (int k = 0; k < rg.length; k += 2) {
                    if (c >= rg[k] && c <= rg[k + 1]) {
                        skip = false;
                        break;
                    }
                }
                if (!skip) {
                    (new int[3])[0] = v[0];
                    (new int[3])[1] = v[1];
                    (new int[3])[2] = c;
                    longTag.put(gi, includeMetrics ? new int[3] : null);
                }
            }
        }
    }

    protected void addRangeUni(HashSet<Integer> longTag, boolean subsetp) {
        if (!subsetp && (this.subsetRanges != null || this.directoryOffset > 0)) {
            HashMap<Integer, int[]> usemap;
            (new int[2])[0] = 0;
            (new int[2])[1] = 65535;
            int[] rg = (this.subsetRanges == null && this.directoryOffset > 0) ? new int[2] : compactRanges(this.subsetRanges);

            if (!this.fontSpecific && this.cmap31 != null) {
                usemap = this.cmap31;
            } else if (this.fontSpecific && this.cmap10 != null) {
                usemap = this.cmap10;
            } else if (this.cmap31 != null) {
                usemap = this.cmap31;
            } else {
                usemap = this.cmap10;
            }
            for (Map.Entry<Integer, int[]> e : usemap.entrySet()) {
                int[] v = e.getValue();
                Integer gi = Integer.valueOf(v[0]);
                if (longTag.contains(gi)) {
                    continue;
                }
                int c = ((Integer) e.getKey()).intValue();
                boolean skip = true;
                for (int k = 0; k < rg.length; k += 2) {
                    if (c >= rg[k] && c <= rg[k + 1]) {
                        skip = false;
                        break;
                    }
                }
                if (!skip) {
                    longTag.add(gi);
                }
            }
        }
    }

    void writeFont(PdfWriter writer, PdfIndirectReference ref, Object[] params) throws DocumentException, IOException {
        int firstChar = ((Integer) params[0]).intValue();
        int lastChar = ((Integer) params[1]).intValue();
        byte[] shortTag = (byte[]) params[2];
        boolean subsetp = (((Boolean) params[3]).booleanValue() && this.subset);

        if (!subsetp) {
            firstChar = 0;
            lastChar = shortTag.length - 1;
            for (int k = 0; k < shortTag.length; k++) {
                shortTag[k] = 1;
            }
        }
        PdfIndirectReference ind_font = null;
        PdfObject pobj = null;
        PdfIndirectObject obj = null;
        String subsetPrefix = "";
        if (this.embedded) {
            if (this.cff) {
                pobj = new BaseFont.StreamFont(readCffFont(), "Type1C", this.compressionLevel);
                obj = writer.addToBody(pobj);
                ind_font = obj.getIndirectReference();
            } else {
                if (subsetp) {
                    subsetPrefix = createSubsetPrefix();
                }
                HashSet<Integer> glyphs = new HashSet<Integer>();
                for (int k = firstChar; k <= lastChar; k++) {
                    if (shortTag[k] != 0) {
                        int[] metrics = null;
                        if (this.specialMap != null) {
                            int[] cd = GlyphList.nameToUnicode(this.differences[k]);
                            if (cd != null) {
                                metrics = getMetricsTT(cd[0]);
                            }
                        } else if (this.fontSpecific) {
                            metrics = getMetricsTT(k);
                        } else {
                            metrics = getMetricsTT(this.unicodeDifferences[k]);
                        }
                        if (metrics != null) {
                            glyphs.add(Integer.valueOf(metrics[0]));
                        }
                    }
                }
                addRangeUni(glyphs, subsetp);
                byte[] b = null;
                if (subsetp || this.directoryOffset != 0 || this.subsetRanges != null) {
                    b = getSubSet(new HashSet<Integer>(glyphs), subsetp);
                } else {
                    b = getFullFont();
                }
                int[] lengths = {b.length};
                pobj = new BaseFont.StreamFont(b, lengths, this.compressionLevel);
                obj = writer.addToBody(pobj);
                ind_font = obj.getIndirectReference();
            }
        }
        pobj = getFontDescriptor(ind_font, subsetPrefix, (PdfIndirectReference) null);
        if (pobj != null) {
            obj = writer.addToBody(pobj);
            ind_font = obj.getIndirectReference();
        }
        pobj = getFontBaseType(ind_font, subsetPrefix, firstChar, lastChar, shortTag);
        writer.addToBody(pobj, ref);
    }

    protected byte[] readCffFont() throws IOException {
        RandomAccessFileOrArray rf2 = new RandomAccessFileOrArray(this.rf);
        byte[] b = new byte[this.cffLength];
        try {
            rf2.reOpen();
            rf2.seek(this.cffOffset);
            rf2.readFully(b);
        } finally {
            try {
                rf2.close();
            } catch (Exception exception) {
            }
        }

        return b;
    }

    public PdfStream getFullFontStream() throws IOException, DocumentException {
        if (this.cff) {
            return new BaseFont.StreamFont(readCffFont(), "Type1C", this.compressionLevel);
        }
        byte[] b = getFullFont();
        int[] lengths = {b.length};
        return new BaseFont.StreamFont(b, lengths, this.compressionLevel);
    }

    public float getFontDescriptor(int key, float fontSize) {
        switch (key) {
            case 1:
                return this.os_2.sTypoAscender * fontSize / this.head.unitsPerEm;
            case 2:
                return this.os_2.sCapHeight * fontSize / this.head.unitsPerEm;
            case 3:
                return this.os_2.sTypoDescender * fontSize / this.head.unitsPerEm;
            case 4:
                return (float) this.italicAngle;
            case 5:
                return fontSize * this.head.xMin / this.head.unitsPerEm;
            case 6:
                return fontSize * this.head.yMin / this.head.unitsPerEm;
            case 7:
                return fontSize * this.head.xMax / this.head.unitsPerEm;
            case 8:
                return fontSize * this.head.yMax / this.head.unitsPerEm;
            case 9:
                return fontSize * this.hhea.Ascender / this.head.unitsPerEm;
            case 10:
                return fontSize * this.hhea.Descender / this.head.unitsPerEm;
            case 11:
                return fontSize * this.hhea.LineGap / this.head.unitsPerEm;
            case 12:
                return fontSize * this.hhea.advanceWidthMax / this.head.unitsPerEm;
            case 13:
                return (this.underlinePosition - this.underlineThickness / 2) * fontSize / this.head.unitsPerEm;
            case 14:
                return this.underlineThickness * fontSize / this.head.unitsPerEm;
            case 15:
                return this.os_2.yStrikeoutPosition * fontSize / this.head.unitsPerEm;
            case 16:
                return this.os_2.yStrikeoutSize * fontSize / this.head.unitsPerEm;
            case 17:
                return this.os_2.ySubscriptYSize * fontSize / this.head.unitsPerEm;
            case 18:
                return -this.os_2.ySubscriptYOffset * fontSize / this.head.unitsPerEm;
            case 19:
                return this.os_2.ySuperscriptYSize * fontSize / this.head.unitsPerEm;
            case 20:
                return this.os_2.ySuperscriptYOffset * fontSize / this.head.unitsPerEm;
            case 21:
                return this.os_2.usWeightClass;
            case 22:
                return this.os_2.usWidthClass;
        }
        return 0.0F;
    }

    public int[] getMetricsTT(int c) {
        if (this.cmapExt != null) {
            return this.cmapExt.get(Integer.valueOf(c));
        }
        if (!this.fontSpecific && this.cmap31 != null) {
            return this.cmap31.get(Integer.valueOf(c));
        }
        if (this.fontSpecific && this.cmap10 != null) {
            return this.cmap10.get(Integer.valueOf(c));
        }
        if (this.cmap31 != null) {
            return this.cmap31.get(Integer.valueOf(c));
        }
        if (this.cmap10 != null) {
            return this.cmap10.get(Integer.valueOf(c));
        }
        return null;
    }

    public String getPostscriptFontName() {
        return this.fontName;
    }

    public String[] getCodePagesSupported() {
        long cp = (this.os_2.ulCodePageRange2 << 32L) + (this.os_2.ulCodePageRange1 & 0xFFFFFFFFL);
        int count = 0;
        long bit = 1L;
        for (int k = 0; k < 64; k++) {
            if ((cp & bit) != 0L && codePages[k] != null) {
                count++;
            }
            bit <<= 1L;
        }
        String[] ret = new String[count];
        count = 0;
        bit = 1L;
        for (int i = 0; i < 64; i++) {
            if ((cp & bit) != 0L && codePages[i] != null) {
                ret[count++] = codePages[i];
            }
            bit <<= 1L;
        }
        return ret;
    }

    public String[][] getFullFontName() {
        return this.fullName;
    }

    public String getSubfamily() {
        if (this.subFamily != null && this.subFamily.length > 0) {
            return this.subFamily[0][3];
        }
        return super.getSubfamily();
    }

    public String[][] getAllNameEntries() {
        return this.allNameEntries;
    }

    public String[][] getFamilyFontName() {
        return this.familyName;
    }

    public boolean hasKernPairs() {
        return (this.kerning.size() > 0);
    }

    public void setPostscriptFontName(String name) {
        this.fontName = name;
    }

    public boolean setKerning(int char1, int char2, int kern) {
        int[] metrics = getMetricsTT(char1);
        if (metrics == null) {
            return false;
        }
        int c1 = metrics[0];
        metrics = getMetricsTT(char2);
        if (metrics == null) {
            return false;
        }
        int c2 = metrics[0];
        this.kerning.put((c1 << 16) + c2, kern);
        return true;
    }

    protected int[] getRawCharBBox(int c, String name) {
        HashMap<Integer, int[]> map = null;
        if (name == null || this.cmap31 == null) {
            map = this.cmap10;
        } else {
            map = this.cmap31;
        }
        if (map == null) {
            return null;
        }
        int[] metric = map.get(Integer.valueOf(c));
        if (metric == null || this.bboxes == null) {
            return null;
        }
        return this.bboxes[metric[0]];
    }
}
