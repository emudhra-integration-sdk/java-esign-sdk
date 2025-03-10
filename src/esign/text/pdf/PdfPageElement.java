package esign.text.pdf;

interface PdfPageElement {
  void setParent(PdfIndirectReference paramPdfIndirectReference);
  
  boolean isParent();
}
