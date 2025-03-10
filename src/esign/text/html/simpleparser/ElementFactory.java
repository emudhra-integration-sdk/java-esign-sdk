package esign.text.html.simpleparser;

import esign.text.BaseColor;
import esign.text.Chunk;
import esign.text.DocListener;
import esign.text.DocumentException;
import esign.text.Font;
import esign.text.FontFactory;
import esign.text.FontProvider;
import esign.text.Image;
import esign.text.List;
import esign.text.ListItem;
import esign.text.Paragraph;
import esign.text.html.HtmlUtilities;
import esign.text.pdf.HyphenationAuto;
import esign.text.pdf.HyphenationEvent;
import esign.text.pdf.draw.LineSeparator;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

@Deprecated
public class ElementFactory {

    private FontProvider provider = (FontProvider) FontFactory.getFontImp();

    public void setFontProvider(FontProvider provider) {
        this.provider = provider;
    }

    public FontProvider getFontProvider() {
        return this.provider;
    }

    public Font getFont(ChainedProperties chain) {
        String face = chain.getProperty("face");

        if (face == null || face.trim().length() == 0) {
            face = chain.getProperty("font-family");
        }

        if (face != null) {
            StringTokenizer tok = new StringTokenizer(face, ",");
            while (tok.hasMoreTokens()) {
                face = tok.nextToken().trim();
                if (face.startsWith("\"")) {
                    face = face.substring(1);
                }
                if (face.endsWith("\"")) {
                    face = face.substring(0, face.length() - 1);
                }
                if (this.provider.isRegistered(face)) {
                    break;
                }
            }
        }

        String encoding = chain.getProperty("encoding");
        if (encoding == null) {
            encoding = "Cp1252";
        }

        String value = chain.getProperty("size");
        float size = 12.0F;
        if (value != null) {
            size = Float.parseFloat(value);
        }

        int style = 0;

        String decoration = chain.getProperty("text-decoration");
        if (decoration != null && decoration.trim().length() != 0) {
            if ("underline".equals(decoration)) {
                style |= 0x4;
            } else if ("line-through".equals(decoration)) {
                style |= 0x8;
            }
        }

        if (chain.hasProperty("i")) {
            style |= 0x2;
        }
        if (chain.hasProperty("b")) {
            style |= 0x1;
        }
        if (chain.hasProperty("u")) {
            style |= 0x4;
        }
        if (chain.hasProperty("s")) {
            style |= 0x8;
        }

        BaseColor color = HtmlUtilities.decodeColor(chain.getProperty("color"));

        return this.provider.getFont(face, encoding, true, size, style, color);
    }

    public Chunk createChunk(String content, ChainedProperties chain) {
        Font font = getFont(chain);
        Chunk ck = new Chunk(content, font);
        if (chain.hasProperty("sub")) {
            ck.setTextRise(-font.getSize() / 2.0F);
        } else if (chain.hasProperty("sup")) {
            ck.setTextRise(font.getSize() / 2.0F);
        }
        ck.setHyphenation(getHyphenation(chain));
        return ck;
    }

    public Paragraph createParagraph(ChainedProperties chain) {
        Paragraph paragraph = new Paragraph();
        updateElement(paragraph, chain);
        return paragraph;
    }

    public ListItem createListItem(ChainedProperties chain) {
        ListItem item = new ListItem();
        updateElement((Paragraph) item, chain);
        return item;
    }

    protected void updateElement(Paragraph paragraph, ChainedProperties chain) {
        String value = chain.getProperty("align");
        paragraph.setAlignment(HtmlUtilities.alignmentValue(value));

        paragraph.setHyphenation(getHyphenation(chain));

        setParagraphLeading(paragraph, chain.getProperty("leading"));

        value = chain.getProperty("after");
        if (value != null) {
            try {
                paragraph.setSpacingBefore(Float.parseFloat(value));
            } catch (Exception exception) {
            }
        }

        value = chain.getProperty("after");
        if (value != null) {
            try {
                paragraph.setSpacingAfter(Float.parseFloat(value));
            } catch (Exception exception) {
            }
        }

        value = chain.getProperty("extraparaspace");
        if (value != null) {
            try {
                paragraph.setExtraParagraphSpace(Float.parseFloat(value));
            } catch (Exception exception) {
            }
        }

        value = chain.getProperty("indent");
        if (value != null) {
            try {
                paragraph.setIndentationLeft(Float.parseFloat(value));
            } catch (Exception exception) {
            }
        }
    }

    protected static void setParagraphLeading(Paragraph paragraph, String leading) {
        if (leading == null) {
            paragraph.setLeading(0.0F, 1.5F);
            return;
        }
        try {
            StringTokenizer tk = new StringTokenizer(leading, " ,");

            String v = tk.nextToken();
            float v1 = Float.parseFloat(v);
            if (!tk.hasMoreTokens()) {
                paragraph.setLeading(v1, 0.0F);

                return;
            }
            v = tk.nextToken();
            float v2 = Float.parseFloat(v);
            paragraph.setLeading(v1, v2);
        } catch (Exception e) {

            paragraph.setLeading(0.0F, 1.5F);
        }
    }

