package esign.text.pdf;

import esign.text.AccessibleElementId;
import esign.text.pdf.interfaces.IAccessibleElement;
import java.util.ArrayList;
import java.util.HashMap;

public class PdfPTableBody
        implements IAccessibleElement {

    protected AccessibleElementId id = new AccessibleElementId();
    protected ArrayList<PdfPRow> rows = null;
    protected PdfName role = PdfName.TBODY;
    protected HashMap<PdfName, PdfObject> accessibleAttributes = null;

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
        return this.id;
    }

    public void setId(AccessibleElementId id) {
        this.id = id;
    }

    public boolean isInline() {
        return false;
    }
}
