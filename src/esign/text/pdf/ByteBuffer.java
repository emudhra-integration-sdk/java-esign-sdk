package esign.text.pdf;

import esign.text.DocWriter;
import esign.text.error_messages.MessageLocalization;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ByteBuffer
        extends OutputStream {

    protected int count;
    protected byte[] buf;
    private static int byteCacheSize = 0;

    private static byte[][] byteCache = new byte[byteCacheSize][];
    public static final byte ZERO = 48;
    private static final char[] chars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static final byte[] bytes = new byte[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 97, 98, 99, 100, 101, 102};

    public static boolean HIGH_PRECISION = false;

    private static final DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);

    public ByteBuffer() {
        this(128);
    }

    public ByteBuffer(int size) {
        if (size < 1) {
            size = 128;
        }
        this.buf = new byte[size];
    }

    public static void setCacheSize(int size) {
        if (size > 3276700) {
            size = 3276700;
        }
        if (size <= byteCacheSize) {
            return;
        }
        byte[][] tmpCache = new byte[size][];
        System.arraycopy(byteCache, 0, tmpCache, 0, byteCacheSize);
        byteCache = tmpCache;
        byteCacheSize = size;
    }

    public static void fillCache(int decimals) {
        int step = 1;
        switch (decimals) {
            case 0:
                step = 100;
                break;
            case 1:
                step = 10;
                break;
        }
        for (int i = 1; i < byteCacheSize; i += step) {
            if (byteCache[i] == null) {
                byteCache[i] = convertToBytes(i);
            }
        }
    }

    private static byte[] convertToBytes(int i) {
        int size = (int) Math.floor(Math.log(i) / Math.log(10.0D));
        if (i % 100 != 0) {
            size += 2;
        }
        if (i % 10 != 0) {
            size++;
        }
        if (i < 100) {
            size++;
            if (i < 10) {
                size++;
            }
        }
        size--;
        byte[] cache = new byte[size];
        size--;
        if (i < 100) {
            cache[0] = 48;
        }
        if (i % 10 != 0) {
            cache[size--] = bytes[i % 10];
        }
        if (i % 100 != 0) {
            cache[size--] = bytes[i / 10 % 10];
            cache[size--] = 46;
        }
        size = (int) Math.floor(Math.log(i) / Math.log(10.0D)) - 1;
        int add = 0;
        while (add < size) {
            cache[add] = bytes[i / (int) Math.pow(10.0D, (size - add + 1)) % 10];
            add++;
        }
        return cache;
    }

    public ByteBuffer append_i(int b) {
        int newcount = this.count + 1;
        if (newcount > this.buf.length) {
            byte[] newbuf = new byte[Math.max(this.buf.length << 1, newcount)];
            System.arraycopy(this.buf, 0, newbuf, 0, this.count);
            this.buf = newbuf;
        }
        this.buf[this.count] = (byte) b;
        this.count = newcount;
        return this;
    }

    public ByteBuffer append(byte[] b, int off, int len) {
        if (off < 0 || off > b.length || len < 0 || off + len > b.length || off + len < 0 || len == 0) {
            return this;
        }
        int newcount = this.count + len;
        if (newcount > this.buf.length) {
            byte[] newbuf = new byte[Math.max(this.buf.length << 1, newcount)];
            System.arraycopy(this.buf, 0, newbuf, 0, this.count);
            this.buf = newbuf;
        }
        System.arraycopy(b, off, this.buf, this.count, len);
        this.count = newcount;
        return this;
    }

    public ByteBuffer append(byte[] b) {
        return append(b, 0, b.length);
    }

    public ByteBuffer append(String str) {
        if (str != null) {
            return append(DocWriter.getISOBytes(str));
        }
        return this;
    }

    public ByteBuffer append(char c) {
        return append_i(c);
    }

    public ByteBuffer append(ByteBuffer buf) {
        return append(buf.buf, 0, buf.count);
    }

//    public ByteBuffer append(int i) {
//        return append(i);
//    }

    public ByteBuffer append(long i) {
        return append(Long.toString(i));
    }

    public ByteBuffer append(byte b) {
        return append_i(b);
    }

    public ByteBuffer appendHex(byte b) {
        append(bytes[b >> 4 & 0xF]);
        return append(bytes[b & 0xF]);
    }

