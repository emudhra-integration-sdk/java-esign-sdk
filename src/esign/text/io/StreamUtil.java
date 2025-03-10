package esign.text.io;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class StreamUtil {

    public static byte[] inputStreamToArray(InputStream is) throws IOException {
        byte[] b = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            int read = is.read(b);
            if (read < 1) {
                break;
            }
            out.write(b, 0, read);
        }
        out.close();
        return out.toByteArray();
    }

    public static void CopyBytes(RandomAccessSource source, long start, long length, OutputStream outs) throws IOException {
        if (length <= 0L) {
            return;
        }
        long idx = start;
        byte[] buf = new byte[8192];
        while (length > 0L) {
            long n = source.get(idx, buf, 0, (int) Math.min(buf.length, length));
            if (n <= 0L) {
                throw new EOFException();
            }
            outs.write(buf, 0, (int) n);
            idx += n;
            length -= n;
        }
    }

    public static InputStream getResourceStream(String key) {
        return getResourceStream(key, null);
    }

    public static InputStream getResourceStream(String key, ClassLoader loader) {
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        InputStream is = null;
        if (loader != null) {
            is = loader.getResourceAsStream(key);
            if (is != null) {
                return is;
            }
        }
        try {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                is = contextClassLoader.getResourceAsStream(key);
            }
        } catch (Throwable throwable) {
        }

        if (is == null) {
            is = StreamUtil.class.getResourceAsStream("/" + key);
        }
        if (is == null) {
            is = ClassLoader.getSystemResourceAsStream(key);
        }
        return is;
    }
}
