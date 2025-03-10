package esign.text.pdf.interfaces;

import esign.text.DocumentException;
import esign.text.pdf.PdfAction;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfTransition;

public interface PdfPageActions {

    void setPageAction(PdfName paramPdfName, PdfAction paramPdfAction) throws DocumentException;

    void setDuration(int paramInt);

    void setTransition(PdfTransition paramPdfTransition);
}