//    public ByteBuffer append(float i) {
//        return append(i);
//    }
    public ByteBuffer append(double d) {
        append(formatDouble(d, this));
        return this;
    }

    public static String formatDouble(double d) {
        return formatDouble(d, null);
    }

    public static String formatDouble(double d, ByteBuffer buf) {
        if (HIGH_PRECISION) {
            DecimalFormat dn = new DecimalFormat("0.######", dfs);
            String sform = dn.format(d);
            if (buf == null) {
                return sform;
            }
            buf.append(sform);
            return null;
        }

        boolean negative = false;
        if (Math.abs(d) < 1.5E-5D) {
            if (buf != null) {
                buf.append((byte) 48);
                return null;
            }
            return "0";
        }

        if (d < 0.0D) {
            negative = true;
            d = -d;
        }
        if (d < 1.0D) {
            d += 5.0E-6D;
            if (d >= 1.0D) {
                if (negative) {
                    if (buf != null) {
                        buf.append((byte) 45);
                        buf.append((byte) 49);
                        return null;
                    }
                    return "-1";
                }

                if (buf != null) {
                    buf.append((byte) 49);
                    return null;
                }
                return "1";
            }

            if (buf != null) {
                int j = (int) (d * 100000.0D);

                if (negative) {
                    buf.append((byte) 45);
                }
                buf.append((byte) 48);
                buf.append((byte) 46);

                buf.append((byte) (j / 10000 + 48));
                if (j % 10000 != 0) {
                    buf.append((byte) (j / 1000 % 10 + 48));
                    if (j % 1000 != 0) {
                        buf.append((byte) (j / 100 % 10 + 48));
                        if (j % 100 != 0) {
                            buf.append((byte) (j / 10 % 10 + 48));
                            if (j % 10 != 0) {
                                buf.append((byte) (j % 10 + 48));
                            }
                        }
                    }
                }
                return null;
            }
            int x = 100000;
            int i = (int) (d * x);

            StringBuilder res = new StringBuilder();
            if (negative) {
                res.append('-');
            }
            res.append("0.");

            while (i < x / 10) {
                res.append('0');
                x /= 10;
            }
            res.append(i);
            int cut = res.length() - 1;
            while (res.charAt(cut) == '0') {
                cut--;
            }
            res.setLength(cut + 1);
            return res.toString();
        }
        if (d <= 32767.0D) {
            d += 0.005D;
            int i = (int) (d * 100.0D);

            if (i < byteCacheSize && byteCache[i] != null) {
                if (buf != null) {
                    if (negative) {
                        buf.append((byte) 45);
                    }
                    buf.append(byteCache[i]);
                    return null;
                }
                String tmp = PdfEncodings.convertToString(byteCache[i], null);
                if (negative) {
                    tmp = "-" + tmp;
                }
                return tmp;
            }

            if (buf != null) {
                if (i < byteCacheSize) {

                    int size = 0;
                    if (i >= 1000000) {

                        size += 5;
                    } else if (i >= 100000) {

                        size += 4;
                    } else if (i >= 10000) {

                        size += 3;
                    } else if (i >= 1000) {

                        size += 2;
                    } else if (i >= 100) {

                        size++;
                    }

                    if (i % 100 != 0) {
                        size += 2;
                    }
                    if (i % 10 != 0) {
                        size++;
                    }
                    byte[] cache = new byte[size];
                    int add = 0;
                    if (i >= 1000000) {
                        cache[add++] = bytes[i / 1000000];
                    }
                    if (i >= 100000) {
                        cache[add++] = bytes[i / 100000 % 10];
                    }
                    if (i >= 10000) {
                        cache[add++] = bytes[i / 10000 % 10];
                    }
                    if (i >= 1000) {
                        cache[add++] = bytes[i / 1000 % 10];
                    }
                    if (i >= 100) {
                        cache[add++] = bytes[i / 100 % 10];
                    }

                    if (i % 100 != 0) {
                        cache[add++] = 46;
                        cache[add++] = bytes[i / 10 % 10];
                        if (i % 10 != 0) {
                            cache[add++] = bytes[i % 10];
                        }
                    }
                    byteCache[i] = cache;
                }

                if (negative) {
                    buf.append((byte) 45);
                }
                if (i >= 1000000) {
                    buf.append(bytes[i / 1000000]);
                }
                if (i >= 100000) {
                    buf.append(bytes[i / 100000 % 10]);
                }
                if (i >= 10000) {
                    buf.append(bytes[i / 10000 % 10]);
                }
                if (i >= 1000) {
                    buf.append(bytes[i / 1000 % 10]);
                }
                if (i >= 100) {
                    buf.append(bytes[i / 100 % 10]);
                }

                if (i % 100 != 0) {
                    buf.append((byte) 46);
                    buf.append(bytes[i / 10 % 10]);
                    if (i % 10 != 0) {
                        buf.append(bytes[i % 10]);
                    }
                }
                return null;
            }
            StringBuilder res = new StringBuilder();
            if (negative) {
                res.append('-');
            }
            if (i >= 1000000) {
                res.append(chars[i / 1000000]);
            }
            if (i >= 100000) {
                res.append(chars[i / 100000 % 10]);
            }
            if (i >= 10000) {
                res.append(chars[i / 10000 % 10]);
            }
            if (i >= 1000) {
                res.append(chars[i / 1000 % 10]);
            }
            if (i >= 100) {
                res.append(chars[i / 100 % 10]);
            }

            if (i % 100 != 0) {
                res.append('.');
                res.append(chars[i / 10 % 10]);
                if (i % 10 != 0) {
                    res.append(chars[i % 10]);
                }
            }
            return res.toString();
        }

        d += 0.5D;
        long v = (long) d;
        if (negative) {
            return "-" + Long.toString(v);
        }
        return Long.toString(v);
    }

    public void reset() {
        this.count = 0;
    }

    public byte[] toByteArray() {
        byte[] newbuf = new byte[this.count];
        System.arraycopy(this.buf, 0, newbuf, 0, this.count);
        return newbuf;
    }

    public int size() {
        return this.count;
    }

    public void setSize(int size) {
        if (size > this.count || size < 0) {
            throw new IndexOutOfBoundsException(MessageLocalization.getComposedMessage("the.new.size.must.be.positive.and.lt.eq.of.the.current.size", new Object[0]));
        }
        this.count = size;
    }

    public String toString() {
        return new String(this.buf, 0, this.count);
    }

    public String toString(String enc) throws UnsupportedEncodingException {
        return new String(this.buf, 0, this.count, enc);
    }

    public void writeTo(OutputStream out) throws IOException {
        out.write(this.buf, 0, this.count);
    }

    public void write(int b) throws IOException {
        append((byte) b);
    }

    public void write(byte[] b, int off, int len) {
        append(b, off, len);
    }

    public byte[] getBuffer() {
        return this.buf;
    }
}
