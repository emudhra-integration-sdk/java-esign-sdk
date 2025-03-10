package esign.text.api;

import esign.text.Document;
import esign.text.DocumentException;
import esign.text.pdf.PdfWriter;

public interface WriterOperation {

    void write(PdfWriter paramPdfWriter, Document paramDocument) throws DocumentException;
}
