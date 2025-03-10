package esign.text;

import esign.text.api.Indentable;
import esign.text.api.Spaceable;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfPTable;
import esign.text.pdf.interfaces.IAccessibleElement;
import java.util.ArrayList;
import java.util.HashMap;
//import java.util.List;

public class Paragraph
        extends Phrase
        implements Indentable, Spaceable, IAccessibleElement {

    private static final long serialVersionUID = 7852314969733375514L;
    protected int alignment = -1;

    protected float indentationLeft;

    protected float indentationRight;

    private float firstLineIndent = 0.0F;

    protected float spacingBefore;

    protected float spacingAfter;

    private float extraParagraphSpace = 0.0F;

    protected boolean keeptogether = false;

    protected float paddingTop;

    protected PdfName role = PdfName.P;
    protected HashMap<PdfName, PdfObject> accessibleAttributes = null;
    protected AccessibleElementId id = null;

    public Paragraph(float leading) {
        super(leading);
    }

    public Paragraph(Chunk chunk) {
        super(chunk);
    }

    public Paragraph(float leading, Chunk chunk) {
        super(leading, chunk);
    }

    public Paragraph(String string) {
        super(string);
    }

    public Paragraph(String string, Font font) {
        super(string, font);
    }

    public Paragraph(float leading, String string) {
        super(leading, string);
    }

    public Paragraph(float leading, String string, Font font) {
        super(leading, string, font);
    }

    public Paragraph(Phrase phrase) {
        super(phrase);
        if (phrase instanceof Paragraph) {
            Paragraph p = (Paragraph) phrase;
            setAlignment(p.alignment);
            setIndentationLeft(p.getIndentationLeft());
            setIndentationRight(p.getIndentationRight());
            setFirstLineIndent(p.getFirstLineIndent());
            setSpacingAfter(p.getSpacingAfter());
            setSpacingBefore(p.getSpacingBefore());
            setExtraParagraphSpace(p.getExtraParagraphSpace());
            setRole(p.role);
            this.id = p.getId();
            if (p.accessibleAttributes != null) {
                this.accessibleAttributes = new HashMap<PdfName, PdfObject>(p.accessibleAttributes);
            }
        }
    }

    public Paragraph cloneShallow(boolean spacingBefore) {
        Paragraph copy = new Paragraph();
        populateProperties(copy, spacingBefore);
        return copy;
    }

    protected void populateProperties(Paragraph copy, boolean spacingBefore) {
        copy.setFont(getFont());
        copy.setAlignment(getAlignment());
        copy.setLeading(getLeading(), this.multipliedLeading);
        copy.setIndentationLeft(getIndentationLeft());
        copy.setIndentationRight(getIndentationRight());
        copy.setFirstLineIndent(getFirstLineIndent());
        copy.setSpacingAfter(getSpacingAfter());
        if (spacingBefore) {
            copy.setSpacingBefore(getSpacingBefore());
        }
        copy.setExtraParagraphSpace(getExtraParagraphSpace());
        copy.setRole(this.role);
        copy.id = getId();
        if (this.accessibleAttributes != null) {
            copy.accessibleAttributes = new HashMap<PdfName, PdfObject>(this.accessibleAttributes);
        }
        copy.setTabSettings(getTabSettings());
        copy.setKeepTogether(getKeepTogether());
    }

    public java.util.List<Element> breakUp() {
        java.util.List<Element> list = new ArrayList<Element>();
        Paragraph tmp = null;
        for (Element e : this) {
            if (e.type() == 14 || e.type() == 23 || e.type() == 12) {
                if (tmp != null && tmp.size() > 0) {
                    tmp.setSpacingAfter(0.0F);
                    list.add(tmp);
                    tmp = cloneShallow(false);
                }
                if (list.size() == 0) {
                    ListItem firstItem;
                    switch (e.type()) {
                        case 23:
                            ((PdfPTable) e).setSpacingBefore(getSpacingBefore());
                            break;
                        case 12:
                            ((Paragraph) e).setSpacingBefore(getSpacingBefore());
                            break;
                        case 14:
                            firstItem = ((List) e).getFirstItem();
                            if (firstItem != null) {
                                firstItem.setSpacingBefore(getSpacingBefore());
                            }
                            break;
                    }

                }
                list.add(e);
                continue;
            }
            if (tmp == null) {
                tmp = cloneShallow((list.size() == 0));
            }
            tmp.add(e);
        }

        if (tmp != null && tmp.size() > 0) {
            list.add(tmp);
        }
        if (list.size() != 0) {
            ListItem lastItem;
            Element lastElement = list.get(list.size() - 1);
            switch (lastElement.type()) {
                case 23:
                    ((PdfPTable) lastElement).setSpacingAfter(getSpacingAfter());
                    break;
                case 12:
                    ((Paragraph) lastElement).setSpacingAfter(getSpacingAfter());
                    break;
                case 14:
                    lastItem = ((List) lastElement).getLastItem();
                    if (lastItem != null) {
                        lastItem.setSpacingAfter(getSpacingAfter());
                    }
                    break;
            }

        }
        return list;
    }

    public int type() {
        return 12;
    }

    public boolean add(Element o) {
        if (o instanceof List) {
            List list = (List) o;
            list.setIndentationLeft(list.getIndentationLeft() + this.indentationLeft);
            list.setIndentationRight(this.indentationRight);
            return super.add(list);
        }
        if (o instanceof Image) {
            addSpecial(o);
            return true;
        }
        if (o instanceof Paragraph) {
            addSpecial(o);
            return true;
        }
        return super.add(o);
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public void setIndentationLeft(float indentation) {
        this.indentationLeft = indentation;
    }

    public void setIndentationRight(float indentation) {
        this.indentationRight = indentation;
    }

    public void setFirstLineIndent(float firstLineIndent) {
        this.firstLineIndent = firstLineIndent;
    }

    public void setSpacingBefore(float spacing) {
        this.spacingBefore = spacing;
    }

    public void setSpacingAfter(float spacing) {
        this.spacingAfter = spacing;
    }

    public void setKeepTogether(boolean keeptogether) {
        this.keeptogether = keeptogether;
    }

    public boolean getKeepTogether() {
        return this.keeptogether;
    }

    public int getAlignment() {
        return this.alignment;
    }

    public float getIndentationLeft() {
        return this.indentationLeft;
    }

    public float getIndentationRight() {
        return this.indentationRight;
    }

    public float getFirstLineIndent() {
        return this.firstLineIndent;
    }

    public float getSpacingBefore() {
        return this.spacingBefore;
    }

    public float getSpacingAfter() {
        return this.spacingAfter;
    }

    public float getExtraParagraphSpace() {
        return this.extraParagraphSpace;
    }

    public void setExtraParagraphSpace(float extraParagraphSpace) {
        this.extraParagraphSpace = extraParagraphSpace;
    }

    @Deprecated
    public float spacingBefore() {
        return getSpacingBefore();
    }

    @Deprecated
    public float spacingAfter() {
        return this.spacingAfter;
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

    public float getPaddingTop() {
        return this.paddingTop;
    }

    public void setPaddingTop(float paddingTop) {
        this.paddingTop = paddingTop;
    }

    public Paragraph() {
    }
}
