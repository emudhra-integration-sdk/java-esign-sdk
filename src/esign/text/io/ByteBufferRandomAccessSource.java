package esign.text.io;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

class ByteBufferRandomAccessSource
        implements RandomAccessSource {

    private final ByteBuffer byteBuffer;

    public ByteBufferRandomAccessSource(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public int get(long position) throws IOException {
        if (position > 2147483647L) {
            throw new IllegalArgumentException("Position must be less than Integer.MAX_VALUE");
        }

        try {
            if (position >= this.byteBuffer.limit()) {
                return -1;
            }
            byte b = this.byteBuffer.get((int) position);

            int n = b & 0xFF;

            return n;
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    public int get(long position, byte[] bytes, int off, int len) throws IOException {
        if (position > 2147483647L) {
            throw new IllegalArgumentException("Position must be less than Integer.MAX_VALUE");
        }
        if (position >= this.byteBuffer.limit()) {
            return -1;
        }
        this.byteBuffer.position((int) position);
        int bytesFromThisBuffer = Math.min(len, this.byteBuffer.remaining());
        this.byteBuffer.get(bytes, off, bytesFromThisBuffer);

        return bytesFromThisBuffer;
    }

    public long length() {
        return this.byteBuffer.limit();
    }

    public void close() throws IOException {
        clean(this.byteBuffer);
    }

    private static boolean clean(final ByteBuffer buffer) {
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
