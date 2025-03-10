package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.ExceptionConverter;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.util.Arrays;

public class BarcodeEAN
        extends Barcode {

    private static final int[] GUARD_EMPTY = new int[0];

    private static final int[] GUARD_UPCA = new int[]{0, 2, 4, 6, 28, 30, 52, 54, 56, 58};

    private static final int[] GUARD_EAN13 = new int[]{0, 2, 28, 30, 56, 58};

    private static final int[] GUARD_EAN8 = new int[]{0, 2, 20, 22, 40, 42};

    private static final int[] GUARD_UPCE = new int[]{0, 2, 28, 30, 32};

    private static final float[] TEXTPOS_EAN13 = new float[]{6.5F, 13.5F, 20.5F, 27.5F, 34.5F, 41.5F, 53.5F, 60.5F, 67.5F, 74.5F, 81.5F, 88.5F};

    private static final float[] TEXTPOS_EAN8 = new float[]{6.5F, 13.5F, 20.5F, 27.5F, 39.5F, 46.5F, 53.5F, 60.5F};

    private static final byte[][] BARS = new byte[][]{{3, 2, 1, 1}, {2, 2, 2, 1}, {2, 1, 2, 2}, {1, 4, 1, 1}, {1, 1, 3, 2}, {1, 2, 3, 1}, {1, 1, 1, 4}, {1, 3, 1, 2}, {1, 2, 1, 3}, {3, 1, 1, 2}};

    private static final int TOTALBARS_EAN13 = 59;

    private static final int TOTALBARS_EAN8 = 43;

    private static final int TOTALBARS_UPCE = 33;

    private static final int TOTALBARS_SUPP2 = 13;

    private static final int TOTALBARS_SUPP5 = 31;

    private static final int ODD = 0;

    private static final int EVEN = 1;

    private static final byte[][] PARITY13 = new byte[][]{{0, 0, 0, 0, 0, 0}, {0, 0, 1, 0, 1, 1}, {0, 0, 1, 1, 0, 1}, {0, 0, 1, 1, 1, 0}, {0, 1, 0, 0, 1, 1}, {0, 1, 1, 0, 0, 1}, {0, 1, 1, 1, 0, 0}, {0, 1, 0, 1, 0, 1}, {0, 1, 0, 1, 1, 0}, {0, 1, 1, 0, 1, 0}};

    private static final byte[][] PARITY2 = new byte[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}};

    private static final byte[][] PARITY5 = new byte[][]{{1, 1, 0, 0, 0}, {1, 0, 1, 0, 0}, {1, 0, 0, 1, 0}, {1, 0, 0, 0, 1}, {0, 1, 1, 0, 0}, {0, 0, 1, 1, 0}, {0, 0, 0, 1, 1}, {0, 1, 0, 1, 0}, {0, 1, 0, 0, 1}, {0, 0, 1, 0, 1}};

    private static final byte[][] PARITYE = new byte[][]{{1, 1, 1, 0, 0, 0}, {1, 1, 0, 1, 0, 0}, {1, 1, 0, 0, 1, 0}, {1, 1, 0, 0, 0, 1}, {1, 0, 1, 1, 0, 0}, {1, 0, 0, 1, 1, 0}, {1, 0, 0, 0, 1, 1}, {1, 0, 1, 0, 1, 0}, {1, 0, 1, 0, 0, 1}, {1, 0, 0, 1, 0, 1}};

    public BarcodeEAN() {
        try {
            this.x = 0.8F;
            this.font = BaseFont.createFont("Helvetica", "winansi", false);
            this.size = 8.0F;
            this.baseline = this.size;
            this.barHeight = this.size * 3.0F;
            this.guardBars = true;
            this.codeType = 1;
            this.code = "";
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public static int calculateEANParity(String code) {
        int mul = 3;
        int total = 0;
        for (int k = code.length() - 1; k >= 0; k--) {
            int n = code.charAt(k) - 48;
            total += mul * n;
            mul ^= 0x2;
        }
        return (10 - total % 10) % 10;
    }

    public static String convertUPCAtoUPCE(String text) {
        if (text.length() != 12 || (!text.startsWith("0") && !text.startsWith("1"))) {
            return null;
        }
        if (text.substring(3, 6).equals("000") || text.substring(3, 6).equals("100") || text
                .substring(3, 6).equals("200")) {
            if (text.substring(6, 8).equals("00")) {
                return text.substring(0, 1) + text.substring(1, 3) + text.substring(8, 11) + text.substring(3, 4) + text.substring(11);
            }
        } else if (text.substring(4, 6).equals("00")) {
            if (text.substring(6, 9).equals("000")) {
                return text.substring(0, 1) + text.substring(1, 4) + text.substring(9, 11) + "3" + text.substring(11);
            }
        } else if (text.substring(5, 6).equals("0")) {
            if (text.substring(6, 10).equals("0000")) {
                return text.substring(0, 1) + text.substring(1, 5) + text.substring(10, 11) + "4" + text.substring(11);
            }
        } else if (text.charAt(10) >= '5'
                && text.substring(6, 10).equals("0000")) {
            return text.substring(0, 1) + text.substring(1, 6) + text.substring(10, 11) + text.substring(11);
        }
        return null;
    }

    public static byte[] getBarsEAN13(String _code) {
        int[] code = new int[_code.length()];
        for (int k = 0; k < code.length; k++) {
            code[k] = _code.charAt(k) - 48;
        }
        byte[] bars = new byte[59];
        int pb = 0;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        byte[] sequence = PARITY13[code[0]];
        int i;
        for (i = 0; i < sequence.length; i++) {
            int c = code[i + 1];
            byte[] stripes = BARS[c];
            if (sequence[i] == 0) {
                bars[pb++] = stripes[0];
                bars[pb++] = stripes[1];
                bars[pb++] = stripes[2];
                bars[pb++] = stripes[3];
            } else {

                bars[pb++] = stripes[3];
                bars[pb++] = stripes[2];
                bars[pb++] = stripes[1];
                bars[pb++] = stripes[0];
            }
        }
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        for (i = 7; i < 13; i++) {
            int c = code[i];
            byte[] stripes = BARS[c];
            bars[pb++] = stripes[0];
            bars[pb++] = stripes[1];
            bars[pb++] = stripes[2];
            bars[pb++] = stripes[3];
        }
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        return bars;
    }

    public static byte[] getBarsEAN8(String _code) {
        int[] code = new int[_code.length()];
        for (int k = 0; k < code.length; k++) {
            code[k] = _code.charAt(k) - 48;
        }
        byte[] bars = new byte[43];
        int pb = 0;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        int i;
        for (i = 0; i < 4; i++) {
            int c = code[i];
            byte[] stripes = BARS[c];
            bars[pb++] = stripes[0];
            bars[pb++] = stripes[1];
            bars[pb++] = stripes[2];
            bars[pb++] = stripes[3];
        }
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        for (i = 4; i < 8; i++) {
            int c = code[i];
            byte[] stripes = BARS[c];
            bars[pb++] = stripes[0];
            bars[pb++] = stripes[1];
            bars[pb++] = stripes[2];
            bars[pb++] = stripes[3];
        }
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        return bars;
    }

    public static byte[] getBarsUPCE(String _code) {
        int[] code = new int[_code.length()];
        for (int k = 0; k < code.length; k++) {
            code[k] = _code.charAt(k) - 48;
        }
        byte[] bars = new byte[33];
        boolean flip = (code[0] != 0);
        int pb = 0;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        byte[] sequence = PARITYE[code[code.length - 1]];
        for (int i = 1; i < code.length - 1; i++) {
            int c = code[i];
            byte[] stripes = BARS[c];
            if (sequence[i - 1] == (flip ? 1 : 0)) {
                bars[pb++] = stripes[0];
                bars[pb++] = stripes[1];
                bars[pb++] = stripes[2];
                bars[pb++] = stripes[3];
            } else {

                bars[pb++] = stripes[3];
                bars[pb++] = stripes[2];
                bars[pb++] = stripes[1];
                bars[pb++] = stripes[0];
            }
        }
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 1;
        return bars;
    }

    public static byte[] getBarsSupplemental2(String _code) {
        int[] code = new int[2];
        for (int k = 0; k < code.length; k++) {
            code[k] = _code.charAt(k) - 48;
        }
        byte[] bars = new byte[13];
        int pb = 0;
        int parity = (code[0] * 10 + code[1]) % 4;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 2;
        byte[] sequence = PARITY2[parity];
        for (int i = 0; i < sequence.length; i++) {
            if (i == 1) {
                bars[pb++] = 1;
                bars[pb++] = 1;
            }
            int c = code[i];
            byte[] stripes = BARS[c];
            if (sequence[i] == 0) {
                bars[pb++] = stripes[0];
                bars[pb++] = stripes[1];
                bars[pb++] = stripes[2];
                bars[pb++] = stripes[3];
            } else {

                bars[pb++] = stripes[3];
                bars[pb++] = stripes[2];
                bars[pb++] = stripes[1];
                bars[pb++] = stripes[0];
            }
        }
        return bars;
    }

    public static byte[] getBarsSupplemental5(String _code) {
        int[] code = new int[5];
        for (int k = 0; k < code.length; k++) {
            code[k] = _code.charAt(k) - 48;
        }
        byte[] bars = new byte[31];
        int pb = 0;
        int parity = ((code[0] + code[2] + code[4]) * 3 + (code[1] + code[3]) * 9) % 10;
        bars[pb++] = 1;
        bars[pb++] = 1;
        bars[pb++] = 2;
        byte[] sequence = PARITY5[parity];
        for (int i = 0; i < sequence.length; i++) {
            if (i != 0) {
                bars[pb++] = 1;
                bars[pb++] = 1;
            }
            int c = code[i];
            byte[] stripes = BARS[c];
            if (sequence[i] == 0) {
                bars[pb++] = stripes[0];
                bars[pb++] = stripes[1];
                bars[pb++] = stripes[2];
                bars[pb++] = stripes[3];
            } else {

                bars[pb++] = stripes[3];
                bars[pb++] = stripes[2];
                bars[pb++] = stripes[1];
                bars[pb++] = stripes[0];
            }
        }
        return bars;
    }

    public Rectangle getBarcodeSize() {
        float width = 0.0F;
        float height = this.barHeight;
        if (this.font != null) {
            if (this.baseline <= 0.0F) {
                height += -this.baseline + this.size;
            } else {
                height += this.baseline - this.font.getFontDescriptor(3, this.size);
            }
        }
        switch (this.codeType) {
            case 1:
                width = this.x * 95.0F;
                if (this.font != null) {
                    width += this.font.getWidthPoint(this.code.charAt(0), this.size);
                }

                return new Rectangle(width, height);
            case 2:
                width = this.x * 67.0F;
                return new Rectangle(width, height);
            case 3:
                width = this.x * 95.0F;
                if (this.font != null) {
                    width += this.font.getWidthPoint(this.code.charAt(0), this.size) + this.font.getWidthPoint(this.code.charAt(11), this.size);
                }
                return new Rectangle(width, height);
            case 4:
                width = this.x * 51.0F;
                if (this.font != null) {
                    width += this.font.getWidthPoint(this.code.charAt(0), this.size) + this.font.getWidthPoint(this.code.charAt(7), this.size);
                }
                return new Rectangle(width, height);
            case 5:
                width = this.x * 20.0F;
                return new Rectangle(width, height);
            case 6:
                width = this.x * 47.0F;
                return new Rectangle(width, height);
        }
        throw new RuntimeException(MessageLocalization.getComposedMessage("invalid.code.type", new Object[0]));
    }

    public Rectangle placeBarcode(PdfContentByte cb, BaseColor barColor, BaseColor textColor) {
        Rectangle rect = getBarcodeSize();
        float barStartX = 0.0F;
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
        switch (this.codeType) {
            case 1:
            case 3:
            case 4:
                if (this.font != null) {
                    barStartX += this.font.getWidthPoint(this.code.charAt(0), this.size);
                }
                break;
        }
        byte[] bars = null;
        int[] guard = GUARD_EMPTY;
        switch (this.codeType) {
            case 1:
                bars = getBarsEAN13(this.code);
                guard = GUARD_EAN13;
                break;
            case 2:
                bars = getBarsEAN8(this.code);
                guard = GUARD_EAN8;
                break;
            case 3:
                bars = getBarsEAN13("0" + this.code);
                guard = GUARD_UPCA;
                break;
            case 4:
                bars = getBarsUPCE(this.code);
                guard = GUARD_UPCE;
                break;
            case 5:
                bars = getBarsSupplemental2(this.code);
                break;
            case 6:
                bars = getBarsSupplemental5(this.code);
                break;
        }
        float keepBarX = barStartX;
        boolean print = true;
        float gd = 0.0F;
        if (this.font != null && this.baseline > 0.0F && this.guardBars) {
            gd = this.baseline / 2.0F;
        }
        if (barColor != null) {
            cb.setColorFill(barColor);
        }
        int k;
        for (k = 0; k < bars.length; k++) {
            float w = bars[k] * this.x;
            if (print) {
                if (Arrays.binarySearch(guard, k) >= 0) {
                    cb.rectangle(barStartX, barStartY - gd, w - this.inkSpreading, this.barHeight + gd);
                } else {
                    cb.rectangle(barStartX, barStartY, w - this.inkSpreading, this.barHeight);
                }
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
            switch (this.codeType) {
                case 1:
                    cb.setTextMatrix(0.0F, textStartY);
                    cb.showText(this.code.substring(0, 1));
                    for (k = 1; k < 13; k++) {
                        String c = this.code.substring(k, k + 1);
                        float len = this.font.getWidthPoint(c, this.size);
                        float pX = keepBarX + TEXTPOS_EAN13[k - 1] * this.x - len / 2.0F;
                        cb.setTextMatrix(pX, textStartY);
                        cb.showText(c);
                    }
                    break;
                case 2:
                    for (k = 0; k < 8; k++) {
                        String c = this.code.substring(k, k + 1);
                        float len = this.font.getWidthPoint(c, this.size);
                        float pX = TEXTPOS_EAN8[k] * this.x - len / 2.0F;
                        cb.setTextMatrix(pX, textStartY);
                        cb.showText(c);
                    }
                    break;
                case 3:
                    cb.setTextMatrix(0.0F, textStartY);
                    cb.showText(this.code.substring(0, 1));
                    for (k = 1; k < 11; k++) {
                        String c = this.code.substring(k, k + 1);
                        float len = this.font.getWidthPoint(c, this.size);
                        float pX = keepBarX + TEXTPOS_EAN13[k] * this.x - len / 2.0F;
                        cb.setTextMatrix(pX, textStartY);
                        cb.showText(c);
                    }
                    cb.setTextMatrix(keepBarX + this.x * 95.0F, textStartY);
                    cb.showText(this.code.substring(11, 12));
                    break;
                case 4:
                    cb.setTextMatrix(0.0F, textStartY);
                    cb.showText(this.code.substring(0, 1));
                    for (k = 1; k < 7; k++) {
                        String c = this.code.substring(k, k + 1);
                        float len = this.font.getWidthPoint(c, this.size);
                        float pX = keepBarX + TEXTPOS_EAN13[k - 1] * this.x - len / 2.0F;
                        cb.setTextMatrix(pX, textStartY);
                        cb.showText(c);
                    }
                    cb.setTextMatrix(keepBarX + this.x * 51.0F, textStartY);
                    cb.showText(this.code.substring(7, 8));
                    break;
                case 5:
                case 6:
                    for (k = 0; k < this.code.length(); k++) {
                        String c = this.code.substring(k, k + 1);
                        float len = this.font.getWidthPoint(c, this.size);
                        float pX = (7.5F + (9 * k)) * this.x - len / 2.0F;
                        cb.setTextMatrix(pX, textStartY);
                        cb.showText(c);
                    }
                    break;
            }
            cb.endText();
        }
        return rect;
    }

    public Image createAwtImage(Color foreground, Color background) {
        int f = foreground.getRGB();
        int g = background.getRGB();
        Canvas canvas = new Canvas();

        int width = 0;
        byte[] bars = null;
        switch (this.codeType) {
            case 1:
                bars = getBarsEAN13(this.code);
                width = 95;
                break;
            case 2:
                bars = getBarsEAN8(this.code);
                width = 67;
                break;
            case 3:
                bars = getBarsEAN13("0" + this.code);
                width = 95;
                break;
            case 4:
                bars = getBarsUPCE(this.code);
                width = 51;
                break;
            case 5:
                bars = getBarsSupplemental2(this.code);
                width = 20;
                break;
            case 6:
                bars = getBarsSupplemental5(this.code);
                width = 47;
                break;
            default:
                throw new RuntimeException(MessageLocalization.getComposedMessage("invalid.code.type", new Object[0]));
        }

        boolean print = true;
        int ptr = 0;
        int height = (int) this.barHeight;
        int[] pix = new int[width * height];
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
        for (k = width; k < pix.length; k += width) {
            System.arraycopy(pix, 0, pix, k, width);
        }
        Image img = canvas.createImage(new MemoryImageSource(width, height, pix, 0, width));

        return img;
    }
}
