package esign.text.pdf.interfaces;

import esign.text.pdf.PdfDeveloperExtension;
import esign.text.pdf.PdfName;

public interface PdfVersion {
  void setPdfVersion(char paramChar);
  
  void setAtLeastPdfVersion(char paramChar);
  
  void setPdfVersion(PdfName paramPdfName);
  
  void addDeveloperExtension(PdfDeveloperExtension paramPdfDeveloperExtension);
}

