package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.Rectangle;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.MemoryImageSource;

public class BarcodePostnet
        extends Barcode {

    private static final byte[][] BARS = new byte[][]{{1, 1, 0, 0, 0}, {0, 0, 0, 1, 1}, {0, 0, 1, 0, 1}, {0, 0, 1, 1, 0}, {0, 1, 0, 0, 1}, {0, 1, 0, 1, 0}, {0, 1, 1, 0, 0}, {1, 0, 0, 0, 1}, {1, 0, 0, 1, 0}, {1, 0, 1, 0, 0}};

    public static byte[] getBarsPostnet(String text) {
        int total = 0;
        for (int k = text.length() - 1; k >= 0; k--) {
            int n = text.charAt(k) - 48;
            total += n;
        }
        text = text + (char) ((10 - total % 10) % 10 + 48);
        byte[] bars = new byte[text.length() * 5 + 2];
        bars[0] = 1;
        bars[bars.length - 1] = 1;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i) - 48;
            System.arraycopy(BARS[c], 0, bars, i * 5 + 1, 5);
        }
        return bars;
    }

    public Rectangle getBarcodeSize() {
        float width = ((this.code.length() + 1) * 5 + 1) * this.n + this.x;
        return new Rectangle(width, this.barHeight);
    }

    public Rectangle placeBarcode(PdfContentByte cb, BaseColor barColor, BaseColor textColor) {
        if (barColor != null) {
            cb.setColorFill(barColor);
        }
        byte[] bars = getBarsPostnet(this.code);
        byte flip = 1;
        if (this.codeType == 8) {
            flip = 0;
            bars[0] = 0;
            bars[bars.length - 1] = 0;
        }
        float startX = 0.0F;
        for (int k = 0; k < bars.length; k++) {
            cb.rectangle(startX, 0.0F, this.x - this.inkSpreading, (bars[k] == flip) ? this.barHeight : this.size);
            startX += this.n;
        }
        cb.fill();
        return getBarcodeSize();
    }

    public Image createAwtImage(Color foreground, Color background) {
        int f = foreground.getRGB();
        int g = background.getRGB();
        Canvas canvas = new Canvas();
        int barWidth = (int) this.x;
        if (barWidth <= 0) {
            barWidth = 1;
        }
        int barDistance = (int) this.n;
        if (barDistance <= barWidth) {
            barDistance = barWidth + 1;
        }
        int barShort = (int) this.size;
        if (barShort <= 0) {
            barShort = 1;
        }
        int barTall = (int) this.barHeight;
        if (barTall <= barShort) {
            barTall = barShort + 1;
        }
        int width = ((this.code.length() + 1) * 5 + 1) * barDistance + barWidth;
        int[] pix = new int[width * barTall];
        byte[] bars = getBarsPostnet(this.code);
        byte flip = 1;
        if (this.codeType == 8) {
            flip = 0;
            bars[0] = 0;
            bars[bars.length - 1] = 0;
        }
        int idx = 0;
        for (int k = 0; k < bars.length; k++) {
            boolean dot = (bars[k] == flip);
            for (int j = 0; j < barDistance; j++) {
                pix[idx + j] = (dot && j < barWidth) ? f : g;
            }
            idx += barDistance;
        }
        int limit = width * (barTall - barShort);
        int i;
        for (i = width; i < limit; i += width) {
            System.arraycopy(pix, 0, pix, i, width);
        }
        idx = limit;
        for (i = 0; i < bars.length; i++) {
            for (int j = 0; j < barDistance; j++) {
                pix[idx + j] = (j < barWidth) ? f : g;
            }
            idx += barDistance;
        }
        for (i = limit + width; i < pix.length; i += width) {
            System.arraycopy(pix, limit, pix, i, width);
        }
        Image img = canvas.createImage(new MemoryImageSource(width, barTall, pix, 0, width));

        return img;
    }
}
