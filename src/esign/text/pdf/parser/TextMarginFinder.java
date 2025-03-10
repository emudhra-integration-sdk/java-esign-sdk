package esign.text.pdf.parser;

import esign.text.awt.geom.Rectangle2D;

public class TextMarginFinder
        implements RenderListener {

    private Rectangle2D.Float textRectangle = null;

    public void renderText(TextRenderInfo renderInfo) {
        if (this.textRectangle == null) {
            this.textRectangle = renderInfo.getDescentLine().getBoundingRectange();
        } else {
            this.textRectangle.add((Rectangle2D) renderInfo.getDescentLine().getBoundingRectange());
        }
        this.textRectangle.add((Rectangle2D) renderInfo.getAscentLine().getBoundingRectange());
    }

    public float getLlx() {
        return this.textRectangle.x;
    }

    public float getLly() {
        return this.textRectangle.y;
    }

    public float getUrx() {
        return this.textRectangle.x + this.textRectangle.width;
    }

    public float getUry() {
        return this.textRectangle.y + this.textRectangle.height;
    }

    public float getWidth() {
        return this.textRectangle.width;
    }

    public float getHeight() {
        return this.textRectangle.height;
    }

    public void beginTextBlock() {
    }

    public void endTextBlock() {
    }

    public void renderImage(ImageRenderInfo renderInfo) {
    }
}
