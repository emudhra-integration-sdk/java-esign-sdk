package esign.text.html;

import esign.text.BaseColor;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

@Deprecated
public class HtmlUtilities {

    public static final float DEFAULT_FONT_SIZE = 12.0F;
    private static HashMap<String, Float> sizes = new HashMap<String, Float>();

    static {
        sizes.put("xx-small", new Float(4.0F));
        sizes.put("x-small", new Float(6.0F));
        sizes.put("small", new Float(8.0F));
        sizes.put("medium", new Float(10.0F));
        sizes.put("large", new Float(13.0F));
        sizes.put("x-large", new Float(18.0F));
        sizes.put("xx-large", new Float(26.0F));
    }

    public static float parseLength(String string) {
        return parseLength(string, 12.0F);
    }

    public static float parseLength(String string, float actualFontSize) {
        if (string == null) {
            return 0.0F;
        }
        Float fl = sizes.get(string.toLowerCase());
        if (fl != null) {
            return fl.floatValue();
        }
        int pos = 0;
        int length = string.length();
        boolean ok = true;
        while (ok && pos < length) {
            switch (string.charAt(pos)) {
                case '+':
                case '-':
                case '.':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    pos++;
                    continue;
            }
            ok = false;
        }

        if (pos == 0) {
            return 0.0F;
        }
        if (pos == length) {
            return Float.parseFloat(string + "f");
        }
        float f = Float.parseFloat(string.substring(0, pos) + "f");
        string = string.substring(pos);

        if (string.startsWith("in")) {
            return f * 72.0F;
        }

        if (string.startsWith("cm")) {
            return f / 2.54F * 72.0F;
        }

        if (string.startsWith("mm")) {
            return f / 25.4F * 72.0F;
        }

        if (string.startsWith("pc")) {
            return f * 12.0F;
        }

        if (string.startsWith("em")) {
            return f * actualFontSize;
        }

        if (string.startsWith("ex")) {
            return f * actualFontSize / 2.0F;
        }

        return f;
    }

    public static BaseColor decodeColor(String s) {
        if (s == null) {
            return null;
        }
        s = s.toLowerCase().trim();
        try {
            return WebColors.getRGBColor(s);
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    public static Properties parseAttributes(String string) {
        Properties result = new Properties();
        if (string == null) {
            return result;
        }
        StringTokenizer keyValuePairs = new StringTokenizer(string, ";");

        while (keyValuePairs.hasMoreTokens()) {
            StringTokenizer keyValuePair = new StringTokenizer(keyValuePairs.nextToken(), ":");
            if (keyValuePair.hasMoreTokens()) {
                String key = keyValuePair.nextToken().trim();

                if (keyValuePair.hasMoreTokens()) {
                    String value = keyValuePair.nextToken().trim();

                    if (value.startsWith("\"")) {
                        value = value.substring(1);
                    }
                    if (value.endsWith("\"")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    result.setProperty(key.toLowerCase(), value);
                }
            }
        }
        return result;
    }

    public static String removeComment(String string, String startComment, String endComment) {
        StringBuffer result = new StringBuffer();
        int pos = 0;
        int end = endComment.length();
        int start = string.indexOf(startComment, pos);
        while (start > -1) {
            result.append(string.substring(pos, start));
            pos = string.indexOf(endComment, start) + end;
            start = string.indexOf(startComment, pos);
        }
        result.append(string.substring(pos));
        return result.toString();
    }

    public static String eliminateWhiteSpace(String content) {
        StringBuffer buf = new StringBuffer();
        int len = content.length();

        boolean newline = false;
        for (int i = 0; i < len; i++) {
            char character;
            switch (character = content.charAt(i)) {
                case ' ':
                    if (!newline) {
                        buf.append(character);
                    }
                    break;
                case '\n':
                    if (i > 0) {
                        newline = true;
                        buf.append(' ');
                    }
                    break;

                case '\r':
                case '\t':
                    break;
                default:
                    newline = false;
                    buf.append(character);
                    break;
            }
        }
        return buf.toString();
    }

    public static final int[] FONTSIZES = new int[]{8, 10, 12, 14, 18, 24, 36};

    public static int getIndexedFontSize(String value, String previous) {
        int sIndex = 0;

        if (value.startsWith("+") || value.startsWith("-")) {

            if (previous == null) {
                previous = "12";
            }
            int c = (int) Float.parseFloat(previous);

            for (int k = FONTSIZES.length - 1; k >= 0; k--) {
                if (c >= FONTSIZES[k]) {
                    sIndex = k;

                    break;
                }
            }

            int diff = Integer.parseInt(value.startsWith("+") ? value
                    .substring(1) : value);

            sIndex += diff;
        } else {

            try {
                sIndex = Integer.parseInt(value) - 1;
            } catch (NumberFormatException nfe) {
                sIndex = 0;
            }
        }
        if (sIndex < 0) {
            sIndex = 0;
        } else if (sIndex >= FONTSIZES.length) {
            sIndex = FONTSIZES.length - 1;
        }
        return FONTSIZES[sIndex];
    }

    public static int alignmentValue(String alignment) {
        if (alignment == null) {
            return -1;
        }
        if ("center".equalsIgnoreCase(alignment)) {
            return 1;
        }
        if ("left".equalsIgnoreCase(alignment)) {
            return 0;
        }
        if ("right".equalsIgnoreCase(alignment)) {
            return 2;
        }
        if ("justify".equalsIgnoreCase(alignment)) {
            return 3;
        }
        if ("JustifyAll".equalsIgnoreCase(alignment)) {
            return 8;
        }
        if ("top".equalsIgnoreCase(alignment)) {
            return 4;
        }
        if ("middle".equalsIgnoreCase(alignment)) {
            return 5;
        }
        if ("bottom".equalsIgnoreCase(alignment)) {
            return 6;
        }
        if ("baseline".equalsIgnoreCase(alignment)) {
            return 7;
        }
        return -1;
    }
}
