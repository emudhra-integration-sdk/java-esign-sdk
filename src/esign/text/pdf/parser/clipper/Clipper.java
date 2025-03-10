package esign.text.pdf.parser.clipper;

public interface Clipper {

    public static final int REVERSE_SOLUTION = 1;
    public static final int STRICTLY_SIMPLE = 2;
    public static final int PRESERVE_COLINEAR = 4;

    boolean addPath(Path paramPath, PolyType paramPolyType, boolean paramBoolean);

    boolean addPaths(Paths paramPaths, PolyType paramPolyType, boolean paramBoolean);

    void clear();

    boolean execute(ClipType paramClipType, Paths paramPaths);

    boolean execute(ClipType paramClipType, Paths paramPaths, PolyFillType paramPolyFillType1, PolyFillType paramPolyFillType2);

    boolean execute(ClipType paramClipType, PolyTree paramPolyTree);

    boolean execute(ClipType paramClipType, PolyTree paramPolyTree, PolyFillType paramPolyFillType1, PolyFillType paramPolyFillType2);

    public enum ClipType {
        INTERSECTION, UNION, DIFFERENCE, XOR;
    }

    public enum Direction {
        RIGHT_TO_LEFT, LEFT_TO_RIGHT;
    }

    public enum EndType {
        CLOSED_POLYGON, CLOSED_LINE, OPEN_BUTT, OPEN_SQUARE, OPEN_ROUND;
    }

    public enum JoinType {
        BEVEL, ROUND, MITER;
    }

    public enum PolyFillType {
        EVEN_ODD, NON_ZERO, POSITIVE, NEGATIVE;
    }

    public enum PolyType {
        SUBJECT, CLIP;
    }

    public static interface ZFillCallback {

        void zFill(Point.LongPoint param1LongPoint1, Point.LongPoint param1LongPoint2, Point.LongPoint param1LongPoint3, Point.LongPoint param1LongPoint4, Point.LongPoint param1LongPoint5);
    }
}
