package esign.text.xml.xmp;

import esign.text.xml.XMLUtil;
import java.util.Enumeration;
import java.util.Properties;

@Deprecated
public abstract class XmpSchema
        extends Properties {

    private static final long serialVersionUID = -176374295948945272L;
    protected String xmlns;

    public XmpSchema(String xmlns) {
        this.xmlns = xmlns;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (Enumeration<?> e = propertyNames(); e.hasMoreElements();) {
            process(buf, e.nextElement());
        }
        return buf.toString();
    }

    protected void process(StringBuffer buf, Object p) {
        buf.append('<');
        buf.append(p);
        buf.append('>');
        buf.append(get(p));
        buf.append("</");
        buf.append(p);
        buf.append('>');
    }

    public String getXmlns() {
        return this.xmlns;
    }

    public Object addProperty(String key, String value) {
        return setProperty(key, value);
    }

    public Object setProperty(String key, String value) {
        return super.setProperty(key, XMLUtil.escapeXML(value, false));
    }

    public Object setProperty(String key, XmpArray value) {
        return super.setProperty(key, value.toString());
    }

    public Object setProperty(String key, LangAlt value) {
        return super.setProperty(key, value.toString());
    }

    public static String escape(String content) {
        return XMLUtil.escapeXML(content, false);
    }
}
