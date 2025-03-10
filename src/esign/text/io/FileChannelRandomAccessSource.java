package esign.text.io;

import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileChannelRandomAccessSource
        implements RandomAccessSource {

    private final FileChannel channel;
    private final MappedChannelRandomAccessSource source;

    public FileChannelRandomAccessSource(FileChannel channel) throws IOException {
        this.channel = channel;
        if (channel.size() == 0L) {
            throw new IOException("File size is 0 bytes");
        }
        this.source = new MappedChannelRandomAccessSource(channel, 0L, channel.size());
        this.source.open();
    }

    public void close() throws IOException {
        this.source.close();
        this.channel.close();
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
}
