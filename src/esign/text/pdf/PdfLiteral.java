package esign.text.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class PdfLiteral
        extends PdfObject {

    private long position;

    public PdfLiteral(String text) {
        super(0, text);
    }

    public PdfLiteral(byte[] b) {
        super(0, b);
    }

    public PdfLiteral(int size) {
        super(0, (byte[]) null);
        this.bytes = new byte[size];
        Arrays.fill(this.bytes, (byte) 32);
    }

    public PdfLiteral(int type, String text) {
        super(type, text);
    }

    public PdfLiteral(int type, byte[] b) {
        super(type, b);
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        if (os instanceof OutputStreamCounter) {
            this.position = ((OutputStreamCounter) os).getCounter();
        }
        super.toPdf(writer, os);
    }

    public long getPosition() {
        return this.position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public int getPosLength() {
        if (this.bytes != null) {
            return this.bytes.length;
        }
        return 0;
    }
}
