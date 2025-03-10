package esign.text.pdf.parser.clipper;

import java.math.BigInteger;
import java.util.Comparator;

public abstract class Point<T extends Number & Comparable<T>> {

    public static class DoublePoint
            extends Point<Double> {

        public DoublePoint() {
            this(0.0D, 0.0D);
        }

        public DoublePoint(double x, double y) {
            this(x, y, 0.0D);
        }

        public DoublePoint(double x, double y, double z) {
            super(Double.valueOf(x), Double.valueOf(y), Double.valueOf(z));
        }

        public DoublePoint(DoublePoint other) {
            super(other);
        }

        public double getX() {
            return this.x.doubleValue();
        }

        public double getY() {
            return this.y.doubleValue();
        }

        public double getZ() {
            return this.z.doubleValue();
        }
    }

    public static class LongPoint extends Point<Long> {

        public static double getDeltaX(LongPoint pt1, LongPoint pt2) {
            if (pt1.getY() == pt2.getY()) {
                return -3.4E38D;
            }

            return (pt2.getX() - pt1.getX()) / (pt2.getY() - pt1.getY());
        }

        public LongPoint() {
            this(0L, 0L);
        }

        public LongPoint(long x, long y) {
            this(x, y, 0L);
        }

        public LongPoint(double x, double y) {
            this((long) x, (long) y);
        }

        public LongPoint(long x, long y, long z) {
            super(Long.valueOf(x), Long.valueOf(y), Long.valueOf(z));
        }

        public LongPoint(LongPoint other) {
            super(other);
        }

        public long getX() {
            return this.x.longValue();
        }

        public long getY() {
            return this.y.longValue();
        }

        public long getZ() {
            return this.z.longValue();
        }
    }

    private static class NumberComparator<T extends Number & Comparable<T>> implements Comparator<T> {

        private NumberComparator() {
        }

        public int compare(T a, T b) throws ClassCastException {
            return ((Comparable<T>) a).compareTo(b);
        }
    }

    static boolean arePointsClose(Point<? extends Number> pt1, Point<? extends Number> pt2, double distSqrd) {
        double dx = pt1.x.doubleValue() - pt2.x.doubleValue();
        double dy = pt1.y.doubleValue() - pt2.y.doubleValue();
        return (dx * dx + dy * dy <= distSqrd);
    }

    static double distanceFromLineSqrd(Point<? extends Number> pt, Point<? extends Number> ln1, Point<? extends Number> ln2) {
        double A = ln1.y.doubleValue() - ln2.y.doubleValue();
        double B = ln2.x.doubleValue() - ln1.x.doubleValue();
        double C = A * ln1.x.doubleValue() + B * ln1.y.doubleValue();
        C = A * pt.x.doubleValue() + B * pt.y.doubleValue() - C;
        return C * C / (A * A + B * B);
    }

    static DoublePoint getUnitNormal(LongPoint pt1, LongPoint pt2) {
        double dx = (pt2.x.longValue() - pt1.x.longValue());
        double dy = (pt2.y.longValue() - pt1.y.longValue());
        if (dx == 0.0D && dy == 0.0D) {
            return new DoublePoint();
        }

        double f = 1.0D / Math.sqrt(dx * dx + dy * dy);
        dx *= f;
        dy *= f;

        return new DoublePoint(dy, -dx);
    }

    protected static boolean isPt2BetweenPt1AndPt3(LongPoint pt1, LongPoint pt2, LongPoint pt3) {
        if (pt1.equals(pt3) || pt1.equals(pt2) || pt3.equals(pt2)) {
            return false;
        }
        if (pt1.x != pt3.x) {
            return (((pt2.x.longValue() > pt1.x.longValue()) ? true : false) == ((pt2.x.longValue() < pt3.x.longValue()) ? true : false));
        }

        return (((pt2.y.longValue() > pt1.y.longValue()) ? true : false) == ((pt2.y.longValue() < pt3.y.longValue()) ? true : false));
    }

    protected static boolean slopesEqual(LongPoint pt1, LongPoint pt2, LongPoint pt3, boolean useFullRange) {
        if (useFullRange) {
            return BigInteger.valueOf(pt1.getY() - pt2.getY()).multiply(BigInteger.valueOf(pt2.getX() - pt3.getX())).equals(
                    BigInteger.valueOf(pt1.getX() - pt2.getX()).multiply(BigInteger.valueOf(pt2.getY() - pt3.getY())));
        }
        return ((pt1.getY() - pt2.getY()) * (pt2.getX() - pt3.getX()) - (pt1.getX() - pt2.getX()) * (pt2.getY() - pt3.getY()) == 0L);
    }

    protected static boolean slopesEqual(LongPoint pt1, LongPoint pt2, LongPoint pt3, LongPoint pt4, boolean useFullRange) {
        if (useFullRange) {
            return BigInteger.valueOf(pt1.getY() - pt2.getY()).multiply(BigInteger.valueOf(pt3.getX() - pt4.getX())).equals(
                    BigInteger.valueOf(pt1.getX() - pt2.getX()).multiply(BigInteger.valueOf(pt3.getY() - pt4.getY())));
        }
        return ((pt1.getY() - pt2.getY()) * (pt3.getX() - pt4.getX()) - (pt1.getX() - pt2.getX()) * (pt3.getY() - pt4.getY()) == 0L);
    }

    static boolean slopesNearCollinear(LongPoint pt1, LongPoint pt2, LongPoint pt3, double distSqrd) {
        if (Math.abs(pt1.x.longValue() - pt2.x.longValue()) > Math.abs(pt1.y.longValue() - pt2.y.longValue())) {
            if (((pt1.x.longValue() > pt2.x.longValue()) ? true : false) == ((pt1.x.longValue() < pt3.x.longValue()) ? true : false)) {
                return (distanceFromLineSqrd(pt1, pt2, pt3) < distSqrd);
            }
            if (((pt2.x.longValue() > pt1.x.longValue()) ? true : false) == ((pt2.x.longValue() < pt3.x.longValue()) ? true : false)) {
                return (distanceFromLineSqrd(pt2, pt1, pt3) < distSqrd);
            }

            return (distanceFromLineSqrd(pt3, pt1, pt2) < distSqrd);
        }

        if (((pt1.y.longValue() > pt2.y.longValue()) ? true : false) == ((pt1.y.longValue() < pt3.y.longValue()) ? true : false)) {
            return (distanceFromLineSqrd(pt1, pt2, pt3) < distSqrd);
        }
        if (((pt2.y.longValue() > pt1.y.longValue()) ? true : false) == ((pt2.y.longValue() < pt3.y.longValue()) ? true : false)) {
            return (distanceFromLineSqrd(pt2, pt1, pt3) < distSqrd);
        }

        return (distanceFromLineSqrd(pt3, pt1, pt2) < distSqrd);
    }

    private final static NumberComparator NUMBER_COMPARATOR = new NumberComparator();

    protected T x;

    protected T y;

    protected T z;

    protected Point(Point<T> pt) {
        this(pt.x, pt.y, pt.z);
    }

    protected Point(T x, T y, T z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Point) {
            Point<?> a = (Point) obj;
            return (NUMBER_COMPARATOR.compare(this.x, a.x) == 0 && NUMBER_COMPARATOR.compare(this.y, a.y) == 0);
        }

        return false;
    }

    public void set(Point<T> other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public void setX(T x) {
        this.x = x;
    }

    public void setY(T y) {
        this.y = y;
    }

    public void setZ(T z) {
        this.z = z;
    }

    public String toString() {
        return "Point [x=" + this.x + ", y=" + this.y + ", z=" + this.z + "]";
    }
}
