package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.ExceptionConverter;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.MemoryImageSource;

public class BarcodeInter25
        extends Barcode {

    private static final byte[][] BARS = new byte[][]{{0, 0, 1, 1, 0}, {1, 0, 0, 0, 1}, {0, 1, 0, 0, 1}, {1, 1, 0, 0, 0}, {0, 0, 1, 0, 1}, {1, 0, 1, 0, 0}, {0, 1, 1, 0, 0}, {0, 0, 0, 1, 1}, {1, 0, 0, 1, 0}, {0, 1, 0, 1, 0}};

    public BarcodeInter25() {
        try {
            this.x = 0.8F;
            this.n = 2.0F;
            this.font = BaseFont.createFont("Helvetica", "winansi", false);
            this.size = 8.0F;
            this.baseline = this.size;
            this.barHeight = this.size * 3.0F;
            this.textAlignment = 1;
            this.generateChecksum = false;
            this.checksumText = false;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public static String keepNumbers(String text) {
        StringBuffer sb = new StringBuffer();
        for (int k = 0; k < text.length(); k++) {
            char c = text.charAt(k);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static char getChecksum(String text) {
        int mul = 3;
        int total = 0;
        for (int k = text.length() - 1; k >= 0; k--) {
            int n = text.charAt(k) - 48;
            total += mul * n;
            mul ^= 0x2;
        }
        return (char) ((10 - total % 10) % 10 + 48);
    }

    public static byte[] getBarsInter25(String text) {
        text = keepNumbers(text);
        if ((text.length() & 0x1) != 0) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.text.length.must.be.even", new Object[0]));
        }
        byte[] bars = new byte[text.length() * 5 + 7];
        int pb = 0;
        bars[pb++] = 0;
        bars[pb++] = 0;
        bars[pb++] = 0;
        bars[pb++] = 0;
        int len = text.length() / 2;
        for (int k = 0; k < len; k++) {
            int c1 = text.charAt(k * 2) - 48;
            int c2 = text.charAt(k * 2 + 1) - 48;
            byte[] b1 = BARS[c1];
            byte[] b2 = BARS[c2];
            for (int j = 0; j < 5; j++) {
                bars[pb++] = b1[j];
                bars[pb++] = b2[j];
            }
        }
        bars[pb++] = 1;
        bars[pb++] = 0;
        bars[pb++] = 0;
        return bars;
    }

    public Rectangle getBarcodeSize() {
        float fontX = 0.0F;
        float fontY = 0.0F;
        if (this.font != null) {
            if (this.baseline > 0.0F) {
                fontY = this.baseline - this.font.getFontDescriptor(3, this.size);
            } else {
                fontY = -this.baseline + this.size;
            }
            String str = this.code;
            if (this.generateChecksum && this.checksumText) {
                str = str + getChecksum(str);
            }
            fontX = this.font.getWidthPoint((this.altText != null) ? this.altText : str, this.size);
        }
        String fullCode = keepNumbers(this.code);
        int len = fullCode.length();
        if (this.generateChecksum) {
            len++;
        }
        float fullWidth = len * (3.0F * this.x + 2.0F * this.x * this.n) + (6.0F + this.n) * this.x;
        fullWidth = Math.max(fullWidth, fontX);
        float fullHeight = this.barHeight + fontY;
        return new Rectangle(fullWidth, fullHeight);
    }

    public Rectangle placeBarcode(PdfContentByte cb, BaseColor barColor, BaseColor textColor) {
        String fullCode = this.code;
        float fontX = 0.0F;
        if (this.font != null) {
            if (this.generateChecksum && this.checksumText) {
                fullCode = fullCode + getChecksum(fullCode);
            }
            fontX = this.font.getWidthPoint(fullCode = (this.altText != null) ? this.altText : fullCode, this.size);
        }
        String bCode = keepNumbers(this.code);
        if (this.generateChecksum) {
            bCode = bCode + getChecksum(bCode);
        }
        int len = bCode.length();
        float fullWidth = len * (3.0F * this.x + 2.0F * this.x * this.n) + (6.0F + this.n) * this.x;
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
        byte[] bars = getBarsInter25(bCode);
        boolean print = true;
        if (barColor != null) {
            cb.setColorFill(barColor);
        }
        for (int k = 0; k < bars.length; k++) {
            float w = (bars[k] == 0) ? this.x : (this.x * this.n);
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

    public Image createAwtImage(Color foreground, Color background) {
        int f = foreground.getRGB();
        int g = background.getRGB();
        Canvas canvas = new Canvas();

        String bCode = keepNumbers(this.code);
        if (this.generateChecksum) {
            bCode = bCode + getChecksum(bCode);
        }
        int len = bCode.length();
        int nn = (int) this.n;
        int fullWidth = len * (3 + 2 * nn) + 6 + nn;
        byte[] bars = getBarsInter25(bCode);
        boolean print = true;
        int ptr = 0;
        int height = (int) this.barHeight;
        int[] pix = new int[fullWidth * height];
        int k;
        for (k = 0; k < bars.length; k++) {
            int w = (bars[k] == 0) ? 1 : nn;
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
        Image img = canvas.createImage(new MemoryImageSource(fullWidth, height, pix, 0, fullWidth));

        return img;
    }
}
