package esign.text.pdf;

import java.io.IOException;
import java.io.OutputStream;

public class PdfIndirectReference
        extends PdfObject {

    protected int number;
    protected int generation = 0;

    protected PdfIndirectReference() {
        super(0);
    }

    PdfIndirectReference(int type, int number, int generation) {
        super(0, number + " " + generation + " R");
        this.number = number;
        this.generation = generation;
    }

    protected PdfIndirectReference(int type, int number) {
        this(type, number, 0);
    }

    public int getNumber() {
        return this.number;
    }

    public int getGeneration() {
        return this.generation;
    }

    public String toString() {
        return this.number + " " + this.generation + " R";
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        os.write(PdfEncodings.convertToBytes(toString(), (String) null));
    }
}
