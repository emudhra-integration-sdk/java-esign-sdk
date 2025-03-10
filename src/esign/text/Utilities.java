package esign.text;

import esign.text.pdf.ByteBuffer;
import esign.text.pdf.PRTokeniser;
import esign.text.pdf.PdfEncodings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

public class Utilities {

    @Deprecated
    public static <K, V> Set<K> getKeySet(Hashtable<K, V> table) {
        return (table == null) ? Collections.<K>emptySet() : table.keySet();
    }

    public static Object[][] addToArray(Object[][] original, Object[] item) {
        if (original == null) {
            original = new Object[1][];
            original[0] = item;
            return original;
        }
        Object[][] original2 = new Object[original.length + 1][];
        System.arraycopy(original, 0, original2, 0, original.length);
        original2[original.length] = item;
        return original2;
    }

    public static boolean checkTrueOrFalse(Properties attributes, String key) {
        return "true".equalsIgnoreCase(attributes.getProperty(key));
    }

    public static String unEscapeURL(String src) {
        StringBuffer bf = new StringBuffer();
        char[] s = src.toCharArray();
        for (int k = 0; k < s.length; k++) {
            char c = s[k];
            if (c == '%') {
                if (k + 2 >= s.length) {
                    bf.append(c);
                } else {

                    int a0 = PRTokeniser.getHex(s[k + 1]);
                    int a1 = PRTokeniser.getHex(s[k + 2]);
                    if (a0 < 0 || a1 < 0) {
                        bf.append(c);
                    } else {

                        bf.append((char) (a0 * 16 + a1));
                        k += 2;
                    }
                }
            } else {
                bf.append(c);
            }

        }
        return bf.toString();
    }

    public static URL toURL(String filename) throws MalformedURLException {
        try {
            return new URL(filename);
        } catch (Exception e) {
            return (new File(filename)).toURI().toURL();
        }
    }

    public static void skip(InputStream is, int size) throws IOException {
        while (size > 0) {
            long n = is.skip(size);
            if (n <= 0L) {
                break;
            }
            size = (int) (size - n);
        }
    }

    public static final float millimetersToPoints(float value) {
        return inchesToPoints(millimetersToInches(value));
    }

    public static final float millimetersToInches(float value) {
        return value / 25.4F;
    }

    public static final float pointsToMillimeters(float value) {
        return inchesToMillimeters(pointsToInches(value));
    }

    public static final float pointsToInches(float value) {
        return value / 72.0F;
    }

    public static final float inchesToMillimeters(float value) {
        return value * 25.4F;
    }

    public static final float inchesToPoints(float value) {
        return value * 72.0F;
    }

    public static boolean isSurrogateHigh(char c) {
//        return (c >= '?' && c <= '?');
          return c >= '\ud800' && c <= '\udbff';
    }

    public static boolean isSurrogateLow(char c) {
//        return (c >= '?' && c <= '?');
         return c >= '\udc00' && c <= '\udfff';
    }

    public static boolean isSurrogatePair(String text, int idx) {
        if (idx < 0 || idx > text.length() - 2) {
            return false;
        }
        return (isSurrogateHigh(text.charAt(idx)) && isSurrogateLow(text.charAt(idx + 1)));
    }

    public static boolean isSurrogatePair(char[] text, int idx) {
        if (idx < 0 || idx > text.length - 2) {
            return false;
        }
        return (isSurrogateHigh(text[idx]) && isSurrogateLow(text[idx + 1]));
    }

    public static int convertToUtf32(char highSurrogate, char lowSurrogate) {
        return (highSurrogate - 55296) * 1024 + lowSurrogate - 56320 + 65536;
    }

    public static int convertToUtf32(char[] text, int idx) {
        return (text[idx] - 55296) * 1024 + text[idx + 1] - 56320 + 65536;
    }

    public static int convertToUtf32(String text, int idx) {
        return (text.charAt(idx) - 55296) * 1024 + text.charAt(idx + 1) - 56320 + 65536;
    }

    public static String convertFromUtf32(int codePoint) {
        if (codePoint < 65536) {
            return Character.toString((char) codePoint);
        }
        codePoint -= 65536;
        return new String(new char[]{(char) (codePoint / 1024 + 55296), (char) (codePoint % 1024 + 56320)});
    }

    public static String readFileToString(String path) throws IOException {
        return readFileToString(new File(path));
    }

    public static String readFileToString(File file) throws IOException {
        byte[] jsBytes = new byte[(int) file.length()];
        FileInputStream f = new FileInputStream(file);
        f.read(jsBytes);
        return new String(jsBytes);
    }

    public static String convertToHex(byte[] bytes) {
        ByteBuffer buf = new ByteBuffer();
        for (byte b : bytes) {
            buf.appendHex(b);
        }
        return PdfEncodings.convertToString(buf.toByteArray(), null).toUpperCase();
    }

    public static char[] copyOfRange(char[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        char[] copy = new char[newLength];
        System.arraycopy(original, from, copy, 0,
                Math.min(original.length - from, newLength));
        return copy;
    }
}
