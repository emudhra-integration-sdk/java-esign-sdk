package esign.text.pdf;

import esign.text.Document;
import esign.text.ExceptionConverter;
import esign.text.io.IndependentRandomAccessSource;
import esign.text.io.RandomAccessSource;
import esign.text.io.RandomAccessSourceFactory;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class RandomAccessFileOrArray
        implements DataInput {

    private final RandomAccessSource byteSource;
    private long byteSourcePosition;
    private byte back;
    private boolean isBack = false;

    @Deprecated
    public RandomAccessFileOrArray(String filename) throws IOException {
        this((new RandomAccessSourceFactory())
                .setForceRead(false)
                .setUsePlainRandomAccess(Document.plainRandomAccess)
                .createBestSource(filename));
    }

    @Deprecated
    public RandomAccessFileOrArray(RandomAccessFileOrArray source) {
        this((RandomAccessSource) new IndependentRandomAccessSource(source.byteSource));
    }

    public RandomAccessFileOrArray createView() {
        return new RandomAccessFileOrArray((RandomAccessSource) new IndependentRandomAccessSource(this.byteSource));
    }

    public RandomAccessSource createSourceView() {
        return (RandomAccessSource) new IndependentRandomAccessSource(this.byteSource);
    }

    public RandomAccessFileOrArray(RandomAccessSource byteSource) {
        this.byteSource = byteSource;
    }

    @Deprecated
    public RandomAccessFileOrArray(String filename, boolean forceRead, boolean plainRandomAccess) throws IOException {
        this((new RandomAccessSourceFactory())
                .setForceRead(forceRead)
                .setUsePlainRandomAccess(plainRandomAccess)
                .createBestSource(filename));
    }

    @Deprecated
    public RandomAccessFileOrArray(URL url) throws IOException {
        this((new RandomAccessSourceFactory()).createSource(url));
    }

    @Deprecated
    public RandomAccessFileOrArray(InputStream is) throws IOException {
        this((new RandomAccessSourceFactory()).createSource(is));
    }

    @Deprecated
    public RandomAccessFileOrArray(byte[] arrayIn) {
        this((new RandomAccessSourceFactory()).createSource(arrayIn));
    }

    @Deprecated
    protected RandomAccessSource getByteSource() {
        return this.byteSource;
    }

    public void pushBack(byte b) {
        this.back = b;
        this.isBack = true;
    }

    public int read() throws IOException {
        if (this.isBack) {
            this.isBack = false;
            return this.back & 0xFF;
        }

        return this.byteSource.get(this.byteSourcePosition++);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        int count = 0;
        if (this.isBack && len > 0) {
            this.isBack = false;
            b[off++] = this.back;
            len--;
            count++;
        }
        if (len > 0) {
            int byteSourceCount = this.byteSource.get(this.byteSourcePosition, b, off, len);
            if (byteSourceCount > 0) {
                count += byteSourceCount;
                this.byteSourcePosition += byteSourceCount;
            }
        }
        if (count == 0) {
            return -1;
        }
        return count;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        int n = 0;
        do {
            int count = read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        } while (n < len);
    }

    public long skip(long n) throws IOException {
        if (n <= 0L) {
            return 0L;
        }
        int adj = 0;
        if (this.isBack) {
            this.isBack = false;
            if (n == 1L) {
                return 1L;
            }

            n--;
            adj = 1;
        }

        long pos = getFilePointer();
        long len = length();
        long newpos = pos + n;
        if (newpos > len) {
            newpos = len;
        }
        seek(newpos);

        return newpos - pos + adj;
    }

    public int skipBytes(int n) throws IOException {
        return (int) skip(n);
    }

    @Deprecated
    public void reOpen() throws IOException {
        seek(0L);
    }

    public void close() throws IOException {
        this.isBack = false;

        this.byteSource.close();
    }

    public long length() throws IOException {
        return this.byteSource.length();
    }

    public void seek(long pos) throws IOException {
        this.byteSourcePosition = pos;
        this.isBack = false;
    }

    public long getFilePointer() throws IOException {
        return this.byteSourcePosition - (this.isBack ? 1L : 0L);
    }

    public boolean readBoolean() throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (ch != 0);
    }

    public byte readByte() throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) ch;
    }

    public int readUnsignedByte() throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    public short readShort() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short) ((ch1 << 8) + ch2);
    }

    public final short readShortLE() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short) ((ch2 << 8) + (ch1 << 0));
    }

    public int readUnsignedShort() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (ch1 << 8) + ch2;
    }

    public final int readUnsignedShortLE() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (ch2 << 8) + (ch1 << 0);
    }

    public char readChar() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (char) ((ch1 << 8) + ch2);
    }

    public final char readCharLE() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (char) ((ch2 << 8) + (ch1 << 0));
    }

    public int readInt() throws IOException {
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4;
    }

    public final int readIntLE() throws IOException {
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0);
    }

    public final long readUnsignedInt() throws IOException {
        long ch1 = read();
        long ch2 = read();
        long ch3 = read();
        long ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0L) {
            throw new EOFException();
        }
        return (ch1 << 24L) + (ch2 << 16L) + (ch3 << 8L) + (ch4 << 0L);
    }

    public final long readUnsignedIntLE() throws IOException {
        long ch1 = read();
        long ch2 = read();
        long ch3 = read();
        long ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0L) {
            throw new EOFException();
        }
        return (ch4 << 24L) + (ch3 << 16L) + (ch2 << 8L) + (ch1 << 0L);
    }

    public long readLong() throws IOException {
        return (readInt() << 32L) + (readInt() & 0xFFFFFFFFL);
    }

    public final long readLongLE() throws IOException {
        int i1 = readIntLE();
        int i2 = readIntLE();
        return (i2 << 32L) + (i1 & 0xFFFFFFFFL);
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final float readFloatLE() throws IOException {
        return Float.intBitsToFloat(readIntLE());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public final double readDoubleLE() throws IOException {
        return Double.longBitsToDouble(readLongLE());
    }

    public String readLine() throws IOException {
        StringBuilder input = new StringBuilder();
        int c = -1;
        boolean eol = false;

        while (!eol) {
            long cur;
            switch (c = read()) {
                case -1:
                case 10:
                    eol = true;
                    continue;
                case 13:
                    eol = true;
                    cur = getFilePointer();
                    if (read() != 10) {
                        seek(cur);
                    }
                    continue;
            }
            input.append((char) c);
        }

        if (c == -1 && input.length() == 0) {
            return null;
        }
        return input.toString();
    }

    public String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    public String readString(int length, String encoding) throws IOException {
        byte[] buf = new byte[length];
        readFully(buf);
        try {
            return new String(buf, encoding);
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }
}

