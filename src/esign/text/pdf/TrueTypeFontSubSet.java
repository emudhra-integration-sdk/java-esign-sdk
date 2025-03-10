package esign.text.pdf;

import esign.text.DocumentException;
import esign.text.ExceptionConverter;
import esign.text.error_messages.MessageLocalization;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

class TrueTypeFontSubSet {

    static final String[] tableNamesSimple = new String[]{"cvt ", "fpgm", "glyf", "head", "hhea", "hmtx", "loca", "maxp", "prep"};

    static final String[] tableNamesCmap = new String[]{"cmap", "cvt ", "fpgm", "glyf", "head", "hhea", "hmtx", "loca", "maxp", "prep"};

    static final String[] tableNamesExtra = new String[]{"OS/2", "cmap", "cvt ", "fpgm", "glyf", "head", "hhea", "hmtx", "loca", "maxp", "name, prep"};

    static final int[] entrySelectors = new int[]{0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4};

    static final int TABLE_CHECKSUM = 0;

    static final int TABLE_OFFSET = 1;

    static final int TABLE_LENGTH = 2;

    static final int HEAD_LOCA_FORMAT_OFFSET = 51;

    static final int ARG_1_AND_2_ARE_WORDS = 1;

    static final int WE_HAVE_A_SCALE = 8;

    static final int MORE_COMPONENTS = 32;

    static final int WE_HAVE_AN_X_AND_Y_SCALE = 64;

    static final int WE_HAVE_A_TWO_BY_TWO = 128;

    protected HashMap<String, int[]> tableDirectory;

    protected RandomAccessFileOrArray rf;

    protected String fileName;

    protected boolean includeCmap;

    protected boolean includeExtras;

    protected boolean locaShortTable;

    protected int[] locaTable;

    protected HashSet<Integer> glyphsUsed;

    protected ArrayList<Integer> glyphsInList;
    protected int tableGlyphOffset;
    protected int[] newLocaTable;
    protected byte[] newLocaTableOut;
    protected byte[] newGlyfTable;
    protected int glyfTableRealSize;
    protected int locaTableRealSize;
    protected byte[] outFont;
    protected int fontPtr;
    protected int directoryOffset;

    TrueTypeFontSubSet(String fileName, RandomAccessFileOrArray rf, HashSet<Integer> glyphsUsed, int directoryOffset, boolean includeCmap, boolean includeExtras) {
        this.fileName = fileName;
        this.rf = rf;
        this.glyphsUsed = glyphsUsed;
        this.includeCmap = includeCmap;
        this.includeExtras = includeExtras;
        this.directoryOffset = directoryOffset;
        this.glyphsInList = new ArrayList<Integer>(glyphsUsed);
    }

    byte[] process() throws IOException, DocumentException {
        try {
            this.rf.reOpen();
            createTableDirectory();
            readLoca();
            flatGlyphs();
            createNewGlyphTables();
            locaTobytes();
            assembleFont();
            return this.outFont;
        } finally {

            try {
                this.rf.close();
            } catch (Exception exception) {
            }
        }
    }

