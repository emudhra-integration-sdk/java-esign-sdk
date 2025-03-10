package esign.text.pdf.parser;

public class MarkedContentRenderFilter
        extends RenderFilter {

    private int mcid;

    public MarkedContentRenderFilter(int mcid) {
        this.mcid = mcid;
    }

    public boolean allowText(TextRenderInfo renderInfo) {
        return renderInfo.hasMcid(this.mcid);
    }
}
