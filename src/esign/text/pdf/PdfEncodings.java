package esign.text.pdf;

import esign.text.ExceptionConverter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashMap;

public class PdfEncodings {

    static final char[] winansiByteToChar = new char[]{Character.MIN_VALUE, '\001', '\002', '\003', '\004', '\005', '\006', '\007', '\b', '\t', '\n', '\013', '\f', '\r', '\016', '\017', '\020', '\021', '\022', '\023', '\024', '\025', '\026', '\027', '\030', '\031', '\032', '\033', '\034', '\035', '\036', '\037', ' ', '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', '\\', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', '', '€', '�', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š', '‹', 'Œ', '�', 'Ž', '�', '�', '‘', '’', '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ', '�', 'ž', 'Ÿ', ' ', '¡', '¢', '£', '¤', '¥', '¦', '§', '¨', '©', 'ª', '«', '¬', '­', '®', '¯', '°', '±', '²', '³', '´', 'µ', '¶', '·', '¸', '¹', 'º', '»', '¼', '½', '¾', '¿', 'À', 'Á', 'Â', 'Ã', 'Ä', 'Å', 'Æ', 'Ç', 'È', 'É', 'Ê', 'Ë', 'Ì', 'Í', 'Î', 'Ï', 'Ð', 'Ñ', 'Ò', 'Ó', 'Ô', 'Õ', 'Ö', '×', 'Ø', 'Ù', 'Ú', 'Û', 'Ü', 'Ý', 'Þ', 'ß', 'à', 'á', 'â', 'ã', 'ä', 'å', 'æ', 'ç', 'è', 'é', 'ê', 'ë', 'ì', 'í', 'î', 'ï', 'ð', 'ñ', 'ò', 'ó', 'ô', 'õ', 'ö', '÷', 'ø', 'ù', 'ú', 'û', 'ü', 'ý', 'þ', 'ÿ'};