    protected void assembleFont() throws IOException {
        String[] tableNames;
        int fullFontSize = 0;

        if (this.includeExtras) {
            tableNames = tableNamesExtra;
        } else if (this.includeCmap) {
            tableNames = tableNamesCmap;
        } else {
            tableNames = tableNamesSimple;
        }
        int tablesUsed = 2;
        int len = 0;
        for (int k = 0; k < tableNames.length; k++) {
            String name = tableNames[k];
            if (!name.equals("glyf") && !name.equals("loca")) {

                int[] tableLocation = this.tableDirectory.get(name);
                if (tableLocation != null) {
                    tablesUsed++;
                    fullFontSize += tableLocation[2] + 3 & 0xFFFFFFFC;
                }
            }
        }
        fullFontSize += this.newLocaTableOut.length;
        fullFontSize += this.newGlyfTable.length;
        int ref = 16 * tablesUsed + 12;
        fullFontSize += ref;
        this.outFont = new byte[fullFontSize];
        this.fontPtr = 0;
        writeFontInt(65536);
        writeFontShort(tablesUsed);
        int selector = entrySelectors[tablesUsed];
        writeFontShort((1 << selector) * 16);
        writeFontShort(selector);
        writeFontShort((tablesUsed - (1 << selector)) * 16);
        int i;
        for (i = 0; i < tableNames.length; i++) {
            String name = tableNames[i];
            int[] tableLocation = this.tableDirectory.get(name);
            if (tableLocation != null) {

                writeFontString(name);
                if (name.equals("glyf")) {
                    writeFontInt(calculateChecksum(this.newGlyfTable));
                    len = this.glyfTableRealSize;
                } else if (name.equals("loca")) {
                    writeFontInt(calculateChecksum(this.newLocaTableOut));
                    len = this.locaTableRealSize;
                } else {

                    writeFontInt(tableLocation[0]);
                    len = tableLocation[2];
                }
                writeFontInt(ref);
                writeFontInt(len);
                ref += len + 3 & 0xFFFFFFFC;
            }
        }
        for (i = 0; i < tableNames.length; i++) {
            String name = tableNames[i];
            int[] tableLocation = this.tableDirectory.get(name);
            if (tableLocation != null) {
                if (name.equals("glyf")) {
                    System.arraycopy(this.newGlyfTable, 0, this.outFont, this.fontPtr, this.newGlyfTable.length);
                    this.fontPtr += this.newGlyfTable.length;
                    this.newGlyfTable = null;
                } else if (name.equals("loca")) {
                    System.arraycopy(this.newLocaTableOut, 0, this.outFont, this.fontPtr, this.newLocaTableOut.length);
                    this.fontPtr += this.newLocaTableOut.length;
                    this.newLocaTableOut = null;
                } else {

                    this.rf.seek(tableLocation[1]);
                    this.rf.readFully(this.outFont, this.fontPtr, tableLocation[2]);
                    this.fontPtr += tableLocation[2] + 3 & 0xFFFFFFFC;
                }
            }
        }
    }

    protected void createTableDirectory() throws IOException, DocumentException {
        this.tableDirectory = (HashMap) new HashMap<String, int[]>();
        this.rf.seek(this.directoryOffset);
        int id = this.rf.readInt();
        if (id != 65536) {
            throw new DocumentException(MessageLocalization.getComposedMessage("1.is.not.a.true.type.file", new Object[]{this.fileName}));
        }
        int num_tables = this.rf.readUnsignedShort();
        this.rf.skipBytes(6);
        for (int k = 0; k < num_tables; k++) {
            String tag = readStandardString(4);
            int[] tableLocation = new int[3];
            tableLocation[0] = this.rf.readInt();
            tableLocation[1] = this.rf.readInt();
            tableLocation[2] = this.rf.readInt();
            this.tableDirectory.put(tag, tableLocation);
        }
    }

