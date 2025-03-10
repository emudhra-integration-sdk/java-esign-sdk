package esign.text;

import esign.text.pdf.PdfName;
import java.util.List;

public class ListItem
        extends Paragraph {

    private static final long serialVersionUID = 1970670787169329006L;
    protected Chunk symbol;
    private ListBody listBody = null;
    private ListLabel listLabel = null;

    public ListItem() {
        setRole(PdfName.LI);
    }

    public ListItem(float leading) {
        super(leading);
        setRole(PdfName.LI);
    }

    public ListItem(Chunk chunk) {
        super(chunk);
        setRole(PdfName.LI);
    }

    public ListItem(String string) {
        super(string);
        setRole(PdfName.LI);
    }

    public ListItem(String string, Font font) {
        super(string, font);
        setRole(PdfName.LI);
    }

    public ListItem(float leading, Chunk chunk) {
        super(leading, chunk);
        setRole(PdfName.LI);
    }

    public ListItem(float leading, String string) {
        super(leading, string);
        setRole(PdfName.LI);
    }

    public ListItem(float leading, String string, Font font) {
        super(leading, string, font);
        setRole(PdfName.LI);
    }

    public ListItem(Phrase phrase) {
        super(phrase);
        setRole(PdfName.LI);
    }

    public int type() {
        return 15;
    }

    public Paragraph cloneShallow(boolean spacingBefore) {
        ListItem copy = new ListItem();
        populateProperties(copy, spacingBefore);
        return copy;
    }

    public void setListSymbol(Chunk symbol) {
        if (this.symbol == null) {
            this.symbol = symbol;
            if (this.symbol.getFont().isStandardFont()) {
                this.symbol.setFont(this.font);
            }
        }
    }

    public void setIndentationLeft(float indentation, boolean autoindent) {
        if (autoindent) {
            setIndentationLeft(getListSymbol().getWidthPoint());
        } else {

            setIndentationLeft(indentation);
        }
    }

    public void adjustListSymbolFont() {
        List<Chunk> cks = getChunks();
        if (!cks.isEmpty() && this.symbol != null) {
            this.symbol.setFont(((Chunk) cks.get(0)).getFont());
        }
    }

    public Chunk getListSymbol() {
        return this.symbol;
    }

    public ListBody getListBody() {
        if (this.listBody == null) {
            this.listBody = new ListBody(this);
        }
        return this.listBody;
    }

    public ListLabel getListLabel() {
        if (this.listLabel == null) {
            this.listLabel = new ListLabel(this);
        }
        return this.listLabel;
    }
}
