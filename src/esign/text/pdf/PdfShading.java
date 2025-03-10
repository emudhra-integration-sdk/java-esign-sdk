package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.error_messages.MessageLocalization;
import java.io.IOException;

public class PdfShading {

    protected PdfDictionary shading;
    protected PdfWriter writer;
    protected int shadingType;
    protected ColorDetails colorDetails;
    protected PdfName shadingName;
    protected PdfIndirectReference shadingReference;
    private BaseColor cspace;
    protected float[] bBox;
    protected boolean antiAlias = false;

    protected PdfShading(PdfWriter writer) {
        this.writer = writer;
    }

    protected void setColorSpace(BaseColor color) {
        SpotColor spot;
        DeviceNColor deviceNColor;
        this.cspace = color;
        int type = ExtendedColor.getType(color);
        PdfObject colorSpace = null;
        switch (type) {
            case 1:
                colorSpace = PdfName.DEVICEGRAY;
                break;

            case 2:
                colorSpace = PdfName.DEVICECMYK;
                break;

            case 3:
                spot = (SpotColor) color;
                this.colorDetails = this.writer.addSimple(spot.getPdfSpotColor());
                colorSpace = this.colorDetails.getIndirectReference();
                break;

            case 6:
                deviceNColor = (DeviceNColor) color;
                this.colorDetails = this.writer.addSimple(deviceNColor.getPdfDeviceNColor());
                colorSpace = this.colorDetails.getIndirectReference();
                break;

            case 4:
            case 5:
                throwColorSpaceError();

            default:
                colorSpace = PdfName.DEVICERGB;
                break;
        }
        this.shading.put(PdfName.COLORSPACE, colorSpace);
    }

    public BaseColor getColorSpace() {
        return this.cspace;
    }

    public static void throwColorSpaceError() {
        throw new IllegalArgumentException(MessageLocalization.getComposedMessage("a.tiling.or.shading.pattern.cannot.be.used.as.a.color.space.in.a.shading.pattern", new Object[0]));
    }

