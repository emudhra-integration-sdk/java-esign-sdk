package esign.text.pdf.interfaces;

import esign.text.pdf.PdfAcroForm;
import esign.text.pdf.PdfAnnotation;
import esign.text.pdf.PdfFormField;

public interface PdfAnnotations {
  PdfAcroForm getAcroForm();
  
  void addAnnotation(PdfAnnotation paramPdfAnnotation);
  
  void addCalculationOrder(PdfFormField paramPdfFormField);
  
  void setSigFlags(int paramInt);
}

