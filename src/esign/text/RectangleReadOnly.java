package esign.text;

import esign.text.error_messages.MessageLocalization;

public class RectangleReadOnly
        extends Rectangle {

    public RectangleReadOnly(float llx, float lly, float urx, float ury) {
        super(llx, lly, urx, ury);
    }

    public RectangleReadOnly(float llx, float lly, float urx, float ury, int rotation) {
        super(llx, lly, urx, ury);
        super.setRotation(rotation);
    }

    public RectangleReadOnly(float urx, float ury) {
        super(0.0F, 0.0F, urx, ury);
    }

    public RectangleReadOnly(float urx, float ury, int rotation) {
        super(0.0F, 0.0F, urx, ury);
        super.setRotation(rotation);
    }

    public RectangleReadOnly(Rectangle rect) {
        super(rect.llx, rect.lly, rect.urx, rect.ury);
        super.cloneNonPositionParameters(rect);
    }

    private void throwReadOnlyError() {
        throw new UnsupportedOperationException(MessageLocalization.getComposedMessage("rectanglereadonly.this.rectangle.is.read.only", new Object[0]));
    }

    public void setRotation(int rotation) {
        throwReadOnlyError();
    }

    public void setLeft(float llx) {
        throwReadOnlyError();
    }

    public void setRight(float urx) {
        throwReadOnlyError();
    }

    public void setTop(float ury) {
        throwReadOnlyError();
    }

    public void setBottom(float lly) {
        throwReadOnlyError();
    }

    public void normalize() {
        throwReadOnlyError();
    }

    public void setBackgroundColor(BaseColor value) {
        throwReadOnlyError();
    }

    public void setGrayFill(float value) {
        throwReadOnlyError();
    }

    public void setBorder(int border) {
        throwReadOnlyError();
    }

    public void setUseVariableBorders(boolean useVariableBorders) {
        throwReadOnlyError();
    }

    public void enableBorderSide(int side) {
        throwReadOnlyError();
    }

    public void disableBorderSide(int side) {
        throwReadOnlyError();
    }

    public void setBorderWidth(float borderWidth) {
        throwReadOnlyError();
    }

    public void setBorderWidthLeft(float borderWidthLeft) {
        throwReadOnlyError();
    }

    public void setBorderWidthRight(float borderWidthRight) {
        throwReadOnlyError();
    }

    public void setBorderWidthTop(float borderWidthTop) {
        throwReadOnlyError();
    }

    public void setBorderWidthBottom(float borderWidthBottom) {
        throwReadOnlyError();
    }

    public void setBorderColor(BaseColor borderColor) {
        throwReadOnlyError();
    }

    public void setBorderColorLeft(BaseColor borderColorLeft) {
        throwReadOnlyError();
    }

    public void setBorderColorRight(BaseColor borderColorRight) {
        throwReadOnlyError();
    }

    public void setBorderColorTop(BaseColor borderColorTop) {
        throwReadOnlyError();
    }

    public void setBorderColorBottom(BaseColor borderColorBottom) {
        throwReadOnlyError();
    }

    public void cloneNonPositionParameters(Rectangle rect) {
        throwReadOnlyError();
    }

    public void softCloneNonPositionParameters(Rectangle rect) {
        throwReadOnlyError();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("RectangleReadOnly: ");
        buf.append(getWidth());
        buf.append('x');
        buf.append(getHeight());
        buf.append(" (rot: ");
        buf.append(this.rotation);
        buf.append(" degrees)");
        return buf.toString();
    }
}
