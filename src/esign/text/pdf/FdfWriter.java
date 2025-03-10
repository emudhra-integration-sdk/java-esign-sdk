package esign.text.pdf;

import esign.text.DocWriter;
import esign.text.ExceptionConverter;
import esign.text.Image;
import esign.text.Rectangle;
import esign.text.log.Counter;
import esign.text.log.CounterFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class FdfWriter {

    private static final byte[] HEADER_FDF = DocWriter.getISOBytes("%FDF-1.4\n%âãÏÓ\n");
    HashMap<String, Object> fields = new HashMap<String, Object>();
    Wrt wrt = null;

    private String file;

    private String statusMessage;

    protected Counter COUNTER;

    public void writeTo(OutputStream os) throws IOException {
        if (this.wrt == null) {
            this.wrt = new Wrt(os, this);
        }
        this.wrt.write();
    }

    public void write() throws IOException {
        this.wrt.write();
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    boolean setField(String field, PdfObject value) {
        String s;
        Map<Object, Object> obj;
        HashMap<String, Object> map = this.fields;
        StringTokenizer tk = new StringTokenizer(field, ".");
        if (!tk.hasMoreTokens()) {
            return false;
        }
        while (true) {
            s = tk.nextToken();
            obj = (Map<Object, Object>) map.get(s);
            if (tk.hasMoreTokens()) {
                if (obj == null) {
                    obj = (Map<Object, Object>) new HashMap<Object, Object>();
                    map.put(s, obj);
                    map = (HashMap) obj;
                    continue;
                }
                if (obj instanceof HashMap) {
                    map = (HashMap) obj;
                    continue;
                }
                return false;
            }
            break;
        }
        if (!(obj instanceof HashMap)) {
            map.put(s, value);
            return true;
        }

        return false;
    }

    void iterateFields(HashMap<String, Object> values, HashMap<String, Object> map, String name) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String s = entry.getKey();
            Object obj = entry.getValue();
            if (obj instanceof HashMap) {
                iterateFields(values, (HashMap<String, Object>) obj, name + "." + s);
                continue;
            }
            values.put((name + "." + s).substring(1), obj);
        }
    }

    public boolean removeField(String field) {
        Object obj;
        HashMap<String, Object> map = this.fields;
        StringTokenizer tk = new StringTokenizer(field, ".");
        if (!tk.hasMoreTokens()) {
            return false;
        }
        ArrayList<Object> hist = new ArrayList();
        while (true) {
            String s = tk.nextToken();
            obj = map.get(s);
            if (obj == null) {
                return false;
            }
            hist.add(map);
            hist.add(s);
            if (tk.hasMoreTokens()) {
                if (obj instanceof HashMap) {
                    map = (HashMap<String, Object>) obj;
                    continue;
                }
                return false;
            }
            break;
        }
        if (obj instanceof HashMap) {
            return false;
        }

        for (int k = hist.size() - 2; k >= 0; k -= 2) {
            map = (HashMap<String, Object>) hist.get(k);
            String s = (String) hist.get(k + 1);
            map.remove(s);
            if (!map.isEmpty()) {
                break;
            }
        }
        return true;
    }

    public HashMap<String, Object> getFields() {
        HashMap<String, Object> values = new HashMap<String, Object>();
        iterateFields(values, this.fields, "");
        return values;
    }

    public String getField(String field) {
        Object obj;
        HashMap<String, Object> map = this.fields;
        StringTokenizer tk = new StringTokenizer(field, ".");
        if (!tk.hasMoreTokens()) {
            return null;
        }
        while (true) {
            String s = tk.nextToken();
            obj = map.get(s);
            if (obj == null) {
                return null;
            }
            if (tk.hasMoreTokens()) {
                if (obj instanceof HashMap) {
                    map = (HashMap<String, Object>) obj;
                    continue;
                }
                return null;
            }
            break;
        }
        if (obj instanceof HashMap) {
            return null;
        }
        if (((PdfObject) obj).isString()) {
            return ((PdfString) obj).toUnicodeString();
        }
        return PdfName.decodeName(obj.toString());
    }

    public boolean setFieldAsName(String field, String value) {
        return setField(field, new PdfName(value));
    }

    public boolean setFieldAsString(String field, String value) {
        return setField(field, new PdfString(value, "UnicodeBig"));
    }

    public boolean setFieldAsAction(String field, PdfAction action) {
        return setField(field, action);
    }

    public boolean setFieldAsTemplate(String field, PdfTemplate template) {
        try {
            PdfDictionary d = new PdfDictionary();
            if (template instanceof PdfImportedPage) {
                d.put(PdfName.N, template.getIndirectReference());
            } else {
                PdfStream str = template.getFormXObject(0);
                PdfIndirectReference ref = this.wrt.addToBody(str).getIndirectReference();
                d.put(PdfName.N, ref);
            }
            return setField(field, d);
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public boolean setFieldAsImage(String field, Image image) {
        try {
            if (Float.isNaN(image.getAbsoluteX())) {
                image.setAbsolutePosition(0.0F, image.getAbsoluteY());
            }
            if (Float.isNaN(image.getAbsoluteY())) {
                image.setAbsolutePosition(image.getAbsoluteY(), 0.0F);
            }
            PdfTemplate tmpl = PdfTemplate.createTemplate(this.wrt, image.getWidth(), image.getHeight());
            tmpl.addImage(image);
            PdfStream str = tmpl.getFormXObject(0);
            PdfIndirectReference ref = this.wrt.addToBody(str).getIndirectReference();
            PdfDictionary d = new PdfDictionary();
            d.put(PdfName.N, ref);
            return setField(field, d);
        } catch (Exception de) {
            throw new ExceptionConverter(de);
        }
    }

    public boolean setFieldAsJavascript(String field, PdfName jsTrigName, String js) {
        PdfAnnotation dict = this.wrt.createAnnotation((Rectangle) null, (PdfName) null);
        PdfAction javascript = PdfAction.javaScript(js, this.wrt);
        dict.put(jsTrigName, javascript);
        return setField(field, dict);
    }

    public PdfImportedPage getImportedPage(PdfReader reader, int pageNumber) {
        return this.wrt.getImportedPage(reader, pageNumber);
    }

    public PdfTemplate createTemplate(float width, float height) {
        return PdfTemplate.createTemplate(this.wrt, width, height);
    }

    public void setFields(FdfReader fdf) {
        HashMap<String, PdfDictionary> map = fdf.getFields();
        for (Map.Entry<String, PdfDictionary> entry : map.entrySet()) {
            String key = entry.getKey();
            PdfDictionary dic = entry.getValue();
            PdfObject v = dic.get(PdfName.V);
            if (v != null) {
                setField(key, v);
            }
            v = dic.get(PdfName.A);
            if (v != null) {
                setField(key, v);
            }
        }
    }

    public void setFields(PdfReader pdf) {
        setFields(pdf.getAcroFields());
    }

    public void setFields(AcroFields af) {
        for (Map.Entry<String, AcroFields.Item> entry : af.getFields().entrySet()) {
            String fn = entry.getKey();
            AcroFields.Item item = entry.getValue();
            PdfDictionary dic = item.getMerged(0);
            PdfObject v = PdfReader.getPdfObjectRelease(dic.get(PdfName.V));
            if (v == null) {
                continue;
            }
            PdfObject ft = PdfReader.getPdfObjectRelease(dic.get(PdfName.FT));
            if (ft == null || PdfName.SIG.equals(ft)) {
                continue;
            }
            setField(fn, v);
        }
    }

    public String getFile() {
        return this.file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    static class Wrt extends PdfWriter {

        private FdfWriter fdf;

        Wrt(OutputStream os, FdfWriter fdf) throws IOException {
            super(new PdfDocument(), os);
            this.fdf = fdf;
            this.os.write(FdfWriter.HEADER_FDF);
            this.body = new PdfWriter.PdfBody(this);
        }

        void write() throws IOException {
            for (PdfReaderInstance element : this.readerInstances.values()) {
                this.currentPdfReaderInstance = element;
                this.currentPdfReaderInstance.writeAllPages();
            }

            PdfDictionary dic = new PdfDictionary();
            dic.put(PdfName.FIELDS, calculate(this.fdf.fields));
            if (this.fdf.file != null) {
                dic.put(PdfName.F, new PdfString(this.fdf.file, "UnicodeBig"));
            }
            if (this.fdf.statusMessage != null && this.fdf.statusMessage.trim().length() != 0) {
                dic.put(PdfName.STATUS, new PdfString(this.fdf.statusMessage));
            }
            PdfDictionary fd = new PdfDictionary();
            fd.put(PdfName.FDF, dic);
            PdfIndirectReference ref = addToBody(fd).getIndirectReference();
            this.os.write(getISOBytes("trailer\n"));
            PdfDictionary trailer = new PdfDictionary();
            trailer.put(PdfName.ROOT, ref);
            trailer.toPdf(null, this.os);
            this.os.write(getISOBytes("\n%%EOF\n"));
            this.os.close();
        }

        PdfArray calculate(HashMap<String, Object> map) throws IOException {
            PdfArray ar = new PdfArray();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object v = entry.getValue();
                PdfDictionary dic = new PdfDictionary();
                dic.put(PdfName.T, new PdfString(key, "UnicodeBig"));
                if (v instanceof HashMap) {
                    dic.put(PdfName.KIDS, calculate((HashMap<String, Object>) v));
                } else if (v instanceof PdfAction) {
                    dic.put(PdfName.A, (PdfAction) v);
                } else if (v instanceof PdfAnnotation) {
                    dic.put(PdfName.AA, (PdfAnnotation) v);
                } else if (v instanceof PdfDictionary && ((PdfDictionary) v).size() == 1 && ((PdfDictionary) v).contains(PdfName.N)) {
                    dic.put(PdfName.AP, (PdfDictionary) v);
                } else {
                    dic.put(PdfName.V, (PdfObject) v);
                }
                ar.add(dic);
            }
            return ar;
        }
    }

    public FdfWriter() {
        this.COUNTER = CounterFactory.getCounter(FdfWriter.class);
    }

    public FdfWriter(OutputStream os) throws IOException {
        this.COUNTER = CounterFactory.getCounter(FdfWriter.class);
        this.wrt = new Wrt(os, this);
    }

    protected Counter getCounter() {
        return this.COUNTER;
    }
}
