package esign.text;

import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.interfaces.IAccessibleElement;
import java.util.HashMap;

public class ListBody
        implements IAccessibleElement {

    protected PdfName role = PdfName.LBODY;
    private AccessibleElementId id = null;
    protected HashMap<PdfName, PdfObject> accessibleAttributes = null;
    protected ListItem parentItem = null;

    protected ListBody(ListItem parentItem) {
        this.parentItem = parentItem;
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
