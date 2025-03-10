package esign.text.pdf.parser;

public abstract class RenderFilter {

    public boolean allowText(TextRenderInfo renderInfo) {
        return true;
    }

    public boolean allowImage(ImageRenderInfo renderInfo) {
        return true;
    }
}
