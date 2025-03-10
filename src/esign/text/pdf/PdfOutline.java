package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.Chunk;
import esign.text.Paragraph;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class PdfOutline
        extends PdfDictionary {

    private PdfIndirectReference reference;
    private int count = 0;

    private PdfOutline parent;

    private PdfDestination destination;

    private PdfAction action;

    protected ArrayList<PdfOutline> kids = new ArrayList<PdfOutline>();

    protected PdfWriter writer;

    private String tag;

    private boolean open;

    private BaseColor color;

    private int style = 0;

    PdfOutline(PdfWriter writer) {
        super(OUTLINES);
        this.open = true;
        this.parent = null;
        this.writer = writer;
    }

    public PdfOutline(PdfOutline parent, PdfAction action, String title) {
        this(parent, action, title, true);
    }

    public PdfOutline(PdfOutline parent, PdfAction action, String title, boolean open) {
        this.action = action;
        initOutline(parent, title, open);
    }

    public PdfOutline(PdfOutline parent, PdfDestination destination, String title) {
        this(parent, destination, title, true);
    }

    public PdfOutline(PdfOutline parent, PdfDestination destination, String title, boolean open) {
        this.destination = destination;
        initOutline(parent, title, open);
    }

    public PdfOutline(PdfOutline parent, PdfAction action, PdfString title) {
        this(parent, action, title, true);
    }

    public PdfOutline(PdfOutline parent, PdfAction action, PdfString title, boolean open) {
        this(parent, action, title.toString(), open);
    }

    public PdfOutline(PdfOutline parent, PdfDestination destination, PdfString title) {
        this(parent, destination, title, true);
    }

    public PdfOutline(PdfOutline parent, PdfDestination destination, PdfString title, boolean open) {
        this(parent, destination, title.toString(), true);
    }

    public PdfOutline(PdfOutline parent, PdfAction action, Paragraph title) {
        this(parent, action, title, true);
    }

    public PdfOutline(PdfOutline parent, PdfAction action, Paragraph title, boolean open) {
        StringBuffer buf = new StringBuffer();
        for (Chunk chunk : title.getChunks()) {
            buf.append(chunk.getContent());
        }
        this.action = action;
        initOutline(parent, buf.toString(), open);
    }

    public PdfOutline(PdfOutline parent, PdfDestination destination, Paragraph title) {
        this(parent, destination, title, true);
    }

    public PdfOutline(PdfOutline parent, PdfDestination destination, Paragraph title, boolean open) {
        StringBuffer buf = new StringBuffer();
        for (Object element : title.getChunks()) {
            Chunk chunk = (Chunk) element;
            buf.append(chunk.getContent());
        }
        this.destination = destination;
        initOutline(parent, buf.toString(), open);
    }

    void initOutline(PdfOutline parent, String title, boolean open) {
        this.open = open;
        this.parent = parent;
        this.writer = parent.writer;
        put(PdfName.TITLE, new PdfString(title, "UnicodeBig"));
        parent.addKid(this);
        if (this.destination != null && !this.destination.hasPage()) {
            setDestinationPage(this.writer.getCurrentPage());
        }
    }

    public void setIndirectReference(PdfIndirectReference reference) {
        this.reference = reference;
    }

    public PdfIndirectReference indirectReference() {
        return this.reference;
    }

    public PdfOutline parent() {
        return this.parent;
    }

    public boolean setDestinationPage(PdfIndirectReference pageReference) {
        if (this.destination == null) {
            return false;
        }
        return this.destination.addPage(pageReference);
    }

    public PdfDestination getPdfDestination() {
        return this.destination;
    }

    int getCount() {
        return this.count;
    }

    void setCount(int count) {
        this.count = count;
    }

    public int level() {
        if (this.parent == null) {
            return 0;
        }
        return this.parent.level() + 1;
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        if (this.color != null && !this.color.equals(BaseColor.BLACK)) {
            put(PdfName.C, new PdfArray(new float[]{this.color.getRed() / 255.0F, this.color.getGreen() / 255.0F, this.color.getBlue() / 255.0F}));
        }
        int flag = 0;
        if ((this.style & 0x1) != 0) {
            flag |= 0x2;
        }
        if ((this.style & 0x2) != 0) {
            flag |= 0x1;
        }
        if (flag != 0) {
            put(PdfName.F, new PdfNumber(flag));
        }
        if (this.parent != null) {
            put(PdfName.PARENT, this.parent.indirectReference());
        }
        if (this.destination != null && this.destination.hasPage()) {
            put(PdfName.DEST, this.destination);
        }
        if (this.action != null) {
            put(PdfName.A, this.action);
        }
        if (this.count != 0) {
            put(PdfName.COUNT, new PdfNumber(this.count));
        }
        super.toPdf(writer, os);
    }

    public void addKid(PdfOutline outline) {
        this.kids.add(outline);
    }

    public ArrayList<PdfOutline> getKids() {
        return this.kids;
    }

    public void setKids(ArrayList<PdfOutline> kids) {
        this.kids = kids;
    }

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTitle() {
        PdfString title = (PdfString) get(PdfName.TITLE);
        return title.toString();
    }

    public void setTitle(String title) {
        put(PdfName.TITLE, new PdfString(title, "UnicodeBig"));
    }

    public boolean isOpen() {
        return this.open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public BaseColor getColor() {
        return this.color;
    }

    public void setColor(BaseColor color) {
        this.color = color;
    }

    public int getStyle() {
        return this.style;
    }

    public void setStyle(int style) {
        this.style = style;
    }
}
