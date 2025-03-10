package esign.text.pdf.codec;

import java.io.Serializable;

public class TIFFField
        implements Comparable<TIFFField>, Serializable {

    private static final long serialVersionUID = 9088332901412823834L;
    public static final int TIFF_BYTE = 1;
    public static final int TIFF_ASCII = 2;
    public static final int TIFF_SHORT = 3;
    public static final int TIFF_LONG = 4;
    public static final int TIFF_RATIONAL = 5;
    public static final int TIFF_SBYTE = 6;
    public static final int TIFF_UNDEFINED = 7;
    public static final int TIFF_SSHORT = 8;
    public static final int TIFF_SLONG = 9;
    public static final int TIFF_SRATIONAL = 10;
    public static final int TIFF_FLOAT = 11;
    public static final int TIFF_DOUBLE = 12;
    int tag;
    int type;
    int count;
    Object data;

    TIFFField() {
    }

    public TIFFField(int tag, int type, int count, Object data) {
        this.tag = tag;
        this.type = type;
        this.count = count;
        this.data = data;
    }

    public int getTag() {
        return this.tag;
    }

    public int getType() {
        return this.type;
    }

    public int getCount() {
        return this.count;
    }

    public byte[] getAsBytes() {
        return (byte[]) this.data;
    }

    public char[] getAsChars() {
        return (char[]) this.data;
    }

    public short[] getAsShorts() {
        return (short[]) this.data;
    }

    public int[] getAsInts() {
        return (int[]) this.data;
    }

    public long[] getAsLongs() {
        return (long[]) this.data;
    }

    public float[] getAsFloats() {
        return (float[]) this.data;
    }

    public double[] getAsDoubles() {
        return (double[]) this.data;
    }

    public int[][] getAsSRationals() {
        return (int[][]) this.data;
    }

    public long[][] getAsRationals() {
        return (long[][]) this.data;
    }

    public int getAsInt(int index) {
        switch (this.type) {
            case 1:
            case 7:
                return ((byte[]) this.data)[index] & 0xFF;
            case 6:
                return ((byte[]) this.data)[index];
            case 3:
                return ((char[]) this.data)[index] & Character.MAX_VALUE;
            case 8:
                return ((short[]) this.data)[index];
            case 9:
                return ((int[]) this.data)[index];
        }

        throw new ClassCastException();
    }

    public long getAsLong(int index) {
        switch (this.type) {
            case 1:
            case 7:
                return (((byte[]) this.data)[index] & 0xFF);
            case 6:
                return ((byte[]) this.data)[index];
            case 3:
                return (((char[]) this.data)[index] & Character.MAX_VALUE);
            case 8:
                return ((short[]) this.data)[index];
            case 9:
                return ((int[]) this.data)[index];
            case 4:
                return ((long[]) this.data)[index];
        }

        throw new ClassCastException();
    }

    public float getAsFloat(int index) {
        int[] ivalue;
        long[] lvalue;
        switch (this.type) {
            case 1:
                return (((byte[]) this.data)[index] & 0xFF);
            case 6:
                return ((byte[]) this.data)[index];
            case 3:
                return (((char[]) this.data)[index] & Character.MAX_VALUE);
            case 8:
                return ((short[]) this.data)[index];
            case 9:
                return ((int[]) this.data)[index];
            case 4:
                return (float) ((long[]) this.data)[index];
            case 11:
                return ((float[]) this.data)[index];
            case 12:
                return (float) ((double[]) this.data)[index];
            case 10:
                ivalue = getAsSRational(index);
                return (float) (ivalue[0] / ivalue[1]);
            case 5:
                lvalue = getAsRational(index);
                return (float) (lvalue[0] / lvalue[1]);
        }
        throw new ClassCastException();
    }

    public double getAsDouble(int index) {
        int[] ivalue;
        long[] lvalue;
        switch (this.type) {
            case 1:
                return (((byte[]) this.data)[index] & 0xFF);
            case 6:
                return ((byte[]) this.data)[index];
            case 3:
                return (((char[]) this.data)[index] & Character.MAX_VALUE);
            case 8:
                return ((short[]) this.data)[index];
            case 9:
                return ((int[]) this.data)[index];
            case 4:
                return ((long[]) this.data)[index];
            case 11:
                return ((float[]) this.data)[index];
            case 12:
                return ((double[]) this.data)[index];
            case 10:
                ivalue = getAsSRational(index);
                return ivalue[0] / ivalue[1];
            case 5:
                lvalue = getAsRational(index);
                return lvalue[0] / lvalue[1];
        }
        throw new ClassCastException();
    }

    public String getAsString(int index) {
        return ((String[]) this.data)[index];
    }

    public int[] getAsSRational(int index) {
        return ((int[][]) this.data)[index];
    }

    public long[] getAsRational(int index) {
        if (this.type == 4) {
            return getAsLongs();
        }
        return ((long[][]) this.data)[index];
    }

    public int compareTo(TIFFField o) {
        if (o == null) {
            throw new IllegalArgumentException();
        }

        int oTag = o.getTag();

        if (this.tag < oTag) {
            return -1;
        }
        if (this.tag > oTag) {
            return 1;
        }
        return 0;
    }
}
