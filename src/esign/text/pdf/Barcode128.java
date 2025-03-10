package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.ExceptionConverter;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.image.MemoryImageSource;

public class Barcode128
        extends Barcode {

    private static final byte[][] BARS = new byte[][]{{2, 1, 2, 2, 2, 2}, {2, 2, 2, 1, 2, 2}, {2, 2, 2, 2, 2, 1}, {1, 2, 1, 2, 2, 3}, {1, 2, 1, 3, 2, 2}, {1, 3, 1, 2, 2, 2}, {1, 2, 2, 2, 1, 3}, {1, 2, 2, 3, 1, 2}, {1, 3, 2, 2, 1, 2}, {2, 2, 1, 2, 1, 3}, {2, 2, 1, 3, 1, 2}, {2, 3, 1, 2, 1, 2}, {1, 1, 2, 2, 3, 2}, {1, 2, 2, 1, 3, 2}, {1, 2, 2, 2, 3, 1}, {1, 1, 3, 2, 2, 2}, {1, 2, 3, 1, 2, 2}, {1, 2, 3, 2, 2, 1}, {2, 2, 3, 2, 1, 1}, {2, 2, 1, 1, 3, 2}, {2, 2, 1, 2, 3, 1}, {2, 1, 3, 2, 1, 2}, {2, 2, 3, 1, 1, 2}, {3, 1, 2, 1, 3, 1}, {3, 1, 1, 2, 2, 2}, {3, 2, 1, 1, 2, 2}, {3, 2, 1, 2, 2, 1}, {3, 1, 2, 2, 1, 2}, {3, 2, 2, 1, 1, 2}, {3, 2, 2, 2, 1, 1}, {2, 1, 2, 1, 2, 3}, {2, 1, 2, 3, 2, 1}, {2, 3, 2, 1, 2, 1}, {1, 1, 1, 3, 2, 3}, {1, 3, 1, 1, 2, 3}, {1, 3, 1, 3, 2, 1}, {1, 1, 2, 3, 1, 3}, {1, 3, 2, 1, 1, 3}, {1, 3, 2, 3, 1, 1}, {2, 1, 1, 3, 1, 3}, {2, 3, 1, 1, 1, 3}, {2, 3, 1, 3, 1, 1}, {1, 1, 2, 1, 3, 3}, {1, 1, 2, 3, 3, 1}, {1, 3, 2, 1, 3, 1}, {1, 1, 3, 1, 2, 3}, {1, 1, 3, 3, 2, 1}, {1, 3, 3, 1, 2, 1}, {3, 1, 3, 1, 2, 1}, {2, 1, 1, 3, 3, 1}, {2, 3, 1, 1, 3, 1}, {2, 1, 3, 1, 1, 3}, {2, 1, 3, 3, 1, 1}, {2, 1, 3, 1, 3, 1}, {3, 1, 1, 1, 2, 3}, {3, 1, 1, 3, 2, 1}, {3, 3, 1, 1, 2, 1}, {3, 1, 2, 1, 1, 3}, {3, 1, 2, 3, 1, 1}, {3, 3, 2, 1, 1, 1}, {3, 1, 4, 1, 1, 1}, {2, 2, 1, 4, 1, 1}, {4, 3, 1, 1, 1, 1}, {1, 1, 1, 2, 2, 4}, {1, 1, 1, 4, 2, 2}, {1, 2, 1, 1, 2, 4}, {1, 2, 1, 4, 2, 1}, {1, 4, 1, 1, 2, 2}, {1, 4, 1, 2, 2, 1}, {1, 1, 2, 2, 1, 4}, {1, 1, 2, 4, 1, 2}, {1, 2, 2, 1, 1, 4}, {1, 2, 2, 4, 1, 1}, {1, 4, 2, 1, 1, 2}, {1, 4, 2, 2, 1, 1}, {2, 4, 1, 2, 1, 1}, {2, 2, 1, 1, 1, 4}, {4, 1, 3, 1, 1, 1}, {2, 4, 1, 1, 1, 2}, {1, 3, 4, 1, 1, 1}, {1, 1, 1, 2, 4, 2}, {1, 2, 1, 1, 4, 2}, {1, 2, 1, 2, 4, 1}, {1, 1, 4, 2, 1, 2}, {1, 2, 4, 1, 1, 2}, {1, 2, 4, 2, 1, 1}, {4, 1, 1, 2, 1, 2}, {4, 2, 1, 1, 1, 2}, {4, 2, 1, 2, 1, 1}, {2, 1, 2, 1, 4, 1}, {2, 1, 4, 1, 2, 1}, {4, 1, 2, 1, 2, 1}, {1, 1, 1, 1, 4, 3}, {1, 1, 1, 3, 4, 1}, {1, 3, 1, 1, 4, 1}, {1, 1, 4, 1, 1, 3}, {1, 1, 4, 3, 1, 1}, {4, 1, 1, 1, 1, 3}, {4, 1, 1, 3, 1, 1}, {1, 1, 3, 1, 4, 1}, {1, 1, 4, 1, 3, 1}, {3, 1, 1, 1, 4, 1}, {4, 1, 1, 1, 3, 1}, {2, 1, 1, 4, 1, 2}, {2, 1, 1, 2, 1, 4}, {2, 1, 1, 2, 3, 2}};

    private static final byte[] BARS_STOP = new byte[]{2, 3, 3, 1, 1, 1, 2};

    public static final char CODE_AB_TO_C = 'c';

    public static final char CODE_AC_TO_B = 'd';

    public static final char CODE_BC_TO_A = 'e';

    public static final char FNC1_INDEX = 'f';

    public static final char START_A = 'g';

    public static final char START_B = 'h';

    public static final char START_C = 'i';

    public static final char FNC1 = 'Ê';

    public static final char DEL = 'Ã';

    public static final char FNC3 = 'Ä';

    public static final char FNC2 = 'Å';

    public static final char SHIFT = 'Æ';

    public static final char CODE_C = 'Ç';

    public static final char CODE_A = 'È';

    public static final char FNC4 = 'È';

    public static final char STARTA = 'Ë';
    public static final char STARTB = 'Ì';
    public static final char STARTC = 'Í';
    private static final IntHashtable ais = new IntHashtable();

    public Barcode128() {
        try {
            this.x = 0.8F;
            this.font = BaseFont.createFont("Helvetica", "winansi", false);
            this.size = 8.0F;
            this.baseline = this.size;
            this.barHeight = this.size * 3.0F;
            this.textAlignment = 1;
            this.codeType = 9;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public enum Barcode128CodeSet {
        A,
        B,
        C,
        AUTO;

        public char getStartSymbol() {
            switch (this) {
                case A:
                    return 'g';
                case B:
                    return 'h';
                case C:
                    return 'i';
            }
            return 'h';
        }
    }

    public void setCodeSet(Barcode128CodeSet codeSet) {
        this.codeSet = codeSet;
    }

    public Barcode128CodeSet getCodeSet() {
        return this.codeSet;
    }

    private Barcode128CodeSet codeSet = Barcode128CodeSet.AUTO;

    public static String removeFNC1(String code) {
        int len = code.length();
        StringBuffer buf = new StringBuffer(len);
        for (int k = 0; k < len; k++) {
            char c = code.charAt(k);
            if (c >= ' ' && c <= '~') {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    public static String getHumanReadableUCCEAN(String code) {
        StringBuffer buf = new StringBuffer();
        String fnc1 = String.valueOf('Ê');
        try {
            while (true) {
                while (code.startsWith(fnc1)) {
                    code = code.substring(1);
                }

                int n = 0;
                int idlen = 0;
                for (int k = 2; k < 5
                        && code.length() >= k; k++) {

                    if ((n = ais.get(Integer.parseInt(code.substring(0, k)))) != 0) {
                        idlen = k;
                        break;
                    }
                }
                if (idlen == 0) {
                    break;
                }
                buf.append('(').append(code.substring(0, idlen)).append(')');
                code = code.substring(idlen);
                if (n > 0) {
                    n -= idlen;
                    if (code.length() <= n) {
                        break;
                    }
                    buf.append(removeFNC1(code.substring(0, n)));
                    code = code.substring(n);
                    continue;
                }
                int idx = code.indexOf('Ê');
                if (idx < 0) {
                    break;
                }
                buf.append(code.substring(0, idx));
                code = code.substring(idx + 1);
            }

        } catch (Exception exception) {
        }

        buf.append(removeFNC1(code));
        return buf.toString();
    }

    static boolean isNextDigits(String text, int textIndex, int numDigits) {
        int len = text.length();
        while (textIndex < len && numDigits > 0) {
            if (text.charAt(textIndex) == 'Ê') {
                textIndex++;
                continue;
            }
            int n = Math.min(2, numDigits);
            if (textIndex + n > len) {
                return false;
            }
            while (n-- > 0) {
                char c = text.charAt(textIndex++);
                if (c < '0' || c > '9') {
                    return false;
                }
                numDigits--;
            }
        }
        return (numDigits == 0);
    }

    static String getPackedRawDigits(String text, int textIndex, int numDigits) {
        StringBuilder out = new StringBuilder("");
        int start = textIndex;
        while (numDigits > 0) {
            if (text.charAt(textIndex) == 'Ê') {
                out.append('f');
                textIndex++;
                continue;
            }
            numDigits -= 2;
            int c1 = text.charAt(textIndex++) - 48;
            int c2 = text.charAt(textIndex++) - 48;
            out.append((char) (c1 * 10 + c2));
        }
        return (char) (textIndex - start) + out.toString();
    }

    public static String getRawText(String text, boolean ucc, Barcode128CodeSet codeSet) {
        String out = "";
        int tLen = text.length();
        if (tLen == 0) {
            out = out + codeSet.getStartSymbol();
            if (ucc) {
                out = out + 'f';
            }
            return out;
        }
        int c = 0;
        for (int k = 0; k < tLen; k++) {
            c = text.charAt(k);
            if (c > 127 && c != 202) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("there.are.illegal.characters.for.barcode.128.in.1", new Object[]{text}));
            }
        }
        c = text.charAt(0);
        char currentCode = 'h';
        int index = 0;
        if ((codeSet == Barcode128CodeSet.AUTO || codeSet == Barcode128CodeSet.C) && isNextDigits(text, index, 2)) {
            currentCode = 'i';
            out = out + currentCode;
            if (ucc) {
                out = out + 'f';
            }
            String out2 = getPackedRawDigits(text, index, 2);
            index += out2.charAt(0);
            out = out + out2.substring(1);
        } else if (c < 32) {
            currentCode = 'g';
            out = out + currentCode;
            if (ucc) {
                out = out + 'f';
            }
            out = out + (char) (c + 64);
            index++;
        } else {

            out = out + currentCode;
            if (ucc) {
                out = out + 'f';
            }
            if (c == 202) {
                out = out + 'f';
            } else {
                out = out + (char) (c - 32);
            }
            index++;
        }
        if (codeSet != Barcode128CodeSet.AUTO && currentCode != codeSet.getStartSymbol()) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("there.are.illegal.characters.for.barcode.128.in.1", new Object[]{text}));
        }
        while (index < tLen) {
            switch (currentCode) {
                case 'g':
                    if (codeSet == Barcode128CodeSet.AUTO && isNextDigits(text, index, 4)) {
                        currentCode = 'i';
                        out = out + 'c';
                        String out2 = getPackedRawDigits(text, index, 4);
                        index += out2.charAt(0);
                        out = out + out2.substring(1);
                        break;
                    }
                    c = text.charAt(index++);
                    if (c == 202) {
                        out = out + 'f';
                        break;
                    }
                    if (c > 95) {
                        currentCode = 'h';
                        out = out + 'd';
                        out = out + (char) (c - 32);
                        break;
                    }
                    if (c < 32) {
                        out = out + (char) (c + 64);
                        break;
                    }
                    out = out + (char) (c - 32);
                    break;

                case 'h':
                    if (codeSet == Barcode128CodeSet.AUTO && isNextDigits(text, index, 4)) {
                        currentCode = 'i';
                        out = out + 'c';
                        String out2 = getPackedRawDigits(text, index, 4);
                        index += out2.charAt(0);
                        out = out + out2.substring(1);
                        break;
                    }
                    c = text.charAt(index++);
                    if (c == 202) {
                        out = out + 'f';
                        break;
                    }
                    if (c < 32) {
                        currentCode = 'g';
                        out = out + 'e';
                        out = out + (char) (c + 64);
                        break;
                    }
                    out = out + (char) (c - 32);
                    break;

                case 'i':
                    if (isNextDigits(text, index, 2)) {
                        String out2 = getPackedRawDigits(text, index, 2);
                        index += out2.charAt(0);
                        out = out + out2.substring(1);
                        break;
                    }
                    c = text.charAt(index++);
                    if (c == 202) {
                        out = out + 'f';
                        break;
                    }
                    if (c < 32) {
                        currentCode = 'g';
                        out = out + 'e';
                        out = out + (char) (c + 64);
                        break;
                    }
                    currentCode = 'h';
                    out = out + 'd';
                    out = out + (char) (c - 32);
                    break;
            }

            if (codeSet != Barcode128CodeSet.AUTO && currentCode != codeSet.getStartSymbol()) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("there.are.illegal.characters.for.barcode.128.in.1", new Object[]{text}));
            }
        }
        return out;
    }

    public static String getRawText(String text, boolean ucc) {
        return getRawText(text, ucc, Barcode128CodeSet.AUTO);
    }

    public static byte[] getBarsCode128Raw(String text) {
        int idx = text.indexOf('￿');
        if (idx >= 0) {
            text = text.substring(0, idx);
        }
        int chk = text.charAt(0);
        for (int k = 1; k < text.length(); k++) {
            chk += k * text.charAt(k);
        }
        chk %= 103;
        text = text + (char) chk;
        byte[] bars = new byte[(text.length() + 1) * 6 + 7];
        int i;
        for (i = 0; i < text.length(); i++) {
            System.arraycopy(BARS[text.charAt(i)], 0, bars, i * 6, 6);
        }
        System.arraycopy(BARS_STOP, 0, bars, i * 6, 7);
        return bars;
    }

    public Rectangle getBarcodeSize() {
        String fullCode;
        float fontX = 0.0F;
        float fontY = 0.0F;

        if (this.font != null) {
            if (this.baseline > 0.0F) {
                fontY = this.baseline - this.font.getFontDescriptor(3, this.size);
            } else {
                fontY = -this.baseline + this.size;
            }
            if (this.codeType == 11) {
                int idx = this.code.indexOf('￿');
                if (idx < 0) {
                    fullCode = "";
                } else {
                    fullCode = this.code.substring(idx + 1);
                }
            } else if (this.codeType == 10) {
                fullCode = getHumanReadableUCCEAN(this.code);
            } else {
                fullCode = removeFNC1(this.code);
            }
            fontX = this.font.getWidthPoint((this.altText != null) ? this.altText : fullCode, this.size);
        }
        if (this.codeType == 11) {
            int idx = this.code.indexOf('￿');
            if (idx >= 0) {
                fullCode = this.code.substring(0, idx);
            } else {
                fullCode = this.code;
            }
        } else {
            fullCode = getRawText(this.code, (this.codeType == 10), this.codeSet);
        }
        int len = fullCode.length();
        float fullWidth = ((len + 2) * 11) * this.x + 2.0F * this.x;
        fullWidth = Math.max(fullWidth, fontX);
        float fullHeight = this.barHeight + fontY;
        return new Rectangle(fullWidth, fullHeight);
    }

    public Rectangle placeBarcode(PdfContentByte cb, BaseColor barColor, BaseColor textColor) {
        String fullCode, bCode;
        if (this.codeType == 11) {
            int idx = this.code.indexOf('￿');
            if (idx < 0) {
                fullCode = "";
            } else {
                fullCode = this.code.substring(idx + 1);
            }
        } else if (this.codeType == 10) {
            fullCode = getHumanReadableUCCEAN(this.code);
        } else {
            fullCode = removeFNC1(this.code);
        }
        float fontX = 0.0F;
        if (this.font != null) {
            fontX = this.font.getWidthPoint(fullCode = (this.altText != null) ? this.altText : fullCode, this.size);
        }

        if (this.codeType == 11) {
            int idx = this.code.indexOf('￿');
            if (idx >= 0) {
                bCode = this.code.substring(0, idx);
            } else {
                bCode = this.code;
            }
        } else {
            bCode = getRawText(this.code, (this.codeType == 10), this.codeSet);
        }
        int len = bCode.length();
        float fullWidth = ((len + 2) * 11) * this.x + 2.0F * this.x;
        float barStartX = 0.0F;
        float textStartX = 0.0F;
        switch (this.textAlignment) {
            case 0:
                break;
            case 2:
                if (fontX > fullWidth) {
                    barStartX = fontX - fullWidth;
                    break;
                }
                textStartX = fullWidth - fontX;
                break;
            default:
                if (fontX > fullWidth) {
                    barStartX = (fontX - fullWidth) / 2.0F;
                    break;
                }
                textStartX = (fullWidth - fontX) / 2.0F;
                break;
        }
        float barStartY = 0.0F;
        float textStartY = 0.0F;
        if (this.font != null) {
            if (this.baseline <= 0.0F) {
                textStartY = this.barHeight - this.baseline;
            } else {
                textStartY = -this.font.getFontDescriptor(3, this.size);
                barStartY = textStartY + this.baseline;
            }
        }
        byte[] bars = getBarsCode128Raw(bCode);
        boolean print = true;
        if (barColor != null) {
            cb.setColorFill(barColor);
        }
        for (int k = 0; k < bars.length; k++) {
            float w = bars[k] * this.x;
            if (print) {
                cb.rectangle(barStartX, barStartY, w - this.inkSpreading, this.barHeight);
            }
            print = !print;
            barStartX += w;
        }
        cb.fill();
        if (this.font != null) {
            if (textColor != null) {
                cb.setColorFill(textColor);
            }
            cb.beginText();
            cb.setFontAndSize(this.font, this.size);
            cb.setTextMatrix(textStartX, textStartY);
            cb.showText(fullCode);
            cb.endText();
        }
        return getBarcodeSize();
    }

    public void setCode(String code) {
        if (getCodeType() == 10 && code.startsWith("(")) {
            int idx = 0;
            StringBuilder ret = new StringBuilder("");
            while (idx >= 0) {
                int end = code.indexOf(')', idx);
                if (end < 0) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("badly.formed.ucc.string.1", new Object[]{code}));
                }
                String sai = code.substring(idx + 1, end);
                if (sai.length() < 2) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("ai.too.short.1", new Object[]{sai}));
                }
                int ai = Integer.parseInt(sai);
                int len = ais.get(ai);
                if (len == 0) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("ai.not.found.1", new Object[]{sai}));
                }
                sai = String.valueOf(ai);
                if (sai.length() == 1) {
                    sai = "0" + sai;
                }
                idx = code.indexOf('(', end);
                int next = (idx < 0) ? code.length() : idx;
                ret.append(sai).append(code.substring(end + 1, next));
                if (len < 0) {
                    if (idx >= 0) {
                        ret.append('Ê');
                    }
                    continue;
                }
                if (next - end - 1 + sai.length() != len) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("invalid.ai.length.1", new Object[]{sai}));
                }
            }
            super.setCode(ret.toString());
        } else {

            super.setCode(code);
        }
    }

    static {
        ais.put(0, 20);
        ais.put(1, 16);
        ais.put(2, 16);
        ais.put(10, -1);
        ais.put(11, 9);
        ais.put(12, 8);
        ais.put(13, 8);
        ais.put(15, 8);
        ais.put(17, 8);
        ais.put(20, 4);
        ais.put(21, -1);
        ais.put(22, -1);
        ais.put(23, -1);
        ais.put(240, -1);
        ais.put(241, -1);
        ais.put(250, -1);
        ais.put(251, -1);
        ais.put(252, -1);
        ais.put(30, -1);
        int k;
        for (k = 3100; k < 3700; k++) {
            ais.put(k, 10);
        }
        ais.put(37, -1);
        for (k = 3900; k < 3940; k++) {
            ais.put(k, -1);
        }
        ais.put(400, -1);
        ais.put(401, -1);
        ais.put(402, 20);
        ais.put(403, -1);
        for (k = 410; k < 416; k++) {
            ais.put(k, 16);
        }
        ais.put(420, -1);
        ais.put(421, -1);
        ais.put(422, 6);
        ais.put(423, -1);
        ais.put(424, 6);
        ais.put(425, 6);
        ais.put(426, 6);
        ais.put(7001, 17);
        ais.put(7002, -1);
        for (k = 7030; k < 7040; k++) {
            ais.put(k, -1);
        }
        ais.put(8001, 18);
        ais.put(8002, -1);
        ais.put(8003, -1);
        ais.put(8004, -1);
        ais.put(8005, 10);
        ais.put(8006, 22);
        ais.put(8007, -1);
        ais.put(8008, -1);
        ais.put(8018, 22);
        ais.put(8020, -1);
        ais.put(8100, 10);
        ais.put(8101, 14);
        ais.put(8102, 6);
        for (k = 90; k < 100; k++) {
            ais.put(k, -1);
        }
    }

    public java.awt.Image createAwtImage(Color foreground, Color background) {
        String bCode;
        int f = foreground.getRGB();
        int g = background.getRGB();
        Canvas canvas = new Canvas();

        if (this.codeType == 11) {
            int idx = this.code.indexOf('￿');
            if (idx >= 0) {
                bCode = this.code.substring(0, idx);
            } else {
                bCode = this.code;
            }
        } else {
            bCode = getRawText(this.code, (this.codeType == 10));
        }
        int len = bCode.length();
        int fullWidth = (len + 2) * 11 + 2;
        byte[] bars = getBarsCode128Raw(bCode);

        boolean print = true;
        int ptr = 0;
        int height = (int) this.barHeight;
        int[] pix = new int[fullWidth * height];
        int k;
        for (k = 0; k < bars.length; k++) {
            int w = bars[k];
            int c = g;
            if (print) {
                c = f;
            }
            print = !print;
            for (int j = 0; j < w; j++) {
                pix[ptr++] = c;
            }
        }
        for (k = fullWidth; k < pix.length; k += fullWidth) {
            System.arraycopy(pix, 0, pix, k, fullWidth);
        }
        java.awt.Image img = canvas.createImage(new MemoryImageSource(fullWidth, height, pix, 0, fullWidth));

        return img;
    }
}
