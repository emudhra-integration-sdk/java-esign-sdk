package esign.text.xmp.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteBuffer {

    private byte[] buffer;
    private int length;
    private String encoding = null;

    public ByteBuffer(int initialCapacity) {
        this.buffer = new byte[initialCapacity];
        this.length = 0;
    }

    public ByteBuffer(byte[] buffer) {
        this.buffer = buffer;
        this.length = buffer.length;
    }

    public ByteBuffer(byte[] buffer, int length) {
        if (length > buffer.length) {
            throw new ArrayIndexOutOfBoundsException("Valid length exceeds the buffer length.");
        }
        this.buffer = buffer;
        this.length = length;
    }

    public ByteBuffer(InputStream in) throws IOException {
        int chunk = 16384;
        this.length = 0;
        this.buffer = new byte[chunk];

        int read;
        while ((read = in.read(this.buffer, this.length, chunk)) > 0) {

            this.length += read;
            if (read == chunk) {
                ensureCapacity(this.length + chunk);
            }
        }
    }

    public ByteBuffer(byte[] buffer, int offset, int length) {
        if (length > buffer.length - offset) {
            throw new ArrayIndexOutOfBoundsException("Valid length exceeds the buffer length.");
        }
        this.buffer = new byte[length];
        System.arraycopy(buffer, offset, this.buffer, 0, length);
        this.length = length;
    }

    public InputStream getByteStream() {
        return new ByteArrayInputStream(this.buffer, 0, this.length);
    }

    public int length() {
        return this.length;
    }

    public byte byteAt(int index) {
        if (index < this.length) {
            return this.buffer[index];
        }

        throw new IndexOutOfBoundsException("The index exceeds the valid buffer area");
    }

    public int charAt(int index) {
        if (index < this.length) {
            return this.buffer[index] & 0xFF;
        }

        throw new IndexOutOfBoundsException("The index exceeds the valid buffer area");
    }

    public void append(byte b) {
        ensureCapacity(this.length + 1);
        this.buffer[this.length++] = b;
    }

    public void append(byte[] bytes, int offset, int len) {
        ensureCapacity(this.length + len);
        System.arraycopy(bytes, offset, this.buffer, this.length, len);
        this.length += len;
    }

    public void append(byte[] bytes) {
        append(bytes, 0, bytes.length);
    }

    public void append(ByteBuffer anotherBuffer) {
        append(anotherBuffer.buffer, 0, anotherBuffer.length);
    }

    public String getEncoding() {
        if (this.encoding == null) {

            if (this.length < 2) {

                this.encoding = "UTF-8";
            } else if (this.buffer[0] == 0) {

                if (this.length < 4 || this.buffer[1] != 0) {
                    this.encoding = "UTF-16BE";
                } else if ((this.buffer[2] & 0xFF) == 254 && (this.buffer[3] & 0xFF) == 255) {
                    this.encoding = "UTF-32BE";
                } else {
                    this.encoding = "UTF-32";
                }

            } else if ((this.buffer[0] & 0xFF) < 128) {

                if (this.buffer[1] != 0) {
                    this.encoding = "UTF-8";
                } else if (this.length < 4 || this.buffer[2] != 0) {
                    this.encoding = "UTF-16LE";
                } else {
                    this.encoding = "UTF-32LE";

                }

            } else if ((this.buffer[0] & 0xFF) == 239) {

                this.encoding = "UTF-8";
            } else if ((this.buffer[0] & 0xFF) == 254) {

                this.encoding = "UTF-16";
            } else if (this.length < 4 || this.buffer[2] != 0) {

                this.encoding = "UTF-16";
            } else {

                this.encoding = "UTF-32";
            }
        }

        return this.encoding;
    }

    private void ensureCapacity(int requestedLength) {
        if (requestedLength > this.buffer.length) {

            byte[] oldBuf = this.buffer;
            this.buffer = new byte[oldBuf.length * 2];
            System.arraycopy(oldBuf, 0, this.buffer, 0, oldBuf.length);
        }
    }
}
