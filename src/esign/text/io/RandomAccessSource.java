package esign.text.io;

import java.io.IOException;

public interface RandomAccessSource {

    int get(long paramLong) throws IOException;

    int get(long paramLong, byte[] paramArrayOfbyte, int paramInt1, int paramInt2) throws IOException;

    long length();

    void close() throws IOException;
}
