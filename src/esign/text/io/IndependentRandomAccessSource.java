package esign.text.io;

import java.io.IOException;

public class IndependentRandomAccessSource
        implements RandomAccessSource {

    private final RandomAccessSource source;

    public IndependentRandomAccessSource(RandomAccessSource source) {
        this.source = source;
    }

    public int get(long position) throws IOException {
        return this.source.get(position);
    }

    public int get(long position, byte[] bytes, int off, int len) throws IOException {
        return this.source.get(position, bytes, off, len);
    }

    public long length() {
        return this.source.length();
    }

    public void close() throws IOException {
    }
}
