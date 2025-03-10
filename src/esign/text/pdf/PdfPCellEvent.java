package esign.text.pdf;

import esign.text.Rectangle;

public interface PdfPCellEvent {
  void cellLayout(PdfPCell paramPdfPCell, Rectangle paramRectangle, PdfContentByte[] paramArrayOfPdfContentByte);
}

