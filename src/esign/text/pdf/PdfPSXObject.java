package esign.text.pdf;

import java.io.IOException;

public class PdfPSXObject
        extends PdfTemplate {

    protected PdfPSXObject() {
    }

    public PdfPSXObject(PdfWriter wr) {
        super(wr);
    }

    public PdfStream getFormXObject(int compressionLevel) throws IOException {
        PdfStream s = new PdfStream(this.content.toByteArray());
        s.put(PdfName.TYPE, PdfName.XOBJECT);
        s.put(PdfName.SUBTYPE, PdfName.PS);
        s.flateCompress(compressionLevel);
        return s;
    }

    public PdfContentByte getDuplicate() {
        PdfPSXObject tpl = new PdfPSXObject();
        tpl.writer = this.writer;
        tpl.pdf = this.pdf;
        tpl.thisReference = this.thisReference;
        tpl.pageResources = this.pageResources;
        tpl.separator = this.separator;
        return tpl;
    }
}
