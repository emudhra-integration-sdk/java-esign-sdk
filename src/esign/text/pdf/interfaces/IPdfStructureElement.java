package esign.text.pdf.interfaces;

import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;

public interface IPdfStructureElement {
  PdfObject getAttribute(PdfName paramPdfName);
  
  void setAttribute(PdfName paramPdfName, PdfObject paramPdfObject);
}

