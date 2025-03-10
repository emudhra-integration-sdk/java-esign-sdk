package esign.text.pdf.parser;

public class GlyphRenderListener
        implements RenderListener {

    private final RenderListener delegate;

    public GlyphRenderListener(RenderListener delegate) {
        this.delegate = delegate;
    }

    public void beginTextBlock() {
        this.delegate.beginTextBlock();
    }

    public void renderText(TextRenderInfo renderInfo) {
        for (TextRenderInfo glyphInfo : renderInfo.getCharacterRenderInfos()) {
            this.delegate.renderText(glyphInfo);
        }
    }

    public void endTextBlock() {
        this.delegate.endTextBlock();
    }

    public void renderImage(ImageRenderInfo renderInfo) {
        this.delegate.renderImage(renderInfo);
    }
}
