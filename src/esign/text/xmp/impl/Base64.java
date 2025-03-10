package esign.text.xmp.impl;

public class Base64 {

    private static final byte INVALID = -1;
    private static final byte WHITESPACE = -2;
    private static final byte EQUAL = -3;
    private static byte[] base64 = new byte[]{65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47};

    private static byte[] ascii = new byte[255];

    static {
        int idx;
        for (idx = 0; idx < 255; idx++) {
            ascii[idx] = -1;
        }

        for (idx = 0; idx < base64.length; idx++) {
            ascii[base64[idx]] = (byte) idx;
        }

        ascii[9] = -2;
        ascii[10] = -2;
        ascii[13] = -2;
        ascii[32] = -2;

        ascii[61] = -3;
    }

    public static final byte[] encode(byte[] src) {
        return encode(src, 0);
    }

    public static final byte[] encode(byte[] src, int lineFeed) {
        lineFeed = lineFeed / 4 * 4;
        if (lineFeed < 0) {
            lineFeed = 0;
        }

        int codeLength = (src.length + 2) / 3 * 4;
        if (lineFeed > 0) {
            codeLength += (codeLength - 1) / lineFeed;
        }

        byte[] dst = new byte[codeLength];

        int didx = 0;
        int sidx = 0;
        int lf = 0;
        while (sidx + 3 <= src.length) {

            int bits24 = (src[sidx++] & 0xFF) << 16;
            bits24 |= (src[sidx++] & 0xFF) << 8;
            bits24 |= (src[sidx++] & 0xFF) << 0;
            int bits6 = (bits24 & 0xFC0000) >> 18;
            dst[didx++] = base64[bits6];
            bits6 = (bits24 & 0x3F000) >> 12;
            dst[didx++] = base64[bits6];
            bits6 = (bits24 & 0xFC0) >> 6;
            dst[didx++] = base64[bits6];
            bits6 = bits24 & 0x3F;
            dst[didx++] = base64[bits6];

            lf += 4;
            if (didx < codeLength && lineFeed > 0 && lf % lineFeed == 0) {
                dst[didx++] = 10;
            }
        }
        if (src.length - sidx == 2) {

            int bits24 = (src[sidx] & 0xFF) << 16;
            bits24 |= (src[sidx + 1] & 0xFF) << 8;
            int bits6 = (bits24 & 0xFC0000) >> 18;
            dst[didx++] = base64[bits6];
            bits6 = (bits24 & 0x3F000) >> 12;
            dst[didx++] = base64[bits6];
            bits6 = (bits24 & 0xFC0) >> 6;
            dst[didx++] = base64[bits6];
            dst[didx++] = 61;
        } else if (src.length - sidx == 1) {

            int bits24 = (src[sidx] & 0xFF) << 16;
            int bits6 = (bits24 & 0xFC0000) >> 18;
            dst[didx++] = base64[bits6];
            bits6 = (bits24 & 0x3F000) >> 12;
            dst[didx++] = base64[bits6];
            dst[didx++] = 61;
            dst[didx++] = 61;
        }
        return dst;
    }

    public static final String encode(String src) {
        return new String(encode(src.getBytes()));
    }

    public static final byte[] decode(byte[] src) throws IllegalArgumentException {
        int srcLen = 0;
        int sidx;
        for (sidx = 0; sidx < src.length; sidx++) {

            byte val = ascii[src[sidx]];
            if (val >= 0) {

                src[srcLen++] = val;
            } else if (val == -1) {

                throw new IllegalArgumentException("Invalid base 64 string");
            }
        }

        while (srcLen > 0 && src[srcLen - 1] == -3) {
            srcLen--;
        }
        byte[] dst = new byte[srcLen * 3 / 4];

        int didx;

        for (sidx = 0, didx = 0; didx < dst.length - 2; sidx += 4, didx += 3) {

            dst[didx] = (byte) (src[sidx] << 2 & 0xFF | src[sidx + 1] >>> 4 & 0x3);

            dst[didx + 1] = (byte) (src[sidx + 1] << 4 & 0xFF | src[sidx + 2] >>> 2 & 0xF);

            dst[didx + 2] = (byte) (src[sidx + 2] << 6 & 0xFF | src[sidx + 3] & 0x3F);
        }

        if (didx < dst.length) {
            dst[didx] = (byte) (src[sidx] << 2 & 0xFF | src[sidx + 1] >>> 4 & 0x3);
        }

        if (++didx < dst.length) {
            dst[didx] = (byte) (src[sidx + 1] << 4 & 0xFF | src[sidx + 2] >>> 2 & 0xF);
        }

        return dst;
    }

    public static final String decode(String src) {
        return new String(decode(src.getBytes()));
    }
}
