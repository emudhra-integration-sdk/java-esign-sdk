package esign.text.io;

import java.io.IOException;
import java.nio.channels.FileChannel;

class MappedChannelRandomAccessSource
        implements RandomAccessSource {

    private final FileChannel channel;
    private final long offset;
    private final long length;
    private ByteBufferRandomAccessSource source;

    public MappedChannelRandomAccessSource(FileChannel channel, long offset, long length) {
        if (offset < 0L) {
            throw new IllegalArgumentException(offset + " is negative");
        }
        if (length <= 0L) {
            throw new IllegalArgumentException(length + " is zero or negative");
        }
        this.channel = channel;
        this.offset = offset;
        this.length = length;
        this.source = null;
    }

    void open() throws IOException {
        if (this.source != null) {
            return;
        }
        if (!this.channel.isOpen()) {
            throw new IllegalStateException("Channel is closed");
        }
        try {
            this.source = new ByteBufferRandomAccessSource(this.channel.map(FileChannel.MapMode.READ_ONLY, this.offset, this.length));
        } catch (IOException e) {
            if (exceptionIsMapFailureException(e)) {
                throw new MapFailedException(e);
            }
            throw e;
        }
    }

    private static boolean exceptionIsMapFailureException(IOException e) {
        if (e.getMessage() != null && e.getMessage().indexOf("Map failed") >= 0) {
            return true;
        }
        return false;
    }

    public int get(long position) throws IOException {
        if (this.source == null) {
            throw new IOException("RandomAccessSource not opened");
        }
        return this.source.get(position);
    }

    public int get(long position, byte[] bytes, int off, int len) throws IOException {
        if (this.source == null) {
            throw new IOException("RandomAccessSource not opened");
        }
        return this.source.get(position, bytes, off, len);
    }

    public long length() {
        return this.length;
    }

    public void close() throws IOException {
        if (this.source == null) {
            return;
        }
        this.source.close();
        this.source = null;
    }

    public String toString() {
        return getClass().getName() + " (" + this.offset + ", " + this.length + ")";
    }
}
