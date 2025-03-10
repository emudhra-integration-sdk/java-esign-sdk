package esign.text;

public class DocumentException
        extends Exception {

    private static final long serialVersionUID = -2191131489390840739L;

    public DocumentException(Exception ex) {
        super(ex);
    }

    public DocumentException() {
    }

    public DocumentException(String message) {
        super(message);
    }

    public DocumentException(String message, Exception ex) {
        super(message, ex);
    }
}
