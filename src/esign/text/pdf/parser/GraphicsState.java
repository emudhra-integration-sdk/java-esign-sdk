package esign.text.pdf.parser;

import esign.text.BaseColor;
import esign.text.pdf.CMapAwareDocumentFont;
import esign.text.pdf.PdfName;

public class GraphicsState {

    Matrix ctm;
    float characterSpacing;
    float wordSpacing;
    float horizontalScaling;
    float leading;
    CMapAwareDocumentFont font;
    float fontSize;
    int renderMode;
    float rise;
    boolean knockout;
    PdfName colorSpaceFill;
    PdfName colorSpaceStroke;
    BaseColor fillColor = BaseColor.BLACK;

    BaseColor strokeColor = BaseColor.BLACK;

    private float lineWidth;

    private int lineCapStyle;

    private int lineJoinStyle;

    private float miterLimit;

    private LineDashPattern lineDashPattern;

    public GraphicsState() {
        this.ctm = new Matrix();
        this.characterSpacing = 0.0F;
        this.wordSpacing = 0.0F;
        this.horizontalScaling = 1.0F;
        this.leading = 0.0F;
        this.font = null;
        this.fontSize = 0.0F;
        this.renderMode = 0;
        this.rise = 0.0F;
        this.knockout = true;
        this.colorSpaceFill = null;
        this.colorSpaceStroke = null;
        this.fillColor = null;
        this.strokeColor = null;
        this.lineWidth = 1.0F;
        this.lineCapStyle = 0;
        this.lineJoinStyle = 0;
        this.miterLimit = 10.0F;
    }

    public GraphicsState(GraphicsState source) {
        this.ctm = source.ctm;
        this.characterSpacing = source.characterSpacing;
        this.wordSpacing = source.wordSpacing;
        this.horizontalScaling = source.horizontalScaling;
        this.leading = source.leading;
        this.font = source.font;
        this.fontSize = source.fontSize;
        this.renderMode = source.renderMode;
        this.rise = source.rise;
        this.knockout = source.knockout;
        this.colorSpaceFill = source.colorSpaceFill;
        this.colorSpaceStroke = source.colorSpaceStroke;
        this.fillColor = source.fillColor;
        this.strokeColor = source.strokeColor;
        this.lineWidth = source.lineWidth;
        this.lineCapStyle = source.lineCapStyle;
        this.lineJoinStyle = source.lineJoinStyle;
        this.miterLimit = source.miterLimit;

        if (source.lineDashPattern != null) {
            this.lineDashPattern = new LineDashPattern(source.lineDashPattern.getDashArray(), source.lineDashPattern.getDashPhase());
        }
    }

    public Matrix getCtm() {
        return this.ctm;
    }

    public float getCharacterSpacing() {
        return this.characterSpacing;
    }

    public float getWordSpacing() {
        return this.wordSpacing;
    }

    public float getHorizontalScaling() {
        return this.horizontalScaling;
    }

    public float getLeading() {
        return this.leading;
    }

    public CMapAwareDocumentFont getFont() {
        return this.font;
    }

    public float getFontSize() {
        return this.fontSize;
    }

    public int getRenderMode() {
        return this.renderMode;
    }

    public float getRise() {
        return this.rise;
    }

    public boolean isKnockout() {
        return this.knockout;
    }

    public PdfName getColorSpaceFill() {
        return this.colorSpaceFill;
    }

    public PdfName getColorSpaceStroke() {
        return this.colorSpaceStroke;
    }

    public BaseColor getFillColor() {
        return this.fillColor;
    }

    public BaseColor getStrokeColor() {
        return this.strokeColor;
    }

    public float getLineWidth() {
        return this.lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public int getLineCapStyle() {
        return this.lineCapStyle;
    }

    public void setLineCapStyle(int lineCapStyle) {
        this.lineCapStyle = lineCapStyle;
    }

    public int getLineJoinStyle() {
        return this.lineJoinStyle;
    }

    public void setLineJoinStyle(int lineJoinStyle) {
        this.lineJoinStyle = lineJoinStyle;
    }

    public float getMiterLimit() {
        return this.miterLimit;
    }

    public void setMiterLimit(float miterLimit) {
        this.miterLimit = miterLimit;
    }

    public LineDashPattern getLineDashPattern() {
        return this.lineDashPattern;
    }

    public void setLineDashPattern(LineDashPattern lineDashPattern) {
        this.lineDashPattern = new LineDashPattern(lineDashPattern.getDashArray(), lineDashPattern.getDashPhase());
    }
}
