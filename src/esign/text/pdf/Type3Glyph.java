package esign.text.pdf;

import esign.text.DocumentException;
import esign.text.Image;
import esign.text.error_messages.MessageLocalization;

public final class Type3Glyph
        extends PdfContentByte {

    private PageResources pageResources;
    private boolean colorized;

    private Type3Glyph() {
        super(null);
    }

    Type3Glyph(PdfWriter writer, PageResources pageResources, float wx, float llx, float lly, float urx, float ury, boolean colorized) {
        super(writer);
        this.pageResources = pageResources;
        this.colorized = colorized;
        if (colorized) {
            this.content.append(wx).append(" 0 d0\n");
        } else {

            this.content.append(wx).append(" 0 ").append(llx).append(' ').append(lly).append(' ').append(urx).append(' ').append(ury).append(" d1\n");
        }
    }

    PageResources getPageResources() {
        return this.pageResources;
    }

    public void addImage(Image image, float a, float b, float c, float d, float e, float f, boolean inlineImage) throws DocumentException {
        if (!this.colorized && (!image.isMask() || (image.getBpc() != 1 && image.getBpc() <= 255))) {
            throw new DocumentException(MessageLocalization.getComposedMessage("not.colorized.typed3.fonts.only.accept.mask.images", new Object[0]));
        }
        super.addImage(image, a, b, c, d, e, f, inlineImage);
    }

    public PdfContentByte getDuplicate() {
        Type3Glyph dup = new Type3Glyph();
        dup.writer = this.writer;
        dup.pdf = this.pdf;
        dup.pageResources = this.pageResources;
        dup.colorized = this.colorized;
        return dup;
    }
}
