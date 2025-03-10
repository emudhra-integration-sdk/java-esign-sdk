package esign.text.pdf.interfaces;

import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;

public interface PdfViewerPreferences {
  void setViewerPreferences(int paramInt);
  
  void addViewerPreference(PdfName paramPdfName, PdfObject paramPdfObject);
}

