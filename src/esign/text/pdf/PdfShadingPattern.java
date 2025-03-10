package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;
import java.io.IOException;

public class PdfShadingPattern
        extends PdfDictionary {

    protected PdfShading shading;
    protected PdfWriter writer;
    protected float[] matrix = new float[]{1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F};

    protected PdfName patternName;

    protected PdfIndirectReference patternReference;

    public PdfShadingPattern(PdfShading shading) {
        this.writer = shading.getWriter();
        put(PdfName.PATTERNTYPE, new PdfNumber(2));
        this.shading = shading;
    }

    PdfName getPatternName() {
        return this.patternName;
    }

    PdfName getShadingName() {
        return this.shading.getShadingName();
    }

    PdfIndirectReference getPatternReference() {
        if (this.patternReference == null) {
            this.patternReference = this.writer.getPdfIndirectReference();
        }
        return this.patternReference;
    }

    PdfIndirectReference getShadingReference() {
        return this.shading.getShadingReference();
    }

    void setName(int number) {
        this.patternName = new PdfName("P" + number);
    }

    public void addToBody() throws IOException {
        put(PdfName.SHADING, getShadingReference());
        put(PdfName.MATRIX, new PdfArray(this.matrix));
        this.writer.addToBody(this, getPatternReference());
    }

    public void setMatrix(float[] matrix) {
        if (matrix.length != 6) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("the.matrix.size.must.be.6", new Object[0]));
        }
        this.matrix = matrix;
    }

    public float[] getMatrix() {
        return this.matrix;
    }

    public PdfShading getShading() {
        return this.shading;
    }

    ColorDetails getColorDetails() {
        return this.shading.getColorDetails();
    }
}
