package esign.text.pdf.parser;

import esign.text.awt.geom.Rectangle2D;

public class RegionTextRenderFilter
        extends RenderFilter {

    private final Rectangle2D filterRect;

    public RegionTextRenderFilter(Rectangle2D filterRect) {
        this.filterRect = filterRect;
    }

    public RegionTextRenderFilter(esign.text.Rectangle filterRect) {
        this.filterRect = new esign.text.awt.geom.Rectangle(filterRect);
    }

    public boolean allowText(TextRenderInfo renderInfo) {
        LineSegment segment = renderInfo.getBaseline();
        Vector startPoint = segment.getStartPoint();
        Vector endPoint = segment.getEndPoint();

        float x1 = startPoint.get(0);
        float y1 = startPoint.get(1);
        float x2 = endPoint.get(0);
        float y2 = endPoint.get(1);

        return this.filterRect.intersectsLine(x1, y1, x2, y2);
    }
}
