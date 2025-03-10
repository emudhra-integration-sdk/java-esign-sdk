package esign.text;

import java.io.PrintStream;
import java.io.PrintWriter;

public class ExceptionConverter
        extends RuntimeException {

    private static final long serialVersionUID = 8657630363395849399L;
    private Exception ex;
    private String prefix;

    public ExceptionConverter(Exception ex) {
        super(ex);
        this.ex = ex;
        this.prefix = (ex instanceof RuntimeException) ? "" : "ExceptionConverter: ";
    }

    public static final RuntimeException convertException(Exception ex) {
        if (ex instanceof RuntimeException) {
            return (RuntimeException) ex;
        }
        return new ExceptionConverter(ex);
    }

    public Exception getException() {
        return this.ex;
    }

    public String getMessage() {
        return this.ex.getMessage();
    }

    public String getLocalizedMessage() {
        return this.ex.getLocalizedMessage();
    }

    public String toString() {
        return this.prefix + this.ex;
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream s) {
        synchronized (s) {
            s.print(this.prefix);
            this.ex.printStackTrace(s);
        }
    }

    public void printStackTrace(PrintWriter s) {
        synchronized (s) {
            s.print(this.prefix);
            this.ex.printStackTrace(s);
        }
    }

    public Throwable fillInStackTrace() {
        return this;
    }
}
