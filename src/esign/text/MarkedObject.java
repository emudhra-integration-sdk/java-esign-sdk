package esign.text;

import java.util.List;
import java.util.Properties;

@Deprecated
public class MarkedObject
        implements Element {

    protected Element element;
    protected Properties markupAttributes = new Properties();

    protected MarkedObject() {
        this.element = null;
    }

    public MarkedObject(Element element) {
        this.element = element;
    }

    public List<Chunk> getChunks() {
        return this.element.getChunks();
    }

    public boolean process(ElementListener listener) {
        try {
            return listener.add(this.element);
        } catch (DocumentException de) {
            return false;
        }
    }

    public int type() {
        return 50;
    }

    public boolean isContent() {
        return true;
    }

    public boolean isNestable() {
        return true;
    }

    public Properties getMarkupAttributes() {
        return this.markupAttributes;
    }

    public void setMarkupAttribute(String key, String value) {
        this.markupAttributes.setProperty(key, value);
    }
}
