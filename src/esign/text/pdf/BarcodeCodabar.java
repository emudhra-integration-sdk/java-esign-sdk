package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.ExceptionConverter;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.MemoryImageSource;

public class BarcodeCodabar
        extends Barcode {

    private static final byte[][] BARS = new byte[][]{{0, 0, 0, 0, 0, 1, 1}, {0, 0, 0, 0, 1, 1, 0}, {0, 0, 0, 1, 0, 0, 1}, {1, 1, 0, 0, 0, 0, 0}, {0, 0, 1, 0, 0, 1, 0}, {1, 0, 0, 0, 0, 1, 0}, {0, 1, 0, 0, 0, 0, 1}, {0, 1, 0, 0, 1, 0, 0}, {0, 1, 1, 0, 0, 0, 0}, {1, 0, 0, 1, 0, 0, 0}, {0, 0, 0, 1, 1, 0, 0}, {0, 0, 1, 1, 0, 0, 0}, {1, 0, 0, 0, 1, 0, 1}, {1, 0, 1, 0, 0, 0, 1}, {1, 0, 1, 0, 1, 0, 0}, {0, 0, 1, 0, 1, 0, 1}, {0, 0, 1, 1, 0, 1, 0}, {0, 1, 0, 1, 0, 0, 1}, {0, 0, 0, 1, 0, 1, 1}, {0, 0, 0, 1, 1, 1, 0}};

    private static final String CHARS = "0123456789-$:/.+ABCD";

    private static final int START_STOP_IDX = 16;

    public BarcodeCodabar() {
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
            this.startStopText = false;
            this.codeType = 12;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public static byte[] getBarsCodabar(String text) {
        text = text.toUpperCase();
        int len = text.length();
        if (len < 2) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("codabar.must.have.at.least.a.start.and.stop.character", new Object[0]));
        }
        if ("0123456789-$:/.+ABCD".indexOf(text.charAt(0)) < 16 || "0123456789-$:/.+ABCD".indexOf(text.charAt(len - 1)) < 16) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("codabar.must.have.one.of.abcd.as.start.stop.character", new Object[0]));
        }
        byte[] bars = new byte[text.length() * 8 - 1];
        for (int k = 0; k < len; k++) {
            int idx = "0123456789-$:/.+ABCD".indexOf(text.charAt(k));
            if (idx >= 16 && k > 0 && k < len - 1) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("in.codabar.start.stop.characters.are.only.allowed.at.the.extremes", new Object[0]));
            }
            if (idx < 0) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.character.1.is.illegal.in.codabar", text.charAt(k)));
            }
            System.arraycopy(BARS[idx], 0, bars, k * 8, 7);
        }
        return bars;
    }

    public static String calculateChecksum(String code) {
        if (code.length() < 2) {
            return code;
        }
        String text = code.toUpperCase();
        int sum = 0;
        int len = text.length();
        for (int k = 0; k < len; k++) {
            sum += "0123456789-$:/.+ABCD".indexOf(text.charAt(k));
        }
        sum = (sum + 15) / 16 * 16 - sum;
        return code.substring(0, len - 1) + "0123456789-$:/.+ABCD".charAt(sum) + code.substring(len - 1);
    }

    public Rectangle getBarcodeSize() {
        float fontX = 0.0F;
        float fontY = 0.0F;
        String text = this.code;
        if (this.generateChecksum && this.checksumText) {
            text = calculateChecksum(this.code);
        }
        if (!this.startStopText) {
            text = text.substring(1, text.length() - 1);
        }
        if (this.font != null) {
            if (this.baseline > 0.0F) {
                fontY = this.baseline - this.font.getFontDescriptor(3, this.size);
            } else {
                fontY = -this.baseline + this.size;
            }
            fontX = this.font.getWidthPoint((this.altText != null) ? this.altText : text, this.size);
        }
        text = this.code;
        if (this.generateChecksum) {
            text = calculateChecksum(this.code);
        }
        byte[] bars = getBarsCodabar(text);
        int wide = 0;
        for (int k = 0; k < bars.length; k++) {
            wide += bars[k];
        }
        int narrow = bars.length - wide;
        float fullWidth = this.x * (narrow + wide * this.n);
        fullWidth = Math.max(fullWidth, fontX);
        float fullHeight = this.barHeight + fontY;
        return new Rectangle(fullWidth, fullHeight);
    }

    public Rectangle placeBarcode(PdfContentByte cb, BaseColor barColor, BaseColor textColor) {
        String fullCode = this.code;
        if (this.generateChecksum && this.checksumText) {
            fullCode = calculateChecksum(this.code);
        }
        if (!this.startStopText) {
            fullCode = fullCode.substring(1, fullCode.length() - 1);
        }
        float fontX = 0.0F;
        if (this.font != null) {
            fontX = this.font.getWidthPoint(fullCode = (this.altText != null) ? this.altText : fullCode, this.size);
        }
        byte[] bars = getBarsCodabar(this.generateChecksum ? calculateChecksum(this.code) : this.code);
        int wide = 0;
        for (int k = 0; k < bars.length; k++) {
            wide += bars[k];
        }
        int narrow = bars.length - wide;
        float fullWidth = this.x * (narrow + wide * this.n);
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
        boolean print = true;
        if (barColor != null) {
            cb.setColorFill(barColor);
        }
        for (int i = 0; i < bars.length; i++) {
            float w = (bars[i] == 0) ? this.x : (this.x * this.n);
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

        String fullCode = this.code;
        if (this.generateChecksum && this.checksumText) {
            fullCode = calculateChecksum(this.code);
        }
        if (!this.startStopText) {
            fullCode = fullCode.substring(1, fullCode.length() - 1);
        }
        byte[] bars = getBarsCodabar(this.generateChecksum ? calculateChecksum(this.code) : this.code);
        int wide = 0;
        for (int k = 0; k < bars.length; k++) {
            wide += bars[k];
        }
        int narrow = bars.length - wide;
        int fullWidth = narrow + wide * (int) this.n;
        boolean print = true;
        int ptr = 0;
        int height = (int) this.barHeight;
        int[] pix = new int[fullWidth * height];
        int i;
        for (i = 0; i < bars.length; i++) {
            int w = (bars[i] == 0) ? 1 : (int) this.n;
            int c = g;
            if (print) {
                c = f;
            }
            print = !print;
            for (int j = 0; j < w; j++) {
                pix[ptr++] = c;
            }
        }
        for (i = fullWidth; i < pix.length; i += fullWidth) {
            System.arraycopy(pix, 0, pix, i, fullWidth);
        }
        Image img = canvas.createImage(new MemoryImageSource(fullWidth, height, pix, 0, fullWidth));

        return img;
    }
}
