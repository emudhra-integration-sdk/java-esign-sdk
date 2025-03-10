package esign.text;

import esign.text.pdf.PdfName;

public class ListLabel
        extends ListBody {

    protected PdfName role = PdfName.LBL;
    protected float indentation = 0.0F;

    protected ListLabel(ListItem parentItem) {
        super(parentItem);
    }

    public PdfName getRole() {
        return this.role;
    }

    public void setRole(PdfName role) {
        this.role = role;
    }

    public float getIndentation() {
        return this.indentation;
    }

    public void setIndentation(float indentation) {
        this.indentation = indentation;
    }

    @Deprecated
    public boolean getTagLabelContent() {
        return false;
    }

    @Deprecated
    public void setTagLabelContent(boolean tagLabelContent) {
    }

    public boolean isInline() {
        return true;
    }
}
