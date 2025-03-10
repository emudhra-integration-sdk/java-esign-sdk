package esign.text.xml;

public class XMLUtil {

    public static String escapeXML(String s, boolean onlyASCII) {
        char[] cc = s.toCharArray();
        int len = cc.length;
        StringBuffer sb = new StringBuffer();
        for (int k = 0; k < len; k++) {
            int c = cc[k];
            switch (c) {
                case 60:
                    sb.append("&lt;");
                    break;
                case 62:
                    sb.append("&gt;");
                    break;
                case 38:
                    sb.append("&amp;");
                    break;
                case 34:
                    sb.append("&quot;");
                    break;
                case 39:
                    sb.append("&apos;");
                    break;
                default:
                    if (isValidCharacterValue(c)) {
                        if (onlyASCII && c > 127) {
                            sb.append("&#").append(c).append(';');
                            break;
                        }
                        sb.append((char) c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    public static String unescapeXML(String s) {
        char[] cc = s.toCharArray();
        int len = cc.length;
        StringBuffer sb = new StringBuffer();

        int i = 0;
        while (true) {
            if (i < len) {
                int c = cc[i];
                if (c == 38) {
                    int pos = findInArray(';', cc, i + 3);
                    if (pos > -1) {
                        String esc = new String(cc, i + 1, pos - i - 1);
                        if (esc.startsWith("#")) {
                            esc = esc.substring(1);
                            if (isValidCharacterValue(esc)) {
                                c = (char) Integer.parseInt(esc);
                                i = pos;
                            } else {
                                i = pos;

                                i++;
                            }
                        } else {
                            int tmp = unescape(esc);
                            if (tmp > 0) {
                                c = tmp;
                                i = pos;
                            }
                        }
                    }
                }
                sb.append((char) c);
            } else {
                break;
            }
            i++;
        }
        return sb.toString();
    }

    public static int unescape(String s) {
        if ("apos".equals(s)) {
            return 39;
        }
        if ("quot".equals(s)) {
            return 34;
        }
        if ("lt".equals(s)) {
            return 60;
        }
        if ("gt".equals(s)) {
            return 62;
        }
        if ("amp".equals(s)) {
            return 38;
        }
        return -1;
    }

    public static boolean isValidCharacterValue(String s) {
        try {
            int i = Integer.parseInt(s);
            return isValidCharacterValue(i);
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    public static boolean isValidCharacterValue(int c) {
        return (c == 9 || c == 10 || c == 13 || (c >= 32 && c <= 55295) || (c >= 57344 && c <= 65533) || (c >= 65536 && c <= 1114111));
    }

    public static int findInArray(char needle, char[] haystack, int start) {
        for (int i = start; i < haystack.length; i++) {
            if (haystack[i] == ';') {
                return i;
            }
        }
        return -1;
    }

    public static String getEncodingName(byte[] b4) {
        int b0 = b4[0] & 0xFF;
        int b1 = b4[1] & 0xFF;
        if (b0 == 254 && b1 == 255) {
            return "UTF-16BE";
        }
        if (b0 == 255 && b1 == 254) {
            return "UTF-16LE";
        }

        int b2 = b4[2] & 0xFF;
        if (b0 == 239 && b1 == 187 && b2 == 191) {
            return "UTF-8";
        }

        int b3 = b4[3] & 0xFF;
        if (b0 == 0 && b1 == 0 && b2 == 0 && b3 == 60) {
            return "ISO-10646-UCS-4";
        }
        if (b0 == 60 && b1 == 0 && b2 == 0 && b3 == 0) {
            return "ISO-10646-UCS-4";
        }
        if (b0 == 0 && b1 == 0 && b2 == 60 && b3 == 0) {

            return "ISO-10646-UCS-4";
        }
        if (b0 == 0 && b1 == 60 && b2 == 0 && b3 == 0) {

            return "ISO-10646-UCS-4";
        }
        if (b0 == 0 && b1 == 60 && b2 == 0 && b3 == 63) {

            return "UTF-16BE";
        }
        if (b0 == 60 && b1 == 0 && b2 == 63 && b3 == 0) {

            return "UTF-16LE";
        }
        if (b0 == 76 && b1 == 111 && b2 == 167 && b3 == 148) {

            return "CP037";
        }

        return "UTF-8";
    }
}
