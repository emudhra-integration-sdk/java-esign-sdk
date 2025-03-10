package esign.text.pdf.parser;

public class FilteredTextRenderListener
        extends FilteredRenderListener
        implements TextExtractionStrategy {

    private final TextExtractionStrategy delegate;

    public FilteredTextRenderListener(TextExtractionStrategy delegate, RenderFilter... filters) {
        super(delegate, filters);
        this.delegate = delegate;
    }

    public String getResultantText() {
        return this.delegate.getResultantText();
    }
}
