package esign.text.pdf;

public class BadPdfFormatException
        extends PdfException {

    private static final long serialVersionUID = 1802317735708833538L;

    BadPdfFormatException() {
    }

    BadPdfFormatException(String message) {
        super(message);
    }
}
