package esign.text;

import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.HyphenationEvent;
import esign.text.pdf.PdfAction;
import esign.text.pdf.PdfAnnotation;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;
import esign.text.pdf.draw.DrawInterface;
import esign.text.pdf.interfaces.IAccessibleElement;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Chunk
        implements Element, IAccessibleElement {

    public static final String OBJECT_REPLACEMENT_CHARACTER = "￼";
    public static final Chunk NEWLINE = new Chunk("\n");

    static {
        NEWLINE.setRole(PdfName.P);
    }

    public static final Chunk NEXTPAGE = new Chunk("");

    static {
        NEXTPAGE.setNewPage();
    }

    public static final Chunk TABBING = new Chunk(Float.valueOf(Float.NaN), false);

    public static final Chunk SPACETABBING = new Chunk(Float.valueOf(Float.NaN), true);

    protected StringBuffer content = null;

    protected Font font = null;

    protected HashMap<String, Object> attributes = null;

    protected PdfName role = null;
    protected HashMap<PdfName, PdfObject> accessibleAttributes = null;
    private AccessibleElementId id = null;

    public static final String SEPARATOR = "SEPARATOR";

    public static final String TAB = "TAB";

    public static final String TABSETTINGS = "TABSETTINGS";

    private String contentWithNoTabs;

    public static final String HSCALE = "HSCALE";

    public static final String UNDERLINE = "UNDERLINE";

    public static final String SUBSUPSCRIPT = "SUBSUPSCRIPT";

    public static final String SKEW = "SKEW";

    public static final String BACKGROUND = "BACKGROUND";

    public static final String TEXTRENDERMODE = "TEXTRENDERMODE";

    public static final String SPLITCHARACTER = "SPLITCHARACTER";

    public static final String HYPHENATION = "HYPHENATION";

    public static final String REMOTEGOTO = "REMOTEGOTO";

    public static final String LOCALGOTO = "LOCALGOTO";

    public static final String LOCALDESTINATION = "LOCALDESTINATION";

    public static final String GENERICTAG = "GENERICTAG";

    public static final String LINEHEIGHT = "LINEHEIGHT";

    public static final String IMAGE = "IMAGE";

    public static final String ACTION = "ACTION";

    public static final String NEWPAGE = "NEWPAGE";

    public static final String PDFANNOTATION = "PDFANNOTATION";

    public static final String COLOR = "COLOR";

    public static final String ENCODING = "ENCODING";

    public static final String CHAR_SPACING = "CHAR_SPACING";

    public static final String WORD_SPACING = "WORD_SPACING";

    public static final String WHITESPACE = "WHITESPACE";

    public Chunk(String content) {
        this(content, new Font());
    }

    public Chunk(char c) {
        this(c, new Font());
    }

    public Chunk(Image image, float offsetX, float offsetY) {
        this("￼", new Font());
        Image copyImage = Image.getInstance(image);
        copyImage.setAbsolutePosition(Float.NaN, Float.NaN);
        setAttribute("IMAGE", new Object[]{copyImage, new Float(offsetX), new Float(offsetY), Boolean.FALSE});

        this.role = null;
    }

    public Chunk(DrawInterface separator) {
        this(separator, false);
    }

    public Chunk(DrawInterface separator, boolean vertical) {
        this("￼", new Font());
        setAttribute("SEPARATOR", new Object[]{separator, Boolean.valueOf(vertical)});
        this.role = null;
    }

    public Chunk() {
        this.contentWithNoTabs = null;
        this.content = new StringBuffer();
        this.font = new Font();
        this.role = PdfName.SPAN;
    }

    public Chunk(Chunk ck) {
        this.contentWithNoTabs = null;
        if (ck.content != null) {
            this.content = new StringBuffer(ck.content.toString());
        }
        if (ck.font != null) {
            this.font = new Font(ck.font);
        }
        if (ck.attributes != null) {
            this.attributes = new HashMap<String, Object>(ck.attributes);
        }
        this.role = ck.role;
        if (ck.accessibleAttributes != null) {
            this.accessibleAttributes = new HashMap<PdfName, PdfObject>(ck.accessibleAttributes);
        }
        this.id = ck.getId();
    }

    public Chunk(String content, Font font) {
        this.contentWithNoTabs = null;
        this.content = new StringBuffer(content);
        this.font = font;
        this.role = PdfName.SPAN;
    }

    public Chunk(char c, Font font) {
        this.contentWithNoTabs = null;
        this.content = new StringBuffer();
        this.content.append(c);
        this.font = font;
        this.role = PdfName.SPAN;
    }

    @Deprecated
    public Chunk(DrawInterface separator, float tabPosition) {
        this(separator, tabPosition, false);
    }

    @Deprecated
    public Chunk(DrawInterface separator, float tabPosition, boolean newline) {
        this("￼", new Font());
        if (tabPosition < 0.0F) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("a.tab.position.may.not.be.lower.than.0.yours.is.1", new Object[]{String.valueOf(tabPosition)}));
        }
        setAttribute("TAB", new Object[]{separator, new Float(tabPosition), Boolean.valueOf(newline), new Float(0.0F)});
        this.role = PdfName.ARTIFACT;
    }

    private Chunk(Float tabInterval, boolean isWhitespace) {
        this("￼", new Font());
        if (tabInterval.floatValue() < 0.0F) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("a.tab.position.may.not.be.lower.than.0.yours.is.1", new Object[]{String.valueOf(tabInterval)}));
        }
        setAttribute("TAB", new Object[]{tabInterval, Boolean.valueOf(isWhitespace)});
        setAttribute("SPLITCHARACTER", TabSplitCharacter.TAB);

        setAttribute("TABSETTINGS", null);
        this.role = PdfName.ARTIFACT;
    }

    public Chunk(Image image, float offsetX, float offsetY, boolean changeLeading) {
        this("￼", new Font());
        setAttribute("IMAGE", new Object[]{image, new Float(offsetX), new Float(offsetY),
            Boolean.valueOf(changeLeading)});
        this.role = PdfName.ARTIFACT;
    }

    public boolean process(ElementListener listener) {
        try {
            return listener.add(this);
        } catch (DocumentException de) {
            return false;
        }
    }

    public int type() {
        return 10;
    }

    public List<Chunk> getChunks() {
        List<Chunk> tmp = new ArrayList<Chunk>();
        tmp.add(this);
        return tmp;
    }

    public StringBuffer append(String string) {
        this.contentWithNoTabs = null;
        return this.content.append(string);
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Font getFont() {
        return this.font;
    }

    public String getContent() {
        if (this.contentWithNoTabs == null) {
            this.contentWithNoTabs = this.content.toString().replaceAll("\t", "");
        }
        return this.contentWithNoTabs;
    }

    public String toString() {
        return getContent();
    }

    public boolean isEmpty() {
        return (this.content.toString().trim().length() == 0 && this.content
                .toString().indexOf("\n") == -1 && this.attributes == null);
    }

    public float getWidthPoint() {
        if (getImage() != null) {
            return getImage().getScaledWidth();
        }

        return this.font.getCalculatedBaseFont(true).getWidthPoint(getContent(), this.font.getCalculatedSize()) * getHorizontalScaling();
    }

    public boolean hasAttributes() {
        return (this.attributes != null && !this.attributes.isEmpty());
    }

    public boolean hasAccessibleAttributes() {
        return (this.accessibleAttributes != null && !this.accessibleAttributes.isEmpty());
    }

    public HashMap<String, Object> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(HashMap<String, Object> attributes) {
        this.attributes = attributes;
    }

    private Chunk setAttribute(String name, Object obj) {
        if (this.attributes == null) {
            this.attributes = new HashMap<String, Object>();
        }
        this.attributes.put(name, obj);
        return this;
    }

    public Chunk setHorizontalScaling(float scale) {
        return setAttribute("HSCALE", new Float(scale));
    }

    public float getHorizontalScaling() {
        if (this.attributes == null) {
            return 1.0F;
        }
        Float f = (Float) this.attributes.get("HSCALE");
        if (f == null) {
            return 1.0F;
        }
        return f.floatValue();
    }

    public Chunk setUnderline(float thickness, float yPosition) {
        return setUnderline(null, thickness, 0.0F, yPosition, 0.0F, 0);
    }

    public Chunk setUnderline(BaseColor color, float thickness, float thicknessMul, float yPosition, float yPositionMul, int cap) {
        if (this.attributes == null) {
            this.attributes = new HashMap<String, Object>();
        }
        Object[] obj = {color, new float[]{thickness, thicknessMul, yPosition, yPositionMul, cap}};

        Object[][] unders = Utilities.addToArray((Object[][]) this.attributes.get("UNDERLINE"), obj);

        return setAttribute("UNDERLINE", unders);
    }

    public Chunk setTextRise(float rise) {
        return setAttribute("SUBSUPSCRIPT", new Float(rise));
    }

    public float getTextRise() {
        if (this.attributes != null && this.attributes.containsKey("SUBSUPSCRIPT")) {
            Float f = (Float) this.attributes.get("SUBSUPSCRIPT");
            return f.floatValue();
        }
        return 0.0F;
    }

    public Chunk setSkew(float alpha, float beta) {
        alpha = (float) Math.tan(alpha * Math.PI / 180.0D);
        beta = (float) Math.tan(beta * Math.PI / 180.0D);
        return setAttribute("SKEW", new float[]{alpha, beta});
    }

    public Chunk setBackground(BaseColor color) {
        return setBackground(color, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    public Chunk setBackground(BaseColor color, float extraLeft, float extraBottom, float extraRight, float extraTop) {
        return setAttribute("BACKGROUND", new Object[]{color, new float[]{extraLeft, extraBottom, extraRight, extraTop}});
    }

    public Chunk setTextRenderMode(int mode, float strokeWidth, BaseColor strokeColor) {
        return setAttribute("TEXTRENDERMODE", new Object[]{Integer.valueOf(mode), new Float(strokeWidth), strokeColor});
    }

    public Chunk setSplitCharacter(SplitCharacter splitCharacter) {
        return setAttribute("SPLITCHARACTER", splitCharacter);
    }

    public Chunk setHyphenation(HyphenationEvent hyphenation) {
        return setAttribute("HYPHENATION", hyphenation);
    }

    public Chunk setRemoteGoto(String filename, String name) {
        return setAttribute("REMOTEGOTO", new Object[]{filename, name});
    }

    public Chunk setRemoteGoto(String filename, int page) {
        return setAttribute("REMOTEGOTO", new Object[]{filename,
            Integer.valueOf(page)});
    }

    public Chunk setLocalGoto(String name) {
        return setAttribute("LOCALGOTO", name);
    }

    public Chunk setLocalDestination(String name) {
        return setAttribute("LOCALDESTINATION", name);
    }

    public Chunk setGenericTag(String text) {
        return setAttribute("GENERICTAG", text);
    }

    public Chunk setLineHeight(float lineheight) {
        return setAttribute("LINEHEIGHT", Float.valueOf(lineheight));
    }

    public Image getImage() {
        if (this.attributes == null) {
            return null;
        }
        Object[] obj = (Object[]) this.attributes.get("IMAGE");
        if (obj == null) {
            return null;
        }
        return (Image) obj[0];
    }

    public Chunk setAction(PdfAction action) {
        setRole(PdfName.LINK);
        return setAttribute("ACTION", action);
    }

    public Chunk setAnchor(URL url) {
        setRole(PdfName.LINK);
        String urlStr = url.toExternalForm();
        setAccessibleAttribute(PdfName.ALT, (PdfObject) new PdfString(urlStr));
        return setAttribute("ACTION", new PdfAction(urlStr));
    }

    public Chunk setAnchor(String url) {
        setRole(PdfName.LINK);
        setAccessibleAttribute(PdfName.ALT, (PdfObject) new PdfString(url));
        return setAttribute("ACTION", new PdfAction(url));
    }

    public Chunk setNewPage() {
        return setAttribute("NEWPAGE", null);
    }

    public Chunk setAnnotation(PdfAnnotation annotation) {
        return setAttribute("PDFANNOTATION", annotation);
    }

    public boolean isContent() {
        return true;
    }

    public boolean isNestable() {
        return true;
    }

    public HyphenationEvent getHyphenation() {
        if (this.attributes == null) {
            return null;
        }
        return (HyphenationEvent) this.attributes.get("HYPHENATION");
    }

    public Chunk setCharacterSpacing(float charSpace) {
        return setAttribute("CHAR_SPACING", new Float(charSpace));
    }

    public float getCharacterSpacing() {
        if (this.attributes != null && this.attributes.containsKey("CHAR_SPACING")) {
            Float f = (Float) this.attributes.get("CHAR_SPACING");
            return f.floatValue();
        }
        return 0.0F;
    }

    public Chunk setWordSpacing(float wordSpace) {
        return setAttribute("WORD_SPACING", new Float(wordSpace));
    }

    public float getWordSpacing() {
        if (this.attributes != null && this.attributes.containsKey("WORD_SPACING")) {
            Float f = (Float) this.attributes.get("WORD_SPACING");
            return f.floatValue();
        }
        return 0.0F;
    }

    public static Chunk createWhitespace(String content) {
        return createWhitespace(content, false);
    }

    public static Chunk createWhitespace(String content, boolean preserve) {
        Chunk whitespace = null;
        if (!preserve) {
            whitespace = new Chunk(' ');
            whitespace.setAttribute("WHITESPACE", content);
        } else {
            whitespace = new Chunk(content);
        }

        return whitespace;
    }

    public boolean isWhitespace() {
        return (this.attributes != null && this.attributes.containsKey("WHITESPACE"));
    }

    @Deprecated
    public static Chunk createTabspace() {
        return createTabspace(60.0F);
    }

    @Deprecated
    public static Chunk createTabspace(float spacing) {
        Chunk tabspace = new Chunk(Float.valueOf(spacing), true);
        return tabspace;
    }

    @Deprecated
    public boolean isTabspace() {
        return (this.attributes != null && this.attributes.containsKey("TAB"));
    }

    public PdfObject getAccessibleAttribute(PdfName key) {
        if (getImage() != null) {
            return getImage().getAccessibleAttribute(key);
        }
        if (this.accessibleAttributes != null) {
            return this.accessibleAttributes.get(key);
        }
        return null;
    }

    public void setAccessibleAttribute(PdfName key, PdfObject value) {
        if (getImage() != null) {
            getImage().setAccessibleAttribute(key, value);
        } else {
            if (this.accessibleAttributes == null) {
                this.accessibleAttributes = new HashMap<PdfName, PdfObject>();
            }
            this.accessibleAttributes.put(key, value);
        }
    }

    public HashMap<PdfName, PdfObject> getAccessibleAttributes() {
        if (getImage() != null) {
            return getImage().getAccessibleAttributes();
        }
        return this.accessibleAttributes;
    }

    public PdfName getRole() {
        if (getImage() != null) {
            return getImage().getRole();
        }
        return this.role;
    }

    public void setRole(PdfName role) {
        if (getImage() != null) {
            getImage().setRole(role);
        } else {
            this.role = role;
        }
    }

    public AccessibleElementId getId() {
        if (this.id == null) {
            this.id = new AccessibleElementId();
        }
        return this.id;
    }

    public void setId(AccessibleElementId id) {
        this.id = id;
    }

    public boolean isInline() {
        return true;
    }

    public String getTextExpansion() {
        PdfObject o = getAccessibleAttribute(PdfName.E);
        if (o instanceof PdfString) {
            return ((PdfString) o).toUnicodeString();
        }
        return null;
    }

    public void setTextExpansion(String value) {
        setAccessibleAttribute(PdfName.E, (PdfObject) new PdfString(value));
    }
}
