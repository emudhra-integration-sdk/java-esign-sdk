package esign.text.pdf.parser;

import esign.text.pdf.PdfLiteral;
import esign.text.pdf.PdfObject;
import java.util.ArrayList;

public interface ContentOperator {
  void invoke(PdfContentStreamProcessor paramPdfContentStreamProcessor, PdfLiteral paramPdfLiteral, ArrayList<PdfObject> paramArrayList) throws Exception;
}

