package esign.text.pdf;

import esign.text.ExceptionConverter;
import esign.text.error_messages.MessageLocalization;
import java.io.IOException;
import java.io.OutputStream;

public class LZWDecoder {

    byte[][] stringTable;
    OutputStream uncompData;
    byte[] data = null;
    int tableIndex;
    int bitsToGet = 9;
    int bytePointer;
    int nextData = 0;
    int bitPointer;
    int nextBits = 0;

    int[] andTable = new int[]{511, 1023, 2047, 4095};

    public void decode(byte[] data, OutputStream uncompData) {
        if (data[0] == 0 && data[1] == 1) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("lzw.flavour.not.supported", new Object[0]));
        }

        initializeStringTable();

        this.data = data;
        this.uncompData = uncompData;

        this.bytePointer = 0;
        this.bitPointer = 0;

        this.nextData = 0;
        this.nextBits = 0;

        int oldCode = 0;

        int code;
        while ((code = getNextCode()) != 257) {

            if (code == 256) {

                initializeStringTable();
                code = getNextCode();

                if (code == 257) {
                    break;
                }

                writeString(this.stringTable[code]);
                oldCode = code;

                continue;
            }
            if (code < this.tableIndex) {

                byte[] arrayOfByte = this.stringTable[code];

                writeString(arrayOfByte);
                addStringToTable(this.stringTable[oldCode], arrayOfByte[0]);
                oldCode = code;

                continue;
            }
            byte[] string = this.stringTable[oldCode];
            string = composeString(string, string[0]);
            writeString(string);
            addStringToTable(string);
            oldCode = code;
        }
    }

    public void initializeStringTable() {
        this.stringTable = new byte[8192][];

        for (int i = 0; i < 256; i++) {
            this.stringTable[i] = new byte[1];
            this.stringTable[i][0] = (byte) i;
        }

        this.tableIndex = 258;
        this.bitsToGet = 9;
    }

    public void writeString(byte[] string) {
        try {
            this.uncompData.write(string);
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    public void addStringToTable(byte[] oldString, byte newString) {
        int length = oldString.length;
        byte[] string = new byte[length + 1];
        System.arraycopy(oldString, 0, string, 0, length);
        string[length] = newString;

        this.stringTable[this.tableIndex++] = string;

        if (this.tableIndex == 511) {
            this.bitsToGet = 10;
        } else if (this.tableIndex == 1023) {
            this.bitsToGet = 11;
        } else if (this.tableIndex == 2047) {
            this.bitsToGet = 12;
        }
    }

    public void addStringToTable(byte[] string) {
        this.stringTable[this.tableIndex++] = string;

        if (this.tableIndex == 511) {
            this.bitsToGet = 10;
        } else if (this.tableIndex == 1023) {
            this.bitsToGet = 11;
        } else if (this.tableIndex == 2047) {
            this.bitsToGet = 12;
        }
    }

    public byte[] composeString(byte[] oldString, byte newString) {
        int length = oldString.length;
        byte[] string = new byte[length + 1];
        System.arraycopy(oldString, 0, string, 0, length);
        string[length] = newString;

        return string;
    }

    public int getNextCode() {
        try {
            this.nextData = this.nextData << 8 | this.data[this.bytePointer++] & 0xFF;
            this.nextBits += 8;

            if (this.nextBits < this.bitsToGet) {
                this.nextData = this.nextData << 8 | this.data[this.bytePointer++] & 0xFF;
                this.nextBits += 8;
            }

            int code = this.nextData >> this.nextBits - this.bitsToGet & this.andTable[this.bitsToGet - 9];

            this.nextBits -= this.bitsToGet;

            return code;
        } catch (ArrayIndexOutOfBoundsException e) {

            return 257;
        }
    }
}
