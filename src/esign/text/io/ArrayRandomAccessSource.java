package esign.text.io;

import java.io.IOException;

class ArrayRandomAccessSource
        implements RandomAccessSource {

    private byte[] array;

    public ArrayRandomAccessSource(byte[] array) {
        if (array == null) {
            throw new NullPointerException();
        }
        this.array = array;
    }

    public int get(long offset) {
        if (offset >= this.array.length) {
            return -1;
        }
        return 0xFF & this.array[(int) offset];
    }

    public int get(long offset, byte[] bytes, int off, int len) {
        if (this.array == null) {
            throw new IllegalStateException("Already closed");
        }

        if (offset >= this.array.length) {
            return -1;
        }
        if (offset + len > this.array.length) {
            len = (int) (this.array.length - offset);
        }
        System.arraycopy(this.array, (int) offset, bytes, off, len);

        return len;
    }

    public long length() {
        return this.array.length;
    }

    public void close() throws IOException {
        this.array = null;
    }
}