    protected void readLoca() throws IOException, DocumentException {
        int[] tableLocation = this.tableDirectory.get("head");
        if (tableLocation == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"head", this.fileName}));
        }
        this.rf.seek((tableLocation[1] + 51));
        this.locaShortTable = (this.rf.readUnsignedShort() == 0);
        tableLocation = this.tableDirectory.get("loca");
        if (tableLocation == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"loca", this.fileName}));
        }
        this.rf.seek(tableLocation[1]);
        if (this.locaShortTable) {
            int entries = tableLocation[2] / 2;
            this.locaTable = new int[entries];
            for (int k = 0; k < entries; k++) {
                this.locaTable[k] = this.rf.readUnsignedShort() * 2;
            }
        } else {
            int entries = tableLocation[2] / 4;
            this.locaTable = new int[entries];
            for (int k = 0; k < entries; k++) {
                this.locaTable[k] = this.rf.readInt();
            }
        }
    }

    protected void createNewGlyphTables() throws IOException {
        this.newLocaTable = new int[this.locaTable.length];
        int[] activeGlyphs = new int[this.glyphsInList.size()];
        for (int k = 0; k < activeGlyphs.length; k++) {
            activeGlyphs[k] = ((Integer) this.glyphsInList.get(k)).intValue();
        }
        Arrays.sort(activeGlyphs);
        int glyfSize = 0;
        for (int i = 0; i < activeGlyphs.length; i++) {
            int glyph = activeGlyphs[i];
            glyfSize += this.locaTable[glyph + 1] - this.locaTable[glyph];
        }
        this.glyfTableRealSize = glyfSize;
        glyfSize = glyfSize + 3 & 0xFFFFFFFC;
        this.newGlyfTable = new byte[glyfSize];
        int glyfPtr = 0;
        int listGlyf = 0;
        for (int j = 0; j < this.newLocaTable.length; j++) {
            this.newLocaTable[j] = glyfPtr;
            if (listGlyf < activeGlyphs.length && activeGlyphs[listGlyf] == j) {
                listGlyf++;
                this.newLocaTable[j] = glyfPtr;
                int start = this.locaTable[j];
                int len = this.locaTable[j + 1] - start;
                if (len > 0) {
                    this.rf.seek((this.tableGlyphOffset + start));
                    this.rf.readFully(this.newGlyfTable, glyfPtr, len);
                    glyfPtr += len;
                }
            }
        }
    }

    protected void locaTobytes() {
        if (this.locaShortTable) {
            this.locaTableRealSize = this.newLocaTable.length * 2;
        } else {
            this.locaTableRealSize = this.newLocaTable.length * 4;
        }
        this.newLocaTableOut = new byte[this.locaTableRealSize + 3 & 0xFFFFFFFC];
        this.outFont = this.newLocaTableOut;
        this.fontPtr = 0;
        for (int k = 0; k < this.newLocaTable.length; k++) {
            if (this.locaShortTable) {
                writeFontShort(this.newLocaTable[k] / 2);
            } else {
                writeFontInt(this.newLocaTable[k]);
            }
        }
    }

    protected void flatGlyphs() throws IOException, DocumentException {
        int[] tableLocation = this.tableDirectory.get("glyf");
        if (tableLocation == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("table.1.does.not.exist.in.2", new Object[]{"glyf", this.fileName}));
        }
        Integer glyph0 = Integer.valueOf(0);
        if (!this.glyphsUsed.contains(glyph0)) {
            this.glyphsUsed.add(glyph0);
            this.glyphsInList.add(glyph0);
        }
        this.tableGlyphOffset = tableLocation[1];
        for (int k = 0; k < this.glyphsInList.size(); k++) {
            int glyph = ((Integer) this.glyphsInList.get(k)).intValue();
            checkGlyphComposite(glyph);
        }
    }

    protected void checkGlyphComposite(int glyph) throws IOException {
        int start = this.locaTable[glyph];
        if (start == this.locaTable[glyph + 1]) {
            return;
        }
        this.rf.seek((this.tableGlyphOffset + start));
        int numContours = this.rf.readShort();
        if (numContours >= 0) {
            return;
        }
        this.rf.skipBytes(8);
        while (true) {
            int skip, flags = this.rf.readUnsignedShort();
            Integer cGlyph = Integer.valueOf(this.rf.readUnsignedShort());
            if (!this.glyphsUsed.contains(cGlyph)) {
                this.glyphsUsed.add(cGlyph);
                this.glyphsInList.add(cGlyph);
            }
            if ((flags & 0x20) == 0) {
                return;
            }
            if ((flags & 0x1) != 0) {
                skip = 4;
            } else {
                skip = 2;
            }
            if ((flags & 0x8) != 0) {
                skip += 2;
            } else if ((flags & 0x40) != 0) {
                skip += 4;
            }
            if ((flags & 0x80) != 0) {
                skip += 8;
            }
            this.rf.skipBytes(skip);
        }
    }

    protected String readStandardString(int length) throws IOException {
        byte[] buf = new byte[length];
        this.rf.readFully(buf);
        try {
            return new String(buf, "Cp1252");
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    protected void writeFontShort(int n) {
        this.outFont[this.fontPtr++] = (byte) (n >> 8);
        this.outFont[this.fontPtr++] = (byte) n;
    }

    protected void writeFontInt(int n) {
        this.outFont[this.fontPtr++] = (byte) (n >> 24);
        this.outFont[this.fontPtr++] = (byte) (n >> 16);
        this.outFont[this.fontPtr++] = (byte) (n >> 8);
        this.outFont[this.fontPtr++] = (byte) n;
    }

    protected void writeFontString(String s) {
        byte[] b = PdfEncodings.convertToBytes(s, "Cp1252");
        System.arraycopy(b, 0, this.outFont, this.fontPtr, b.length);
        this.fontPtr += b.length;
    }

    protected int calculateChecksum(byte[] b) {
        int len = b.length / 4;
        int v0 = 0;
        int v1 = 0;
        int v2 = 0;
        int v3 = 0;
        int ptr = 0;
        for (int k = 0; k < len; k++) {
            v3 += b[ptr++] & 0xFF;
            v2 += b[ptr++] & 0xFF;
            v1 += b[ptr++] & 0xFF;
            v0 += b[ptr++] & 0xFF;
        }
        return v0 + (v1 << 8) + (v2 << 16) + (v3 << 24);
    }
}
