package esign.text.awt.geom;

import esign.text.awt.geom.misc.Messages;
import java.util.NoSuchElementException;

public abstract class Line2D
        implements Shape, Cloneable {

    public abstract double getX1();

    public abstract double getY1();

    public abstract double getX2();

    public abstract double getY2();

    public abstract Point2D getP1();

    public abstract Point2D getP2();

    public abstract void setLine(double paramDouble1, double paramDouble2, double paramDouble3, double paramDouble4);

    public static class Float
            extends Line2D {

        public float x1;
        public float y1;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1, float x2, float y2) {
            setLine(x1, y1, x2, y2);
        }

        public Float(Point2D p1, Point2D p2) {
            setLine(p1, p2);
        }

        public double getX1() {
            return this.x1;
        }

        public double getY1() {
            return this.y1;
        }

        public double getX2() {
            return this.x2;
        }

        public double getY2() {
            return this.y2;
        }

        public Point2D getP1() {
            return new Point2D.Float(this.x1, this.y1);
        }

        public Point2D getP2() {
            return new Point2D.Float(this.x2, this.y2);
        }

        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        public void setLine(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public Rectangle2D getBounds2D() {
            float rx;
            float ry;
            float rw;
            float rh;
            if (this.x1 < this.x2) {
                rx = this.x1;
                rw = this.x2 - this.x1;
            } else {
                rx = this.x2;
                rw = this.x1 - this.x2;
            }
            if (this.y1 < this.y2) {
                ry = this.y1;
                rh = this.y2 - this.y1;
            } else {
                ry = this.y2;
                rh = this.y1 - this.y2;
            }
            return new Rectangle2D.Float(rx, ry, rw, rh);
        }
    }

    public static class Double
            extends Line2D {

        public double x1;
        public double y1;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1, double x2, double y2) {
            setLine(x1, y1, x2, y2);
        }

        public Double(Point2D p1, Point2D p2) {
            setLine(p1, p2);
        }

        public double getX1() {
            return this.x1;
        }

        public double getY1() {
            return this.y1;
        }

        public double getX2() {
            return this.x2;
        }

        public double getY2() {
            return this.y2;
        }

        public Point2D getP1() {
            return new Point2D.Double(this.x1, this.y1);
        }

        public Point2D getP2() {
            return new Point2D.Double(this.x2, this.y2);
        }

        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public Rectangle2D getBounds2D() {
            double rx;
            double ry;
            double rw;
            double rh;
            if (this.x1 < this.x2) {
                rx = this.x1;
                rw = this.x2 - this.x1;
            } else {
                rx = this.x2;
                rw = this.x1 - this.x2;
            }
            if (this.y1 < this.y2) {
                ry = this.y1;
                rh = this.y2 - this.y1;
            } else {
                ry = this.y2;
                rh = this.y1 - this.y2;
            }
            return new Rectangle2D.Double(rx, ry, rw, rh);
        }
    }

    class Iterator
            implements PathIterator {

        double x1;

        double y1;

        double x2;

        double y2;

        AffineTransform t;

        int index;

        Iterator(Line2D l, AffineTransform at) {
            this.x1 = l.getX1();
            this.y1 = l.getY1();
            this.x2 = l.getX2();
            this.y2 = l.getY2();
            this.t = at;
        }

        public int getWindingRule() {
            return 1;
        }

        public boolean isDone() {
            return (this.index > 1);
        }

        public void next() {
            this.index++;
        }

        public int currentSegment(double[] coords) {
            int type;
            if (isDone()) {
                throw new NoSuchElementException(Messages.getString("awt.4B"));
            }

            if (this.index == 0) {
                type = 0;
                coords[0] = this.x1;
                coords[1] = this.y1;
            } else {
                type = 1;
                coords[0] = this.x2;
                coords[1] = this.y2;
            }
            if (this.t != null) {
                this.t.transform(coords, 0, coords, 0, 1);
            }
            return type;
        }

        public int currentSegment(float[] coords) {
            int type;
            if (isDone()) {
                throw new NoSuchElementException(Messages.getString("awt.4B"));
            }

            if (this.index == 0) {
                type = 0;
                coords[0] = (float) this.x1;
                coords[1] = (float) this.y1;
            } else {
                type = 1;
                coords[0] = (float) this.x2;
                coords[1] = (float) this.y2;
            }
            if (this.t != null) {
                this.t.transform(coords, 0, coords, 0, 1);
            }
            return type;
        }
    }

    public void setLine(Point2D p1, Point2D p2) {
        setLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public void setLine(Line2D line) {
        setLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    public static int relativeCCW(double x1, double y1, double x2, double y2, double px, double py) {
        x2 -= x1;
        y2 -= y1;
        px -= x1;
        py -= y1;
        double t = px * y2 - py * x2;
        if (t == 0.0D) {
            t = px * x2 + py * y2;
            if (t > 0.0D) {
                px -= x2;
                py -= y2;
                t = px * x2 + py * y2;
                if (t < 0.0D) {
                    t = 0.0D;
                }
            }
        }

        return (t < 0.0D) ? -1 : ((t > 0.0D) ? 1 : 0);
    }

    public int relativeCCW(double px, double py) {
        return relativeCCW(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public int relativeCCW(Point2D p) {
        return relativeCCW(getX1(), getY1(), getX2(), getY2(), p.getX(), p.getY());
    }

    public static boolean linesIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        x2 -= x1;
        y2 -= y1;
        x3 -= x1;
        y3 -= y1;
        x4 -= x1;
        y4 -= y1;

        double AvB = x2 * y3 - x3 * y2;
        double AvC = x2 * y4 - x4 * y2;

        if (AvB == 0.0D && AvC == 0.0D) {
            if (x2 != 0.0D) {
                return (x4 * x3 <= 0.0D || (x3 * x2 >= 0.0D && ((x2 > 0.0D) ? (x3 <= x2 || x4 <= x2) : (x3 >= x2 || x4 >= x2))));
            }

            if (y2 != 0.0D) {
                return (y4 * y3 <= 0.0D || (y3 * y2 >= 0.0D && ((y2 > 0.0D) ? (y3 <= y2 || y4 <= y2) : (y3 >= y2 || y4 >= y2))));
            }

            return false;
        }

        double BvC = x3 * y4 - x4 * y3;

        return (AvB * AvC <= 0.0D && BvC * (AvB + BvC - AvC) <= 0.0D);
    }

    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        return linesIntersect(x1, y1, x2, y2, getX1(), getY1(), getX2(), getY2());
    }

    public boolean intersectsLine(Line2D l) {
        return linesIntersect(l.getX1(), l.getY1(), l.getX2(), l.getY2(), getX1(), getY1(), getX2(), getY2());
    }

    public static double ptSegDistSq(double x1, double y1, double x2, double y2, double px, double py) {
        double dist;
        x2 -= x1;
        y2 -= y1;
        px -= x1;
        py -= y1;

        if (px * x2 + py * y2 <= 0.0D) {
            dist = px * px + py * py;
        } else {
            px = x2 - px;
            py = y2 - py;
            if (px * x2 + py * y2 <= 0.0D) {
                dist = px * px + py * py;
            } else {
                dist = px * y2 - py * x2;
                dist = dist * dist / (x2 * x2 + y2 * y2);
            }
        }
        if (dist < 0.0D) {
            dist = 0.0D;
        }
        return dist;
    }

    public static double ptSegDist(double x1, double y1, double x2, double y2, double px, double py) {
        return Math.sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
    }

    public double ptSegDistSq(double px, double py) {
        return ptSegDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptSegDistSq(Point2D p) {
        return ptSegDistSq(getX1(), getY1(), getX2(), getY2(), p.getX(), p.getY());
    }

    public double ptSegDist(double px, double py) {
        return ptSegDist(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptSegDist(Point2D p) {
        return ptSegDist(getX1(), getY1(), getX2(), getY2(), p.getX(), p.getY());
    }

    public static double ptLineDistSq(double x1, double y1, double x2, double y2, double px, double py) {
        x2 -= x1;
        y2 -= y1;
        px -= x1;
        py -= y1;
        double s = px * y2 - py * x2;
        return s * s / (x2 * x2 + y2 * y2);
    }

    public static double ptLineDist(double x1, double y1, double x2, double y2, double px, double py) {
        return Math.sqrt(ptLineDistSq(x1, y1, x2, y2, px, py));
    }

    public double ptLineDistSq(double px, double py) {
        return ptLineDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptLineDistSq(Point2D p) {
        return ptLineDistSq(getX1(), getY1(), getX2(), getY2(), p.getX(), p.getY());
    }

    public double ptLineDist(double px, double py) {
        return ptLineDist(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptLineDist(Point2D p) {
        return ptLineDist(getX1(), getY1(), getX2(), getY2(), p.getX(), p.getY());
    }

    public boolean contains(double px, double py) {
        return false;
    }

    public boolean contains(Point2D p) {
        return false;
    }

    public boolean contains(Rectangle2D r) {
        return false;
    }

    public boolean contains(double rx, double ry, double rw, double rh) {
        return false;
    }

    public boolean intersects(double rx, double ry, double rw, double rh) {
        return intersects(new Rectangle2D.Double(rx, ry, rw, rh));
    }

    public boolean intersects(Rectangle2D r) {
        return r.intersectsLine(getX1(), getY1(), getX2(), getY2());
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new Iterator(this, at);
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new Iterator(this, at);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}