    public static void checkCompatibleColors(BaseColor c1, BaseColor c2) {
        int type1 = ExtendedColor.getType(c1);
        int type2 = ExtendedColor.getType(c2);
        if (type1 != type2) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("both.colors.must.be.of.the.same.type", new Object[0]));
        }
        if (type1 == 3 && ((SpotColor) c1).getPdfSpotColor() != ((SpotColor) c2).getPdfSpotColor()) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.spot.color.must.be.the.same.only.the.tint.can.vary", new Object[0]));
        }
        if (type1 == 4 || type1 == 5) {
            throwColorSpaceError();
        }
    }

    public static float[] getColorArray(BaseColor color) {
        CMYKColor cmyk;
        int type = ExtendedColor.getType(color);
        switch (type) {
            case 1:
                return new float[]{((GrayColor) color).getGray()};

            case 2:
                cmyk = (CMYKColor) color;
                return new float[]{cmyk.getCyan(), cmyk.getMagenta(), cmyk.getYellow(), cmyk.getBlack()};

            case 3:
                return new float[]{((SpotColor) color).getTint()};

            case 6:
                return ((DeviceNColor) color).getTints();

            case 0:
                return new float[]{color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F};
        }

        throwColorSpaceError();
        return null;
    }

    public static PdfShading type1(PdfWriter writer, BaseColor colorSpace, float[] domain, float[] tMatrix, PdfFunction function) {
        PdfShading sp = new PdfShading(writer);
        sp.shading = new PdfDictionary();
        sp.shadingType = 1;
        sp.shading.put(PdfName.SHADINGTYPE, new PdfNumber(sp.shadingType));
        sp.setColorSpace(colorSpace);
        if (domain != null) {
            sp.shading.put(PdfName.DOMAIN, new PdfArray(domain));
        }
        if (tMatrix != null) {
            sp.shading.put(PdfName.MATRIX, new PdfArray(tMatrix));
        }
        sp.shading.put(PdfName.FUNCTION, function.getReference());
        return sp;
    }

    public static PdfShading type2(PdfWriter writer, BaseColor colorSpace, float[] coords, float[] domain, PdfFunction function, boolean[] extend) {
        PdfShading sp = new PdfShading(writer);
        sp.shading = new PdfDictionary();
        sp.shadingType = 2;
        sp.shading.put(PdfName.SHADINGTYPE, new PdfNumber(sp.shadingType));
        sp.setColorSpace(colorSpace);
        sp.shading.put(PdfName.COORDS, new PdfArray(coords));
        if (domain != null) {
            sp.shading.put(PdfName.DOMAIN, new PdfArray(domain));
        }
        sp.shading.put(PdfName.FUNCTION, function.getReference());
        if (extend != null && (extend[0] || extend[1])) {
            PdfArray array = new PdfArray(extend[0] ? PdfBoolean.PDFTRUE : PdfBoolean.PDFFALSE);
            array.add(extend[1] ? PdfBoolean.PDFTRUE : PdfBoolean.PDFFALSE);
            sp.shading.put(PdfName.EXTEND, array);
        }
        return sp;
    }

    public static PdfShading type3(PdfWriter writer, BaseColor colorSpace, float[] coords, float[] domain, PdfFunction function, boolean[] extend) {
        PdfShading sp = type2(writer, colorSpace, coords, domain, function, extend);
        sp.shadingType = 3;
        sp.shading.put(PdfName.SHADINGTYPE, new PdfNumber(sp.shadingType));
        return sp;
    }

    public static PdfShading simpleAxial(PdfWriter writer, float x0, float y0, float x1, float y1, BaseColor startColor, BaseColor endColor, boolean extendStart, boolean extendEnd) {
        checkCompatibleColors(startColor, endColor);
        PdfFunction function = PdfFunction.type2(writer, new float[]{0.0F, 1.0F}, null, getColorArray(startColor),
                getColorArray(endColor), 1.0F);
        return type2(writer, startColor, new float[]{x0, y0, x1, y1}, null, function, new boolean[]{extendStart, extendEnd});
    }

    public static PdfShading simpleAxial(PdfWriter writer, float x0, float y0, float x1, float y1, BaseColor startColor, BaseColor endColor) {
        return simpleAxial(writer, x0, y0, x1, y1, startColor, endColor, true, true);
    }

    public static PdfShading simpleRadial(PdfWriter writer, float x0, float y0, float r0, float x1, float y1, float r1, BaseColor startColor, BaseColor endColor, boolean extendStart, boolean extendEnd) {
        checkCompatibleColors(startColor, endColor);
        PdfFunction function = PdfFunction.type2(writer, new float[]{0.0F, 1.0F}, null, getColorArray(startColor),
                getColorArray(endColor), 1.0F);
        return type3(writer, startColor, new float[]{x0, y0, r0, x1, y1, r1}, null, function, new boolean[]{extendStart, extendEnd});
    }

    public static PdfShading simpleRadial(PdfWriter writer, float x0, float y0, float r0, float x1, float y1, float r1, BaseColor startColor, BaseColor endColor) {
        return simpleRadial(writer, x0, y0, r0, x1, y1, r1, startColor, endColor, true, true);
    }

    PdfName getShadingName() {
        return this.shadingName;
    }

    PdfIndirectReference getShadingReference() {
        if (this.shadingReference == null) {
            this.shadingReference = this.writer.getPdfIndirectReference();
        }
        return this.shadingReference;
    }

    void setName(int number) {
        this.shadingName = new PdfName("Sh" + number);
    }

    public void addToBody() throws IOException {
        if (this.bBox != null) {
            this.shading.put(PdfName.BBOX, new PdfArray(this.bBox));
        }
        if (this.antiAlias) {
            this.shading.put(PdfName.ANTIALIAS, PdfBoolean.PDFTRUE);
        }
        this.writer.addToBody(this.shading, getShadingReference());
    }

    PdfWriter getWriter() {
        return this.writer;
    }

    ColorDetails getColorDetails() {
        return this.colorDetails;
    }

    public float[] getBBox() {
        return this.bBox;
    }

    public void setBBox(float[] bBox) {
        if (bBox.length != 4) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("bbox.must.be.a.4.element.array", new Object[0]));
        }
        this.bBox = bBox;
    }

    public boolean isAntiAlias() {
        return this.antiAlias;
    }

    public void setAntiAlias(boolean antiAlias) {
        this.antiAlias = antiAlias;
    }
}
