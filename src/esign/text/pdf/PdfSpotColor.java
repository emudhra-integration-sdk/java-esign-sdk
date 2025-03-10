package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.error_messages.MessageLocalization;

public class PdfSpotColor
        implements ICachedColorSpace, IPdfSpecialColorSpace {

    public PdfName name;
    public BaseColor altcs;
    public ColorDetails altColorDetails;

    public PdfSpotColor(String name, BaseColor altcs) {
        this.name = new PdfName(name);
        this.altcs = altcs;
    }

    public ColorDetails[] getColorantDetails(PdfWriter writer) {
        if (this.altColorDetails == null && this.altcs instanceof ExtendedColor && ((ExtendedColor) this.altcs).getType() == 7) {
            this.altColorDetails = writer.addSimple(((LabColor) this.altcs).getLabColorSpace());
        }
        return new ColorDetails[]{this.altColorDetails};
    }

    public BaseColor getAlternativeCS() {
        return this.altcs;
    }

    public PdfName getName() {
        return this.name;
    }

    @Deprecated
    protected PdfObject getSpotObject(PdfWriter writer) {
        return getPdfObject(writer);
    }

    public PdfObject getPdfObject(PdfWriter writer) {
        PdfArray array = new PdfArray(PdfName.SEPARATION);
        array.add(this.name);
        PdfFunction func = null;
        if (this.altcs instanceof ExtendedColor) {
            CMYKColor cmyk;
            LabColor lab;
            int type = ((ExtendedColor) this.altcs).type;
            switch (type) {
                case 1:
                    array.add(PdfName.DEVICEGRAY);
                    func = PdfFunction.type2(writer, new float[]{0.0F, 1.0F}, null, new float[]{1.0F}, new float[]{((GrayColor) this.altcs).getGray()}, 1.0F);

                    array.add(func.getReference());
                    return array;
                case 2:
                    array.add(PdfName.DEVICECMYK);
                    cmyk = (CMYKColor) this.altcs;
                    func = PdfFunction.type2(writer, new float[]{0.0F, 1.0F}, null, new float[]{0.0F, 0.0F, 0.0F, 0.0F}, new float[]{cmyk.getCyan(), cmyk.getMagenta(), cmyk.getYellow(), cmyk.getBlack()}, 1.0F);
                    array.add(func.getReference());
                    return array;
                case 7:
                    lab = (LabColor) this.altcs;
                    if (this.altColorDetails != null) {
                        array.add(this.altColorDetails.getIndirectReference());
                    } else {
                        array.add(lab.getLabColorSpace().getPdfObject(writer));
                    }
                    func = PdfFunction.type2(writer, new float[]{0.0F, 1.0F}, null, new float[]{100.0F, 0.0F, 0.0F}, new float[]{lab.getL(), lab.getA(), lab.getB()}, 1.0F);
                    array.add(func.getReference());
                    return array;
            }
            throw new RuntimeException(MessageLocalization.getComposedMessage("only.rgb.gray.and.cmyk.are.supported.as.alternative.color.spaces", new Object[0]));
        }
        array.add(PdfName.DEVICERGB);
        func = PdfFunction.type2(writer, new float[]{0.0F, 1.0F}, null, new float[]{1.0F, 1.0F, 1.0F}, new float[]{this.altcs.getRed() / 255.0F, this.altcs.getGreen() / 255.0F, this.altcs.getBlue() / 255.0F}, 1.0F);
        array.add(func.getReference());
        return array;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PdfSpotColor)) {
            return false;
        }

        PdfSpotColor spotColor = (PdfSpotColor) o;

        if (!this.altcs.equals(spotColor.altcs)) {
            return false;
        }
        if (!this.name.equals(spotColor.name)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = this.name.hashCode();
        result = 31 * result + this.altcs.hashCode();
        return result;
    }
}
