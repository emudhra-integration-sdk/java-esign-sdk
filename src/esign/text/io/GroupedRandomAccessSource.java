package esign.text.io;

import java.io.IOException;

class GroupedRandomAccessSource
        implements RandomAccessSource {

    private final SourceEntry[] sources;
    private SourceEntry currentSourceEntry;
    private final long size;

    public GroupedRandomAccessSource(RandomAccessSource[] sources) throws IOException {
        this.sources = new SourceEntry[sources.length];

        long totalSize = 0L;
        for (int i = 0; i < sources.length; i++) {
            this.sources[i] = new SourceEntry(i, sources[i], totalSize);
            totalSize += sources[i].length();
        }
        this.size = totalSize;
        this.currentSourceEntry = this.sources[sources.length - 1];
        sourceInUse(this.currentSourceEntry.source);
    }

    protected int getStartingSourceIndex(long offset) {
        if (offset >= this.currentSourceEntry.firstByte) {
            return this.currentSourceEntry.index;
        }
        return 0;
    }

    private SourceEntry getSourceEntryForOffset(long offset) throws IOException {
        if (offset >= this.size) {
            return null;
        }
        if (offset >= this.currentSourceEntry.firstByte && offset <= this.currentSourceEntry.lastByte) {
            return this.currentSourceEntry;
        }

        sourceReleased(this.currentSourceEntry.source);

        int startAt = getStartingSourceIndex(offset);

        for (int i = startAt; i < this.sources.length; i++) {
            if (offset >= (this.sources[i]).firstByte && offset <= (this.sources[i]).lastByte) {
                this.currentSourceEntry = this.sources[i];
                sourceInUse(this.currentSourceEntry.source);
                return this.currentSourceEntry;
            }
        }

        return null;
    }

    protected void sourceReleased(RandomAccessSource source) throws IOException {
    }

    protected void sourceInUse(RandomAccessSource source) throws IOException {
    }

    public int get(long position) throws IOException {
        SourceEntry entry = getSourceEntryForOffset(position);

        if (entry == null) {
            return -1;
        }
        return entry.source.get(entry.offsetN(position));
    }

    public int get(long position, byte[] bytes, int off, int len) throws IOException {
        SourceEntry entry = getSourceEntryForOffset(position);

        if (entry == null) {
            return -1;
        }
        long offN = entry.offsetN(position);

        int remaining = len;

        while (remaining > 0
                && entry != null) {

            if (offN > entry.source.length()) {
                break;
            }
            int count = entry.source.get(offN, bytes, off, remaining);
            if (count == -1) {
                break;
            }
            off += count;
            position += count;
            remaining -= count;

            offN = 0L;
            entry = getSourceEntryForOffset(position);
        }
        return (remaining == len) ? -1 : (len - remaining);
    }

    public long length() {
        return this.size;
    }

    public void close() throws IOException {
        for (SourceEntry entry : this.sources) {
            entry.source.close();
        }
    }

    private static class SourceEntry {

        final RandomAccessSource source;

        final long firstByte;

        final long lastByte;

        final int index;

        public SourceEntry(int index, RandomAccessSource source, long offset) {
            this.index = index;
            this.source = source;
            this.firstByte = offset;
            this.lastByte = offset + source.length() - 1L;
        }

        public long offsetN(long absoluteOffset) {
            return absoluteOffset - this.firstByte;
        }
    }
}
