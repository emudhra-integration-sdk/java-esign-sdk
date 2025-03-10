package esign.text.pdf.events;

import esign.text.Rectangle;
import esign.text.pdf.PdfContentByte;
import esign.text.pdf.PdfPCell;
import esign.text.pdf.PdfPCellEvent;
import java.util.ArrayList;

public class PdfPCellEventForwarder
        implements PdfPCellEvent {

    protected ArrayList<PdfPCellEvent> events = new ArrayList<PdfPCellEvent>();

    public void addCellEvent(PdfPCellEvent event) {
        this.events.add(event);
    }

    public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
        for (PdfPCellEvent event : this.events) {
            event.cellLayout(cell, position, canvases);
        }
    }
}
