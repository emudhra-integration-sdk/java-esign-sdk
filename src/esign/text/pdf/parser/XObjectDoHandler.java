package esign.text.pdf.parser;

import esign.text.pdf.PdfIndirectReference;
import esign.text.pdf.PdfStream;

public interface XObjectDoHandler {
  void handleXObject(PdfContentStreamProcessor paramPdfContentStreamProcessor, PdfStream paramPdfStream, PdfIndirectReference paramPdfIndirectReference);
}

