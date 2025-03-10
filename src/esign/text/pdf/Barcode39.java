package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.ExceptionConverter;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.MemoryImageSource;

public class Barcode39
        extends Barcode {

    private static final byte[][] BARS = new byte[][]{{0, 0, 0, 1, 1, 0, 1, 0, 0}, {1, 0, 0, 1, 0, 0, 0, 0, 1}, {0, 0, 1, 1, 0, 0, 0, 0, 1}, {1, 0, 1, 1, 0, 0, 0, 0, 0}, {0, 0, 0, 1, 1, 0, 0, 0, 1}, {1, 0, 0, 1, 1, 0, 0, 0, 0}, {0, 0, 1, 1, 1, 0, 0, 0, 0}, {0, 0, 0, 1, 0, 0, 1, 0, 1}, {1, 0, 0, 1, 0, 0, 1, 0, 0}, {0, 0, 1, 1, 0, 0, 1, 0, 0}, {1, 0, 0, 0, 0, 1, 0, 0, 1}, {0, 0, 1, 0, 0, 1, 0, 0, 1}, {1, 0, 1, 0, 0, 1, 0, 0, 0}, {0, 0, 0, 0, 1, 1, 0, 0, 1}, {1, 0, 0, 0, 1, 1, 0, 0, 0}, {0, 0, 1, 0, 1, 1, 0, 0, 0}, {0, 0, 0, 0, 0, 1, 1, 0, 1}, {1, 0, 0, 0, 0, 1, 1, 0, 0}, {0, 0, 1, 0, 0, 1, 1, 0, 0}, {0, 0, 0, 0, 1, 1, 1, 0, 0}, {1, 0, 0, 0, 0, 0, 0, 1, 1}, {0, 0, 1, 0, 0, 0, 0, 1, 1}, {1, 0, 1, 0, 0, 0, 0, 1, 0}, {0, 0, 0, 0, 1, 0, 0, 1, 1}, {1, 0, 0, 0, 1, 0, 0, 1, 0}, {0, 0, 1, 0, 1, 0, 0, 1, 0}, {0, 0, 0, 0, 0, 0, 1, 1, 1}, {1, 0, 0, 0, 0, 0, 1, 1, 0}, {0, 0, 1, 0, 0, 0, 1, 1, 0}, {0, 0, 0, 0, 1, 0, 1, 1, 0}, {1, 1, 0, 0, 0, 0, 0, 0, 1}, {0, 1, 1, 0, 0, 0, 0, 0, 1}, {1, 1, 1, 0, 0, 0, 0, 0, 0}, {0, 1, 0, 0, 1, 0, 0, 0, 1}, {1, 1, 0, 0, 1, 0, 0, 0, 0}, {0, 1, 1, 0, 1, 0, 0, 0, 0}, {0, 1, 0, 0, 0, 0, 1, 0, 1}, {1, 1, 0, 0, 0, 0, 1, 0, 0}, {0, 1, 1, 0, 0, 0, 1, 0, 0}, {0, 1, 0, 1, 0, 1, 0, 0, 0}, {0, 1, 0, 1, 0, 0, 0, 1, 0}, {0, 1, 0, 0, 0, 1, 0, 1, 0}, {0, 0, 0, 1, 0, 1, 0, 1, 0}, {0, 1, 0, 0, 1, 0, 1, 0, 0}};

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%*";

    private static final String EXTENDED = "%U$A$B$C$D$E$F$G$H$I$J$K$L$M$N$O$P$Q$R$S$T$U$V$W$X$Y$Z%A%B%C%D%E  /A/B/C/D/E/F/G/H/I/J/K/L - ./O 0 1 2 3 4 5 6 7 8 9/Z%F%G%H%I%J%V A B C D E F G H I J K L M N O P Q R S T U V W X Y Z%K%L%M%N%O%W+A+B+C+D+E+F+G+H+I+J+K+L+M+N+O+P+Q+R+S+T+U+V+W+X+Y+Z%P%Q%R%S%T";

    public Barcode39() {
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
            this.startStopText = true;
            this.extended = false;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public static byte[] getBarsCode39(String text) {
        text = "*" + text + "*";
        byte[] bars = new byte[text.length() * 10 - 1];
        for (int k = 0; k < text.length(); k++) {
            int idx = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%*".indexOf(text.charAt(k));
            if (idx < 0) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.character.1.is.illegal.in.code.39", text.charAt(k)));
            }
            System.arraycopy(BARS[idx], 0, bars, k * 10, 9);
        }
        return bars;
    }

    public static String getCode39Ex(String text) {
        StringBuilder out = new StringBuilder("");
        for (int k = 0; k < text.length(); k++) {
            char c = text.charAt(k);
            if (c > '') {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.character.1.is.illegal.in.code.39.extended", c));
            }
            char c1 = "%U$A$B$C$D$E$F$G$H$I$J$K$L$M$N$O$P$Q$R$S$T$U$V$W$X$Y$Z%A%B%C%D%E  /A/B/C/D/E/F/G/H/I/J/K/L - ./O 0 1 2 3 4 5 6 7 8 9/Z%F%G%H%I%J%V A B C D E F G H I J K L M N O P Q R S T U V W X Y Z%K%L%M%N%O%W+A+B+C+D+E+F+G+H+I+J+K+L+M+N+O+P+Q+R+S+T+U+V+W+X+Y+Z%P%Q%R%S%T".charAt(c * 2);
            char c2 = "%U$A$B$C$D$E$F$G$H$I$J$K$L$M$N$O$P$Q$R$S$T$U$V$W$X$Y$Z%A%B%C%D%E  /A/B/C/D/E/F/G/H/I/J/K/L - ./O 0 1 2 3 4 5 6 7 8 9/Z%F%G%H%I%J%V A B C D E F G H I J K L M N O P Q R S T U V W X Y Z%K%L%M%N%O%W+A+B+C+D+E+F+G+H+I+J+K+L+M+N+O+P+Q+R+S+T+U+V+W+X+Y+Z%P%Q%R%S%T".charAt(c * 2 + 1);
            if (c1 != ' ') {
                out.append(c1);
            }
            out.append(c2);
        }
        return out.toString();
    }

    static char getChecksum(String text) {
        int chk = 0;
        for (int k = 0; k < text.length(); k++) {
            int idx = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%*".indexOf(text.charAt(k));
            if (idx < 0) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.character.1.is.illegal.in.code.39", text.charAt(k)));
            }
            chk += idx;
        }
        return "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%*".charAt(chk % 43);
    }

    public Rectangle getBarcodeSize() {
        float fontX = 0.0F;
        float fontY = 0.0F;
        String fCode = this.code;
        if (this.extended) {
            fCode = getCode39Ex(this.code);
        }
        if (this.font != null) {
            if (this.baseline > 0.0F) {
                fontY = this.baseline - this.font.getFontDescriptor(3, this.size);
            } else {
                fontY = -this.baseline + this.size;
            }
            String fullCode = this.code;
            if (this.generateChecksum && this.checksumText) {
                fullCode = fullCode + getChecksum(fCode);
            }
            if (this.startStopText) {
                fullCode = "*" + fullCode + "*";
            }
            fontX = this.font.getWidthPoint((this.altText != null) ? this.altText : fullCode, this.size);
        }
        int len = fCode.length() + 2;
        if (this.generateChecksum) {
            len++;
        }
        float fullWidth = len * (6.0F * this.x + 3.0F * this.x * this.n) + (len - 1) * this.x;
        fullWidth = Math.max(fullWidth, fontX);
        float fullHeight = this.barHeight + fontY;
        return new Rectangle(fullWidth, fullHeight);
    }

    public Rectangle placeBarcode(PdfContentByte cb, BaseColor barColor, BaseColor textColor) {
        String fullCode = this.code;
        float fontX = 0.0F;
        String bCode = this.code;
        if (this.extended) {
            bCode = getCode39Ex(this.code);
        }
        if (this.font != null) {
            if (this.generateChecksum && this.checksumText) {
                fullCode = fullCode + getChecksum(bCode);
            }
            if (this.startStopText) {
                fullCode = "*" + fullCode + "*";
            }
            fontX = this.font.getWidthPoint(fullCode = (this.altText != null) ? this.altText : fullCode, this.size);
        }
        if (this.generateChecksum) {
            bCode = bCode + getChecksum(bCode);
        }
        int len = bCode.length() + 2;
        float fullWidth = len * (6.0F * this.x + 3.0F * this.x * this.n) + (len - 1) * this.x;
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
        byte[] bars = getBarsCode39(bCode);
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

        String bCode = this.code;
        if (this.extended) {
            bCode = getCode39Ex(this.code);
        }
        if (this.generateChecksum) {
            bCode = bCode + getChecksum(bCode);
        }
        int len = bCode.length() + 2;
        int nn = (int) this.n;
        int fullWidth = len * (6 + 3 * nn) + len - 1;
        byte[] bars = getBarsCode39(bCode);
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
