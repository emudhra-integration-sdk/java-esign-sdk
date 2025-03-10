package esign.text.pdf;

import esign.text.DocWriter;

public class StringUtils {

    private static final byte[] r = DocWriter.getISOBytes("\\r");
    private static final byte[] n = DocWriter.getISOBytes("\\n");
    private static final byte[] t = DocWriter.getISOBytes("\\t");
    private static final byte[] b = DocWriter.getISOBytes("\\b");
    private static final byte[] f = DocWriter.getISOBytes("\\f");

    public static byte[] escapeString(byte[] bytes) {
        ByteBuffer content = new ByteBuffer();
        escapeString(bytes, content);
        return content.toByteArray();
    }

    public static void escapeString(byte[] bytes, ByteBuffer content) {
        content.append_i(40);
        for (int k = 0; k < bytes.length; k++) {
            byte c = bytes[k];
            switch (c) {
                case 13:
                    content.append(r);
                    break;
                case 10:
                    content.append(n);
                    break;
                case 9:
                    content.append(t);
                    break;
                case 8:
                    content.append(b);
                    break;
                case 12:
                    content.append(f);
                    break;
                case 40:
                case 41:
                case 92:
                    content.append_i(92).append_i(c);
                    break;
                default:
                    content.append_i(c);
                    break;
            }
        }
        content.append_i(41);
    }

    public static byte[] convertCharsToBytes(char[] chars) {
        byte[] result = new byte[chars.length * 2];
        for (int i = 0; i < chars.length; i++) {
            result[2 * i] = (byte) (chars[i] / 256);
            result[2 * i + 1] = (byte) (chars[i] % 256);
        }
        return result;
    }
}
