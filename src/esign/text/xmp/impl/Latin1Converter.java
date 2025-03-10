package esign.text.xmp.impl;

import java.io.UnsupportedEncodingException;

public class Latin1Converter {

    private static final int STATE_START = 0;
    private static final int STATE_UTF8CHAR = 11;

    public static ByteBuffer convert(ByteBuffer buffer) {
        if ("UTF-8".equals(buffer.getEncoding())) {

            byte[] readAheadBuffer = new byte[8];

            int readAhead = 0;

            int expectedBytes = 0;

            ByteBuffer out = new ByteBuffer(buffer.length() * 4 / 3);

            int state = 0;
            for (int i = 0; i < buffer.length(); i++) {
                byte[] utf8;
                int b = buffer.charAt(i);

                switch (state) {

                    default:
                        if (b < 127) {

                            out.append((byte) b);
                            break;
                        }
                        if (b >= 192) {

                            expectedBytes = -1;
                            int test = b;
                            for (; expectedBytes < 8 && (test & 0x80) == 128; test <<= 1) {
                                expectedBytes++;
                            }
                            readAheadBuffer[readAhead++] = (byte) b;
                            state = 11;

                            break;
                        }

                        utf8 = convertToUTF8((byte) b);
                        out.append(utf8);
                        break;

                    case 11:
                        if (expectedBytes > 0 && (b & 0xC0) == 128) {

                            readAheadBuffer[readAhead++] = (byte) b;
                            expectedBytes--;

                            if (expectedBytes == 0) {

                                out.append(readAheadBuffer, 0, readAhead);
                                readAhead = 0;

                                state = 0;
                            }

                            break;
                        }

                        utf8 = convertToUTF8(readAheadBuffer[0]);
                        out.append(utf8);

                        i -= readAhead;
                        readAhead = 0;

                        state = 0;
                        break;
                }

            }
            if (state == 11) {
                for (int j = 0; j < readAhead; j++) {

                    byte b = readAheadBuffer[j];
                    byte[] utf8 = convertToUTF8(b);
                    out.append(utf8);
                }
            }

            return out;
        }

        return buffer;
    }

    private static byte[] convertToUTF8(byte ch) {
        int c = ch & 0xFF;

        try {
            if (c >= 128) {
                if (c == 129 || c == 141 || c == 143 || c == 144 || c == 157) {
                    return new byte[]{32};
                }

                return (new String(new byte[]{ch}, "cp1252")).getBytes("UTF-8");
            }

        } catch (UnsupportedEncodingException unsupportedEncodingException) {
        }

        return new byte[]{ch};
    }
}
