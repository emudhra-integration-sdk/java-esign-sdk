package esign.text.pdf;

import esign.text.Document;
import esign.text.ExceptionConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class PRStream
        extends PdfStream {

    protected PdfReader reader;
    protected long offset;
    protected int length;
    protected int objNum = 0;
    protected int objGen = 0;

    public PRStream(PRStream stream, PdfDictionary newDic) {
        this.reader = stream.reader;
        this.offset = stream.offset;
        this.length = stream.length;
        this.compressed = stream.compressed;
        this.compressionLevel = stream.compressionLevel;
        this.streamBytes = stream.streamBytes;
        this.bytes = stream.bytes;
        this.objNum = stream.objNum;
        this.objGen = stream.objGen;
        if (newDic != null) {
            putAll(newDic);
        } else {
            this.hashMap.putAll(stream.hashMap);
        }
    }

    public PRStream(PRStream stream, PdfDictionary newDic, PdfReader reader) {
        this(stream, newDic);
        this.reader = reader;
    }

    public PRStream(PdfReader reader, long offset) {
        this.reader = reader;
        this.offset = offset;
    }

    public PRStream(PdfReader reader, byte[] conts) {
        this(reader, conts, -1);
    }

    public PRStream(PdfReader reader, byte[] conts, int compressionLevel) {
        this.reader = reader;
        this.offset = -1L;
        if (Document.compress) {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                Deflater deflater = new Deflater(compressionLevel);
                DeflaterOutputStream zip = new DeflaterOutputStream(stream, deflater);
                zip.write(conts);
                zip.close();
                deflater.end();
                this.bytes = stream.toByteArray();
            } catch (IOException ioe) {
                throw new ExceptionConverter(ioe);
            }
            put(PdfName.FILTER, PdfName.FLATEDECODE);
        } else {

            this.bytes = conts;
        }
        setLength(this.bytes.length);
    }

    public void setData(byte[] data, boolean compress) {
        setData(data, compress, -1);
    }

    public void setData(byte[] data, boolean compress, int compressionLevel) {
        remove(PdfName.FILTER);
        this.offset = -1L;
        if (Document.compress && compress) {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                Deflater deflater = new Deflater(compressionLevel);
                DeflaterOutputStream zip = new DeflaterOutputStream(stream, deflater);
                zip.write(data);
                zip.close();
                deflater.end();
                this.bytes = stream.toByteArray();
                this.compressionLevel = compressionLevel;
            } catch (IOException ioe) {
                throw new ExceptionConverter(ioe);
            }
            put(PdfName.FILTER, PdfName.FLATEDECODE);
        } else {

            this.bytes = data;
        }
        setLength(this.bytes.length);
    }

    public void setDataRaw(byte[] data) {
        this.offset = -1L;
        this.bytes = data;
        setLength(this.bytes.length);
    }

    public void setData(byte[] data) {
        setData(data, true);
    }

    public void setLength(int length) {
        this.length = length;
        put(PdfName.LENGTH, new PdfNumber(length));
    }

    public long getOffset() {
        return this.offset;
    }

    public int getLength() {
        return this.length;
    }

    public PdfReader getReader() {
        return this.reader;
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public void setObjNum(int objNum, int objGen) {
        this.objNum = objNum;
        this.objGen = objGen;
    }

    int getObjNum() {
        return this.objNum;
    }

    int getObjGen() {
        return this.objGen;
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        byte[] b = PdfReader.getStreamBytesRaw(this);
        PdfEncryption crypto = null;
        if (writer != null) {
            crypto = writer.getEncryption();
        }
        PdfObject objLen = get(PdfName.LENGTH);
        int nn = b.length;
        if (crypto != null) {
            nn = crypto.calculateStreamSize(nn);
        }
        put(PdfName.LENGTH, new PdfNumber(nn));
        superToPdf(writer, os);
        put(PdfName.LENGTH, objLen);
        os.write(STARTSTREAM);
        if (this.length > 0) {
            if (crypto != null && !crypto.isEmbeddedFilesOnly()) {
                b = crypto.encryptByteArray(b);
            }
            os.write(b);
        }
        os.write(ENDSTREAM);
    }
}
