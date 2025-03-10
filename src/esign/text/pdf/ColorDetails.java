package esign.text.pdf;

class ColorDetails {

    PdfIndirectReference indirectReference;
    PdfName colorSpaceName;
    ICachedColorSpace colorSpace;

    ColorDetails(PdfName colorName, PdfIndirectReference indirectReference, ICachedColorSpace scolor) {
        this.colorSpaceName = colorName;
        this.indirectReference = indirectReference;
        this.colorSpace = scolor;
    }

    public PdfIndirectReference getIndirectReference() {
        return this.indirectReference;
    }

    PdfName getColorSpaceName() {
        return this.colorSpaceName;
    }

    public PdfObject getPdfObject(PdfWriter writer) {
        return this.colorSpace.getPdfObject(writer);
    }
}
