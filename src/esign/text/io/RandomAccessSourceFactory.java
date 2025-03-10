package esign.text.io;

import esign.text.error_messages.MessageLocalization;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;

public final class RandomAccessSourceFactory {

    private boolean forceRead = false;
    private boolean usePlainRandomAccess = false;
    private boolean exclusivelyLockFile = false;

    public RandomAccessSourceFactory setForceRead(boolean forceRead) {
        this.forceRead = forceRead;
        return this;
    }

    public RandomAccessSourceFactory setUsePlainRandomAccess(boolean usePlainRandomAccess) {
        this.usePlainRandomAccess = usePlainRandomAccess;
        return this;
    }

    public RandomAccessSourceFactory setExclusivelyLockFile(boolean exclusivelyLockFile) {
        this.exclusivelyLockFile = exclusivelyLockFile;
        return this;
    }

    public RandomAccessSource createSource(byte[] data) {
        return new ArrayRandomAccessSource(data);
    }

    public RandomAccessSource createSource(RandomAccessFile raf) throws IOException {
        return new RAFRandomAccessSource(raf);
    }

    public RandomAccessSource createSource(URL url) throws IOException {
        InputStream is = url.openStream();
        try {
            return createSource(is);
        } finally {

            try {
                is.close();
            } catch (IOException iOException) {
            }
        }
    }

    public RandomAccessSource createSource(InputStream is) throws IOException {
        try {
            return createSource(StreamUtil.inputStreamToArray(is));
        } finally {

            try {
                is.close();
            } catch (IOException iOException) {
            }
        }
    }

    public RandomAccessSource createBestSource(String filename) throws IOException {
        File file = new File(filename);
        if (!file.canRead()) {
            if (filename.startsWith("file:/") || filename
                    .startsWith("http://") || filename
                    .startsWith("https://") || filename
                    .startsWith("jar:") || filename
                    .startsWith("wsjar:") || filename
                    .startsWith("wsjar:") || filename
                    .startsWith("vfszip:")) {
                return createSource(new URL(filename));
            }
            return createByReadingToMemory(filename);
        }

        if (this.forceRead) {
            return createByReadingToMemory(new FileInputStream(filename));
        }

        String openMode = this.exclusivelyLockFile ? "rw" : "r";

        RandomAccessFile raf = new RandomAccessFile(file, openMode);

        if (this.exclusivelyLockFile) {
            raf.getChannel().lock();
        }

        try {
            return createBestSource(raf);
        } catch (IOException e) {
            try {
                raf.close();
            } catch (IOException iOException) {
            }
            throw e;
        } catch (RuntimeException e) {
            try {
                raf.close();
            } catch (IOException iOException) {
            }
            throw e;
        }
    }

    public RandomAccessSource createBestSource(RandomAccessFile raf) throws IOException {
        if (this.usePlainRandomAccess) {
            return new RAFRandomAccessSource(raf);
        }

        if (raf.length() <= 0L) {
            return new RAFRandomAccessSource(raf);
        }

        try {
            return createBestSource(raf.getChannel());
        } catch (MapFailedException e) {
            return new RAFRandomAccessSource(raf);
        }
    }

    public RandomAccessSource createBestSource(FileChannel channel) throws IOException {
        if (channel.size() <= 67108864L) {
            return new GetBufferedRandomAccessSource(new FileChannelRandomAccessSource(channel));
        }
        return new GetBufferedRandomAccessSource(new PagedChannelRandomAccessSource(channel));
    }

    public RandomAccessSource createRanged(RandomAccessSource source, long[] ranges) throws IOException {
        RandomAccessSource[] sources = new RandomAccessSource[ranges.length / 2];
        for (int i = 0; i < ranges.length; i += 2) {
            sources[i / 2] = new WindowRandomAccessSource(source, ranges[i], ranges[i + 1]);
        }
        return new GroupedRandomAccessSource(sources);
    }

    private RandomAccessSource createByReadingToMemory(String filename) throws IOException {
        InputStream is = StreamUtil.getResourceStream(filename);
        if (is == null) {
            throw new IOException(MessageLocalization.getComposedMessage("1.not.found.as.file.or.resource", new Object[]{filename}));
        }
        return createByReadingToMemory(is);
    }

    private RandomAccessSource createByReadingToMemory(InputStream is) throws IOException {
        try {
            return new ArrayRandomAccessSource(StreamUtil.inputStreamToArray(is));
        } finally {

            try {
                is.close();
            } catch (IOException iOException) {
            }
        }
    }
}
