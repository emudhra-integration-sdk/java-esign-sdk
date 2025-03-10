package esign.text.io;

import java.io.IOException;
import java.io.InputStream;

public class RASInputStream
        extends InputStream {

    private final RandomAccessSource source;
    private long position = 0L;

    public RASInputStream(RandomAccessSource source) {
        this.source = source;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int count = this.source.get(this.position, b, off, len);
        this.position += count;
        return count;
    }

    public int read() throws IOException {
        return this.source.get(this.position++);
    }
}
