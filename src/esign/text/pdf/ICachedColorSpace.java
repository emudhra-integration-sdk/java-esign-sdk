package esign.text.pdf;

public interface ICachedColorSpace {

    PdfObject getPdfObject(PdfWriter paramPdfWriter);

    boolean equals(Object paramObject);

    int hashCode();
}
