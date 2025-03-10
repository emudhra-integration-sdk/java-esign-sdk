package esign.text.pdf.internal;

import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.BaseFont;
import esign.text.pdf.ExtendedColor;
import esign.text.pdf.PatternColor;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfGState;
import esign.text.pdf.PdfImage;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfWriter;
import esign.text.pdf.PdfXConformanceException;
import esign.text.pdf.ShadingColor;
import esign.text.pdf.SpotColor;
import esign.text.pdf.interfaces.PdfXConformance;

public class PdfXConformanceImp
        implements PdfXConformance {

    protected int pdfxConformance = 0;
    protected PdfWriter writer;

    public PdfXConformanceImp(PdfWriter writer) {
        this.writer = writer;
    }

    public void setPDFXConformance(int pdfxConformance) {
        this.pdfxConformance = pdfxConformance;
    }

    public int getPDFXConformance() {
        return this.pdfxConformance;
    }

    public boolean isPdfIso() {
        return isPdfX();
    }

    public boolean isPdfX() {
        return (this.pdfxConformance != 0);
    }

    public boolean isPdfX1A2001() {
        return (this.pdfxConformance == 1);
    }

    public boolean isPdfX32002() {
        return (this.pdfxConformance == 2);
    }

    public void checkPdfIsoConformance(int key, Object obj1) {
        PdfImage image;
        PdfObject cs;
        PdfDictionary gs;
        PdfObject obj;
        double v;
        if (this.writer == null || !this.writer.isPdfX()) {
            return;
        }
        int conf = this.writer.getPDFXConformance();
        switch (key) {
            case 1:
                switch (conf) {
                    case 1:
                        if (obj1 instanceof ExtendedColor) {
                            SpotColor sc;
                            ShadingColor xc;
                            PatternColor pc;
                            ExtendedColor ec = (ExtendedColor) obj1;
                            switch (ec.getType()) {
                                case 1:
                                case 2:
                                    return;
                                case 0:
                                    throw new PdfXConformanceException(MessageLocalization.getComposedMessage("colorspace.rgb.is.not.allowed", new Object[0]));
                                case 3:
                                    sc = (SpotColor) ec;
                                    checkPdfIsoConformance(1, sc.getPdfSpotColor().getAlternativeCS());
                                    break;
                                case 5:
                                    xc = (ShadingColor) ec;
                                    checkPdfIsoConformance(1, xc.getPdfShadingPattern().getShading().getColorSpace());
                                    break;
                                case 4:
                                    pc = (PatternColor) ec;
                                    checkPdfIsoConformance(1, pc.getPainter().getDefaultColor());
                                    break;
                            }
                            break;
                        }
                        if (obj1 instanceof esign.text.BaseColor) {
                            throw new PdfXConformanceException(MessageLocalization.getComposedMessage("colorspace.rgb.is.not.allowed", new Object[0]));
                        }
                        break;
                }

                break;
            case 3:
                if (conf == 1) {
                    throw new PdfXConformanceException(MessageLocalization.getComposedMessage("colorspace.rgb.is.not.allowed", new Object[0]));
                }
                break;
            case 4:
                if (!((BaseFont) obj1).isEmbedded()) {
                    throw new PdfXConformanceException(MessageLocalization.getComposedMessage("all.the.fonts.must.be.embedded.this.one.isn.t.1", new Object[]{((BaseFont) obj1).getPostscriptFontName()}));
                }
                break;
            case 5:
                image = (PdfImage) obj1;
                if (image.get(PdfName.SMASK) != null) {
                    throw new PdfXConformanceException(MessageLocalization.getComposedMessage("the.smask.key.is.not.allowed.in.images", new Object[0]));
                }
                switch (conf) {
                    case 1:
                        cs = image.get(PdfName.COLORSPACE);
                        if (cs == null) {
                            return;
                        }
                        if (cs.isName()) {
                            if (PdfName.DEVICERGB.equals(cs)) {
                                throw new PdfXConformanceException(MessageLocalization.getComposedMessage("colorspace.rgb.is.not.allowed", new Object[0]));
                            }
                            break;
                        }
                        if (cs.isArray()
                                && PdfName.CALRGB.equals(((PdfArray) cs).getPdfObject(0))) {
                            throw new PdfXConformanceException(MessageLocalization.getComposedMessage("colorspace.calrgb.is.not.allowed", new Object[0]));
                        }
                        break;
                }
                break;
            case 6:
                gs = (PdfDictionary) obj1;

                if (gs == null) {
                    break;
                }
                obj = gs.get(PdfName.BM);
                if (obj != null && !PdfGState.BM_NORMAL.equals(obj) && !PdfGState.BM_COMPATIBLE.equals(obj)) {
                    throw new PdfXConformanceException(MessageLocalization.getComposedMessage("blend.mode.1.not.allowed", new Object[]{obj.toString()}));
                }
                obj = gs.get(PdfName.CA);
                v = 0.0D;
                if (obj != null && (v = ((PdfNumber) obj).doubleValue()) != 1.0D) {
                    throw new PdfXConformanceException(MessageLocalization.getComposedMessage("transparency.is.not.allowed.ca.eq.1", new Object[]{String.valueOf(v)}));
                }
                obj = gs.get(PdfName.ca);
                v = 0.0D;
                if (obj != null && (v = ((PdfNumber) obj).doubleValue()) != 1.0D) {
                    throw new PdfXConformanceException(MessageLocalization.getComposedMessage("transparency.is.not.allowed.ca.eq.1", new Object[]{String.valueOf(v)}));
                }
                break;
            case 7:
                throw new PdfXConformanceException(MessageLocalization.getComposedMessage("layers.are.not.allowed", new Object[0]));
        }
    }
}
