package esign.text.xml.xmp;

import esign.text.xml.XMLUtil;
import java.util.ArrayList;

@Deprecated
public class XmpArray
        extends ArrayList<String> {

    private static final long serialVersionUID = 5722854116328732742L;
    public static final String UNORDERED = "rdf:Bag";
    public static final String ORDERED = "rdf:Seq";
    public static final String ALTERNATIVE = "rdf:Alt";
    protected String type;

    public XmpArray(String type) {
        this.type = type;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("<");
        buf.append(this.type);
        buf.append('>');

        for (String string : this) {
            String s = string;
            buf.append("<rdf:li>");
            buf.append(XMLUtil.escapeXML(s, false));
            buf.append("</rdf:li>");
        }
        buf.append("</");
        buf.append(this.type);
        buf.append('>');
        return buf.toString();
    }
}