    static final char[] pdfEncodingByteToChar = new char[]{Character.MIN_VALUE, '\001', '\002', '\003', '\004', '\005', '\006', '\007', '\b', '\t', '\n', '\013', '\f', '\r', '\016', '\017', '\020', '\021', '\022', '\023', '\024', '\025', '\026', '\027', '\030', '\031', '\032', '\033', '\034', '\035', '\036', '\037', ' ', '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', '\\', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', '', '•', '†', '‡', '…', '—', '–', 'ƒ', '⁄', '‹', '›', '−', '‰', '„', '“', '”', '‘', '’', '‚', '™', 'ﬁ', 'ﬂ', 'Ł', 'Œ', 'Š', 'Ÿ', 'Ž', 'ı', 'ł', 'œ', 'š', 'ž', '�', '€', '¡', '¢', '£', '¤', '¥', '¦', '§', '¨', '©', 'ª', '«', '¬', '­', '®', '¯', '°', '±', '²', '³', '´', 'µ', '¶', '·', '¸', '¹', 'º', '»', '¼', '½', '¾', '¿', 'À', 'Á', 'Â', 'Ã', 'Ä', 'Å', 'Æ', 'Ç', 'È', 'É', 'Ê', 'Ë', 'Ì', 'Í', 'Î', 'Ï', 'Ð', 'Ñ', 'Ò', 'Ó', 'Ô', 'Õ', 'Ö', '×', 'Ø', 'Ù', 'Ú', 'Û', 'Ü', 'Ý', 'Þ', 'ß', 'à', 'á', 'â', 'ã', 'ä', 'å', 'æ', 'ç', 'è', 'é', 'ê', 'ë', 'ì', 'í', 'î', 'ï', 'ð', 'ñ', 'ò', 'ó', 'ô', 'õ', 'ö', '÷', 'ø', 'ù', 'ú', 'û', 'ü', 'ý', 'þ', 'ÿ'};

    static final IntHashtable winansi = new IntHashtable();

    static final IntHashtable pdfEncoding = new IntHashtable();

    static HashMap<String, ExtraEncoding> extraEncodings = new HashMap<String, ExtraEncoding>();

    static {
        int k;
        for (k = 128; k < 161; k++) {
            char c = winansiByteToChar[k];
            if (c != '�') {
                winansi.put(c, k);
            }
        }
        for (k = 128; k < 161; k++) {
            char c = pdfEncodingByteToChar[k];
            if (c != '�') {
                pdfEncoding.put(c, k);
            }
        }
        addExtraEncoding("Wingdings", new WingdingsConversion());
        addExtraEncoding("Symbol", new SymbolConversion(true));
        addExtraEncoding("ZapfDingbats", new SymbolConversion(false));
        addExtraEncoding("SymbolTT", new SymbolTTConversion());
        addExtraEncoding("Cp437", new Cp437Conversion());
    }

    public static final byte[] convertToBytes(String text, String encoding) {
        if (text == null) {
            return new byte[0];
        }
        if (encoding == null || encoding.length() == 0) {
            int len = text.length();
            byte[] b = new byte[len];
            for (int k = 0; k < len; k++) {
                b[k] = (byte) text.charAt(k);
            }
            return b;
        }
        ExtraEncoding extra = extraEncodings.get(encoding.toLowerCase());
        if (extra != null) {
            byte[] b = extra.charToByte(text, encoding);
            if (b != null) {
                return b;
            }
        }
        IntHashtable hash = null;
        if (encoding.equals("Cp1252")) {
            hash = winansi;
        } else if (encoding.equals("PDF")) {
            hash = pdfEncoding;
        }
        if (hash != null) {
            char[] cc = text.toCharArray();
            int len = cc.length;
            int ptr = 0;
            byte[] b = new byte[len];
            int c = 0;
            for (int k = 0; k < len; k++) {
                char char1 = cc[k];
                if (char1 < '' || (char1 > ' ' && char1 <= 'ÿ')) {
                    c = char1;
                } else {
                    c = hash.get(char1);
                }
                if (c != 0) {
                    b[ptr++] = (byte) c;
                }
            }
            if (ptr == len) {
                return b;
            }
            byte[] b2 = new byte[ptr];
            System.arraycopy(b, 0, b2, 0, ptr);
            return b2;
        }
        if (encoding.equals("UnicodeBig")) {

            char[] cc = text.toCharArray();
            int len = cc.length;
            byte[] b = new byte[cc.length * 2 + 2];
            b[0] = -2;
            b[1] = -1;
            int bptr = 2;
            for (int k = 0; k < len; k++) {
                char c = cc[k];
                b[bptr++] = (byte) (c >> 8);
                b[bptr++] = (byte) (c & 0xFF);
            }
            return b;
        }
        try {
            Charset cc = Charset.forName(encoding);
            CharsetEncoder ce = cc.newEncoder();
            ce.onUnmappableCharacter(CodingErrorAction.IGNORE);
            CharBuffer cb = CharBuffer.wrap(text.toCharArray());
            ByteBuffer bb = ce.encode(cb);
            bb.rewind();
            int lim = bb.limit();
            byte[] br = new byte[lim];
            bb.get(br);
            return br;
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    public static final byte[] convertToBytes(char char1, String encoding) {
        if (encoding == null || encoding.length() == 0) {
            return new byte[]{(byte) char1};
        }
        ExtraEncoding extra = extraEncodings.get(encoding.toLowerCase());
        if (extra != null) {
            byte[] b = extra.charToByte(char1, encoding);
            if (b != null) {
                return b;
            }
        }
        IntHashtable hash = null;
        if (encoding.equals("Cp1252")) {
            hash = winansi;
        } else if (encoding.equals("PDF")) {
            hash = pdfEncoding;
        }
        if (hash != null) {
            int c = 0;
            if (char1 < '' || (char1 > ' ' && char1 <= 'ÿ')) {
                c = char1;
            } else {
                c = hash.get(char1);
            }
            if (c != 0) {
                return new byte[]{(byte) c};
            }
            return new byte[0];
        }
        if (encoding.equals("UnicodeBig")) {

            byte[] b = new byte[4];
            b[0] = -2;
            b[1] = -1;
            b[2] = (byte) (char1 >> 8);
            b[3] = (byte) (char1 & 0xFF);
            return b;
        }
        try {
            Charset cc = Charset.forName(encoding);
            CharsetEncoder ce = cc.newEncoder();
            ce.onUnmappableCharacter(CodingErrorAction.IGNORE);
            CharBuffer cb = CharBuffer.wrap(new char[]{char1});
            ByteBuffer bb = ce.encode(cb);
            bb.rewind();
            int lim = bb.limit();
            byte[] br = new byte[lim];
            bb.get(br);
            return br;
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    public static final String convertToString(byte[] bytes, String encoding) {
        if (bytes == null) {
            return "";
        }
        if (encoding == null || encoding.length() == 0) {
            char[] c = new char[bytes.length];
            for (int k = 0; k < bytes.length; k++) {
                c[k] = (char) (bytes[k] & 0xFF);
            }
            return new String(c);
        }
        ExtraEncoding extra = extraEncodings.get(encoding.toLowerCase());
        if (extra != null) {
            String text = extra.byteToChar(bytes, encoding);
            if (text != null) {
                return text;
            }
        }
        char[] ch = null;
        if (encoding.equals("Cp1252")) {
            ch = winansiByteToChar;
        } else if (encoding.equals("PDF")) {
            ch = pdfEncodingByteToChar;
        }
        if (ch != null) {
            int len = bytes.length;
            char[] c = new char[len];
            for (int k = 0; k < len; k++) {
                c[k] = ch[bytes[k] & 0xFF];
            }
            return new String(c);
        }
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new ExceptionConverter(e);
        }
    }

    public static boolean isPdfDocEncoding(String text) {
        if (text == null) {
            return true;
        }
        int len = text.length();
        for (int k = 0; k < len; k++) {
            char char1 = text.charAt(k);
            if (char1 >= '' && (char1 <= ' ' || char1 > 'ÿ')) {
                if (!pdfEncoding.containsKey(char1)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void addExtraEncoding(String name, ExtraEncoding enc) {
        synchronized (extraEncodings) {
            HashMap<String, ExtraEncoding> newEncodings = (HashMap<String, ExtraEncoding>) extraEncodings.clone();
            newEncodings.put(name.toLowerCase(), enc);
            extraEncodings = newEncodings;
        }
    }

    private static class WingdingsConversion implements ExtraEncoding {

        private WingdingsConversion() {
        }

        public byte[] charToByte(char char1, String encoding) {
            if (char1 == ' ') {
                return new byte[]{(byte) char1};
            }
            if (char1 >= '✁' && char1 <= '➾') {
                byte v = table[char1 - 9984];
                if (v != 0) {
                    return new byte[]{v};
                }
            }
            return new byte[0];
        }

        public byte[] charToByte(String text, String encoding) {
            char[] cc = text.toCharArray();
            byte[] b = new byte[cc.length];
            int ptr = 0;
            int len = cc.length;
            for (int k = 0; k < len; k++) {
                char c = cc[k];
                if (c == ' ') {
                    b[ptr++] = (byte) c;
                } else if (c >= '✁' && c <= '➾') {
                    byte v = table[c - 9984];
                    if (v != 0) {
                        b[ptr++] = v;
                    }
                }
            }
            if (ptr == len) {
                return b;
            }
            byte[] b2 = new byte[ptr];
            System.arraycopy(b, 0, b2, 0, ptr);
            return b2;
        }

        public String byteToChar(byte[] b, String encoding) {
            return null;
        }

        private static final byte[] table = new byte[]{0, 35, 34, 0, 0, 0, 41, 62, 81, 42, 0, 0, 65, 63, 0, 0, 0, 0, 0, -4, 0, 0, 0, -5, 0, 0, 0, 0, 0, 0, 86, 0, 88, 89, 0, 0, 0, 0, 0, 0, 0, 0, -75, 0, 0, 0, 0, 0, -74, 0, 0, 0, -83, -81, -84, 0, 0, 0, 0, 0, 0, 0, 0, 124, 123, 0, 0, 0, 84, 0, 0, 0, 0, 0, 0, 0, 0, -90, 0, 0, 0, 113, 114, 0, 0, 0, 117, 0, 0, 0, 0, 0, 0, 125, 126, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -24, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -24, -40, 0, 0, -60, -58, 0, 0, -16, 0, 0, 0, 0, 0, 0, 0, 0, 0, -36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    }

    private static class Cp437Conversion
            implements ExtraEncoding {

        private Cp437Conversion() {
        }

        private static IntHashtable c2b = new IntHashtable();

        public byte[] charToByte(String text, String encoding) {
            char[] cc = text.toCharArray();
            byte[] b = new byte[cc.length];
            int ptr = 0;
            int len = cc.length;
            for (int k = 0; k < len; k++) {
                char c = cc[k];
                if (c < '') {
                    b[ptr++] = (byte) c;
                } else {
                    byte v = (byte) c2b.get(c);
                    if (v != 0) {
                        b[ptr++] = v;
                    }
                }
            }
            if (ptr == len) {
                return b;
            }
            byte[] b2 = new byte[ptr];
            System.arraycopy(b, 0, b2, 0, ptr);
            return b2;
        }

        public byte[] charToByte(char char1, String encoding) {
            if (char1 < '') {
                return new byte[]{(byte) char1};
            }
            byte v = (byte) c2b.get(char1);
            if (v != 0) {
                return new byte[]{v};
            }
            return new byte[0];
        }

        public String byteToChar(byte[] b, String encoding) {
            int len = b.length;
            char[] cc = new char[len];
            int ptr = 0;
            for (int k = 0; k < len; k++) {
                int c = b[k] & 0xFF;
                if (c >= 32) {
                    if (c < 128) {
                        cc[ptr++] = (char) c;
                    } else {
                        char v = table[c - 128];
                        cc[ptr++] = v;
                    }
                }
            }
            return new String(cc, 0, ptr);
        }

        private static final char[] table = new char[]{'Ç', 'ü', 'é', 'â', 'ä', 'à', 'å', 'ç', 'ê', 'ë', 'è', 'ï', 'î', 'ì', 'Ä', 'Å', 'É', 'æ', 'Æ', 'ô', 'ö', 'ò', 'û', 'ù', 'ÿ', 'Ö', 'Ü', '¢', '£', '¥', '₧', 'ƒ', 'á', 'í', 'ó', 'ú', 'ñ', 'Ñ', 'ª', 'º', '¿', '⌐', '¬', '½', '¼', '¡', '«', '»', '░', '▒', '▓', '│', '┤', '╡', '╢', '╖', '╕', '╣', '║', '╗', '╝', '╜', '╛', '┐', '└', '┴', '┬', '├', '─', '┼', '╞', '╟', '╚', '╔', '╩', '╦', '╠', '═', '╬', '╧', '╨', '╤', '╥', '╙', '╘', '╒', '╓', '╫', '╪', '┘', '┌', '█', '▄', '▌', '▐', '▀', 'α', 'ß', 'Γ', 'π', 'Σ', 'σ', 'µ', 'τ', 'Φ', 'Θ', 'Ω', 'δ', '∞', 'φ', 'ε', '∩', '≡', '±', '≥', '≤', '⌠', '⌡', '÷', '≈', '°', '∙', '·', '√', 'ⁿ', '²', '■', ' '};

        static {
            for (int k = 0; k < table.length; k++) {
                c2b.put(table[k], k + 128);
            }
        }
    }

    private static class SymbolConversion
            implements ExtraEncoding {

        private static final IntHashtable t1 = new IntHashtable();
        private static final IntHashtable t2 = new IntHashtable();
        private IntHashtable translation;
        private final char[] byteToChar;

        SymbolConversion(boolean symbol) {
            if (symbol) {
                this.translation = t1;
                this.byteToChar = table1;
            } else {
                this.translation = t2;
                this.byteToChar = table2;
            }
        }

        public byte[] charToByte(String text, String encoding) {
            char[] cc = text.toCharArray();
            byte[] b = new byte[cc.length];
            int ptr = 0;
            int len = cc.length;
            for (int k = 0; k < len; k++) {
                char c = cc[k];
                byte v = (byte) this.translation.get(c);
                if (v != 0) {
                    b[ptr++] = v;
                }
            }
            if (ptr == len) {
                return b;
            }
            byte[] b2 = new byte[ptr];
            System.arraycopy(b, 0, b2, 0, ptr);
            return b2;
        }

        public byte[] charToByte(char char1, String encoding) {
            byte v = (byte) this.translation.get(char1);
            if (v != 0) {
                return new byte[]{v};
            }
            return new byte[0];
        }

        public String byteToChar(byte[] b, String encoding) {
            int len = b.length;
            char[] cc = new char[len];
            int ptr = 0;
            for (int k = 0; k < len; k++) {
                int c = b[k] & 0xFF;
                char v = this.byteToChar[c];
                cc[ptr++] = v;
            }
            return new String(cc, 0, ptr);
        }

        private static final char[] table1 = new char[]{Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, ' ', '!', '∀', '#', '∃', '%', '&', '∋', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?', '≅', 'Α', 'Β', 'Χ', 'Δ', 'Ε', 'Φ', 'Γ', 'Η', 'Ι', 'ϑ', 'Κ', 'Λ', 'Μ', 'Ν', 'Ο', 'Π', 'Θ', 'Ρ', 'Σ', 'Τ', 'Υ', 'ς', 'Ω', 'Ξ', 'Ψ', 'Ζ', '[', '∴', ']', '⊥', '_', '̅', 'α', 'β', 'χ', 'δ', 'ε', 'ϕ', 'γ', 'η', 'ι', 'φ', 'κ', 'λ', 'μ', 'ν', 'ο', 'π', 'θ', 'ρ', 'σ', 'τ', 'υ', 'ϖ', 'ω', 'ξ', 'ψ', 'ζ', '{', '|', '}', '~', Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, '€', 'ϒ', '′', '≤', '⁄', '∞', 'ƒ', '♣', '♦', '♥', '♠', '↔', '←', '↑', '→', '↓', '°', '±', '″', '≥', '×', '∝', '∂', '•', '÷', '≠', '≡', '≈', '…', '│', '─', '↵', 'ℵ', 'ℑ', 'ℜ', '℘', '⊗', '⊕', '∅', '∩', '∪', '⊃', '⊇', '⊄', '⊂', '⊆', '∈', '∉', '∠', '∇', '®', '©', '™', '∏', '√', '⋅', '¬', '∧', '∨', '⇔', '⇐', '⇑', '⇒', '⇓', '◊', '〈', Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, '∑', '⎛', '⎜', '⎝', '⎡', '⎢', '⎣', '⎧', '⎨', '⎩', '⎪', Character.MIN_VALUE, '〉', '∫', '⌠', '⎮', '⌡', '⎞', '⎟', '⎠', '⎤', '⎥', '⎦', '⎫', '⎬', '⎭', Character.MIN_VALUE};

        private static final char[] table2 = new char[]{Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, ' ', '✁', '✂', '✃', '✄', '☎', '✆', '✇', '✈', '✉', '☛', '☞', '✌', '✍', '✎', '✏', '✐', '✑', '✒', '✓', '✔', '✕', '✖', '✗', '✘', '✙', '✚', '✛', '✜', '✝', '✞', '✟', '✠', '✡', '✢', '✣', '✤', '✥', '✦', '✧', '★', '✩', '✪', '✫', '✬', '✭', '✮', '✯', '✰', '✱', '✲', '✳', '✴', '✵', '✶', '✷', '✸', '✹', '✺', '✻', '✼', '✽', '✾', '✿', '❀', '❁', '❂', '❃', '❄', '❅', '❆', '❇', '❈', '❉', '❊', '❋', '●', '❍', '■', '❏', '❐', '❑', '❒', '▲', '▼', '◆', '❖', '◗', '❘', '❙', '❚', '❛', '❜', '❝', '❞', Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE, '❡', '❢', '❣', '❤', '❥', '❦', '❧', '♣', '♦', '♥', '♠', '①', '②', '③', '④', '⑤', '⑥', '⑦', '⑧', '⑨', '⑩', '❶', '❷', '❸', '❹', '❺', '❻', '❼', '❽', '❾', '❿', '➀', '➁', '➂', '➃', '➄', '➅', '➆', '➇', '➈', '➉', '➊', '➋', '➌', '➍', '➎', '➏', '➐', '➑', '➒', '➓', '➔', '→', '↔', '↕', '➘', '➙', '➚', '➛', '➜', '➝', '➞', '➟', '➠', '➡', '➢', '➣', '➤', '➥', '➦', '➧', '➨', '➩', '➪', '➫', '➬', '➭', '➮', '➯', Character.MIN_VALUE, '➱', '➲', '➳', '➴', '➵', '➶', '➷', '➸', '➹', '➺', '➻', '➼', '➽', '➾', Character.MIN_VALUE};

        static {
            int k;
            for (k = 0; k < 256; k++) {
                int v = table1[k];
                if (v != 0) {
                    t1.put(v, k);
                }
            }
            for (k = 0; k < 256; k++) {
                int v = table2[k];
                if (v != 0) {
                    t2.put(v, k);
                }
            }
        }
    }

    private static class SymbolTTConversion implements ExtraEncoding {

        private SymbolTTConversion() {
        }

        public byte[] charToByte(char char1, String encoding) {
            if ((char1 & 0xFF00) == 0 || (char1 & 0xFF00) == 61440) {
                return new byte[]{(byte) char1};
            }
            return new byte[0];
        }

        public byte[] charToByte(String text, String encoding) {
            char[] ch = text.toCharArray();
            byte[] b = new byte[ch.length];
            int ptr = 0;
            int len = ch.length;
            for (int k = 0; k < len; k++) {
                char c = ch[k];
                if ((c & 0xFF00) == 0 || (c & 0xFF00) == 61440) {
                    b[ptr++] = (byte) c;
                }
            }
            if (ptr == len) {
                return b;
            }
            byte[] b2 = new byte[ptr];
            System.arraycopy(b, 0, b2, 0, ptr);
            return b2;
        }

        public String byteToChar(byte[] b, String encoding) {
            return null;
        }
    }
}
