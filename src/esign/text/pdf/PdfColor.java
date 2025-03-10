package esign.text.pdf;

import esign.text.BaseColor;

class PdfColor
        extends PdfArray {

    PdfColor(int red, int green, int blue) {
        super(new PdfNumber((red & 0xFF) / 255.0D));
        add(new PdfNumber((green & 0xFF) / 255.0D));
        add(new PdfNumber((blue & 0xFF) / 255.0D));
    }

    PdfColor(BaseColor color) {
        this(color.getRed(), color.getGreen(), color.getBlue());
    }
}
