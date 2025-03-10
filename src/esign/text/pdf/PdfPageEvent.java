package esign.text.pdf;

import esign.text.Document;
import esign.text.Paragraph;
import esign.text.Rectangle;

public interface PdfPageEvent {
  void onOpenDocument(PdfWriter paramPdfWriter, Document paramDocument);
  
  void onStartPage(PdfWriter paramPdfWriter, Document paramDocument);
  
  void onEndPage(PdfWriter paramPdfWriter, Document paramDocument);
  
  void onCloseDocument(PdfWriter paramPdfWriter, Document paramDocument);
  
  void onParagraph(PdfWriter paramPdfWriter, Document paramDocument, float paramFloat);
  
  void onParagraphEnd(PdfWriter paramPdfWriter, Document paramDocument, float paramFloat);
  
  void onChapter(PdfWriter paramPdfWriter, Document paramDocument, float paramFloat, Paragraph paramParagraph);
  
  void onChapterEnd(PdfWriter paramPdfWriter, Document paramDocument, float paramFloat);
  
  void onSection(PdfWriter paramPdfWriter, Document paramDocument, float paramFloat, int paramInt, Paragraph paramParagraph);
  
  void onSectionEnd(PdfWriter paramPdfWriter, Document paramDocument, float paramFloat);
  
  void onGenericTag(PdfWriter paramPdfWriter, Document paramDocument, Rectangle paramRectangle, String paramString);
}


