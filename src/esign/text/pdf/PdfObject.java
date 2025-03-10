package esign.text.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

public abstract class PdfObject
        implements Serializable {

    public static final int BOOLEAN = 1;
    public static final int NUMBER = 2;
    public static final int STRING = 3;
    public static final int NAME = 4;
    public static final int ARRAY = 5;
    public static final int DICTIONARY = 6;
    public static final int STREAM = 7;
    public static final int NULL = 8;
    public static final int INDIRECT = 10;
    public static final String NOTHING = "";
    public static final String TEXT_PDFDOCENCODING = "PDF";
    public static final String TEXT_UNICODE = "UnicodeBig";
    protected byte[] bytes;
    protected int type;
    protected PRIndirectReference indRef;

    protected PdfObject(int type) {
        this.type = type;
    }

    protected PdfObject(int type, String content) {
        this.type = type;
        this.bytes = PdfEncodings.convertToBytes(content, (String) null);
    }

    protected PdfObject(int type, byte[] bytes) {
        this.bytes = bytes;
        this.type = type;
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        if (this.bytes != null) {
            PdfWriter.checkPdfIsoConformance(writer, 11, this);
            os.write(this.bytes);
        }
    }

    public String toString() {
        if (this.bytes == null) {
            return super.toString();
        }
        return PdfEncodings.convertToString(this.bytes, null);
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public boolean canBeInObjStm() {
        switch (this.type) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
                return true;
        }

        return false;
    }

    public int length() {
        return toString().length();
    }

    protected void setContent(String content) {
        this.bytes = PdfEncodings.convertToBytes(content, (String) null);
    }

    public int type() {
        return this.type;
    }

    public boolean isNull() {
        return (this.type == 8);
    }

    public boolean isBoolean() {
        return (this.type == 1);
    }

    public boolean isNumber() {
        return (this.type == 2);
    }

    public boolean isString() {
        return (this.type == 3);
    }

    public boolean isName() {
        return (this.type == 4);
    }

    public boolean isArray() {
        return (this.type == 5);
    }

    public boolean isDictionary() {
        return (this.type == 6);
    }

    public boolean isStream() {
        return (this.type == 7);
    }

    public boolean isIndirect() {
        return (this.type == 10);
    }

    public PRIndirectReference getIndRef() {
        return this.indRef;
    }

    public void setIndRef(PRIndirectReference indRef) {
        this.indRef = indRef;
    }
}
