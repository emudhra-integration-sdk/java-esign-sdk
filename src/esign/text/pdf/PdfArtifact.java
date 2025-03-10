package esign.text.pdf;

import esign.text.AccessibleElementId;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.interfaces.IAccessibleElement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class PdfArtifact
        implements IAccessibleElement {

    private static final HashSet<String> allowedArtifactTypes = new HashSet<String>(Arrays.asList(new String[]{"Pagination", "Layout", "Page", "Background"}));

    protected PdfName role = PdfName.ARTIFACT;
    protected HashMap<PdfName, PdfObject> accessibleAttributes = null;
    protected AccessibleElementId id = new AccessibleElementId();

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
    }

    public AccessibleElementId getId() {
        return this.id;
    }

    public void setId(AccessibleElementId id) {
        this.id = id;
    }

    public boolean isInline() {
        return true;
    }

    public PdfString getType() {
        return (this.accessibleAttributes == null) ? null : (PdfString) this.accessibleAttributes.get(PdfName.TYPE);
    }

    public void setType(PdfString type) {
        if (!allowedArtifactTypes.contains(type.toString())) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.artifact.type.1.is.invalid", new Object[]{type}));
        }
        setAccessibleAttribute(PdfName.TYPE, type);
    }

    public void setType(ArtifactType type) {
        PdfString artifactType = null;
        switch (type) {
            case BACKGROUND:
                artifactType = new PdfString("Background");
                break;
            case LAYOUT:
                artifactType = new PdfString("Layout");
                break;
            case PAGE:
                artifactType = new PdfString("Page");
                break;
            case PAGINATION:
                artifactType = new PdfString("Pagination");
                break;
        }
        setAccessibleAttribute(PdfName.TYPE, artifactType);
    }

    public PdfArray getBBox() {
        return (this.accessibleAttributes == null) ? null : (PdfArray) this.accessibleAttributes.get(PdfName.BBOX);
    }

    public void setBBox(PdfArray bbox) {
        setAccessibleAttribute(PdfName.BBOX, bbox);
    }

    public PdfArray getAttached() {
        return (this.accessibleAttributes == null) ? null : (PdfArray) this.accessibleAttributes.get(PdfName.ATTACHED);
    }

    public void setAttached(PdfArray attached) {
        setAccessibleAttribute(PdfName.ATTACHED, attached);
    }

    public enum ArtifactType {
        PAGINATION,
        LAYOUT,
        PAGE,
        BACKGROUND;
    }
}
