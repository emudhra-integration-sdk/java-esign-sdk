package esign.text.pdf.parser;

public interface RenderListener {

    void beginTextBlock();

    void renderText(TextRenderInfo paramTextRenderInfo);

    void endTextBlock();

    void renderImage(ImageRenderInfo paramImageRenderInfo);
}
