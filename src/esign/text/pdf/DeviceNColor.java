package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;

public class DeviceNColor
        extends ExtendedColor {

    PdfDeviceNColor pdfDeviceNColor;
    float[] tints;

    public DeviceNColor(PdfDeviceNColor pdfDeviceNColor, float[] tints) {
        super(6);
        if ((pdfDeviceNColor.getSpotColors()).length != tints.length) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("devicen.color.shall.have.the.same.number.of.colorants.as.the.destination.DeviceN.color.space", new Object[0]));
        }
        this.pdfDeviceNColor = pdfDeviceNColor;
        this.tints = tints;
    }

    public PdfDeviceNColor getPdfDeviceNColor() {
        return this.pdfDeviceNColor;
    }

    public float[] getTints() {
        return this.tints;
    }

    public boolean equals(Object obj) {
        if (obj instanceof DeviceNColor && ((DeviceNColor) obj).tints.length == this.tints.length) {
            int i = 0;
            for (float tint : this.tints) {
                if (tint != ((DeviceNColor) obj).tints[i]) {
                    return false;
                }
                i++;
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        int hashCode = this.pdfDeviceNColor.hashCode();
        float[] arrayOfFloat;
        int i;
        byte b;
        for (arrayOfFloat = this.tints, i = arrayOfFloat.length, b = 0; b < i;) {
            Float tint = Float.valueOf(arrayOfFloat[b]);
            hashCode ^= tint.hashCode();
            b++;
        }

        return hashCode;
    }
}
