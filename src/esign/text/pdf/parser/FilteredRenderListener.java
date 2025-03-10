package esign.text.pdf.parser;

public class FilteredRenderListener
        implements RenderListener {

    private final RenderListener delegate;
    private final RenderFilter[] filters;

    public FilteredRenderListener(RenderListener delegate, RenderFilter... filters) {
        this.delegate = delegate;
        this.filters = filters;
    }

    public void renderText(TextRenderInfo renderInfo) {
        for (RenderFilter filter : this.filters) {
            if (!filter.allowText(renderInfo)) {
                return;
            }
        }
        this.delegate.renderText(renderInfo);
    }

    public void beginTextBlock() {
        this.delegate.beginTextBlock();
    }

    public void endTextBlock() {
        this.delegate.endTextBlock();
    }

    public void renderImage(ImageRenderInfo renderInfo) {
        for (RenderFilter filter : this.filters) {
            if (!filter.allowImage(renderInfo)) {
                return;
            }
        }
        this.delegate.renderImage(renderInfo);
    }
}
