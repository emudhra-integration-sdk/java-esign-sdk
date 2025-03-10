package esign.text.pdf;

public class StampContent
        extends PdfContentByte {

    PdfStamperImp.PageStamp ps;
    PageResources pageResources;

    StampContent(PdfStamperImp stamper, PdfStamperImp.PageStamp ps) {
        super(stamper);
        this.ps = ps;
        this.pageResources = ps.pageResources;
    }

    public void setAction(PdfAction action, float llx, float lly, float urx, float ury) {
        ((PdfStamperImp) this.writer).addAnnotation(this.writer.createAnnotation(llx, lly, urx, ury, action, null), this.ps.pageN);
    }

    public PdfContentByte getDuplicate() {
        return new StampContent((PdfStamperImp) this.writer, this.ps);
    }

    PageResources getPageResources() {
        return this.pageResources;
    }

    void addAnnotation(PdfAnnotation annot) {
        ((PdfStamperImp) this.writer).addAnnotation(annot, this.ps.pageN);
    }
}
