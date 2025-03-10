package esign.text.awt.geom;

import esign.text.awt.geom.gl.Crossing;
import esign.text.awt.geom.misc.Messages;
import java.util.NoSuchElementException;

public final class GeneralPath
        implements Shape, Cloneable {

    public static final int WIND_EVEN_ODD = 0;
    public static final int WIND_NON_ZERO = 1;
    private static final int BUFFER_SIZE = 10;
    private static final int BUFFER_CAPACITY = 10;
    byte[] types;
    float[] points;
    int typeSize;
    int pointSize;
    int rule;
    static int[] pointShift = new int[]{2, 2, 4, 6, 0};

    class Iterator
            implements PathIterator {

        int typeIndex;

        int pointIndex;

        GeneralPath p;

        AffineTransform t;

        Iterator(GeneralPath path) {
            this(path, null);
        }

        Iterator(GeneralPath path, AffineTransform at) {
            this.p = path;
            this.t = at;
        }

        public int getWindingRule() {
            return this.p.getWindingRule();
        }

        public boolean isDone() {
            return (this.typeIndex >= this.p.typeSize);
        }

        public void next() {
            this.typeIndex++;
        }

        public int currentSegment(double[] coords) {
            if (isDone()) {
                throw new NoSuchElementException(Messages.getString("awt.4B"));
            }
            int type = this.p.types[this.typeIndex];
            int count = GeneralPath.pointShift[type];
            for (int i = 0; i < count; i++) {
                coords[i] = this.p.points[this.pointIndex + i];
            }
            if (this.t != null) {
                this.t.transform(coords, 0, coords, 0, count / 2);
            }
            this.pointIndex += count;
            return type;
        }

        public int currentSegment(float[] coords) {
            if (isDone()) {
                throw new NoSuchElementException(Messages.getString("awt.4B"));
            }
            int type = this.p.types[this.typeIndex];
            int count = GeneralPath.pointShift[type];
            System.arraycopy(this.p.points, this.pointIndex, coords, 0, count);
            if (this.t != null) {
                this.t.transform(coords, 0, coords, 0, count / 2);
            }
            this.pointIndex += count;
            return type;
        }
    }

    public GeneralPath() {
        this(1, 10);
    }

    public GeneralPath(int rule) {
        this(rule, 10);
    }

    public GeneralPath(int rule, int initialCapacity) {
        setWindingRule(rule);
        this.types = new byte[initialCapacity];
        this.points = new float[initialCapacity * 2];
    }

    public GeneralPath(Shape shape) {
        this(1, 10);
        PathIterator p = shape.getPathIterator(null);
        setWindingRule(p.getWindingRule());
        append(p, false);
    }

    public void setWindingRule(int rule) {
        if (rule != 0 && rule != 1) {
            throw new IllegalArgumentException(Messages.getString("awt.209"));
        }
        this.rule = rule;
    }

    public int getWindingRule() {
        return this.rule;
    }

    void checkBuf(int pointCount, boolean checkMove) {
        if (checkMove && this.typeSize == 0) {
            throw new IllegalPathStateException(Messages.getString("awt.20A"));
        }
        if (this.typeSize == this.types.length) {
            byte[] tmp = new byte[this.typeSize + 10];
            System.arraycopy(this.types, 0, tmp, 0, this.typeSize);
            this.types = tmp;
        }
        if (this.pointSize + pointCount > this.points.length) {
            float[] tmp = new float[this.pointSize + Math.max(20, pointCount)];
            System.arraycopy(this.points, 0, tmp, 0, this.pointSize);
            this.points = tmp;
        }
    }

    public void moveTo(float x, float y) {
        if (this.typeSize > 0 && this.types[this.typeSize - 1] == 0) {
            this.points[this.pointSize - 2] = x;
            this.points[this.pointSize - 1] = y;
        } else {
            checkBuf(2, false);
            this.types[this.typeSize++] = 0;
            this.points[this.pointSize++] = x;
            this.points[this.pointSize++] = y;
        }
    }

    public void lineTo(float x, float y) {
        checkBuf(2, true);
        this.types[this.typeSize++] = 1;
        this.points[this.pointSize++] = x;
        this.points[this.pointSize++] = y;
    }

    public void quadTo(float x1, float y1, float x2, float y2) {
        checkBuf(4, true);
        this.types[this.typeSize++] = 2;
        this.points[this.pointSize++] = x1;
        this.points[this.pointSize++] = y1;
        this.points[this.pointSize++] = x2;
        this.points[this.pointSize++] = y2;
    }

    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        checkBuf(6, true);
        this.types[this.typeSize++] = 3;
        this.points[this.pointSize++] = x1;
        this.points[this.pointSize++] = y1;
        this.points[this.pointSize++] = x2;
        this.points[this.pointSize++] = y2;
        this.points[this.pointSize++] = x3;
        this.points[this.pointSize++] = y3;
    }

    public void closePath() {
        if (this.typeSize == 0 || this.types[this.typeSize - 1] != 4) {
            checkBuf(0, true);
            this.types[this.typeSize++] = 4;
        }
    }

    public void append(Shape shape, boolean connect) {
        PathIterator p = shape.getPathIterator(null);
        append(p, connect);
    }

    public void append(PathIterator path, boolean connect) {
        while (!path.isDone()) {
            float[] coords = new float[6];
            switch (path.currentSegment(coords)) {
                case 0:
                    if (!connect || this.typeSize == 0) {
                        moveTo(coords[0], coords[1]);
                        break;
                    }
                    if (this.types[this.typeSize - 1] != 4 && this.points[this.pointSize - 2] == coords[0] && this.points[this.pointSize - 1] == coords[1]) {
                        break;
                    }

                case 1:
                    lineTo(coords[0], coords[1]);
                    break;
                case 2:
                    quadTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
                case 3:
                    curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    break;
                case 4:
                    closePath();
                    break;
            }
            path.next();
            connect = false;
        }
    }

    public Point2D getCurrentPoint() {
        if (this.typeSize == 0) {
            return null;
        }
        int j = this.pointSize - 2;
        if (this.types[this.typeSize - 1] == 4) {
            for (int i = this.typeSize - 2; i > 0; i--) {
                int type = this.types[i];
                if (type == 0) {
                    break;
                }
                j -= pointShift[type];
            }
        }
        return new Point2D.Float(this.points[j], this.points[j + 1]);
    }

    public void reset() {
        this.typeSize = 0;
        this.pointSize = 0;
    }

    public void transform(AffineTransform t) {
        t.transform(this.points, 0, this.points, 0, this.pointSize / 2);
    }

    public Shape createTransformedShape(AffineTransform t) {
        GeneralPath p = (GeneralPath) clone();
        if (t != null) {
            p.transform(t);
        }
        return p;
    }

    public Rectangle2D getBounds2D() {
        float rx1;
        float ry1;
        float rx2;
        float ry2;
        if (this.pointSize == 0) {
            rx1 = ry1 = rx2 = ry2 = 0.0F;
        } else {
            int i = this.pointSize - 1;
            ry1 = ry2 = this.points[i--];
            rx1 = rx2 = this.points[i--];
            while (i > 0) {
                float y = this.points[i--];
                float x = this.points[i--];
                if (x < rx1) {
                    rx1 = x;
                } else if (x > rx2) {
                    rx2 = x;
                }
                if (y < ry1) {
                    ry1 = y;
                    continue;
                }
                if (y > ry2) {
                    ry2 = y;
                }
            }
        }
        return new Rectangle2D.Float(rx1, ry1, rx2 - rx1, ry2 - ry1);
    }

    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    boolean isInside(int cross) {
        if (this.rule == 1) {
            return Crossing.isInsideNonZero(cross);
        }
        return Crossing.isInsideEvenOdd(cross);
    }

    public boolean contains(double px, double py) {
        return isInside(Crossing.crossShape(this, px, py));
    }

    public boolean contains(double rx, double ry, double rw, double rh) {
        int cross = Crossing.intersectShape(this, rx, ry, rw, rh);
        return (cross != 255 && isInside(cross));
    }

    public boolean intersects(double rx, double ry, double rw, double rh) {
        int cross = Crossing.intersectShape(this, rx, ry, rw, rh);
        return (cross == 255 || isInside(cross));
    }

    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public PathIterator getPathIterator(AffineTransform t) {
        return new Iterator(this, t);
    }

    public PathIterator getPathIterator(AffineTransform t, double flatness) {
        return new FlatteningPathIterator(getPathIterator(t), flatness);
    }

    public Object clone() {
        try {
            GeneralPath p = (GeneralPath) super.clone();
            p.types = (byte[]) this.types.clone();
            p.points = (float[]) this.points.clone();
            return p;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}
