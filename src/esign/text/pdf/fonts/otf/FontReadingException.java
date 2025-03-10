package esign.text.pdf.fonts.otf;

public class FontReadingException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public FontReadingException(String message) {
        super(message);
    }

    public FontReadingException(String message, Exception e) {
        super(message, e);
    }
}
