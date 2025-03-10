package esign.text.pdf;

import esign.text.awt.geom.AffineTransform;
import esign.text.AccessibleElementId;
import esign.text.BaseColor;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.interfaces.IAccessibleElement;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PdfAnnotation
        extends PdfDictionary
        implements IAccessibleElement {

    public static final PdfName HIGHLIGHT_NONE = PdfName.N;

    public static final PdfName HIGHLIGHT_INVERT = PdfName.I;

    public static final PdfName HIGHLIGHT_OUTLINE = PdfName.O;

    public static final PdfName HIGHLIGHT_PUSH = PdfName.P;

    public static final PdfName HIGHLIGHT_TOGGLE = PdfName.T;

    public static final int FLAGS_INVISIBLE = 1;

    public static final int FLAGS_HIDDEN = 2;

    public static final int FLAGS_PRINT = 4;

    public static final int FLAGS_NOZOOM = 8;

    public static final int FLAGS_NOROTATE = 16;

    public static final int FLAGS_NOVIEW = 32;

    public static final int FLAGS_READONLY = 64;

    public static final int FLAGS_LOCKED = 128;

    public static final int FLAGS_TOGGLENOVIEW = 256;

    public static final int FLAGS_LOCKEDCONTENTS = 512;

    public static final PdfName APPEARANCE_NORMAL = PdfName.N;

    public static final PdfName APPEARANCE_ROLLOVER = PdfName.R;

    public static final PdfName APPEARANCE_DOWN = PdfName.D;

    public static final PdfName AA_ENTER = PdfName.E;

    public static final PdfName AA_EXIT = PdfName.X;

    public static final PdfName AA_DOWN = PdfName.D;

    public static final PdfName AA_UP = PdfName.U;

    public static final PdfName AA_FOCUS = PdfName.FO;

    public static final PdfName AA_BLUR = PdfName.BL;

    public static final PdfName AA_JS_KEY = PdfName.K;

    public static final PdfName AA_JS_FORMAT = PdfName.F;

    public static final PdfName AA_JS_CHANGE = PdfName.V;

    public static final PdfName AA_JS_OTHER_CHANGE = PdfName.C;

    public static final int MARKUP_HIGHLIGHT = 0;

    public static final int MARKUP_UNDERLINE = 1;

    public static final int MARKUP_STRIKEOUT = 2;

    public static final int MARKUP_SQUIGGLY = 3;

    protected PdfWriter writer;

    protected PdfIndirectReference reference;

    protected HashSet<PdfTemplate> templates;

    protected boolean form = false;

    protected boolean annotation = true;

    protected boolean used = false;

    private int placeInPage = -1;

    protected PdfName role = null;
    protected HashMap<PdfName, PdfObject> accessibleAttributes = null;
    private AccessibleElementId id = null;

    public PdfAnnotation(PdfWriter writer, Rectangle rect) {
        this.writer = writer;
        if (rect != null) {
            put(PdfName.RECT, new PdfRectangle(rect));
        }
    }

    public PdfAnnotation(PdfWriter writer, float llx, float lly, float urx, float ury, PdfString title, PdfString content) {
        this.writer = writer;
        put(PdfName.SUBTYPE, PdfName.TEXT);
        put(PdfName.T, title);
        put(PdfName.RECT, new PdfRectangle(llx, lly, urx, ury));
        put(PdfName.CONTENTS, content);
    }

    public PdfAnnotation(PdfWriter writer, float llx, float lly, float urx, float ury, PdfAction action) {
        this.writer = writer;
        put(PdfName.SUBTYPE, PdfName.LINK);
        put(PdfName.RECT, new PdfRectangle(llx, lly, urx, ury));
        put(PdfName.A, action);
        put(PdfName.BORDER, new PdfBorderArray(0.0F, 0.0F, 0.0F));
        put(PdfName.C, new PdfColor(0, 0, 255));
    }

    public static PdfAnnotation createScreen(PdfWriter writer, Rectangle rect, String clipTitle, PdfFileSpecification fs, String mimeType, boolean playOnDisplay) throws IOException {
        PdfAnnotation ann = writer.createAnnotation(rect, PdfName.SCREEN);
        ann.put(PdfName.F, new PdfNumber(4));
        ann.put(PdfName.TYPE, PdfName.ANNOT);
        ann.setPage();
        PdfIndirectReference ref = ann.getIndirectReference();
        PdfAction action = PdfAction.rendition(clipTitle, fs, mimeType, ref);
        PdfIndirectReference actionRef = writer.addToBody(action).getIndirectReference();

        if (playOnDisplay) {

            PdfDictionary aa = new PdfDictionary();
            aa.put(new PdfName("PV"), actionRef);
            ann.put(PdfName.AA, aa);
        }
        ann.put(PdfName.A, actionRef);
        return ann;
    }

    public PdfIndirectReference getIndirectReference() {
        if (this.reference == null) {
            this.reference = this.writer.getPdfIndirectReference();
        }
        return this.reference;
    }

    public static PdfAnnotation createText(PdfWriter writer, Rectangle rect, String title, String contents, boolean open, String icon) {
        PdfAnnotation annot = writer.createAnnotation(rect, PdfName.TEXT);
        if (title != null) {
            annot.put(PdfName.T, new PdfString(title, "UnicodeBig"));
        }
        if (contents != null) {
            annot.put(PdfName.CONTENTS, new PdfString(contents, "UnicodeBig"));
        }
        if (open) {
            annot.put(PdfName.OPEN, PdfBoolean.PDFTRUE);
        }
        if (icon != null) {
            annot.put(PdfName.NAME, new PdfName(icon));
        }
        return annot;
    }

    protected static PdfAnnotation createLink(PdfWriter writer, Rectangle rect, PdfName highlight) {
        PdfAnnotation annot = writer.createAnnotation(rect, PdfName.LINK);
        if (!highlight.equals(HIGHLIGHT_INVERT)) {
            annot.put(PdfName.H, highlight);
        }
        return annot;
    }

    public static PdfAnnotation createLink(PdfWriter writer, Rectangle rect, PdfName highlight, PdfAction action) {
        PdfAnnotation annot = createLink(writer, rect, highlight);
        annot.putEx(PdfName.A, action);
        return annot;
    }

    public static PdfAnnotation createLink(PdfWriter writer, Rectangle rect, PdfName highlight, String namedDestination) {
        PdfAnnotation annot = createLink(writer, rect, highlight);
        annot.put(PdfName.DEST, new PdfString(namedDestination, "UnicodeBig"));
        return annot;
    }

    public static PdfAnnotation createLink(PdfWriter writer, Rectangle rect, PdfName highlight, int page, PdfDestination dest) {
        PdfAnnotation annot = createLink(writer, rect, highlight);
        PdfIndirectReference ref = writer.getPageReference(page);
        PdfDestination d = new PdfDestination(dest);
        d.addPage(ref);
        annot.put(PdfName.DEST, d);
        return annot;
    }

    public static PdfAnnotation createFreeText(PdfWriter writer, Rectangle rect, String contents, PdfContentByte defaultAppearance) {
        PdfAnnotation annot = writer.createAnnotation(rect, PdfName.FREETEXT);
        annot.put(PdfName.CONTENTS, new PdfString(contents, "UnicodeBig"));
        annot.setDefaultAppearanceString(defaultAppearance);
        return annot;
    }

    public static PdfAnnotation createLine(PdfWriter writer, Rectangle rect, String contents, float x1, float y1, float x2, float y2) {
        PdfAnnotation annot = writer.createAnnotation(rect, PdfName.LINE);
        annot.put(PdfName.CONTENTS, new PdfString(contents, "UnicodeBig"));
        PdfArray array = new PdfArray(new PdfNumber(x1));
        array.add(new PdfNumber(y1));
        array.add(new PdfNumber(x2));
        array.add(new PdfNumber(y2));
        annot.put(PdfName.L, array);
        return annot;
    }

    public static PdfAnnotation createSquareCircle(PdfWriter writer, Rectangle rect, String contents, boolean square) {
        PdfAnnotation annot;
        if (square) {
            annot = writer.createAnnotation(rect, PdfName.SQUARE);
        } else {
            annot = writer.createAnnotation(rect, PdfName.CIRCLE);
        }
        annot.put(PdfName.CONTENTS, new PdfString(contents, "UnicodeBig"));
        return annot;
    }

    public static PdfAnnotation createMarkup(PdfWriter writer, Rectangle rect, String contents, int type, float[] quadPoints) {
        PdfName name = PdfName.HIGHLIGHT;
        switch (type) {
            case 1:
                name = PdfName.UNDERLINE;
                break;
            case 2:
                name = PdfName.STRIKEOUT;
                break;
            case 3:
                name = PdfName.SQUIGGLY;
                break;
        }
        PdfAnnotation annot = writer.createAnnotation(rect, name);
        annot.put(PdfName.CONTENTS, new PdfString(contents, "UnicodeBig"));
        PdfArray array = new PdfArray();
        for (int k = 0; k < quadPoints.length; k++) {
            array.add(new PdfNumber(quadPoints[k]));
        }
        annot.put(PdfName.QUADPOINTS, array);
        return annot;
    }

    public static PdfAnnotation createStamp(PdfWriter writer, Rectangle rect, String contents, String name) {
        PdfAnnotation annot = writer.createAnnotation(rect, PdfName.STAMP);
        annot.put(PdfName.CONTENTS, new PdfString(contents, "UnicodeBig"));
        annot.put(PdfName.NAME, new PdfName(name));
        return annot;
    }

    public static PdfAnnotation createInk(PdfWriter writer, Rectangle rect, String contents, float[][] inkList) {
        PdfAnnotation annot = writer.createAnnotation(rect, PdfName.INK);
        annot.put(PdfName.CONTENTS, new PdfString(contents, "UnicodeBig"));
        PdfArray outer = new PdfArray();
        for (int k = 0; k < inkList.length; k++) {
            PdfArray inner = new PdfArray();
            float[] deep = inkList[k];
            for (int j = 0; j < deep.length; j++) {
                inner.add(new PdfNumber(deep[j]));
            }
            outer.add(inner);
        }
        annot.put(PdfName.INKLIST, outer);
        return annot;
    }

    public static PdfAnnotation createFileAttachment(PdfWriter writer, Rectangle rect, String contents, byte[] fileStore, String file, String fileDisplay) throws IOException {
        return createFileAttachment(writer, rect, contents, PdfFileSpecification.fileEmbedded(writer, file, fileDisplay, fileStore));
    }

    public static PdfAnnotation createFileAttachment(PdfWriter writer, Rectangle rect, String contents, PdfFileSpecification fs) throws IOException {
        PdfAnnotation annot = writer.createAnnotation(rect, PdfName.FILEATTACHMENT);
        if (contents != null) {
            annot.put(PdfName.CONTENTS, new PdfString(contents, "UnicodeBig"));
        }
        annot.put(PdfName.FS, fs.getReference());
        return annot;
    }

    public static PdfAnnotation createPopup(PdfWriter writer, Rectangle rect, String contents, boolean open) {
        PdfAnnotation annot = writer.createAnnotation(rect, PdfName.POPUP);
        if (contents != null) {
            annot.put(PdfName.CONTENTS, new PdfString(contents, "UnicodeBig"));
        }
        if (open) {
            annot.put(PdfName.OPEN, PdfBoolean.PDFTRUE);
        }
        return annot;
    }

    public static PdfAnnotation createPolygonPolyline(PdfWriter writer, Rectangle rect, String contents, boolean polygon, PdfArray vertices) {
        PdfAnnotation annot = null;
        if (polygon) {
            annot = writer.createAnnotation(rect, PdfName.POLYGON);
        } else {
            annot = writer.createAnnotation(rect, PdfName.POLYLINE);
        }
        annot.put(PdfName.CONTENTS, new PdfString(contents, "UnicodeBig"));
        annot.put(PdfName.VERTICES, new PdfArray(vertices));
        return annot;
    }

    public void setDefaultAppearanceString(PdfContentByte cb) {
        byte[] b = cb.getInternalBuffer().toByteArray();
        int len = b.length;
        for (int k = 0; k < len; k++) {
            if (b[k] == 10) {
                b[k] = 32;
            }
        }
        put(PdfName.DA, new PdfString(b));
    }

    public void setFlags(int flags) {
        if (flags == 0) {
            remove(PdfName.F);
        } else {
            put(PdfName.F, new PdfNumber(flags));
        }
    }

    public void setBorder(PdfBorderArray border) {
        put(PdfName.BORDER, border);
    }

    public void setBorderStyle(PdfBorderDictionary border) {
        put(PdfName.BS, border);
    }

    public void setHighlighting(PdfName highlight) {
        if (highlight.equals(HIGHLIGHT_INVERT)) {
            remove(PdfName.H);
        } else {
            put(PdfName.H, highlight);
        }
    }

    public void setAppearance(PdfName ap, PdfTemplate template) {
        PdfDictionary dic = (PdfDictionary) get(PdfName.AP);
        if (dic == null) {
            dic = new PdfDictionary();
        }
        dic.put(ap, template.getIndirectReference());
        put(PdfName.AP, dic);
        if (!this.form) {
            return;
        }
        if (this.templates == null) {
            this.templates = new HashSet<PdfTemplate>();
        }
        this.templates.add(template);
    }

    public void setAppearance(PdfName ap, String state, PdfTemplate template) {
        PdfDictionary dic, dicAp = (PdfDictionary) get(PdfName.AP);
        if (dicAp == null) {
            dicAp = new PdfDictionary();
        }

        PdfObject obj = dicAp.get(ap);
        if (obj != null && obj.isDictionary()) {
            dic = (PdfDictionary) obj;
        } else {
            dic = new PdfDictionary();
        }
        dic.put(new PdfName(state), template.getIndirectReference());
        dicAp.put(ap, dic);
        put(PdfName.AP, dicAp);
        if (!this.form) {
            return;
        }
        if (this.templates == null) {
            this.templates = new HashSet<PdfTemplate>();
        }
        this.templates.add(template);
    }

    public void setAppearanceState(String state) {
        if (state == null) {
            remove(PdfName.AS);
            return;
        }
        put(PdfName.AS, new PdfName(state));
    }

    public void setColor(BaseColor color) {
        put(PdfName.C, new PdfColor(color));
    }

    public void setTitle(String title) {
        if (title == null) {
            remove(PdfName.T);
            return;
        }
        put(PdfName.T, new PdfString(title, "UnicodeBig"));
    }

    public void setPopup(PdfAnnotation popup) {
        put(PdfName.POPUP, popup.getIndirectReference());
        popup.put(PdfName.PARENT, getIndirectReference());
    }

    public void setAction(PdfAction action) {
        put(PdfName.A, action);
    }

    public void setAdditionalActions(PdfName key, PdfAction action) {
        PdfDictionary dic;
        PdfObject obj = get(PdfName.AA);
        if (obj != null && obj.isDictionary()) {
            dic = (PdfDictionary) obj;
        } else {
            dic = new PdfDictionary();
        }
        dic.put(key, action);
        put(PdfName.AA, dic);
    }

    public boolean isUsed() {
        return this.used;
    }

    public void setUsed() {
        this.used = true;
    }

    public HashSet<PdfTemplate> getTemplates() {
        return this.templates;
    }

    public boolean isForm() {
        return this.form;
    }

    public boolean isAnnotation() {
        return this.annotation;
    }

    public void setPage(int page) {
        put(PdfName.P, this.writer.getPageReference(page));
    }

    public void setPage() {
        put(PdfName.P, this.writer.getCurrentPage());
    }

    public int getPlaceInPage() {
        return this.placeInPage;
    }

    public void setPlaceInPage(int placeInPage) {
        this.placeInPage = placeInPage;
    }

    public void setRotate(int v) {
        put(PdfName.ROTATE, new PdfNumber(v));
    }

    PdfDictionary getMK() {
        PdfDictionary mk = (PdfDictionary) get(PdfName.MK);
        if (mk == null) {
            mk = new PdfDictionary();
            put(PdfName.MK, mk);
        }
        return mk;
    }

    public void setMKRotation(int rotation) {
        getMK().put(PdfName.R, new PdfNumber(rotation));
    }

    public static PdfArray getMKColor(BaseColor color) {
        CMYKColor cmyk;
        PdfArray array = new PdfArray();
        int type = ExtendedColor.getType(color);
        switch (type) {
            case 1:
                array.add(new PdfNumber(((GrayColor) color).getGray()));

                return array;
            case 2:
                cmyk = (CMYKColor) color;
                array.add(new PdfNumber(cmyk.getCyan()));
                array.add(new PdfNumber(cmyk.getMagenta()));
                array.add(new PdfNumber(cmyk.getYellow()));
                array.add(new PdfNumber(cmyk.getBlack()));
                return array;
            case 3:
            case 4:
            case 5:
                throw new RuntimeException(MessageLocalization.getComposedMessage("separations.patterns.and.shadings.are.not.allowed.in.mk.dictionary", new Object[0]));
        }
        array.add(new PdfNumber(color.getRed() / 255.0F));
        array.add(new PdfNumber(color.getGreen() / 255.0F));
        array.add(new PdfNumber(color.getBlue() / 255.0F));
        return array;
    }

    public void setMKBorderColor(BaseColor color) {
        if (color == null) {
            getMK().remove(PdfName.BC);
        } else {
            getMK().put(PdfName.BC, getMKColor(color));
        }
    }

    public void setMKBackgroundColor(BaseColor color) {
        if (color == null) {
            getMK().remove(PdfName.BG);
        } else {
            getMK().put(PdfName.BG, getMKColor(color));
        }
    }

    public void setMKNormalCaption(String caption) {
        getMK().put(PdfName.CA, new PdfString(caption, "UnicodeBig"));
    }

    public void setMKRolloverCaption(String caption) {
        getMK().put(PdfName.RC, new PdfString(caption, "UnicodeBig"));
    }

    public void setMKAlternateCaption(String caption) {
        getMK().put(PdfName.AC, new PdfString(caption, "UnicodeBig"));
    }

    public void setMKNormalIcon(PdfTemplate template) {
        getMK().put(PdfName.I, template.getIndirectReference());
    }

    public void setMKRolloverIcon(PdfTemplate template) {
        getMK().put(PdfName.RI, template.getIndirectReference());
    }

    public void setMKAlternateIcon(PdfTemplate template) {
        getMK().put(PdfName.IX, template.getIndirectReference());
    }

    public void setMKIconFit(PdfName scale, PdfName scalingType, float leftoverLeft, float leftoverBottom, boolean fitInBounds) {
        PdfDictionary dic = new PdfDictionary();
        if (!scale.equals(PdfName.A)) {
            dic.put(PdfName.SW, scale);
        }
        if (!scalingType.equals(PdfName.P)) {
            dic.put(PdfName.S, scalingType);
        }
        if (leftoverLeft != 0.5F || leftoverBottom != 0.5F) {
            PdfArray array = new PdfArray(new PdfNumber(leftoverLeft));
            array.add(new PdfNumber(leftoverBottom));
            dic.put(PdfName.A, array);
        }
        if (fitInBounds) {
            dic.put(PdfName.FB, PdfBoolean.PDFTRUE);
        }
        getMK().put(PdfName.IF, dic);
    }

    public void setMKTextPosition(int tp) {
        getMK().put(PdfName.TP, new PdfNumber(tp));
    }

    public void setLayer(PdfOCG layer) {
        put(PdfName.OC, layer.getRef());
    }

    public void setName(String name) {
        put(PdfName.NM, new PdfString(name));
    }

    public void applyCTM(AffineTransform ctm) {
        PdfArray origRect = getAsArray(PdfName.RECT);
        if (origRect != null) {
            PdfRectangle rect;
            if (origRect.size() == 4) {
                rect = new PdfRectangle(origRect.getAsNumber(0).floatValue(), origRect.getAsNumber(1).floatValue(), origRect.getAsNumber(2).floatValue(), origRect.getAsNumber(3).floatValue());
            } else {

                rect = new PdfRectangle(origRect.getAsNumber(0).floatValue(), origRect.getAsNumber(1).floatValue());
            }
            put(PdfName.RECT, rect.transform(ctm));
        }
    }

    public static class PdfImportedLink {

        float llx;

        float lly;

        float urx;

        float ury;

        HashMap<PdfName, PdfObject> parameters = new HashMap<PdfName, PdfObject>();
        PdfArray destination = null;
        int newPage = 0;
        PdfArray rect;

        PdfImportedLink(PdfDictionary annotation) {
            this.parameters.putAll(annotation.hashMap);
            try {
                this.destination = (PdfArray) this.parameters.remove(PdfName.DEST);
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("you.have.to.consolidate.the.named.destinations.of.your.reader", new Object[0]));
            }
            if (this.destination != null) {
                this.destination = new PdfArray(this.destination);
            }
            PdfArray rc = (PdfArray) this.parameters.remove(PdfName.RECT);
            this.llx = rc.getAsNumber(0).floatValue();
            this.lly = rc.getAsNumber(1).floatValue();
            this.urx = rc.getAsNumber(2).floatValue();
            this.ury = rc.getAsNumber(3).floatValue();

            this.rect = new PdfArray(rc);
        }

        public Map<PdfName, PdfObject> getParameters() {
            return new HashMap<PdfName, PdfObject>(this.parameters);
        }

        public PdfArray getRect() {
            return new PdfArray(this.rect);
        }

        public boolean isInternal() {
            return (this.destination != null);
        }

        public int getDestinationPage() {
            if (!isInternal()) {
                return 0;
            }

            PdfIndirectReference ref = this.destination.getAsIndirectObject(0);

            PRIndirectReference pr = (PRIndirectReference) ref;
            PdfReader r = pr.getReader();
            for (int i = 1; i <= r.getNumberOfPages(); i++) {
                PRIndirectReference pp = r.getPageOrigRef(i);
                if (pp.getGeneration() == pr.getGeneration() && pp.getNumber() == pr.getNumber()) {
                    return i;
                }
            }
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("page.not.found", new Object[0]));
        }

        public void setDestinationPage(int newPage) {
            if (!isInternal()) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("cannot.change.destination.of.external.link", new Object[0]));
            }
            this.newPage = newPage;
        }

        public void transformDestination(float a, float b, float c, float d, float e, float f) {
            if (!isInternal()) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("cannot.change.destination.of.external.link", new Object[0]));
            }
            if (this.destination.getAsName(1).equals(PdfName.XYZ)) {
                float x = this.destination.getAsNumber(2).floatValue();
                float y = this.destination.getAsNumber(3).floatValue();
                float xx = x * a + y * c + e;
                float yy = x * b + y * d + f;
                this.destination.set(2, new PdfNumber(xx));
                this.destination.set(3, new PdfNumber(yy));
            }
        }

        public void transformRect(float a, float b, float c, float d, float e, float f) {
            float x = this.llx * a + this.lly * c + e;
            float y = this.llx * b + this.lly * d + f;
            this.llx = x;
            this.lly = y;
            x = this.urx * a + this.ury * c + e;
            y = this.urx * b + this.ury * d + f;
            this.urx = x;
            this.ury = y;
        }

        public PdfAnnotation createAnnotation(PdfWriter writer) {
            PdfAnnotation annotation = writer.createAnnotation(new Rectangle(this.llx, this.lly, this.urx, this.ury), null);
            if (this.newPage != 0) {
                PdfIndirectReference ref = writer.getPageReference(this.newPage);
                this.destination.set(0, ref);
            }
            if (this.destination != null) {
                annotation.put(PdfName.DEST, this.destination);
            }
            annotation.hashMap.putAll(this.parameters);
            return annotation;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer("Imported link: location [");
            buf.append(this.llx);
            buf.append(' ');
            buf.append(this.lly);
            buf.append(' ');
            buf.append(this.urx);
            buf.append(' ');
            buf.append(this.ury);
            buf.append("] destination ");
            buf.append(this.destination);
            buf.append(" parameters ");
            buf.append(this.parameters);
            if (this.parameters != null) {
                appendDictionary(buf, this.parameters);
            }

            return buf.toString();
        }

        private void appendDictionary(StringBuffer buf, HashMap<PdfName, PdfObject> dict) {
            buf.append(" <<");
            for (Map.Entry<PdfName, PdfObject> entry : dict.entrySet()) {
                buf.append(entry.getKey());
                buf.append(":");
                if (entry.getValue() instanceof PdfDictionary) {
                    appendDictionary(buf, ((PdfDictionary) entry.getValue()).hashMap);
                } else {
                    buf.append(entry.getValue());
                }
                buf.append(" ");
            }

            buf.append(">> ");
        }
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        PdfWriter.checkPdfIsoConformance(writer, 13, this);
        super.toPdf(writer, os);
    }

    public PdfObject getAccessibleAttribute(PdfName key) {
        if (this.accessibleAttributes != null) {
            return this.accessibleAttributes.get(key);
        }
        return null;
    }

    public void setAccessibleAttribute(PdfName key, PdfObject value) {
        if (this.accessibleAttributes == null) {
            this.accessibleAttributes = new HashMap<PdfName, PdfObject>();
        }
        this.accessibleAttributes.put(key, value);
    }

    public HashMap<PdfName, PdfObject> getAccessibleAttributes() {
        return this.accessibleAttributes;
    }

    public PdfName getRole() {
        return this.role;
    }

    public void setRole(PdfName role) {
        this.role = role;
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
        return false;
    }
}