    public HyphenationEvent getHyphenation(ChainedProperties chain) {
        int leftMin;
        String value = chain.getProperty("hyphenation");

        if (value == null || value.length() == 0) {
            return null;
        }

        int pos = value.indexOf('_');
        if (pos == -1) {
            return (HyphenationEvent) new HyphenationAuto(value, null, 2, 2);
        }

        String lang = value.substring(0, pos);
        String country = value.substring(pos + 1);

        pos = country.indexOf(',');
        if (pos == -1) {
            return (HyphenationEvent) new HyphenationAuto(lang, country, 2, 2);
        }

        int rightMin = 2;
        value = country.substring(pos + 1);
        country = country.substring(0, pos);
        pos = value.indexOf(',');
        if (pos == -1) {
            leftMin = Integer.parseInt(value);
        } else {
            leftMin = Integer.parseInt(value.substring(0, pos));
            rightMin = Integer.parseInt(value.substring(pos + 1));
        }
        return (HyphenationEvent) new HyphenationAuto(lang, country, leftMin, rightMin);
    }

    public LineSeparator createLineSeparator(Map<String, String> attrs, float offset) {
        float lineWidth = 1.0F;
        String size = attrs.get("size");
        if (size != null) {
            float tmpSize = HtmlUtilities.parseLength(size, 12.0F);
            if (tmpSize > 0.0F) {
                lineWidth = tmpSize;
            }
        }
        String width = attrs.get("width");
        float percentage = 100.0F;
        if (width != null) {
            float tmpWidth = HtmlUtilities.parseLength(width, 12.0F);
            if (tmpWidth > 0.0F) {
                percentage = tmpWidth;
            }
            if (!width.endsWith("%")) {
                percentage = 100.0F;
            }
        }
        BaseColor lineColor = null;

        int align = HtmlUtilities.alignmentValue(attrs.get("align"));
        return new LineSeparator(lineWidth, percentage, lineColor, align, offset);
    }

    public Image createImage(String src, Map<String, String> attrs, ChainedProperties chain, DocListener document, ImageProvider img_provider, HashMap<String, Image> img_store, String img_baseurl) throws DocumentException, IOException {
        Image img = null;

        if (img_provider != null) {
            img = img_provider.getImage(src, attrs, chain, document);
        }
        if (img == null && img_store != null) {
            Image tim = img_store.get(src);
            if (tim != null) {
                img = Image.getInstance(tim);
            }
        }
        if (img != null) {
            return img;
        }

        if (!src.startsWith("http") && img_baseurl != null) {
            src = img_baseurl + src;
        } else if (img == null && !src.startsWith("http")) {
            String path = chain.getProperty("image_path");
            if (path == null) {
                path = "";
            }
            src = (new File(path, src)).getPath();
        }
        img = Image.getInstance(src);
        if (img == null) {
            return null;
        }
        float actualFontSize = HtmlUtilities.parseLength(chain
                .getProperty("size"), 12.0F);

        if (actualFontSize <= 0.0F) {
            actualFontSize = 12.0F;
        }
        String width = attrs.get("width");
        float widthInPoints = HtmlUtilities.parseLength(width, actualFontSize);
        String height = attrs.get("height");
        float heightInPoints = HtmlUtilities.parseLength(height, actualFontSize);
        if (widthInPoints > 0.0F && heightInPoints > 0.0F) {
            img.scaleAbsolute(widthInPoints, heightInPoints);
        } else if (widthInPoints > 0.0F) {

            heightInPoints = img.getHeight() * widthInPoints / img.getWidth();
            img.scaleAbsolute(widthInPoints, heightInPoints);
        } else if (heightInPoints > 0.0F) {

            widthInPoints = img.getWidth() * heightInPoints / img.getHeight();
            img.scaleAbsolute(widthInPoints, heightInPoints);
        }

        String before = chain.getProperty("before");
        if (before != null) {
            img.setSpacingBefore(Float.parseFloat(before));
        }
        String after = chain.getProperty("after");
        if (after != null) {
            img.setSpacingAfter(Float.parseFloat(after));
        }
        img.setWidthPercentage(0.0F);
        return img;
    }

    public List createList(String tag, ChainedProperties chain) {
        List list;
        if ("ul".equalsIgnoreCase(tag)) {
            list = new List(false);
            list.setListSymbol("• ");
        } else {

            list = new List(true);
        }
        try {
            list.setIndentationLeft((new Float(chain.getProperty("indent"))).floatValue());
        } catch (Exception e) {
            list.setAutoindent(true);
        }
        return list;
    }
}
