package esign.text.xml.xmp;

import esign.text.Version;
import esign.text.pdf.PdfDate;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;
import esign.text.xmp.XMPException;
import esign.text.xmp.XMPMeta;
import esign.text.xmp.XMPMetaFactory;
import esign.text.xmp.XMPUtils;
import esign.text.xmp.options.PropertyOptions;
import esign.text.xmp.options.SerializeOptions;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class XmpWriter {

    public static final String UTF8 = "UTF-8";
    public static final String UTF16 = "UTF-16";
    public static final String UTF16BE = "UTF-16BE";
    public static final String UTF16LE = "UTF-16LE";
    protected XMPMeta xmpMeta;
    protected OutputStream outputStream;
    protected SerializeOptions serializeOptions;

    public XmpWriter(OutputStream os, String utfEncoding, int extraSpace) throws IOException {
        this.outputStream = os;
        this.serializeOptions = new SerializeOptions();
        if ("UTF-16BE".equals(utfEncoding) || "UTF-16".equals(utfEncoding)) {
            this.serializeOptions.setEncodeUTF16BE(true);
        } else if ("UTF-16LE".equals(utfEncoding)) {
            this.serializeOptions.setEncodeUTF16LE(true);
        }
        this.serializeOptions.setPadding(extraSpace);
        this.xmpMeta = XMPMetaFactory.create();
        this.xmpMeta.setObjectName("xmpmeta");
        this.xmpMeta.setObjectName("");
        try {
            this.xmpMeta.setProperty("http://purl.org/dc/elements/1.1/", "format", "application/pdf");
            this.xmpMeta.setProperty("http://ns.adobe.com/pdf/1.3/", "Producer", Version.getInstance().getVersion());
        } catch (XMPException xMPException) {
        }
    }

    public XmpWriter(OutputStream os) throws IOException {
        this(os, "UTF-8", 2000);
    }

    public XmpWriter(OutputStream os, PdfDictionary info) throws IOException {
        this(os);
        if (info != null) {

            for (PdfName pdfName : info.getKeys()) {
                PdfName key = pdfName;
                PdfObject obj = info.get(key);
                if (obj == null) {
                    continue;
                }
                if (!obj.isString()) {
                    continue;
                }
                String value = ((PdfString) obj).toUnicodeString();
                try {
                    addDocInfoProperty(key, value);
                } catch (XMPException xmpExc) {
                    throw new IOException(xmpExc.getMessage());
                }
            }
        }
    }

    public XmpWriter(OutputStream os, Map<String, String> info) throws IOException {
        this(os);
        if (info != null) {

            for (Map.Entry<String, String> entry : info.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null) {
                    continue;
                }
                try {
                    addDocInfoProperty(key, value);
                } catch (XMPException xmpExc) {
                    throw new IOException(xmpExc.getMessage());
                }
            }
        }
    }

    public XMPMeta getXmpMeta() {
        return this.xmpMeta;
    }

    public void setReadOnly() {
        this.serializeOptions.setReadOnlyPacket(true);
    }

    public void setAbout(String about) {
        this.xmpMeta.setObjectName(about);
    }

    @Deprecated
    public void addRdfDescription(String xmlns, String content) throws IOException {
        try {
            String str = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><rdf:Description rdf:about=\"" + this.xmpMeta.getObjectName() + "\" " + xmlns + ">" + content + "</rdf:Description></rdf:RDF>\n";

            XMPMeta extMeta = XMPMetaFactory.parseFromString(str);
            XMPUtils.appendProperties(extMeta, this.xmpMeta, true, true);
        } catch (XMPException xmpExc) {
            throw new IOException(xmpExc.getMessage());
        }
    }

    @Deprecated
    public void addRdfDescription(XmpSchema s) throws IOException {
        try {
            String str = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><rdf:Description rdf:about=\"" + this.xmpMeta.getObjectName() + "\" " + s.getXmlns() + ">" + s.toString() + "</rdf:Description></rdf:RDF>\n";

            XMPMeta extMeta = XMPMetaFactory.parseFromString(str);
            XMPUtils.appendProperties(extMeta, this.xmpMeta, true, true);
        } catch (XMPException xmpExc) {
            throw new IOException(xmpExc.getMessage());
        }
    }

    public void setProperty(String schemaNS, String propName, Object value) throws XMPException {
        this.xmpMeta.setProperty(schemaNS, propName, value);
    }

    public void appendArrayItem(String schemaNS, String arrayName, String value) throws XMPException {
        this.xmpMeta.appendArrayItem(schemaNS, arrayName, new PropertyOptions(512), value, null);
    }

    public void appendOrderedArrayItem(String schemaNS, String arrayName, String value) throws XMPException {
        this.xmpMeta.appendArrayItem(schemaNS, arrayName, new PropertyOptions(1024), value, null);
    }

    public void appendAlternateArrayItem(String schemaNS, String arrayName, String value) throws XMPException {
        this.xmpMeta.appendArrayItem(schemaNS, arrayName, new PropertyOptions(2048), value, null);
    }

    public void serialize(OutputStream externalOutputStream) throws XMPException {
        XMPMetaFactory.serialize(this.xmpMeta, externalOutputStream, this.serializeOptions);
    }

    public void close() throws IOException {
        if (this.outputStream == null) {
            return;
        }
        try {
            XMPMetaFactory.serialize(this.xmpMeta, this.outputStream, this.serializeOptions);
            this.outputStream = null;
        } catch (XMPException xmpExc) {
            throw new IOException(xmpExc.getMessage());
        }
    }

    public void addDocInfoProperty(Object key, String value) throws XMPException {
        if (key instanceof String) {
            key = new PdfName((String) key);
        }
        if (PdfName.TITLE.equals(key)) {
            this.xmpMeta.setLocalizedText("http://purl.org/dc/elements/1.1/", "title", "x-default", "x-default", value);
        } else if (PdfName.AUTHOR.equals(key)) {
            this.xmpMeta.appendArrayItem("http://purl.org/dc/elements/1.1/", "creator", new PropertyOptions(1024), value, null);
        } else if (PdfName.SUBJECT.equals(key)) {
            this.xmpMeta.setLocalizedText("http://purl.org/dc/elements/1.1/", "description", "x-default", "x-default", value);
        } else if (PdfName.KEYWORDS.equals(key)) {
            for (String v : value.split(",|;")) {
                if (v.trim().length() > 0) {
                    this.xmpMeta.appendArrayItem("http://purl.org/dc/elements/1.1/", "subject", new PropertyOptions(512), v.trim(), null);
                }
            }
            this.xmpMeta.setProperty("http://ns.adobe.com/pdf/1.3/", "Keywords", value);
        } else if (PdfName.PRODUCER.equals(key)) {
            this.xmpMeta.setProperty("http://ns.adobe.com/pdf/1.3/", "Producer", value);
        } else if (PdfName.CREATOR.equals(key)) {
            this.xmpMeta.setProperty("http://ns.adobe.com/xap/1.0/", "CreatorTool", value);
        } else if (PdfName.CREATIONDATE.equals(key)) {
            this.xmpMeta.setProperty("http://ns.adobe.com/xap/1.0/", "CreateDate", PdfDate.getW3CDate(value));
        } else if (PdfName.MODDATE.equals(key)) {
            this.xmpMeta.setProperty("http://ns.adobe.com/xap/1.0/", "ModifyDate", PdfDate.getW3CDate(value));
        }
    }
}


