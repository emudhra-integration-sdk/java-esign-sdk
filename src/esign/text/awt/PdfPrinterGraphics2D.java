package esign.text.awt;

import esign.text.pdf.PdfContentByte;
import java.awt.print.PrinterGraphics;
import java.awt.print.PrinterJob;

public class PdfPrinterGraphics2D
        extends PdfGraphics2D
        implements PrinterGraphics {

    private PrinterJob printerJob;

    public PdfPrinterGraphics2D(PdfContentByte cb, float width, float height, PrinterJob printerJob) {
        super(cb, width, height);
        this.printerJob = printerJob;
    }

    public PdfPrinterGraphics2D(PdfContentByte cb, float width, float height, boolean onlyShapes, PrinterJob printerJob) {
        super(cb, width, height, onlyShapes);
        this.printerJob = printerJob;
    }

    public PdfPrinterGraphics2D(PdfContentByte cb, float width, float height, FontMapper fontMapper, PrinterJob printerJob) {
        super(cb, width, height, fontMapper, false, false, 0.0F);
        this.printerJob = printerJob;
    }

    public PdfPrinterGraphics2D(PdfContentByte cb, float width, float height, FontMapper fontMapper, boolean onlyShapes, boolean convertImagesToJPEG, float quality, PrinterJob printerJob) {
        super(cb, width, height, fontMapper, onlyShapes, convertImagesToJPEG, quality);
        this.printerJob = printerJob;
    }

    public PrinterJob getPrinterJob() {
        return this.printerJob;
    }
}
