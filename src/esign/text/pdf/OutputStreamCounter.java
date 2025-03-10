package esign.text.pdf;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamCounter
        extends OutputStream {

    protected OutputStream out;
    protected long counter = 0L;

    public OutputStreamCounter(OutputStream out) {
        this.out = out;
    }

    public void close() throws IOException {
        this.out.close();
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    public void write(byte[] b) throws IOException {
        this.counter += b.length;
        this.out.write(b);
    }

    public void write(int b) throws IOException {
        this.counter++;
        this.out.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        this.counter += len;
        this.out.write(b, off, len);
    }

    public long getCounter() {
        return this.counter;
    }

    public void resetCounter() {
        this.counter = 0L;
    }
}
