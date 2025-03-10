package esign.text.pdf;

import esign.text.log.Counter;
import esign.text.log.CounterFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

public class FdfReader
        extends PdfReader {

    HashMap<String, PdfDictionary> fields;
    String fileSpec;
    PdfName encoding;

    public FdfReader(String filename) throws IOException {
        super(filename);
    }

    public FdfReader(byte[] pdfIn) throws IOException {
        super(pdfIn);
    }

    public FdfReader(URL url) throws IOException {
        super(url);
    }

    public FdfReader(InputStream is) throws IOException {
        super(is);
    }

    protected static Counter COUNTER = CounterFactory.getCounter(FdfReader.class);

    protected Counter getCounter() {
        return COUNTER;
    }

    protected void readPdf() throws IOException {
        this.fields = new HashMap<String, PdfDictionary>();
        this.tokens.checkFdfHeader();
        rebuildXref();
        readDocObj();
        readFields();
    }

    protected void kidNode(PdfDictionary merged, String name) {
        PdfArray kids = merged.getAsArray(PdfName.KIDS);
        if (kids == null || kids.isEmpty()) {
            if (name.length() > 0) {
                name = name.substring(1);
            }
            this.fields.put(name, merged);
        } else {

            merged.remove(PdfName.KIDS);
            for (int k = 0; k < kids.size(); k++) {
                PdfDictionary dic = new PdfDictionary();
                dic.merge(merged);
                PdfDictionary newDic = kids.getAsDict(k);
                PdfString t = newDic.getAsString(PdfName.T);
                String newName = name;
                if (t != null) {
                    newName = newName + "." + t.toUnicodeString();
                }
                dic.merge(newDic);
                dic.remove(PdfName.T);
                kidNode(dic, newName);
            }
        }
    }

    protected void readFields() {
        this.catalog = this.trailer.getAsDict(PdfName.ROOT);
        PdfDictionary fdf = this.catalog.getAsDict(PdfName.FDF);
        if (fdf == null) {
            return;
        }
        PdfString fs = fdf.getAsString(PdfName.F);
        if (fs != null) {
            this.fileSpec = fs.toUnicodeString();
        }
        PdfArray fld = fdf.getAsArray(PdfName.FIELDS);
        if (fld == null) {
            return;
        }
        this.encoding = fdf.getAsName(PdfName.ENCODING);
        PdfDictionary merged = new PdfDictionary();
        merged.put(PdfName.KIDS, fld);
        kidNode(merged, "");
    }

    public HashMap<String, PdfDictionary> getFields() {
        return this.fields;
    }

    public PdfDictionary getField(String name) {
        return this.fields.get(name);
    }

    public byte[] getAttachedFile(String name) throws IOException {
        PdfDictionary field = this.fields.get(name);
        if (field != null) {
            PdfIndirectReference ir = (PRIndirectReference) field.get(PdfName.V);
            PdfDictionary filespec = (PdfDictionary) getPdfObject(ir.getNumber());
            PdfDictionary ef = filespec.getAsDict(PdfName.EF);
            ir = (PRIndirectReference) ef.get(PdfName.F);
            PRStream stream = (PRStream) getPdfObject(ir.getNumber());
            return getStreamBytes(stream);
        }
        return new byte[0];
    }

    public String getFieldValue(String name) {
        PdfDictionary field = this.fields.get(name);
        if (field == null) {
            return null;
        }
        PdfObject v = getPdfObject(field.get(PdfName.V));
        if (v == null) {
            return null;
        }
        if (v.isName()) {
            return PdfName.decodeName(((PdfName) v).toString());
        }
        if (v.isString()) {
            PdfString vs = (PdfString) v;
            if (this.encoding == null || vs.getEncoding() != null) {
                return vs.toUnicodeString();
            }
            byte[] b = vs.getBytes();
            if (b.length >= 2 && b[0] == -2 && b[1] == -1) {
                return vs.toUnicodeString();
            }
            try {
                if (this.encoding.equals(PdfName.SHIFT_JIS)) {
                    return new String(b, "SJIS");
                }
                if (this.encoding.equals(PdfName.UHC)) {
                    return new String(b, "MS949");
                }
                if (this.encoding.equals(PdfName.GBK)) {
                    return new String(b, "GBK");
                }
                if (this.encoding.equals(PdfName.BIGFIVE)) {
                    return new String(b, "Big5");
                }
                if (this.encoding.equals(PdfName.UTF_8)) {
                    return new String(b, "UTF8");
                }
            } catch (Exception exception) {
            }

            return vs.toUnicodeString();
        }
        return null;
    }

    public String getFileSpec() {
        return this.fileSpec;
    }
}
