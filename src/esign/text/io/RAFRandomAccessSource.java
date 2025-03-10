package esign.text.io;

import java.io.IOException;
import java.io.RandomAccessFile;

class RAFRandomAccessSource
        implements RandomAccessSource {

    private final RandomAccessFile raf;
    private final long length;

    public RAFRandomAccessSource(RandomAccessFile raf) throws IOException {
        this.raf = raf;
        this.length = raf.length();
    }

    public int get(long position) throws IOException {
        if (position > this.raf.length()) {
            return -1;
        }

        this.raf.seek(position);

        return this.raf.read();
    }

    public int get(long position, byte[] bytes, int off, int len) throws IOException {
        if (position > this.length) {
            return -1;
        }

        this.raf.seek(position);

        return this.raf.read(bytes, off, len);
    }

    public long length() {
        return this.length;
    }

    public void close() throws IOException {
        this.raf.close();
    }
}
