package esign.text.pdf.parser;

public class PathPaintingRenderInfo {

    public static final int NONZERO_WINDING_RULE = 1;
    public static final int EVEN_ODD_RULE = 2;
    public static final int NO_OP = 0;
    public static final int STROKE = 1;
    public static final int FILL = 2;
    private int operation;
    private int rule;
    private GraphicsState gs;

    public PathPaintingRenderInfo(int operation, int rule, GraphicsState gs) {
        this.operation = operation;
        this.rule = rule;
        this.gs = gs;
    }

    public PathPaintingRenderInfo(int operation, GraphicsState gs) {
        this(operation, 1, gs);
    }

    public int getOperation() {
        return this.operation;
    }

    public int getRule() {
        return this.rule;
    }

    public Matrix getCtm() {
        return this.gs.ctm;
    }

    public float getLineWidth() {
        return this.gs.getLineWidth();
    }

    public int getLineCapStyle() {
        return this.gs.getLineCapStyle();
    }

    public int getLineJoinStyle() {
        return this.gs.getLineJoinStyle();
    }

    public float getMiterLimit() {
        return this.gs.getMiterLimit();
    }

    public LineDashPattern getLineDashPattern() {
        return this.gs.getLineDashPattern();
    }
}
