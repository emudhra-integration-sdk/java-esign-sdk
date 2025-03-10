package esign.text.pdf;

import esign.text.AccessibleElementId;
import esign.text.BaseColor;
import esign.text.Chunk;
import esign.text.Document;
import esign.text.DocumentException;
import esign.text.ExceptionConverter;
import esign.text.Image;
import esign.text.List;
import esign.text.ListBody;
import esign.text.ListItem;
import esign.text.ListLabel;
import esign.text.Paragraph;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.interfaces.IAccessibleElement;
import esign.text.pdf.interfaces.IPdfStructureElement;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class PdfStructureElement
        extends PdfDictionary
        implements IPdfStructureElement {

    private transient PdfStructureElement parent;
    private transient PdfStructureTreeRoot top;
    private AccessibleElementId elementId;
    private PdfIndirectReference reference;
    private PdfName structureType;

    public PdfStructureElement(PdfStructureElement parent, PdfName structureType) {
        this.top = parent.top;
        init(parent, structureType);
        this.parent = parent;
        put(PdfName.P, parent.reference);
        put(PdfName.TYPE, PdfName.STRUCTELEM);
    }

    public PdfStructureElement(PdfStructureTreeRoot parent, PdfName structureType) {
        this.top = parent;
        init(parent, structureType);
        put(PdfName.P, parent.getReference());
        put(PdfName.TYPE, PdfName.STRUCTELEM);
    }

    protected PdfStructureElement(PdfDictionary parent, PdfName structureType, AccessibleElementId elementId) {
        this.elementId = elementId;
        if (parent instanceof PdfStructureElement) {
            this.top = ((PdfStructureElement) parent).top;
            init(parent, structureType);
            this.parent = (PdfStructureElement) parent;
            put(PdfName.P, ((PdfStructureElement) parent).reference);
            put(PdfName.TYPE, PdfName.STRUCTELEM);
        } else if (parent instanceof PdfStructureTreeRoot) {
            this.top = (PdfStructureTreeRoot) parent;
            init(parent, structureType);
            put(PdfName.P, ((PdfStructureTreeRoot) parent).getReference());
            put(PdfName.TYPE, PdfName.STRUCTELEM);
        }
    }

    public PdfName getStructureType() {
        return this.structureType;
    }

    private void init(PdfDictionary parent, PdfName structureType) {
        if (!this.top.getWriter().getStandardStructElems().contains(structureType)) {
            PdfDictionary roleMap = this.top.getAsDict(PdfName.ROLEMAP);
            if (roleMap == null || !roleMap.contains(structureType)) {
                throw new ExceptionConverter(new DocumentException(MessageLocalization.getComposedMessage("unknown.structure.element.role.1", new Object[]{structureType.toString()})));
            }
            this.structureType = roleMap.getAsName(structureType);
        } else {
            this.structureType = structureType;
        }
        PdfObject kido = parent.get(PdfName.K);
        PdfArray kids = null;
        if (kido == null) {
            kids = new PdfArray();
            parent.put(PdfName.K, kids);
        } else if (kido instanceof PdfArray) {
            kids = (PdfArray) kido;
        } else {
            kids = new PdfArray();
            kids.add(kido);
            parent.put(PdfName.K, kids);
        }
        if (kids.size() > 0) {
            if (kids.getAsNumber(0) != null) {
                kids.remove(0);
            }
            if (kids.size() > 0) {
                PdfDictionary mcr = kids.getAsDict(0);
                if (mcr != null && PdfName.MCR.equals(mcr.getAsName(PdfName.TYPE))) {
                    kids.remove(0);
                }
            }
        }
        put(PdfName.S, structureType);
        this.reference = this.top.getWriter().getPdfIndirectReference();
        kids.add(this.reference);
    }

    public PdfDictionary getParent() {
        return getParent(false);
    }

    public PdfDictionary getParent(boolean includeStructTreeRoot) {
        if (this.parent == null && includeStructTreeRoot) {
            return this.top;
        }
        return this.parent;
    }

    void setPageMark(int page, int mark) {
        if (mark >= 0) {
            put(PdfName.K, new PdfNumber(mark));
        }
        this.top.setPageMark(page, this.reference);
    }

    void setAnnotation(PdfAnnotation annot, PdfIndirectReference currentPage) {
        PdfArray kArray = getAsArray(PdfName.K);
        if (kArray == null) {
            kArray = new PdfArray();
            PdfObject k = get(PdfName.K);
            if (k != null) {
                kArray.add(k);
            }
            put(PdfName.K, kArray);
        }
        PdfDictionary dict = new PdfDictionary();
        dict.put(PdfName.TYPE, PdfName.OBJR);
        dict.put(PdfName.OBJ, annot.getIndirectReference());
        if (annot.getRole() == PdfName.FORM) {
            dict.put(PdfName.PG, currentPage);
        }
        kArray.add(dict);
    }

    public PdfIndirectReference getReference() {
        return this.reference;
    }

    public PdfObject getAttribute(PdfName name) {
        PdfDictionary attr = getAsDict(PdfName.A);
        if (attr != null
                && attr.contains(name)) {
            return attr.get(name);
        }
        PdfDictionary parent = getParent();
        if (parent instanceof PdfStructureElement) {
            return ((PdfStructureElement) parent).getAttribute(name);
        }
        if (parent instanceof PdfStructureTreeRoot) {
            return ((PdfStructureTreeRoot) parent).getAttribute(name);
        }
        return new PdfNull();
    }

    public void setAttribute(PdfName name, PdfObject obj) {
        PdfDictionary attr = getAsDict(PdfName.A);
        if (attr == null) {
            attr = new PdfDictionary();
            put(PdfName.A, attr);
        }
        attr.put(name, obj);
    }

    public void writeAttributes(IAccessibleElement element) {
        if (element instanceof ListItem) {
            writeAttributes((ListItem) element);
        } else if (element instanceof Paragraph) {
            writeAttributes((Paragraph) element);
        } else if (element instanceof Chunk) {
            writeAttributes((Chunk) element);
        } else if (element instanceof Image) {
            writeAttributes((Image) element);
        } else if (element instanceof List) {
            writeAttributes((List) element);
        } else if (element instanceof ListLabel) {
            writeAttributes((ListLabel) element);
        } else if (element instanceof ListBody) {
            writeAttributes((ListBody) element);
        } else if (element instanceof PdfPTable) {
            writeAttributes((PdfPTable) element);
        } else if (element instanceof PdfPRow) {
            writeAttributes((PdfPRow) element);
        } else if (element instanceof PdfPHeaderCell) {
            writeAttributes((PdfPHeaderCell) element);
        } else if (element instanceof PdfPCell) {
            writeAttributes((PdfPCell) element);
        } else if (element instanceof PdfPTableHeader) {
            writeAttributes((PdfPTableHeader) element);
        } else if (element instanceof PdfPTableFooter) {
            writeAttributes((PdfPTableFooter) element);
        } else if (element instanceof PdfPTableBody) {
            writeAttributes((PdfPTableBody) element);
        } else if (element instanceof PdfDiv) {
            writeAttributes((PdfDiv) element);
        } else if (element instanceof PdfTemplate) {
            writeAttributes((PdfTemplate) element);
        } else if (element instanceof Document) {
            writeAttributes((Document) element);
        }
        if (element.getAccessibleAttributes() != null) {
            for (PdfName key : element.getAccessibleAttributes().keySet()) {
                if (key.equals(PdfName.ID)) {
                    PdfObject attr = element.getAccessibleAttribute(key);
                    put(key, attr);
                    this.top.putIDTree(attr.toString(), getReference());
                    continue;
                }
                if (key.equals(PdfName.LANG) || key.equals(PdfName.ALT) || key.equals(PdfName.ACTUALTEXT) || key.equals(PdfName.E) || key.equals(PdfName.T)) {
                    put(key, element.getAccessibleAttribute(key));
                    continue;
                }
                setAttribute(key, element.getAccessibleAttribute(key));
            }
        }
    }

    private void writeAttributes(Chunk chunk) {
        if (chunk != null) {
            if (chunk.getImage() != null) {
                writeAttributes(chunk.getImage());
            } else {
                HashMap<String, Object> attr = chunk.getAttributes();
                if (attr != null) {
                    setAttribute(PdfName.O, PdfName.LAYOUT);

                    if (attr.containsKey("UNDERLINE")) {
                        setAttribute(PdfName.TEXTDECORATIONTYPE, PdfName.UNDERLINE);
                    }
                    if (attr.containsKey("BACKGROUND")) {
                        Object[] back = (Object[]) attr.get("BACKGROUND");
                        BaseColor color = (BaseColor) back[0];
                        setAttribute(PdfName.BACKGROUNDCOLOR, new PdfArray(new float[]{color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F}));
                    }

                    IPdfStructureElement parent = (IPdfStructureElement) getParent(true);
                    PdfObject obj = getParentAttribute(parent, PdfName.COLOR);
                    if (chunk.getFont() != null && chunk.getFont().getColor() != null) {
                        BaseColor c = chunk.getFont().getColor();
                        setColorAttribute(c, obj, PdfName.COLOR);
                    }
                    PdfObject decorThickness = getParentAttribute(parent, PdfName.TEXTDECORATIONTHICKNESS);
                    PdfObject decorColor = getParentAttribute(parent, PdfName.TEXTDECORATIONCOLOR);
                    if (attr.containsKey("UNDERLINE")) {
                        Object[][] unders = (Object[][]) attr.get("UNDERLINE");
                        Object[] arr = unders[unders.length - 1];
                        BaseColor color = (BaseColor) arr[0];
                        float[] floats = (float[]) arr[1];
                        float thickness = floats[0];

                        if (decorThickness instanceof PdfNumber) {
                            float t = ((PdfNumber) decorThickness).floatValue();
                            if (Float.compare(thickness, t) != 0) {
                                setAttribute(PdfName.TEXTDECORATIONTHICKNESS, new PdfNumber(thickness));
                            }
                        } else {

                            setAttribute(PdfName.TEXTDECORATIONTHICKNESS, new PdfNumber(thickness));
                        }

                        if (color != null) {
                            setColorAttribute(color, decorColor, PdfName.TEXTDECORATIONCOLOR);
                        }
                    }

                    if (attr.containsKey("LINEHEIGHT")) {
                        float height = ((Float) attr.get("LINEHEIGHT")).floatValue();
                        PdfObject parentLH = getParentAttribute(parent, PdfName.LINEHEIGHT);
                        if (parentLH instanceof PdfNumber) {
                            float pLH = ((PdfNumber) parentLH).floatValue();
                            if (Float.compare(pLH, height) != 0) {
                                setAttribute(PdfName.LINEHEIGHT, new PdfNumber(height));
                            }
                        } else {

                            setAttribute(PdfName.LINEHEIGHT, new PdfNumber(height));
                        }
                    }
                }
            }
        }
    }

    private void writeAttributes(Image image) {
        if (image != null) {
            setAttribute(PdfName.O, PdfName.LAYOUT);
            if (image.getWidth() > 0.0F) {
                setAttribute(PdfName.WIDTH, new PdfNumber(image.getWidth()));
            }
            if (image.getHeight() > 0.0F) {
                setAttribute(PdfName.HEIGHT, new PdfNumber(image.getHeight()));
            }
            PdfRectangle rect = new PdfRectangle((Rectangle) image, image.getRotation());
            setAttribute(PdfName.BBOX, rect);
            if (image.getBackgroundColor() != null) {
                BaseColor color = image.getBackgroundColor();
                setAttribute(PdfName.BACKGROUNDCOLOR, new PdfArray(new float[]{color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F}));
            }
        }
    }

    private void writeAttributes(PdfTemplate template) {
        if (template != null) {
            setAttribute(PdfName.O, PdfName.LAYOUT);
            if (template.getWidth() > 0.0F) {
                setAttribute(PdfName.WIDTH, new PdfNumber(template.getWidth()));
            }
            if (template.getHeight() > 0.0F) {
                setAttribute(PdfName.HEIGHT, new PdfNumber(template.getHeight()));
            }
            PdfRectangle rect = new PdfRectangle(template.getBoundingBox());
            setAttribute(PdfName.BBOX, rect);
        }
    }

    private void writeAttributes(Paragraph paragraph) {
        if (paragraph != null) {
            setAttribute(PdfName.O, PdfName.LAYOUT);

            if (Float.compare(paragraph.getSpacingBefore(), 0.0F) != 0) {
                setAttribute(PdfName.SPACEBEFORE, new PdfNumber(paragraph.getSpacingBefore()));
            }
            if (Float.compare(paragraph.getSpacingAfter(), 0.0F) != 0) {
                setAttribute(PdfName.SPACEAFTER, new PdfNumber(paragraph.getSpacingAfter()));
            }

            IPdfStructureElement parent = (IPdfStructureElement) getParent(true);
            PdfObject obj = getParentAttribute(parent, PdfName.COLOR);
            if (paragraph.getFont() != null && paragraph.getFont().getColor() != null) {
                BaseColor c = paragraph.getFont().getColor();
                setColorAttribute(c, obj, PdfName.COLOR);
            }
            obj = getParentAttribute(parent, PdfName.TEXTINDENT);
            if (Float.compare(paragraph.getFirstLineIndent(), 0.0F) != 0) {
                boolean writeIndent = true;
                if (obj instanceof PdfNumber
                        && Float.compare(((PdfNumber) obj).floatValue(), (new Float(paragraph.getFirstLineIndent())).floatValue()) == 0) {
                    writeIndent = false;
                }
                if (writeIndent) {
                    setAttribute(PdfName.TEXTINDENT, new PdfNumber(paragraph.getFirstLineIndent()));
                }
            }
            obj = getParentAttribute(parent, PdfName.STARTINDENT);
            if (obj instanceof PdfNumber) {
                float startIndent = ((PdfNumber) obj).floatValue();
                if (Float.compare(startIndent, paragraph.getIndentationLeft()) != 0) {
                    setAttribute(PdfName.STARTINDENT, new PdfNumber(paragraph.getIndentationLeft()));
                }
            } else if (Math.abs(paragraph.getIndentationLeft()) > Float.MIN_VALUE) {
                setAttribute(PdfName.STARTINDENT, new PdfNumber(paragraph.getIndentationLeft()));
            }

            obj = getParentAttribute(parent, PdfName.ENDINDENT);
            if (obj instanceof PdfNumber) {
                float endIndent = ((PdfNumber) obj).floatValue();
                if (Float.compare(endIndent, paragraph.getIndentationRight()) != 0) {
                    setAttribute(PdfName.ENDINDENT, new PdfNumber(paragraph.getIndentationRight()));
                }
            } else if (Float.compare(paragraph.getIndentationRight(), 0.0F) != 0) {
                setAttribute(PdfName.ENDINDENT, new PdfNumber(paragraph.getIndentationRight()));
            }

            setTextAlignAttribute(paragraph.getAlignment());
        }
    }

    private void writeAttributes(List list) {
        if (list != null) {
            setAttribute(PdfName.O, PdfName.LIST);
            if (list.isAutoindent()) {
                if (list.isNumbered()) {
                    if (list.isLettered()) {
                        if (list.isLowercase()) {
                            setAttribute(PdfName.LISTNUMBERING, PdfName.LOWERROMAN);
                        } else {
                            setAttribute(PdfName.LISTNUMBERING, PdfName.UPPERROMAN);
                        }
                    } else {
                        setAttribute(PdfName.LISTNUMBERING, PdfName.DECIMAL);
                    }

                } else if (list.isLettered()) {
                    if (list.isLowercase()) {
                        setAttribute(PdfName.LISTNUMBERING, PdfName.LOWERALPHA);
                    } else {
                        setAttribute(PdfName.LISTNUMBERING, PdfName.UPPERALPHA);
                    }
                }
            }
            PdfObject obj = getParentAttribute(this.parent, PdfName.STARTINDENT);
            if (obj instanceof PdfNumber) {
                float startIndent = ((PdfNumber) obj).floatValue();
                if (Float.compare(startIndent, list.getIndentationLeft()) != 0) {
                    setAttribute(PdfName.STARTINDENT, new PdfNumber(list.getIndentationLeft()));
                }
            } else if (Math.abs(list.getIndentationLeft()) > Float.MIN_VALUE) {
                setAttribute(PdfName.STARTINDENT, new PdfNumber(list.getIndentationLeft()));
            }

            obj = getParentAttribute(this.parent, PdfName.ENDINDENT);
            if (obj instanceof PdfNumber) {
                float endIndent = ((PdfNumber) obj).floatValue();
                if (Float.compare(endIndent, list.getIndentationRight()) != 0) {
                    setAttribute(PdfName.ENDINDENT, new PdfNumber(list.getIndentationRight()));
                }
            } else if (Float.compare(list.getIndentationRight(), 0.0F) != 0) {
                setAttribute(PdfName.ENDINDENT, new PdfNumber(list.getIndentationRight()));
            }
        }
    }

    private void writeAttributes(ListItem listItem) {
        if (listItem != null) {
            PdfObject obj = getParentAttribute(this.parent, PdfName.STARTINDENT);
            if (obj instanceof PdfNumber) {
                float startIndent = ((PdfNumber) obj).floatValue();
                if (Float.compare(startIndent, listItem.getIndentationLeft()) != 0) {
                    setAttribute(PdfName.STARTINDENT, new PdfNumber(listItem.getIndentationLeft()));
                }
            } else if (Math.abs(listItem.getIndentationLeft()) > Float.MIN_VALUE) {
                setAttribute(PdfName.STARTINDENT, new PdfNumber(listItem.getIndentationLeft()));
            }

            obj = getParentAttribute(this.parent, PdfName.ENDINDENT);
            if (obj instanceof PdfNumber) {
                float endIndent = ((PdfNumber) obj).floatValue();
                if (Float.compare(endIndent, listItem.getIndentationRight()) != 0) {
                    setAttribute(PdfName.ENDINDENT, new PdfNumber(listItem.getIndentationRight()));
                }
            } else if (Float.compare(listItem.getIndentationRight(), 0.0F) != 0) {
                setAttribute(PdfName.ENDINDENT, new PdfNumber(listItem.getIndentationRight()));
            }
        }
    }

    private void writeAttributes(ListBody listBody) {
        if (listBody != null);
    }

    private void writeAttributes(ListLabel listLabel) {
        if (listLabel != null) {
            PdfObject obj = getParentAttribute(this.parent, PdfName.STARTINDENT);
            if (obj instanceof PdfNumber) {
                float startIndent = ((PdfNumber) obj).floatValue();
                if (Float.compare(startIndent, listLabel.getIndentation()) != 0) {
                    setAttribute(PdfName.STARTINDENT, new PdfNumber(listLabel.getIndentation()));
                }
            } else if (Math.abs(listLabel.getIndentation()) > Float.MIN_VALUE) {
                setAttribute(PdfName.STARTINDENT, new PdfNumber(listLabel.getIndentation()));
            }
        }
    }

    private void writeAttributes(PdfPTable table) {
        if (table != null) {
            setAttribute(PdfName.O, PdfName.TABLE);

            if (Float.compare(table.getSpacingBefore(), 0.0F) != 0) {
                setAttribute(PdfName.SPACEBEFORE, new PdfNumber(table.getSpacingBefore()));
            }
            if (Float.compare(table.getSpacingAfter(), 0.0F) != 0) {
                setAttribute(PdfName.SPACEAFTER, new PdfNumber(table.getSpacingAfter()));
            }
            if (table.getTotalHeight() > 0.0F) {
                setAttribute(PdfName.HEIGHT, new PdfNumber(table.getTotalHeight()));
            }
            if (table.getTotalWidth() > 0.0F) {
                setAttribute(PdfName.WIDTH, new PdfNumber(table.getTotalWidth()));
            }
        }
    }

    private void writeAttributes(PdfPRow row) {
        if (row != null) {
            setAttribute(PdfName.O, PdfName.TABLE);
        }
    }

    private void writeAttributes(PdfPCell cell) {
        if (cell != null) {
            setAttribute(PdfName.O, PdfName.TABLE);
            if (cell.getColspan() != 1) {
                setAttribute(PdfName.COLSPAN, new PdfNumber(cell.getColspan()));
            }
            if (cell.getRowspan() != 1) {
                setAttribute(PdfName.ROWSPAN, new PdfNumber(cell.getRowspan()));
            }
            if (cell.getHeaders() != null) {
                PdfArray headers = new PdfArray();
                ArrayList<PdfPHeaderCell> list = cell.getHeaders();
                for (PdfPHeaderCell header : list) {
                    if (header.getName() != null) {
                        headers.add(new PdfString(header.getName()));
                    }
                }
                if (!headers.isEmpty()) {
                    setAttribute(PdfName.HEADERS, headers);
                }
            }
            if (cell.getCalculatedHeight() > 0.0F) {
                setAttribute(PdfName.HEIGHT, new PdfNumber(cell.getCalculatedHeight()));
            }

            if (cell.getWidth() > 0.0F) {
                setAttribute(PdfName.WIDTH, new PdfNumber(cell.getWidth()));
            }

            if (cell.getBackgroundColor() != null) {
                BaseColor color = cell.getBackgroundColor();
                setAttribute(PdfName.BACKGROUNDCOLOR, new PdfArray(new float[]{color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F}));
            }
        }
    }

    private void writeAttributes(PdfPHeaderCell headerCell) {
        if (headerCell != null) {
            if (headerCell.getScope() != 0) {
                switch (headerCell.getScope()) {
                    case 1:
                        setAttribute(PdfName.SCOPE, PdfName.ROW);
                        break;
                    case 2:
                        setAttribute(PdfName.SCOPE, PdfName.COLUMN);
                        break;
                    case 3:
                        setAttribute(PdfName.SCOPE, PdfName.BOTH);
                        break;
                }
            }

            if (headerCell.getName() != null) {
                setAttribute(PdfName.NAME, new PdfName(headerCell.getName()));
            }
            writeAttributes(headerCell);
        }
    }

    private void writeAttributes(PdfPTableHeader header) {
        if (header != null) {
            setAttribute(PdfName.O, PdfName.TABLE);
        }
    }

    private void writeAttributes(PdfPTableBody body) {
        if (body != null);
    }

    private void writeAttributes(PdfPTableFooter footer) {
        if (footer != null);
    }

    private void writeAttributes(PdfDiv div) {
        if (div != null) {

            if (div.getBackgroundColor() != null) {
                setColorAttribute(div.getBackgroundColor(), (PdfObject) null, PdfName.BACKGROUNDCOLOR);
            }

            setTextAlignAttribute(div.getTextAlignment());
        }
    }

    private void writeAttributes(Document document) {
        if (document != null);
    }

    private boolean colorsEqual(PdfArray parentColor, float[] color) {
        if (Float.compare(color[0], parentColor.getAsNumber(0).floatValue()) != 0) {
            return false;
        }
        if (Float.compare(color[1], parentColor.getAsNumber(1).floatValue()) != 0) {
            return false;
        }
        if (Float.compare(color[2], parentColor.getAsNumber(2).floatValue()) != 0) {
            return false;
        }
        return true;
    }

    private void setColorAttribute(BaseColor newColor, PdfObject oldColor, PdfName attributeName) {
        float[] colorArr = {newColor.getRed() / 255.0F, newColor.getGreen() / 255.0F, newColor.getBlue() / 255.0F};
        if (oldColor != null && oldColor instanceof PdfArray) {
            PdfArray oldC = (PdfArray) oldColor;
            if (colorsEqual(oldC, colorArr)) {

                setAttribute(attributeName, new PdfArray(colorArr));
            } else {

                setAttribute(attributeName, new PdfArray(colorArr));
            }
        } else {
            setAttribute(attributeName, new PdfArray(colorArr));
        }
    }

    private void setTextAlignAttribute(int elementAlign) {
        PdfName align = null;
        switch (elementAlign) {
            case 0:
                align = PdfName.START;
                break;
            case 1:
                align = PdfName.CENTER;
                break;
            case 2:
                align = PdfName.END;
                break;
            case 3:
                align = PdfName.JUSTIFY;
                break;
        }
        PdfObject obj = getParentAttribute(this.parent, PdfName.TEXTALIGN);
        if (obj instanceof PdfName) {
            PdfName textAlign = (PdfName) obj;
            if (align != null && !textAlign.equals(align)) {
                setAttribute(PdfName.TEXTALIGN, align);
            }
        } else if (align != null && !PdfName.START.equals(align)) {
            setAttribute(PdfName.TEXTALIGN, align);
        }
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        PdfWriter.checkPdfIsoConformance(writer, 16, this);
        super.toPdf(writer, os);
    }

    private PdfObject getParentAttribute(IPdfStructureElement parent, PdfName name) {
        if (parent == null) {
            return null;
        }
        return parent.getAttribute(name);
    }

    protected void setStructureTreeRoot(PdfStructureTreeRoot root) {
        this.top = root;
    }

    protected void setStructureElementParent(PdfStructureElement parent) {
        this.parent = parent;
    }

    protected AccessibleElementId getElementId() {
        return this.elementId;
    }
}
