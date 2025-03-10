package esign.text.pdf;

import java.util.Collection;
import java.util.HashSet;

public class PdfLayerMembership
        extends PdfDictionary
        implements PdfOCG {

    public static final PdfName ALLON = new PdfName("AllOn");

    public static final PdfName ANYON = new PdfName("AnyOn");

    public static final PdfName ANYOFF = new PdfName("AnyOff");

    public static final PdfName ALLOFF = new PdfName("AllOff");

    PdfIndirectReference ref;
    PdfArray members = new PdfArray();
    HashSet<PdfLayer> layers = new HashSet<PdfLayer>();

    public PdfLayerMembership(PdfWriter writer) {
        super(PdfName.OCMD);
        put(PdfName.OCGS, this.members);
        this.ref = writer.getPdfIndirectReference();
    }

    public PdfIndirectReference getRef() {
        return this.ref;
    }

    public void addMember(PdfLayer layer) {
        if (!this.layers.contains(layer)) {
            this.members.add(layer.getRef());
            this.layers.add(layer);
        }
    }

    public Collection<PdfLayer> getLayers() {
        return this.layers;
    }

    public void setVisibilityPolicy(PdfName type) {
        put(PdfName.P, type);
    }

    public void setVisibilityExpression(PdfVisibilityExpression ve) {
        put(PdfName.VE, ve);
    }

    public PdfObject getPdfObject() {
        return this;
    }
}
