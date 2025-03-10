package esign.text.html.simpleparser;

import esign.text.html.HtmlUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Deprecated
public class ChainedProperties {

    private static final class TagAttributes {

        final String tag;
        final Map<String, String> attrs;

        TagAttributes(String tag, Map<String, String> attrs) {
            this.tag = tag;
            this.attrs = attrs;
        }
    }

    public List<TagAttributes> chain = new ArrayList<TagAttributes>();

    public String getProperty(String key) {
        for (int k = this.chain.size() - 1; k >= 0; k--) {
            TagAttributes p = this.chain.get(k);
            Map<String, String> attrs = p.attrs;
            String ret = attrs.get(key);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public boolean hasProperty(String key) {
        for (int k = this.chain.size() - 1; k >= 0; k--) {
            TagAttributes p = this.chain.get(k);
            Map<String, String> attrs = p.attrs;
            if (attrs.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    public void addToChain(String tag, Map<String, String> props) {
        adjustFontSize(props);
        this.chain.add(new TagAttributes(tag, props));
    }

    public void removeChain(String tag) {
        for (int k = this.chain.size() - 1; k >= 0; k--) {
            if (tag.equals(((TagAttributes) this.chain.get(k)).tag)) {
                this.chain.remove(k);
                return;
            }
        }
    }

    protected void adjustFontSize(Map<String, String> attrs) {
        String value = attrs.get("size");

        if (value == null) {
            return;
        }
        if (value.endsWith("pt")) {
            attrs.put("size", value
                    .substring(0, value.length() - 2));
            return;
        }
        String old = getProperty("size");
        attrs.put("size", Integer.toString(HtmlUtilities.getIndexedFontSize(value, old)));
    }
}
