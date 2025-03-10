package esign.text.pdf;

import esign.text.DocumentException;
import esign.text.error_messages.MessageLocalization;
import java.io.IOException;
import java.util.HashMap;

class EnumerateTTC
        extends TrueTypeFont {

    protected String[] names;

    EnumerateTTC(String ttcFile) throws DocumentException, IOException {
        this.fileName = ttcFile;
        this.rf = new RandomAccessFileOrArray(ttcFile);
        findNames();
    }

    EnumerateTTC(byte[] ttcArray) throws DocumentException, IOException {
        this.fileName = "Byte array TTC";
        this.rf = new RandomAccessFileOrArray(ttcArray);
        findNames();
    }

    void findNames() throws DocumentException, IOException {
        this.tables = (HashMap) new HashMap<String, int[]>();

        try {
            String mainTag = readStandardString(4);
            if (!mainTag.equals("ttcf")) {
                throw new DocumentException(MessageLocalization.getComposedMessage("1.is.not.a.valid.ttc.file", new Object[]{this.fileName}));
            }
            this.rf.skipBytes(4);
            int dirCount = this.rf.readInt();
            this.names = new String[dirCount];
            int dirPos = (int) this.rf.getFilePointer();
            for (int dirIdx = 0; dirIdx < dirCount; dirIdx++) {
                this.tables.clear();
                this.rf.seek(dirPos);
                this.rf.skipBytes(dirIdx * 4);
                this.directoryOffset = this.rf.readInt();
                this.rf.seek(this.directoryOffset);
                if (this.rf.readInt() != 65536) {
                    throw new DocumentException(MessageLocalization.getComposedMessage("1.is.not.a.valid.ttf.file", new Object[]{this.fileName}));
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
                this.names[dirIdx] = getBaseFont();
            }
        } finally {

            if (this.rf != null) {
                this.rf.close();
            }
        }
    }

    String[] getNames() {
        return this.names;
    }
}
