package esign.text.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedList;

class PagedChannelRandomAccessSource
        extends GroupedRandomAccessSource
        implements RandomAccessSource {

    public static final int DEFAULT_TOTAL_BUFSIZE = 67108864;
    public static final int DEFAULT_MAX_OPEN_BUFFERS = 16;
    private final int bufferSize;
    private final FileChannel channel;
    private final MRU<RandomAccessSource> mru;

    public PagedChannelRandomAccessSource(FileChannel channel) throws IOException {
        this(channel, 67108864, 16);
    }

    public PagedChannelRandomAccessSource(FileChannel channel, int totalBufferSize, int maxOpenBuffers) throws IOException {
        super(buildSources(channel, totalBufferSize / maxOpenBuffers));
        this.channel = channel;
        this.bufferSize = totalBufferSize / maxOpenBuffers;
        this.mru = new MRU<RandomAccessSource>(maxOpenBuffers);
    }

    private static RandomAccessSource[] buildSources(FileChannel channel, int bufferSize) throws IOException {
        long size = channel.size();
        if (size <= 0L) {
            throw new IOException("File size must be greater than zero");
        }
        int bufferCount = (int) (size / bufferSize) + ((size % bufferSize == 0L) ? 0 : 1);

        MappedChannelRandomAccessSource[] sources = new MappedChannelRandomAccessSource[bufferCount];
        for (int i = 0; i < bufferCount; i++) {
            long pageOffset = i * bufferSize;
            long pageLength = Math.min(size - pageOffset, bufferSize);
            sources[i] = new MappedChannelRandomAccessSource(channel, pageOffset, pageLength);
        }
        return (RandomAccessSource[]) sources;
    }

    protected int getStartingSourceIndex(long offset) {
        return (int) (offset / this.bufferSize);
    }

    protected void sourceReleased(RandomAccessSource source) throws IOException {
        RandomAccessSource old = this.mru.enqueue(source);
        if (old != null) {
            old.close();
        }
    }

    protected void sourceInUse(RandomAccessSource source) throws IOException {
        ((MappedChannelRandomAccessSource) source).open();
    }

    public void close() throws IOException {
        super.close();
        this.channel.close();
    }

    private static class MRU<E> {

        private final int limit;

        private LinkedList<E> queue = new LinkedList<E>();

        public MRU(int limit) {
            this.limit = limit;
        }

        public E enqueue(E newElement) {
            if (this.queue.size() > 0 && this.queue.getFirst() == newElement) {
                return null;
            }
            for (Iterator<E> it = this.queue.iterator(); it.hasNext();) {
                E element = it.next();
                if (newElement == element) {
                    it.remove();
                    this.queue.addFirst(newElement);
                    return null;
                }
            }
            this.queue.addFirst(newElement);

            if (this.queue.size() > this.limit) {
                return this.queue.removeLast();
            }
            return null;
        }
    }
}
