package esign.text.pdf.parser;

import java.util.ArrayList;
import java.util.List;

public class MultiFilteredRenderListener
        implements RenderListener {

    private final List<RenderListener> delegates = new ArrayList<RenderListener>();
    private final List<RenderFilter[]> filters = (List) new ArrayList<RenderFilter>();

    public <E extends RenderListener> E attachRenderListener(E delegate, RenderFilter... filterSet) {
        this.delegates.add((RenderListener) delegate);
        this.filters.add(filterSet);

        return delegate;
    }

    public void beginTextBlock() {
        for (RenderListener delegate : this.delegates) {
            delegate.beginTextBlock();
        }
    }

    public void renderText(TextRenderInfo renderInfo) {
        for (int i = 0; i < this.delegates.size(); i++) {
            boolean filtersPassed = true;
            for (RenderFilter filter : (RenderFilter[]) this.filters.get(i)) {
                if (!filter.allowText(renderInfo)) {
                    filtersPassed = false;
                    break;
                }
            }
            if (filtersPassed) {
                ((RenderListener) this.delegates.get(i)).renderText(renderInfo);
            }
        }
    }

    public void endTextBlock() {
        for (RenderListener delegate : this.delegates) {
            delegate.endTextBlock();
        }
    }

    public void renderImage(ImageRenderInfo renderInfo) {
        for (int i = 0; i < this.delegates.size(); i++) {
            boolean filtersPassed = true;
            for (RenderFilter filter : (RenderFilter[]) this.filters.get(i)) {
                if (!filter.allowImage(renderInfo)) {
                    filtersPassed = false;
                    break;
                }
            }
            if (filtersPassed) {
                ((RenderListener) this.delegates.get(i)).renderImage(renderInfo);
            }
        }
    }
}
