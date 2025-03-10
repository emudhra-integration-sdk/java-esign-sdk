package esign.text.pdf;

import java.io.IOException;
import java.io.OutputStream;

public class PdfGState
        extends PdfDictionary {

    public static final PdfName BM_NORMAL = new PdfName("Normal");

    public static final PdfName BM_COMPATIBLE = new PdfName("Compatible");

    public static final PdfName BM_MULTIPLY = new PdfName("Multiply");

    public static final PdfName BM_SCREEN = new PdfName("Screen");

    public static final PdfName BM_OVERLAY = new PdfName("Overlay");

    public static final PdfName BM_DARKEN = new PdfName("Darken");

    public static final PdfName BM_LIGHTEN = new PdfName("Lighten");

    public static final PdfName BM_COLORDODGE = new PdfName("ColorDodge");

    public static final PdfName BM_COLORBURN = new PdfName("ColorBurn");

    public static final PdfName BM_HARDLIGHT = new PdfName("HardLight");

    public static final PdfName BM_SOFTLIGHT = new PdfName("SoftLight");

    public static final PdfName BM_DIFFERENCE = new PdfName("Difference");

    public static final PdfName BM_EXCLUSION = new PdfName("Exclusion");

    public void setOverPrintStroking(boolean op) {
        put(PdfName.OP, op ? PdfBoolean.PDFTRUE : PdfBoolean.PDFFALSE);
    }

    public void setOverPrintNonStroking(boolean op) {
        put(PdfName.op, op ? PdfBoolean.PDFTRUE : PdfBoolean.PDFFALSE);
    }

    public void setOverPrintMode(int opm) {
        put(PdfName.OPM, new PdfNumber((opm == 0) ? 0 : 1));
    }

    public void setStrokeOpacity(float ca) {
        put(PdfName.CA, new PdfNumber(ca));
    }

    public void setFillOpacity(float ca) {
        put(PdfName.ca, new PdfNumber(ca));
    }

    public void setAlphaIsShape(boolean ais) {
        put(PdfName.AIS, ais ? PdfBoolean.PDFTRUE : PdfBoolean.PDFFALSE);
    }

    public void setTextKnockout(boolean tk) {
        put(PdfName.TK, tk ? PdfBoolean.PDFTRUE : PdfBoolean.PDFFALSE);
    }

    public void setBlendMode(PdfName bm) {
        put(PdfName.BM, bm);
    }

    public void setRenderingIntent(PdfName ri) {
        put(PdfName.RI, ri);
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        PdfWriter.checkPdfIsoConformance(writer, 6, this);
        super.toPdf(writer, os);
    }
}
