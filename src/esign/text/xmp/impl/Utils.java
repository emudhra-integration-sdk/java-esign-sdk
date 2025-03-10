package esign.text.xmp.impl;

import esign.text.xmp.XMPConst;

public class Utils
        implements XMPConst {

    public static final int UUID_SEGMENT_COUNT = 4;
    public static final int UUID_LENGTH = 36;
    private static boolean[] xmlNameStartChars;
    private static boolean[] xmlNameChars;

    static {
        initCharTables();
    }

    public static String normalizeLangValue(String value) {
        if ("x-default".equals(value)) {
            return value;
        }

        int subTag = 1;
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < value.length(); i++) {

            switch (value.charAt(i)) {

                case '-':
                case '_':
                    buffer.append('-');
                    subTag++;
                    break;

                case ' ':
                    break;

                default:
                    if (subTag != 2) {

                        buffer.append(Character.toLowerCase(value.charAt(i)));

                        break;
                    }
                    buffer.append(Character.toUpperCase(value.charAt(i)));
                    break;
            }

        }
        return buffer.toString();
    }

    static String[] splitNameAndValue(String selector) {
        int eq = selector.indexOf('=');
        int pos = 1;
        if (selector.charAt(pos) == '?') {
            pos++;
        }
        String name = selector.substring(pos, eq);

        pos = eq + 1;
        char quote = selector.charAt(pos);
        pos++;
        int end = selector.length() - 2;
        StringBuffer value = new StringBuffer(end - eq);
        while (pos < end) {

            value.append(selector.charAt(pos));
            pos++;
            if (selector.charAt(pos) == quote) {

                pos++;
            }
        }
        return new String[]{name, value.toString()};
    }

    static boolean isInternalProperty(String schema, String prop) {
        boolean isInternal = false;

        if ("http://purl.org/dc/elements/1.1/".equals(schema)) {

            if ("dc:format".equals(prop) || "dc:language".equals(prop)) {
                isInternal = true;
            }
        } else if ("http://ns.adobe.com/xap/1.0/".equals(schema)) {

            if ("xmp:BaseURL".equals(prop) || "xmp:CreatorTool".equals(prop) || "xmp:Format"
                    .equals(prop) || "xmp:Locale".equals(prop) || "xmp:MetadataDate"
                    .equals(prop) || "xmp:ModifyDate".equals(prop)) {
                isInternal = true;
            }
        } else if ("http://ns.adobe.com/pdf/1.3/".equals(schema)) {

            if ("pdf:BaseURL".equals(prop) || "pdf:Creator".equals(prop) || "pdf:ModDate"
                    .equals(prop) || "pdf:PDFVersion".equals(prop) || "pdf:Producer"
                    .equals(prop)) {
                isInternal = true;
            }
        } else if ("http://ns.adobe.com/tiff/1.0/".equals(schema)) {

            isInternal = true;
            if ("tiff:ImageDescription".equals(prop) || "tiff:Artist".equals(prop) || "tiff:Copyright"
                    .equals(prop)) {
                isInternal = false;
            }
        } else if ("http://ns.adobe.com/exif/1.0/".equals(schema)) {

            isInternal = true;
            if ("exif:UserComment".equals(prop)) {
                isInternal = false;
            }
        } else if ("http://ns.adobe.com/exif/1.0/aux/".equals(schema)) {

            isInternal = true;
        } else if ("http://ns.adobe.com/photoshop/1.0/".equals(schema)) {

            if ("photoshop:ICCProfile".equals(prop)) {
                isInternal = true;
            }
        } else if ("http://ns.adobe.com/camera-raw-settings/1.0/".equals(schema)) {

            if ("crs:Version".equals(prop) || "crs:RawFileName".equals(prop) || "crs:ToneCurveName"
                    .equals(prop)) {
                isInternal = true;
            }
        } else if ("http://ns.adobe.com/StockPhoto/1.0/".equals(schema)) {

            isInternal = true;
        } else if ("http://ns.adobe.com/xap/1.0/mm/".equals(schema)) {

            isInternal = true;
        } else if ("http://ns.adobe.com/xap/1.0/t/".equals(schema)) {

            isInternal = true;
        } else if ("http://ns.adobe.com/xap/1.0/t/pg/".equals(schema)) {

            isInternal = true;
        } else if ("http://ns.adobe.com/xap/1.0/g/".equals(schema)) {

            isInternal = true;
        } else if ("http://ns.adobe.com/xap/1.0/g/img/".equals(schema)) {

            isInternal = true;
        } else if ("http://ns.adobe.com/xap/1.0/sType/Font#".equals(schema)) {

            isInternal = true;
        }

        return isInternal;
    }

    static boolean checkUUIDFormat(String uuid) {
        boolean result = true;
        int delimCnt = 0;
        int delimPos = 0;

        if (uuid == null) {
            return false;
        }

        for (delimPos = 0; delimPos < uuid.length(); delimPos++) {

            if (uuid.charAt(delimPos) == '-') {

                delimCnt++;
                result = (result && (delimPos == 8 || delimPos == 13 || delimPos == 18 || delimPos == 23));
            }
        }

        return (result && 4 == delimCnt && 36 == delimPos);
    }

    public static boolean isXMLName(String name) {
        if (name.length() > 0 && !isNameStartChar(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {

            if (!isNameChar(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isXMLNameNS(String name) {
        if (name.length() > 0 && (!isNameStartChar(name.charAt(0)) || name.charAt(0) == ':')) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {

            if (!isNameChar(name.charAt(i)) || name.charAt(i) == ':') {
                return false;
            }
        }
        return true;
    }

    static boolean isControlChar(char c) {
        return ((c <= '\037' || c == '') && c != '\t' && c != '\n' && c != '\r');
    }

    public static String escapeXML(String value, boolean forAttribute, boolean escapeWhitespaces) {
        boolean needsEscaping = false;
        for (int i = 0; i < value.length(); i++) {

            char c = value.charAt(i);
            if (c == '<' || c == '>' || c == '&' || (escapeWhitespaces && (c == '\t' || c == '\n' || c == '\r')) || (forAttribute && c == '"')) {

                needsEscaping = true;

                break;
            }
        }
        if (!needsEscaping) {

            return value;
        }

        StringBuffer buffer = new StringBuffer(value.length() * 4 / 3);
        for (int j = 0; j < value.length(); j++) {

            char c = value.charAt(j);
            if (!escapeWhitespaces || (c != '\t' && c != '\n' && c != '\r')) {

                switch (c) {

                    case '<':
                        buffer.append("&lt;");
                        break;
                    case '>':
                        buffer.append("&gt;");
                        break;
                    case '&':
                        buffer.append("&amp;");
                        break;
                    case '"':
                        buffer.append(forAttribute ? "&quot;" : "\"");
                        break;
                    default:
                        buffer.append(c);
                        break;
                }

            } else {
                buffer.append("&#x");
                buffer.append(Integer.toHexString(c).toUpperCase());
                buffer.append(';');
            }
        }
        return buffer.toString();
    }

    static String removeControlChars(String value) {
        StringBuffer buffer = new StringBuffer(value);
        for (int i = 0; i < buffer.length(); i++) {

            if (isControlChar(buffer.charAt(i))) {
                buffer.setCharAt(i, ' ');
            }
        }
        return buffer.toString();
    }

    private static boolean isNameStartChar(char ch) {
        return ((ch <= 'ÿ' && xmlNameStartChars[ch]) || (ch >= 'Ā' && ch <= '˿') || (ch >= 'Ͱ' && ch <= 'ͽ') || (ch >= 'Ϳ' && ch <= '῿') || (ch >= '‌' && ch <= '‍') || (ch >= '⁰' && ch <= '↏') || (ch >= 'Ⰰ' && ch <= '⿯') || (ch >= '、' && ch <= '퟿') || (ch >= '豈' && ch <= '﷏') || (ch >= 'ﷰ' && ch <= '�') || (ch >= 65536 && ch <= 983039));
    }

    private static boolean isNameChar(char ch) {
        return ((ch <= 'ÿ' && xmlNameChars[ch])
                || isNameStartChar(ch) || (ch >= '̀' && ch <= 'ͯ') || (ch >= '‿' && ch <= '⁀'));
    }

    private static void initCharTables() {
        xmlNameChars = new boolean[256];
        xmlNameStartChars = new boolean[256];

        for (char ch = Character.MIN_VALUE; ch < xmlNameChars.length; ch = (char) (ch + 1)) {

            xmlNameStartChars[ch] = (ch == ':' || ('A' <= ch && ch <= 'Z') || ch == '_' || ('a' <= ch && ch <= 'z') || ('À' <= ch && ch <= 'Ö') || ('Ø' <= ch && ch <= 'ö') || ('ø' <= ch && ch <= 'ÿ'));

            xmlNameChars[ch] = (xmlNameStartChars[ch] || ch == '-' || ch == '.' || ('0' <= ch && ch <= '9') || ch == '·');
        }
    }
}
