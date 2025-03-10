package esign.text.pdf.draw;

import esign.text.BaseColor;
import esign.text.Font;
import esign.text.pdf.PdfContentByte;

public class LineSeparator
        extends VerticalPositionMark {

    protected float lineWidth = 1.0F;

    protected float percentage = 100.0F;

    protected BaseColor lineColor;

    protected int alignment = 6;

    public LineSeparator(float lineWidth, float percentage, BaseColor lineColor, int align, float offset) {
        this.lineWidth = lineWidth;
        this.percentage = percentage;
        this.lineColor = lineColor;
        this.alignment = align;
        this.offset = offset;
    }

    public LineSeparator(Font font) {
        this.lineWidth = 0.06666667F * font.getSize();
        this.offset = -0.33333334F * font.getSize();
        this.percentage = 100.0F;
        this.lineColor = font.getColor();
    }

    public LineSeparator() {
    }

    public void draw(PdfContentByte canvas, float llx, float lly, float urx, float ury, float y) {
        canvas.saveState();
        drawLine(canvas, llx, urx, y);
        canvas.restoreState();
    }

    public void drawLine(PdfContentByte canvas, float leftX, float rightX, float y) {
        float w, s;
        if (getPercentage() < 0.0F) {
            w = -getPercentage();
        } else {
            w = (rightX - leftX) * getPercentage() / 100.0F;
        }
        switch (getAlignment()) {
            case 0:
                s = 0.0F;
                break;
            case 2:
                s = rightX - leftX - w;
                break;
            default:
                s = (rightX - leftX - w) / 2.0F;
                break;
        }
        canvas.setLineWidth(getLineWidth());
        if (getLineColor() != null) {
            canvas.setColorStroke(getLineColor());
        }
        canvas.moveTo(s + leftX, y + this.offset);
        canvas.lineTo(s + w + leftX, y + this.offset);
        canvas.stroke();
    }

    public float getLineWidth() {
        return this.lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public float getPercentage() {
        return this.percentage;
    }

    public void setPercentage(float percentage) {
        this.percentage = percentage;
    }

    public BaseColor getLineColor() {
        return this.lineColor;
    }

    public void setLineColor(BaseColor color) {
        this.lineColor = color;
    }

    public int getAlignment() {
        return this.alignment;
    }

    public void setAlignment(int align) {
        this.alignment = align;
    }
}
