package esign.text;

import esign.text.api.WriterOperation;
import java.util.ArrayList;
import java.util.List;

public abstract class WritableDirectElement
        implements Element, WriterOperation {

    public static final int DIRECT_ELEMENT_TYPE_UNKNOWN = 0;
    public static final int DIRECT_ELEMENT_TYPE_HEADER = 1;
    protected int directElementType = 0;

    public WritableDirectElement() {
    }

    public WritableDirectElement(int directElementType) {
        this.directElementType = directElementType;
    }

    public boolean process(ElementListener listener) {
        throw new UnsupportedOperationException();
    }

    public int type() {
        return 666;
    }

    public boolean isContent() {
        return false;
    }

    public boolean isNestable() {
        throw new UnsupportedOperationException();
    }

    public List<Chunk> getChunks() {
        return new ArrayList<Chunk>(0);
    }

    public int getDirectElementType() {
        return this.directElementType;
    }
}
