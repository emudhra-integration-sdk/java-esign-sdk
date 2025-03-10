package esign.text.pdf;

public class PatternColor
        extends ExtendedColor {

    private static final long serialVersionUID = -1185448552860615964L;
    PdfPatternPainter painter;

    public PatternColor(PdfPatternPainter painter) {
        super(4, 0.5F, 0.5F, 0.5F);
        this.painter = painter;
    }

    public PdfPatternPainter getPainter() {
        return this.painter;
    }

    public boolean equals(Object obj) {
        return (obj instanceof PatternColor && ((PatternColor) obj).painter.equals(this.painter));
    }

    public int hashCode() {
        return this.painter.hashCode();
    }
}
