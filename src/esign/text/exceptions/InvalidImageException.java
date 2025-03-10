package esign.text.exceptions;

public class InvalidImageException
        extends RuntimeException {

    private static final long serialVersionUID = -1319471492541702697L;
    private final Throwable cause;

    public InvalidImageException(String message) {
        this(message, null);
    }

    public InvalidImageException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    public Throwable getCause() {
        return this.cause;
    }
}
