package esign.text.exceptions;

import java.io.IOException;

public class InvalidPdfException
        extends IOException {

    private static final long serialVersionUID = -2319614911517026938L;
    private final Throwable cause;

    public InvalidPdfException(String message) {
        this(message, null);
    }

    public InvalidPdfException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    public Throwable getCause() {
        return this.cause;
    }
}
