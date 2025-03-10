package esign.text.io;

import java.io.IOException;

public class GetBufferedRandomAccessSource
        implements RandomAccessSource {

    private final RandomAccessSource source;
    private final byte[] getBuffer;
    private long getBufferStart = -1L;
    private long getBufferEnd = -1L;

    public GetBufferedRandomAccessSource(RandomAccessSource source) {
        this.source = source;

        this.getBuffer = new byte[(int) Math.min(Math.max(source.length() / 4L, 1L), 4096L)];
        this.getBufferStart = -1L;
        this.getBufferEnd = -1L;
    }

    public int get(long position) throws IOException {
        if (position < this.getBufferStart || position > this.getBufferEnd) {
            int count = this.source.get(position, this.getBuffer, 0, this.getBuffer.length);
            if (count == -1) {
                return -1;
            }
            this.getBufferStart = position;
            this.getBufferEnd = position + count - 1L;
        }
        int bufPos = (int) (position - this.getBufferStart);
        return 0xFF & this.getBuffer[bufPos];
    }

    public int get(long position, byte[] bytes, int off, int len) throws IOException {
        return this.source.get(position, bytes, off, len);
    }

    public long length() {
        return this.source.length();
    }

    public void close() throws IOException {
        this.source.close();
        this.getBufferStart = -1L;
        this.getBufferEnd = -1L;
    }
}
