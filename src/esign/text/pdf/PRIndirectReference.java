package esign.text.pdf;

import java.io.IOException;
import java.io.OutputStream;

public class PRIndirectReference
        extends PdfIndirectReference {

    protected PdfReader reader;

    public PRIndirectReference(PdfReader reader, int number, int generation) {
        this.type = 10;
        this.number = number;
        this.generation = generation;
        this.reader = reader;
    }

    public PRIndirectReference(PdfReader reader, int number) {
        this(reader, number, 0);
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        if (writer != null) {
            int n = writer.getNewObjectNumber(this.reader, this.number, this.generation);
            os.write(PdfEncodings.convertToBytes(n + " " + (this.reader.isAppendable() ? this.generation : 0) + " R", (String) null));
        } else {

            super.toPdf((PdfWriter) null, os);
        }
    }

    public PdfReader getReader() {
        return this.reader;
    }

    public void setNumber(int number, int generation) {
        this.number = number;
        this.generation = generation;
    }
}
