package esign.text.pdf.qrcode;

public final class ByteArray {

    private static final int INITIAL_SIZE = 32;
    private byte[] bytes;
    private int size;

    public ByteArray() {
        this.bytes = null;
        this.size = 0;
    }

    public ByteArray(int size) {
        this.bytes = new byte[size];
        this.size = size;
    }

    public ByteArray(byte[] byteArray) {
        this.bytes = byteArray;
        this.size = this.bytes.length;
    }

    public int at(int index) {
        return this.bytes[index] & 0xFF;
    }

    public void set(int index, int value) {
        this.bytes[index] = (byte) value;
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return (this.size == 0);
    }

    public void appendByte(int value) {
        if (this.size == 0 || this.size >= this.bytes.length) {
            int newSize = Math.max(32, this.size << 1);
            reserve(newSize);
        }
        this.bytes[this.size] = (byte) value;
        this.size++;
    }

    public void reserve(int capacity) {
        if (this.bytes == null || this.bytes.length < capacity) {
            byte[] newArray = new byte[capacity];
            if (this.bytes != null) {
                System.arraycopy(this.bytes, 0, newArray, 0, this.bytes.length);
            }
            this.bytes = newArray;
        }
    }

    public void set(byte[] source, int offset, int count) {
        this.bytes = new byte[count];
        this.size = count;
        for (int x = 0; x < count; x++) {
            this.bytes[x] = source[offset + x];
        }
    }
}
