package esign.text.pdf.interfaces;

import esign.text.AccessibleElementId;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import java.util.HashMap;

public interface IAccessibleElement {

    PdfObject getAccessibleAttribute(PdfName paramPdfName);

    void setAccessibleAttribute(PdfName paramPdfName, PdfObject paramPdfObject);

    HashMap<PdfName, PdfObject> getAccessibleAttributes();

    PdfName getRole();

    void setRole(PdfName paramPdfName);

    AccessibleElementId getId();

    void setId(AccessibleElementId paramAccessibleElementId);

    boolean isInline();
}
