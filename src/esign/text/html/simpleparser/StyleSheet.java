package esign.text.html.simpleparser;

import esign.text.BaseColor;
import esign.text.html.HtmlUtilities;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Deprecated
public class StyleSheet {

    protected Map<String, Map<String, String>> tagMap = new HashMap<String, Map<String, String>>();

    protected Map<String, Map<String, String>> classMap = new HashMap<String, Map<String, String>>();

    public void loadTagStyle(String tag, Map<String, String> attrs) {
        this.tagMap.put(tag.toLowerCase(), attrs);
    }

    public void loadTagStyle(String tag, String key, String value) {
        tag = tag.toLowerCase();
        Map<String, String> styles = this.tagMap.get(tag);
        if (styles == null) {
            styles = new HashMap<String, String>();
            this.tagMap.put(tag, styles);
        }
        styles.put(key, value);
    }

    public void loadStyle(String className, HashMap<String, String> attrs) {
        this.classMap.put(className.toLowerCase(), attrs);
    }

    public void loadStyle(String className, String key, String value) {
        className = className.toLowerCase();
        Map<String, String> styles = this.classMap.get(className);
        if (styles == null) {
            styles = new HashMap<String, String>();
            this.classMap.put(className, styles);
        }
        styles.put(key, value);
    }

    public void applyStyle(String tag, Map<String, String> attrs) {
        Map<String, String> map = this.tagMap.get(tag.toLowerCase());
        if (map != null) {

            Map<String, String> map1 = new HashMap<String, String>(map);

            map1.putAll(attrs);

            attrs.putAll(map1);
        }

        String cm = attrs.get("class");
        if (cm == null) {
            return;
        }
        map = this.classMap.get(cm.toLowerCase());
        if (map == null) {
            return;
        }
        attrs.remove("class");

        Map<String, String> temp = new HashMap<String, String>(map);

        temp.putAll(attrs);

        attrs.putAll(temp);
    }

    public static void resolveStyleAttribute(Map<String, String> h, ChainedProperties chain) {
        String style = h.get("style");
        if (style == null) {
            return;
        }
        Properties prop = HtmlUtilities.parseAttributes(style);
        for (Object element : prop.keySet()) {
            String key = (String) element;
            if (key.equals("font-family")) {
                h.put("face", prop.getProperty(key));
                continue;
            }
            if (key.equals("font-size")) {
                float actualFontSize = HtmlUtilities.parseLength(chain
                        .getProperty("size"), 12.0F);

                if (actualFontSize <= 0.0F) {
                    actualFontSize = 12.0F;
                }
                h.put("size", Float.toString(HtmlUtilities.parseLength(prop
                        .getProperty(key), actualFontSize)) + "pt");
                continue;
            }
            if (key.equals("font-style")) {
                String ss = prop.getProperty(key).trim().toLowerCase();
                if (ss.equals("italic") || ss.equals("oblique")) {
                    h.put("i", null);
                }
                continue;
            }
            if (key.equals("font-weight")) {
                String ss = prop.getProperty(key).trim().toLowerCase();
                if (ss.equals("bold") || ss.equals("700") || ss.equals("800") || ss
                        .equals("900")) {
                    h.put("b", null);
                }
                continue;
            }
            if (key.equals("text-decoration")) {
                String ss = prop.getProperty(key).trim().toLowerCase();
                if (ss.equals("underline")) {
                    h.put("u", null);
                }
                continue;
            }
            if (key.equals("color")) {
                BaseColor c = HtmlUtilities.decodeColor(prop.getProperty(key));
                if (c != null) {
                    int hh = c.getRGB();
                    String hs = Integer.toHexString(hh);
                    hs = "000000" + hs;
                    hs = "#" + hs.substring(hs.length() - 6);
                    h.put("color", hs);
                }
                continue;
            }
            if (key.equals("line-height")) {
                String ss = prop.getProperty(key).trim();
                float actualFontSize = HtmlUtilities.parseLength(chain
                        .getProperty("size"), 12.0F);

                if (actualFontSize <= 0.0F) {
                    actualFontSize = 12.0F;
                }
                float v = HtmlUtilities.parseLength(prop.getProperty(key), actualFontSize);

                if (ss.endsWith("%")) {
                    h.put("leading", "0," + (v / 100.0F));
                    return;
                }
                if ("normal".equalsIgnoreCase(ss)) {
                    h.put("leading", "0,1.5");
                    return;
                }
                h.put("leading", v + ",0");
                continue;
            }
            if (key.equals("text-align")) {
                String ss = prop.getProperty(key).trim().toLowerCase();
                h.put("align", ss);
                continue;
            }
            if (key.equals("padding-left")) {
                String ss = prop.getProperty(key).trim().toLowerCase();
                h.put("indent", Float.toString(HtmlUtilities.parseLength(ss)));
            }
        }
    }
}
