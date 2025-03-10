package esign.text.pdf;

import java.io.IOException;
import java.io.OutputStream;

public class PdfString
        extends PdfObject {

    protected String value = "";

    protected String originalValue = null;

    protected String encoding = "PDF";

    protected int objNum = 0;

    protected int objGen = 0;

    protected boolean hexWriting = false;

    public PdfString() {
        super(3);
    }

    public PdfString(String value) {
        super(3);
        this.value = value;
    }

    public PdfString(String value, String encoding) {
        super(3);
        this.value = value;
        this.encoding = encoding;
    }

    public PdfString(byte[] bytes) {
        super(3);
        this.value = PdfEncodings.convertToString(bytes, null);
        this.encoding = "";
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        PdfWriter.checkPdfIsoConformance(writer, 11, this);
        byte[] b = getBytes();
        PdfEncryption crypto = null;
        if (writer != null) {
            crypto = writer.getEncryption();
        }
        if (crypto != null && !crypto.isEmbeddedFilesOnly()) {
            b = crypto.encryptByteArray(b);
        }
        if (this.hexWriting) {
            ByteBuffer buf = new ByteBuffer();
            buf.append('<');
            int len = b.length;
            for (int k = 0; k < len; k++) {
                buf.appendHex(b[k]);
            }
            buf.append('>');
            os.write(buf.toByteArray());
        } else {

            os.write(StringUtils.escapeString(b));
        }
    }

    public String toString() {
        return this.value;
    }

    public byte[] getBytes() {
        if (this.bytes == null) {
            if (this.encoding != null && this.encoding.equals("UnicodeBig") && PdfEncodings.isPdfDocEncoding(this.value)) {
                this.bytes = PdfEncodings.convertToBytes(this.value, "PDF");
            } else {
                this.bytes = PdfEncodings.convertToBytes(this.value, this.encoding);
            }
        }
        return this.bytes;
    }

    public String toUnicodeString() {
        if (this.encoding != null && this.encoding.length() != 0) {
            return this.value;
        }
        getBytes();
        if (this.bytes.length >= 2 && this.bytes[0] == -2 && this.bytes[1] == -1) {
            return PdfEncodings.convertToString(this.bytes, "UnicodeBig");
        }
        return PdfEncodings.convertToString(this.bytes, "PDF");
    }

    public String getEncoding() {
        return this.encoding;
    }

    void setObjNum(int objNum, int objGen) {
        this.objNum = objNum;
        this.objGen = objGen;
    }

    void decrypt(PdfReader reader) {
        PdfEncryption decrypt = reader.getDecrypt();
        if (decrypt != null) {
            this.originalValue = this.value;
            decrypt.setHashKey(this.objNum, this.objGen);
            this.bytes = PdfEncodings.convertToBytes(this.value, (String) null);
            this.bytes = decrypt.decryptByteArray(this.bytes);
            this.value = PdfEncodings.convertToString(this.bytes, null);
        }
    }

    public byte[] getOriginalBytes() {
        if (this.originalValue == null) {
            return getBytes();
        }
        return PdfEncodings.convertToBytes(this.originalValue, (String) null);
    }

    public PdfString setHexWriting(boolean hexWriting) {
        this.hexWriting = hexWriting;
        return this;
    }

    public boolean isHexWriting() {
        return this.hexWriting;
    }
}
