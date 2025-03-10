package esign.text.pdf.codec;

import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.RandomAccessFileOrArray;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

public class TIFFDirectory
        implements Serializable {

    private static final long serialVersionUID = -168636766193675380L;
    boolean isBigEndian;
    int numEntries;
    TIFFField[] fields;
    Hashtable<Integer, Integer> fieldIndex = new Hashtable<Integer, Integer>();

    long IFDOffset = 8L;

    long nextIFDOffset = 0L;

    TIFFDirectory() {
    }

    private static boolean isValidEndianTag(int endian) {
        return (endian == 18761 || endian == 19789);
    }

    public TIFFDirectory(RandomAccessFileOrArray stream, int directory) throws IOException {
        long global_save_offset = stream.getFilePointer();

        stream.seek(0L);
        int endian = stream.readUnsignedShort();
        if (!isValidEndianTag(endian)) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("bad.endianness.tag.not.0x4949.or.0x4d4d", new Object[0]));
        }
        this.isBigEndian = (endian == 19789);

        int magic = readUnsignedShort(stream);
        if (magic != 42) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("bad.magic.number.should.be.42", new Object[0]));
        }

        long ifd_offset = readUnsignedInt(stream);

        for (int i = 0; i < directory; i++) {
            if (ifd_offset == 0L) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("directory.number.too.large", new Object[0]));
            }

            stream.seek(ifd_offset);
            int entries = readUnsignedShort(stream);
            stream.skip((12 * entries));

            ifd_offset = readUnsignedInt(stream);
        }

        stream.seek(ifd_offset);
        initialize(stream);
        stream.seek(global_save_offset);
    }

    public TIFFDirectory(RandomAccessFileOrArray stream, long ifd_offset, int directory) throws IOException {
        long global_save_offset = stream.getFilePointer();
        stream.seek(0L);
        int endian = stream.readUnsignedShort();
        if (!isValidEndianTag(endian)) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("bad.endianness.tag.not.0x4949.or.0x4d4d", new Object[0]));
        }
        this.isBigEndian = (endian == 19789);

        stream.seek(ifd_offset);

        int dirNum = 0;
        while (dirNum < directory) {

            int numEntries = readUnsignedShort(stream);

            stream.seek(ifd_offset + (12 * numEntries));

            ifd_offset = readUnsignedInt(stream);

            stream.seek(ifd_offset);

            dirNum++;
        }

        initialize(stream);
        stream.seek(global_save_offset);
    }

    private static final int[] sizeOfType = new int[]{0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};

    private void initialize(RandomAccessFileOrArray stream) throws IOException {
        long nextTagOffset = 0L;
        long maxOffset = stream.length();

        this.IFDOffset = stream.getFilePointer();

        this.numEntries = readUnsignedShort(stream);
        this.fields = new TIFFField[this.numEntries];

        for (int i = 0; i < this.numEntries && nextTagOffset < maxOffset; i++) {
            int tag = readUnsignedShort(stream);
            int type = readUnsignedShort(stream);
            int count = (int) readUnsignedInt(stream);
            boolean processTag = true;

            nextTagOffset = stream.getFilePointer() + 4L;

            try {
                if (count * sizeOfType[type] > 4) {
                    long valueOffset = readUnsignedInt(stream);

                    if (valueOffset < maxOffset) {
                        stream.seek(valueOffset);
                    } else {

                        processTag = false;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ae) {

                processTag = false;
            }

            if (processTag) {
                int j;
                byte[] bvalues;
                char[] cvalues;
                long lvalues[], llvalues[][];
                short[] svalues;
                int ivalues[], iivalues[][];
                float[] fvalues;
                double[] dvalues;
                this.fieldIndex.put(Integer.valueOf(tag), Integer.valueOf(i));
                Object obj = null;

                switch (type) {
                    case 1:
                    case 2:
                    case 6:
                    case 7:
                        bvalues = new byte[count];
                        stream.readFully(bvalues, 0, count);

                        if (type == 2) {

                            int index = 0, prevIndex = 0;
                            ArrayList<String> v = new ArrayList<String>();

                            while (index < count) {

                                while (index < count && bvalues[index++] != 0);

                                v.add(new String(bvalues, prevIndex, index - prevIndex));

                                prevIndex = index;
                            }

                            count = v.size();
                            String[] strings = new String[count];
                            for (int c = 0; c < count; c++) {
                                strings[c] = v.get(c);
                            }

                            obj = strings;
                            break;
                        }
                        obj = bvalues;
                        break;

                    case 3:
                        cvalues = new char[count];
                        for (j = 0; j < count; j++) {
                            cvalues[j] = (char) readUnsignedShort(stream);
                        }
                        obj = cvalues;
                        break;

                    case 4:
                        lvalues = new long[count];
                        for (j = 0; j < count; j++) {
                            lvalues[j] = readUnsignedInt(stream);
                        }
                        obj = lvalues;
                        break;

                    case 5:
                        llvalues = new long[count][2];
                        for (j = 0; j < count; j++) {
                            llvalues[j][0] = readUnsignedInt(stream);
                            llvalues[j][1] = readUnsignedInt(stream);
                        }
                        obj = llvalues;
                        break;

                    case 8:
                        svalues = new short[count];
                        for (j = 0; j < count; j++) {
                            svalues[j] = readShort(stream);
                        }
                        obj = svalues;
                        break;

                    case 9:
                        ivalues = new int[count];
                        for (j = 0; j < count; j++) {
                            ivalues[j] = readInt(stream);
                        }
                        obj = ivalues;
                        break;

                    case 10:
                        iivalues = new int[count][2];
                        for (j = 0; j < count; j++) {
                            iivalues[j][0] = readInt(stream);
                            iivalues[j][1] = readInt(stream);
                        }
                        obj = iivalues;
                        break;

                    case 11:
                        fvalues = new float[count];
                        for (j = 0; j < count; j++) {
                            fvalues[j] = readFloat(stream);
                        }
                        obj = fvalues;
                        break;

                    case 12:
                        dvalues = new double[count];
                        for (j = 0; j < count; j++) {
                            dvalues[j] = readDouble(stream);
                        }
                        obj = dvalues;
                        break;
                }

                this.fields[i] = new TIFFField(tag, type, count, obj);
            }

            stream.seek(nextTagOffset);
        }

        try {
            this.nextIFDOffset = readUnsignedInt(stream);
        } catch (Exception e) {

            this.nextIFDOffset = 0L;
        }
    }

    public int getNumEntries() {
        return this.numEntries;
    }

    public TIFFField getField(int tag) {
        Integer i = this.fieldIndex.get(Integer.valueOf(tag));
        if (i == null) {
            return null;
        }
        return this.fields[i.intValue()];
    }

    public boolean isTagPresent(int tag) {
        return this.fieldIndex.containsKey(Integer.valueOf(tag));
    }

    public int[] getTags() {
        int[] tags = new int[this.fieldIndex.size()];
        Enumeration<Integer> e = this.fieldIndex.keys();
        int i = 0;

        while (e.hasMoreElements()) {
            tags[i++] = ((Integer) e.nextElement()).intValue();
        }

        return tags;
    }

    public TIFFField[] getFields() {
        return this.fields;
    }

    public byte getFieldAsByte(int tag, int index) {
        Integer i = this.fieldIndex.get(Integer.valueOf(tag));
        byte[] b = this.fields[i.intValue()].getAsBytes();
        return b[index];
    }

    public byte getFieldAsByte(int tag) {
        return getFieldAsByte(tag, 0);
    }

    public long getFieldAsLong(int tag, int index) {
        Integer i = this.fieldIndex.get(Integer.valueOf(tag));
        return this.fields[i.intValue()].getAsLong(index);
    }

    public long getFieldAsLong(int tag) {
        return getFieldAsLong(tag, 0);
    }

    public float getFieldAsFloat(int tag, int index) {
        Integer i = this.fieldIndex.get(Integer.valueOf(tag));
        return this.fields[i.intValue()].getAsFloat(index);
    }

    public float getFieldAsFloat(int tag) {
        return getFieldAsFloat(tag, 0);
    }

    public double getFieldAsDouble(int tag, int index) {
        Integer i = this.fieldIndex.get(Integer.valueOf(tag));
        return this.fields[i.intValue()].getAsDouble(index);
    }

    public double getFieldAsDouble(int tag) {
        return getFieldAsDouble(tag, 0);
    }

    private short readShort(RandomAccessFileOrArray stream) throws IOException {
        if (this.isBigEndian) {
            return stream.readShort();
        }
        return stream.readShortLE();
    }

    private int readUnsignedShort(RandomAccessFileOrArray stream) throws IOException {
        if (this.isBigEndian) {
            return stream.readUnsignedShort();
        }
        return stream.readUnsignedShortLE();
    }

    private int readInt(RandomAccessFileOrArray stream) throws IOException {
        if (this.isBigEndian) {
            return stream.readInt();
        }
        return stream.readIntLE();
    }

    private long readUnsignedInt(RandomAccessFileOrArray stream) throws IOException {
        if (this.isBigEndian) {
            return stream.readUnsignedInt();
        }
        return stream.readUnsignedIntLE();
    }

    private long readLong(RandomAccessFileOrArray stream) throws IOException {
        if (this.isBigEndian) {
            return stream.readLong();
        }
        return stream.readLongLE();
    }

    private float readFloat(RandomAccessFileOrArray stream) throws IOException {
        if (this.isBigEndian) {
            return stream.readFloat();
        }
        return stream.readFloatLE();
    }

    private double readDouble(RandomAccessFileOrArray stream) throws IOException {
        if (this.isBigEndian) {
            return stream.readDouble();
        }
        return stream.readDoubleLE();
    }

    private static int readUnsignedShort(RandomAccessFileOrArray stream, boolean isBigEndian) throws IOException {
        if (isBigEndian) {
            return stream.readUnsignedShort();
        }
        return stream.readUnsignedShortLE();
    }

    private static long readUnsignedInt(RandomAccessFileOrArray stream, boolean isBigEndian) throws IOException {
        if (isBigEndian) {
            return stream.readUnsignedInt();
        }
        return stream.readUnsignedIntLE();
    }

    public static int getNumDirectories(RandomAccessFileOrArray stream) throws IOException {
        long pointer = stream.getFilePointer();

        stream.seek(0L);
        int endian = stream.readUnsignedShort();
        if (!isValidEndianTag(endian)) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("bad.endianness.tag.not.0x4949.or.0x4d4d", new Object[0]));
        }
        boolean isBigEndian = (endian == 19789);
        int magic = readUnsignedShort(stream, isBigEndian);
        if (magic != 42) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("bad.magic.number.should.be.42", new Object[0]));
        }

        stream.seek(4L);
        long offset = readUnsignedInt(stream, isBigEndian);

        int numDirectories = 0;
        while (offset != 0L) {
            numDirectories++;

            try {
                stream.seek(offset);
                int entries = readUnsignedShort(stream, isBigEndian);
                stream.skip((12 * entries));
                offset = readUnsignedInt(stream, isBigEndian);
            } catch (EOFException eof) {
                numDirectories--;

                break;
            }
        }
        stream.seek(pointer);
        return numDirectories;
    }

    public boolean isBigEndian() {
        return this.isBigEndian;
    }

    public long getIFDOffset() {
        return this.IFDOffset;
    }

    public long getNextIFDOffset() {
        return this.nextIFDOffset;
    }
}
