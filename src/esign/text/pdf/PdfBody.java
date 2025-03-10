package esign.text.pdf;

import esign.text.Chunk;
import esign.text.Element;
import esign.text.ElementListener;
import esign.text.Rectangle;
import java.util.List;

public class PdfBody
        extends Rectangle
        implements Element {

    public PdfBody(Rectangle rectangle) {
        super(rectangle);
    }

    public boolean process(ElementListener listener) {
        return false;
    }

    public int type() {
        return 38;
    }

    public boolean isContent() {
        return false;
    }

    public boolean isNestable() {
        return false;
    }

    public List<Chunk> getChunks() {
        return null;
    }
}
