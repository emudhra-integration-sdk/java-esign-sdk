package esign.text.pdf.parser;

import esign.text.BaseColor;
import esign.text.pdf.PRStream;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfIndirectReference;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfReader;
import java.io.IOException;

public class ImageRenderInfo {

    private final GraphicsState gs;
    private final PdfIndirectReference ref;
    private final InlineImageInfo inlineImageInfo;
    private final PdfDictionary colorSpaceDictionary;
    private PdfImageObject imageObject = null;

    private ImageRenderInfo(GraphicsState gs, PdfIndirectReference ref, PdfDictionary colorSpaceDictionary) {
        this.gs = gs;
        this.ref = ref;
        this.inlineImageInfo = null;
        this.colorSpaceDictionary = colorSpaceDictionary;
    }

    private ImageRenderInfo(GraphicsState gs, InlineImageInfo inlineImageInfo, PdfDictionary colorSpaceDictionary) {
        this.gs = gs;
        this.ref = null;
        this.inlineImageInfo = inlineImageInfo;
        this.colorSpaceDictionary = colorSpaceDictionary;
    }

    public static ImageRenderInfo createForXObject(GraphicsState gs, PdfIndirectReference ref, PdfDictionary colorSpaceDictionary) {
        return new ImageRenderInfo(gs, ref, colorSpaceDictionary);
    }

    protected static ImageRenderInfo createForEmbeddedImage(GraphicsState gs, InlineImageInfo inlineImageInfo, PdfDictionary colorSpaceDictionary) {
        ImageRenderInfo renderInfo = new ImageRenderInfo(gs, inlineImageInfo, colorSpaceDictionary);
        return renderInfo;
    }

    public PdfImageObject getImage() throws IOException {
        prepareImageObject();
        return this.imageObject;
    }

    private void prepareImageObject() throws IOException {
        if (this.imageObject != null) {
            return;
        }
        if (this.ref != null) {
            PRStream stream = (PRStream) PdfReader.getPdfObject((PdfObject) this.ref);
            this.imageObject = new PdfImageObject(stream, this.colorSpaceDictionary);
        } else if (this.inlineImageInfo != null) {
            this.imageObject = new PdfImageObject(this.inlineImageInfo.getImageDictionary(), this.inlineImageInfo.getSamples(), this.colorSpaceDictionary);
        }
    }

    public Vector getStartPoint() {
        return (new Vector(0.0F, 0.0F, 1.0F)).cross(this.gs.ctm);
    }

    public Matrix getImageCTM() {
        return this.gs.ctm;
    }

    public float getArea() {
        return this.gs.ctm.getDeterminant();
    }

    public PdfIndirectReference getRef() {
        return this.ref;
    }

    public BaseColor getCurrentFillColor() {
        return this.gs.fillColor;
    }
}
