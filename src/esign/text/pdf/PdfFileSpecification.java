package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;
import esign.text.io.StreamUtil;
import esign.text.pdf.collection.PdfCollectionItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class PdfFileSpecification
        extends PdfDictionary {

    protected PdfWriter writer;
    protected PdfIndirectReference ref;

    public PdfFileSpecification() {
        super(PdfName.FILESPEC);
    }

    public static PdfFileSpecification url(PdfWriter writer, String url) {
        PdfFileSpecification fs = new PdfFileSpecification();
        fs.writer = writer;
        fs.put(PdfName.FS, PdfName.URL);
        fs.put(PdfName.F, new PdfString(url));
        return fs;
    }

    public static PdfFileSpecification fileEmbedded(PdfWriter writer, String filePath, String fileDisplay, byte[] fileStore) throws IOException {
        return fileEmbedded(writer, filePath, fileDisplay, fileStore, 9);
    }

    public static PdfFileSpecification fileEmbedded(PdfWriter writer, String filePath, String fileDisplay, byte[] fileStore, int compressionLevel) throws IOException {
        return fileEmbedded(writer, filePath, fileDisplay, fileStore, (String) null, (PdfDictionary) null, compressionLevel);
    }

    public static PdfFileSpecification fileEmbedded(PdfWriter writer, String filePath, String fileDisplay, byte[] fileStore, boolean compress) throws IOException {
        return fileEmbedded(writer, filePath, fileDisplay, fileStore, (String) null, (PdfDictionary) null, compress ? 9 : 0);
    }

    public static PdfFileSpecification fileEmbedded(PdfWriter writer, String filePath, String fileDisplay, byte[] fileStore, boolean compress, String mimeType, PdfDictionary fileParameter) throws IOException {
        return fileEmbedded(writer, filePath, fileDisplay, fileStore, mimeType, fileParameter, compress ? 9 : 0);
    }

    public static PdfFileSpecification fileEmbedded(PdfWriter writer, String filePath, String fileDisplay, byte[] fileStore, String mimeType, PdfDictionary fileParameter, int compressionLevel) throws IOException {
        PdfIndirectReference ref;
        PdfFileSpecification fs = new PdfFileSpecification();
        fs.writer = writer;
        fs.put(PdfName.F, new PdfString(fileDisplay));
        fs.setUnicodeFileName(fileDisplay, false);

        InputStream in = null;

        PdfIndirectReference refFileLength = null;
        try {
            PdfEFStream stream;
            if (fileStore == null) {
                refFileLength = writer.getPdfIndirectReference();
                File file = new File(filePath);
                if (file.canRead()) {
                    in = new FileInputStream(filePath);

                } else if (filePath.startsWith("file:/") || filePath.startsWith("http://") || filePath.startsWith("https://") || filePath.startsWith("jar:")) {
                    in = (new URL(filePath)).openStream();
                } else {

                    in = StreamUtil.getResourceStream(filePath);
                    if (in == null) {
                        throw new IOException(MessageLocalization.getComposedMessage("1.not.found.as.file.or.resource", new Object[]{filePath}));
                    }
                }
                stream = new PdfEFStream(in, writer);
            } else {

                stream = new PdfEFStream(fileStore);
            }
            stream.put(PdfName.TYPE, PdfName.EMBEDDEDFILE);
            stream.flateCompress(compressionLevel);
            PdfDictionary param = new PdfDictionary();
            if (fileParameter != null) {
                param.merge(fileParameter);
            }
            if (!param.contains(PdfName.MODDATE)) {
                param.put(PdfName.MODDATE, new PdfDate());
            }
            if (fileStore == null) {
                stream.put(PdfName.PARAMS, refFileLength);
            } else {

                param.put(PdfName.SIZE, new PdfNumber(stream.getRawLength()));
                stream.put(PdfName.PARAMS, param);
            }

            if (mimeType != null) {
                stream.put(PdfName.SUBTYPE, new PdfName(mimeType));
            }
            ref = writer.addToBody(stream).getIndirectReference();
            if (fileStore == null) {
                stream.writeLength();
                param.put(PdfName.SIZE, new PdfNumber(stream.getRawLength()));
                writer.addToBody(param, refFileLength);
            }
        } finally {

            if (in != null) {
                try {
                    in.close();
                } catch (Exception exception) {
                }
            }
        }
        PdfDictionary f = new PdfDictionary();
        f.put(PdfName.F, ref);
        f.put(PdfName.UF, ref);
        fs.put(PdfName.EF, f);
        return fs;
    }

    public static PdfFileSpecification fileExtern(PdfWriter writer, String filePath) {
        PdfFileSpecification fs = new PdfFileSpecification();
        fs.writer = writer;
        fs.put(PdfName.F, new PdfString(filePath));
        fs.setUnicodeFileName(filePath, false);
        return fs;
    }

    public PdfIndirectReference getReference() throws IOException {
        if (this.ref != null) {
            return this.ref;
        }
        this.ref = this.writer.addToBody(this).getIndirectReference();
        return this.ref;
    }

    public void setMultiByteFileName(byte[] fileName) {
        put(PdfName.F, (new PdfString(fileName)).setHexWriting(true));
    }

    public void setUnicodeFileName(String filename, boolean unicode) {
        put(PdfName.UF, new PdfString(filename, unicode ? "UnicodeBig" : "PDF"));
    }

    public void setVolatile(boolean volatile_file) {
        put(PdfName.V, new PdfBoolean(volatile_file));
    }

    public void addDescription(String description, boolean unicode) {
        put(PdfName.DESC, new PdfString(description, unicode ? "UnicodeBig" : "PDF"));
    }

    public void addCollectionItem(PdfCollectionItem ci) {
        put(PdfName.CI, (PdfObject) ci);
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        PdfWriter.checkPdfIsoConformance(writer, 10, this);
        super.toPdf(writer, os);
    }
}
