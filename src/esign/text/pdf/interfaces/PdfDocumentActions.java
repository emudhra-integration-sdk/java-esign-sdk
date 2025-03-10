package esign.text.pdf.interfaces;

import esign.text.DocumentException;
import esign.text.pdf.PdfAction;
import esign.text.pdf.PdfName;

public interface PdfDocumentActions {

    void setOpenAction(String paramString);

    void setOpenAction(PdfAction paramPdfAction);

    void setAdditionalAction(PdfName paramPdfName, PdfAction paramPdfAction) throws DocumentException;
}
