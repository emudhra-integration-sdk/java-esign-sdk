package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.error_messages.MessageLocalization;
import java.util.Arrays;

public class PdfLabColor
        implements ICachedColorSpace {

    float[] whitePoint = new float[]{0.9505F, 1.0F, 1.089F};
    float[] blackPoint = null;
    float[] range = null;

    public PdfLabColor() {
    }

    public PdfLabColor(float[] whitePoint) {
        if (whitePoint == null || whitePoint.length != 3 || whitePoint[0] < 1.0E-6F || whitePoint[2] < 1.0E-6F || whitePoint[1] < 0.999999F || whitePoint[1] > 1.000001F) {

            throw new RuntimeException(MessageLocalization.getComposedMessage("lab.cs.white.point", new Object[0]));
        }
        this.whitePoint = whitePoint;
    }

    public PdfLabColor(float[] whitePoint, float[] blackPoint) {
        this(whitePoint);
        this.blackPoint = blackPoint;
    }

    public PdfLabColor(float[] whitePoint, float[] blackPoint, float[] range) {
        this(whitePoint, blackPoint);
        this.range = range;
    }

    public PdfObject getPdfObject(PdfWriter writer) {
        PdfArray array = new PdfArray(PdfName.LAB);
        PdfDictionary dictionary = new PdfDictionary();
        if (this.whitePoint == null || this.whitePoint.length != 3 || this.whitePoint[0] < 1.0E-6F || this.whitePoint[2] < 1.0E-6F || this.whitePoint[1] < 0.999999F || this.whitePoint[1] > 1.000001F) {

            throw new RuntimeException(MessageLocalization.getComposedMessage("lab.cs.white.point", new Object[0]));
        }
        dictionary.put(PdfName.WHITEPOINT, new PdfArray(this.whitePoint));
        if (this.blackPoint != null) {
            if (this.blackPoint.length != 3 || this.blackPoint[0] < -1.0E-6F || this.blackPoint[1] < -1.0E-6F || this.blackPoint[2] < -1.0E-6F) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("lab.cs.black.point", new Object[0]));
            }
            dictionary.put(PdfName.BLACKPOINT, new PdfArray(this.blackPoint));
        }
        if (this.range != null) {
            if (this.range.length != 4 || this.range[0] > this.range[1] || this.range[2] > this.range[3]) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("lab.cs.range", new Object[0]));
            }
            dictionary.put(PdfName.RANGE, new PdfArray(this.range));
        }
        array.add(dictionary);
        return array;
    }

    public BaseColor lab2Rgb(float l, float a, float b) {
        double[] clinear = lab2RgbLinear(l, a, b);
        return new BaseColor((float) clinear[0], (float) clinear[1], (float) clinear[2]);
    }

    CMYKColor lab2Cmyk(float l, float a, float b) {
        double[] clinear = lab2RgbLinear(l, a, b);

        double r = clinear[0];
        double g = clinear[1];
        double bee = clinear[2];
        double computedC = 0.0D, computedM = 0.0D, computedY = 0.0D, computedK = 0.0D;

        if (r == 0.0D && g == 0.0D && b == 0.0F) {
            computedK = 1.0D;
        } else {
            computedC = 1.0D - r;
            computedM = 1.0D - g;
            computedY = 1.0D - bee;

            double minCMY = Math.min(computedC,
                    Math.min(computedM, computedY));
            computedC = (computedC - minCMY) / (1.0D - minCMY);
            computedM = (computedM - minCMY) / (1.0D - minCMY);
            computedY = (computedY - minCMY) / (1.0D - minCMY);
            computedK = minCMY;
        }

        return new CMYKColor((float) computedC, (float) computedM, (float) computedY, (float) computedK);
    }

    protected double[] lab2RgbLinear(float l, float a, float b) {
        if (this.range != null && this.range.length == 4) {
            if (a < this.range[0]) {
                a = this.range[0];
            }
            if (a > this.range[1]) {
                a = this.range[1];
            }
            if (b < this.range[2]) {
                b = this.range[2];
            }
            if (b > this.range[3]) {
                b = this.range[3];
            }
        }
        double theta = 0.20689655172413793D;

        double fy = (l + 16.0F) / 116.0D;
        double fx = fy + a / 500.0D;
        double fz = fy - b / 200.0D;

        double x = (fx > theta) ? (this.whitePoint[0] * fx * fx * fx) : ((fx - 0.13793103448275862D) * 3.0D * theta * theta * this.whitePoint[0]);
        double y = (fy > theta) ? (this.whitePoint[1] * fy * fy * fy) : ((fy - 0.13793103448275862D) * 3.0D * theta * theta * this.whitePoint[1]);
        double z = (fz > theta) ? (this.whitePoint[2] * fz * fz * fz) : ((fz - 0.13793103448275862D) * 3.0D * theta * theta * this.whitePoint[2]);

        double[] clinear = new double[3];
        clinear[0] = x * 3.241D - y * 1.5374D - z * 0.4986D;
        clinear[1] = -x * 0.9692D + y * 1.876D - z * 0.0416D;
        clinear[2] = x * 0.0556D - y * 0.204D + z * 1.057D;

        for (int i = 0; i < 3; i++) {
            clinear[i] = (clinear[i] <= 0.0031308D) ? (12.92D * clinear[i]) : (1.055D
                    * Math.pow(clinear[i], 0.4166666666666667D) - 0.055D);
            if (clinear[i] < 0.0D) {
                clinear[i] = 0.0D;
            } else if (clinear[i] > 1.0D) {
                clinear[i] = 1.0D;
            }
        }
        return clinear;
    }

    public LabColor rgb2lab(BaseColor baseColor) {
        double rLinear = (baseColor.getRed() / 255.0F);
        double gLinear = (baseColor.getGreen() / 255.0F);
        double bLinear = (baseColor.getBlue() / 255.0F);

        double r = (rLinear > 0.04045D) ? Math.pow((rLinear + 0.055D) / 1.055D, 2.2D) : (rLinear / 12.92D);
        double g = (gLinear > 0.04045D) ? Math.pow((gLinear + 0.055D) / 1.055D, 2.2D) : (gLinear / 12.92D);
        double b = (bLinear > 0.04045D) ? Math.pow((bLinear + 0.055D) / 1.055D, 2.2D) : (bLinear / 12.92D);

        double x = r * 0.4124D + g * 0.3576D + b * 0.1805D;
        double y = r * 0.2126D + g * 0.7152D + b * 0.0722D;
        double z = r * 0.0193D + g * 0.1192D + b * 0.9505D;

        float l = (float) Math.round((116.0D * fXyz(y / this.whitePoint[1]) - 16.0D) * 1000.0D) / 1000.0F;
        float a = (float) Math.round(500.0D * (fXyz(x / this.whitePoint[0]) - fXyz(y / this.whitePoint[1])) * 1000.0D) / 1000.0F;
        float bee = (float) Math.round(200.0D * (fXyz(y / this.whitePoint[1]) - fXyz(z / this.whitePoint[2])) * 1000.0D) / 1000.0F;

        return new LabColor(this, l, a, bee);
    }

    private static double fXyz(double t) {
        return (t > 0.008856D) ? Math.pow(t, 0.3333333333333333D) : (7.787D * t + 0.13793103448275862D);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PdfLabColor)) {
            return false;
        }

        PdfLabColor that = (PdfLabColor) o;

        if (!Arrays.equals(this.blackPoint, that.blackPoint)) {
            return false;
        }
        if (!Arrays.equals(this.range, that.range)) {
            return false;
        }
        if (!Arrays.equals(this.whitePoint, that.whitePoint)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = Arrays.hashCode(this.whitePoint);
        result = 31 * result + ((this.blackPoint != null) ? Arrays.hashCode(this.blackPoint) : 0);
        result = 31 * result + ((this.range != null) ? Arrays.hashCode(this.range) : 0);
        return result;
    }
}
