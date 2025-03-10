package esign.text.pdf.interfaces;

public interface PdfXConformance extends PdfIsoConformance {
  void setPDFXConformance(int paramInt);
  
  int getPDFXConformance();
  
  boolean isPdfX();
}

