package esign.text.pdf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class MappedRandomAccessFile {

    private static final int BUFSIZE = 1073741824;
    private FileChannel channel = null;

    private MappedByteBuffer[] mappedBuffers;

    private long size;

    private long pos;

    public MappedRandomAccessFile(String filename, String mode) throws FileNotFoundException, IOException {
        if (mode.equals("rw")) {
            init((new RandomAccessFile(filename, mode))
                    .getChannel(), FileChannel.MapMode.READ_WRITE);
        } else {

            init((new FileInputStream(filename))
                    .getChannel(), FileChannel.MapMode.READ_ONLY);
        }
    }

    private void init(FileChannel channel, FileChannel.MapMode mapMode) throws IOException {
        this.channel = channel;

        this.size = channel.size();
        this.pos = 0L;
        int requiredBuffers = (int) (this.size / 1073741824L) + ((this.size % 1073741824L == 0L) ? 0 : 1);

        this.mappedBuffers = new MappedByteBuffer[requiredBuffers];
        try {
            int index = 0;
            long offset;
            for (offset = 0L; offset < this.size; offset += 1073741824L) {
                long size2 = Math.min(this.size - offset, 1073741824L);
                this.mappedBuffers[index] = channel.map(mapMode, offset, size2);
                this.mappedBuffers[index].load();
                index++;
            }
            if (index != requiredBuffers) {
                throw new Error("Should never happen - " + index + " != " + requiredBuffers);
            }
        } catch (IOException e) {
            close();
            throw e;
        } catch (RuntimeException e) {
            close();
            throw e;
        }
    }

    public FileChannel getChannel() {
        return this.channel;
    }

    public int read() {
        try {
            int mapN = (int) (this.pos / 1073741824L);
            int offN = (int) (this.pos % 1073741824L);

            if (mapN >= this.mappedBuffers.length) {
                return -1;
            }
            if (offN >= this.mappedBuffers[mapN].limit()) {
                return -1;
            }
            byte b = this.mappedBuffers[mapN].get(offN);
            this.pos++;
            int n = b & 0xFF;

            return n;
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    public int read(byte[] bytes, int off, int len) {
        int mapN = (int) (this.pos / 1073741824L);
        int offN = (int) (this.pos % 1073741824L);
        int totalRead = 0;

        while (totalRead < len
                && mapN < this.mappedBuffers.length) {

            MappedByteBuffer currentBuffer = this.mappedBuffers[mapN];
            if (offN > currentBuffer.limit()) {
                break;
            }
            currentBuffer.position(offN);
            int bytesFromThisBuffer = Math.min(len - totalRead, currentBuffer.remaining());
            currentBuffer.get(bytes, off, bytesFromThisBuffer);
            off += bytesFromThisBuffer;
            this.pos += bytesFromThisBuffer;
            totalRead += bytesFromThisBuffer;

            mapN++;
            offN = 0;
        }

        return (totalRead == 0) ? -1 : totalRead;
    }

    public long getFilePointer() {
        return this.pos;
    }

    public void seek(long pos) {
        this.pos = pos;
    }

    public long length() {
        return this.size;
    }

    public void close() throws IOException {
        for (int i = 0; i < this.mappedBuffers.length; i++) {
            if (this.mappedBuffers[i] != null) {
                clean(this.mappedBuffers[i]);
                this.mappedBuffers[i] = null;
            }
        }

        if (this.channel != null) {
            this.channel.close();
        }
        this.channel = null;
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public static boolean clean(final ByteBuffer buffer) {
        if (buffer == null || !buffer.isDirect()) {
            return false;
        }
        Boolean b = AccessController.<Boolean>doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                Boolean success = Boolean.FALSE;
                try {
                    Method getCleanerMethod = buffer.getClass().getMethod("cleaner", (Class[]) null);
                    getCleanerMethod.setAccessible(true);
                    Object cleaner = getCleanerMethod.invoke(buffer, (Object[]) null);
                    Method clean = cleaner.getClass().getMethod("clean", (Class[]) null);
                    clean.invoke(cleaner, (Object[]) null);
                    success = Boolean.TRUE;
                } catch (Exception exception) {
                }

                return success;
            }
        });

        return b.booleanValue();
    }
}
