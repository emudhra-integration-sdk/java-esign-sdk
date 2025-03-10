package esign.text.pdf.events;

import esign.text.pdf.PdfContentByte;
import esign.text.pdf.PdfPRow;
import esign.text.pdf.PdfPTable;
import esign.text.pdf.PdfPTableEvent;
import esign.text.pdf.PdfPTableEventAfterSplit;
import esign.text.pdf.PdfPTableEventSplit;
import java.util.ArrayList;

public class PdfPTableEventForwarder
        implements PdfPTableEventAfterSplit {

    protected ArrayList<PdfPTableEvent> events = new ArrayList<PdfPTableEvent>();

    public void addTableEvent(PdfPTableEvent event) {
        this.events.add(event);
    }

    public void tableLayout(PdfPTable table, float[][] widths, float[] heights, int headerRows, int rowStart, PdfContentByte[] canvases) {
        for (PdfPTableEvent event : this.events) {
            event.tableLayout(table, widths, heights, headerRows, rowStart, canvases);
        }
    }

    public void splitTable(PdfPTable table) {
        for (PdfPTableEvent event : this.events) {
            if (event instanceof PdfPTableEventSplit) {
                ((PdfPTableEventSplit) event).splitTable(table);
            }
        }
    }

    public void afterSplitTable(PdfPTable table, PdfPRow startRow, int startIdx) {
        for (PdfPTableEvent event : this.events) {
            if (event instanceof PdfPTableEventAfterSplit) {
                ((PdfPTableEventAfterSplit) event).afterSplitTable(table, startRow, startIdx);
            }
        }
    }
}
