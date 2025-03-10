package esign.text.pdf;

import esign.text.BaseColor;

public class LabColor
        extends ExtendedColor {

    PdfLabColor labColorSpace;
    private float l;
    private float a;
    private float b;

    public LabColor(PdfLabColor labColorSpace, float l, float a, float b) {
        super(7);
        this.labColorSpace = labColorSpace;
        this.l = l;
        this.a = a;
        this.b = b;
        BaseColor altRgbColor = labColorSpace.lab2Rgb(l, a, b);
        setValue(altRgbColor.getRed(), altRgbColor.getGreen(), altRgbColor.getBlue(), 255);
    }

    public PdfLabColor getLabColorSpace() {
        return this.labColorSpace;
    }

    public float getL() {
        return this.l;
    }

    public float getA() {
        return this.a;
    }

    public float getB() {
        return this.b;
    }

    public BaseColor toRgb() {
        return this.labColorSpace.lab2Rgb(this.l, this.a, this.b);
    }

    CMYKColor toCmyk() {
        return this.labColorSpace.lab2Cmyk(this.l, this.a, this.b);
    }
}
