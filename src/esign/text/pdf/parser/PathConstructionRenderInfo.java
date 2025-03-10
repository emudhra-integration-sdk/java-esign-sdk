package esign.text.pdf.parser;

import java.util.List;

public class PathConstructionRenderInfo {

    public static final int MOVETO = 1;
    public static final int LINETO = 2;
    public static final int CURVE_123 = 3;
    public static final int CURVE_23 = 4;
    public static final int CURVE_13 = 5;
    public static final int CLOSE = 6;
    public static final int RECT = 7;
    private int operation;
    private List<Float> segmentData;
    private Matrix ctm;

    public PathConstructionRenderInfo(int operation, List<Float> segmentData, Matrix ctm) {
        this.operation = operation;
        this.segmentData = segmentData;
        this.ctm = ctm;
    }

    public PathConstructionRenderInfo(int operation, Matrix ctm) {
        this(operation, null, ctm);
    }

    public int getOperation() {
        return this.operation;
    }

    public List<Float> getSegmentData() {
        return this.segmentData;
    }

    public Matrix getCtm() {
        return this.ctm;
    }
}
