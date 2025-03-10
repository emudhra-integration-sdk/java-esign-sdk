package esign.text.html;

import esign.text.BaseColor;
import java.util.HashSet;
import java.util.Set;

@Deprecated
public final class HtmlEncoder {

    private static final String[] HTML_CODE = new String[256];

    static {
        int i;
        for (i = 0; i < 10; i++) {
            HTML_CODE[i] = "&#00" + i + ";";
        }

        for (i = 10; i < 32; i++) {
            HTML_CODE[i] = "&#0" + i + ";";
        }

        for (i = 32; i < 128; i++) {
            HTML_CODE[i] = String.valueOf((char) i);
        }

        HTML_CODE[9] = "\t";
        HTML_CODE[10] = "<br />\n";
        HTML_CODE[34] = "&quot;";
        HTML_CODE[38] = "&amp;";
        HTML_CODE[60] = "&lt;";
        HTML_CODE[62] = "&gt;";

        for (i = 128; i < 256; i++) {
            HTML_CODE[i] = "&#" + i + ";";
        }
    }

    public static String encode(String string) {
        int n = string.length();

        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < n; i++) {
            char character = string.charAt(i);

            if (character < 'Ā') {
                buffer.append(HTML_CODE[character]);
            } else {

                buffer.append("&#").append(character).append(';');
            }
        }
        return buffer.toString();
    }

    public static String encode(BaseColor color) {
        StringBuffer buffer = new StringBuffer("#");
        if (color.getRed() < 16) {
            buffer.append('0');
        }
        buffer.append(Integer.toString(color.getRed(), 16));
        if (color.getGreen() < 16) {
            buffer.append('0');
        }
        buffer.append(Integer.toString(color.getGreen(), 16));
        if (color.getBlue() < 16) {
            buffer.append('0');
        }
        buffer.append(Integer.toString(color.getBlue(), 16));
        return buffer.toString();
    }

    public static String getAlignment(int alignment) {
        switch (alignment) {
            case 0:
                return "left";
            case 1:
                return "center";
            case 2:
                return "right";
            case 3:
            case 8:
                return "justify";
            case 4:
                return "top";
            case 5:
                return "middle";
            case 6:
                return "bottom";
            case 7:
                return "baseline";
        }
        return "";
    }

    private static final Set<String> NEWLINETAGS = new HashSet<String>();

    static {
        NEWLINETAGS.add("p");
        NEWLINETAGS.add("blockquote");
        NEWLINETAGS.add("br");
    }

    public static boolean isNewLineTag(String tag) {
        return NEWLINETAGS.contains(tag);
    }
}
