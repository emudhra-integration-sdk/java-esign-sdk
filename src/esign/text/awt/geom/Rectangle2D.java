package esign.text.awt.geom;

import esign.text.awt.geom.misc.HashCode;
import esign.text.awt.geom.misc.Messages;
import java.util.NoSuchElementException;

public abstract class Rectangle2D
        extends RectangularShape {

    public static final int OUT_LEFT = 1;
    public static final int OUT_TOP = 2;
    public static final int OUT_RIGHT = 4;
    public static final int OUT_BOTTOM = 8;

    public abstract void setRect(double paramDouble1, double paramDouble2, double paramDouble3, double paramDouble4);

    public abstract int outcode(double paramDouble1, double paramDouble2);

    public abstract Rectangle2D createIntersection(Rectangle2D paramRectangle2D);

    public abstract Rectangle2D createUnion(Rectangle2D paramRectangle2D);

    public static class Float
            extends Rectangle2D {

        public float x;
        public float y;
        public float width;
        public float height;

        public Float() {
        }

        public Float(float x, float y, float width, float height) {
            setRect(x, y, width, height);
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public double getWidth() {
            return this.width;
        }

        public double getHeight() {
            return this.height;
        }

        public boolean isEmpty() {
            return (this.width <= 0.0F || this.height <= 0.0F);
        }

        public void setRect(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void setRect(double x, double y, double width, double height) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) width;
            this.height = (float) height;
        }

        public void setRect(Rectangle2D r) {
            this.x = (float) r.getX();
            this.y = (float) r.getY();
            this.width = (float) r.getWidth();
            this.height = (float) r.getHeight();
        }

        public int outcode(double px, double py) {
            int code = 0;

            if (this.width <= 0.0F) {
                code |= 0x5;
            } else if (px < this.x) {
                code |= 0x1;
            } else if (px > (this.x + this.width)) {
                code |= 0x4;
            }

            if (this.height <= 0.0F) {
                code |= 0xA;
            } else if (py < this.y) {
                code |= 0x2;
            } else if (py > (this.y + this.height)) {
                code |= 0x8;
            }

            return code;
        }

        public Rectangle2D getBounds2D() {
            return new Float(this.x, this.y, this.width, this.height);
        }

        public Rectangle2D createIntersection(Rectangle2D r) {
            Rectangle2D dst;
            if (r instanceof Rectangle2D.Double) {
                dst = new Rectangle2D.Double();
            } else {
                dst = new Float();
            }
            Rectangle2D.intersect(this, r, dst);
            return dst;
        }

        public Rectangle2D createUnion(Rectangle2D r) {
            Rectangle2D dst;
            if (r instanceof Rectangle2D.Double) {
                dst = new Rectangle2D.Double();
            } else {
                dst = new Float();
            }
            Rectangle2D.union(this, r, dst);
            return dst;
        }

        public String toString() {
            return getClass().getName() + "[x=" + this.x + ",y=" + this.y + ",width=" + this.width + ",height=" + this.height + "]";
        }
    }

    public static class Double
            extends Rectangle2D {

        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double width, double height) {
            setRect(x, y, width, height);
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public double getWidth() {
            return this.width;
        }

        public double getHeight() {
            return this.height;
        }

        public boolean isEmpty() {
            return (this.width <= 0.0D || this.height <= 0.0D);
        }

        public void setRect(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void setRect(Rectangle2D r) {
            this.x = r.getX();
            this.y = r.getY();
            this.width = r.getWidth();
            this.height = r.getHeight();
        }

        public int outcode(double px, double py) {
            int code = 0;

            if (this.width <= 0.0D) {
                code |= 0x5;
            } else if (px < this.x) {
                code |= 0x1;
            } else if (px > this.x + this.width) {
                code |= 0x4;
            }

            if (this.height <= 0.0D) {
                code |= 0xA;
            } else if (py < this.y) {
                code |= 0x2;
            } else if (py > this.y + this.height) {
                code |= 0x8;
            }

            return code;
        }

        public Rectangle2D getBounds2D() {
            return new Double(this.x, this.y, this.width, this.height);
        }

        public Rectangle2D createIntersection(Rectangle2D r) {
            Rectangle2D dst = new Double();
            Rectangle2D.intersect(this, r, dst);
            return dst;
        }

        public Rectangle2D createUnion(Rectangle2D r) {
            Rectangle2D dest = new Double();
            Rectangle2D.union(this, r, dest);
            return dest;
        }

        public String toString() {
            return getClass().getName() + "[x=" + this.x + ",y=" + this.y + ",width=" + this.width + ",height=" + this.height + "]";
        }
    }

    class Iterator
            implements PathIterator {

        double x;

        double y;

        double width;

        double height;

        AffineTransform t;

        int index;

        Iterator(Rectangle2D r, AffineTransform at) {
            this.x = r.getX();
            this.y = r.getY();
            this.width = r.getWidth();
            this.height = r.getHeight();
            this.t = at;
            if (this.width < 0.0D || this.height < 0.0D) {
                this.index = 6;
            }
        }

        public int getWindingRule() {
            return 1;
        }

        public boolean isDone() {
            return (this.index > 5);
        }

        public void next() {
            this.index++;
        }

        public int currentSegment(double[] coords) {
            int type;
            if (isDone()) {
                throw new NoSuchElementException(Messages.getString("awt.4B"));
            }
            if (this.index == 5) {
                return 4;
            }

            if (this.index == 0) {
                type = 0;
                coords[0] = this.x;
                coords[1] = this.y;
            } else {
                type = 1;
                switch (this.index) {
                    case 1:
                        coords[0] = this.x + this.width;
                        coords[1] = this.y;
                        break;
                    case 2:
                        coords[0] = this.x + this.width;
                        coords[1] = this.y + this.height;
                        break;
                    case 3:
                        coords[0] = this.x;
                        coords[1] = this.y + this.height;
                        break;
                    case 4:
                        coords[0] = this.x;
                        coords[1] = this.y;
                        break;
                }
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
            if (this.index == 5) {
                return 4;
            }

            if (this.index == 0) {
                coords[0] = (float) this.x;
                coords[1] = (float) this.y;
                type = 0;
            } else {
                type = 1;
                switch (this.index) {
                    case 1:
                        coords[0] = (float) (this.x + this.width);
                        coords[1] = (float) this.y;
                        break;
                    case 2:
                        coords[0] = (float) (this.x + this.width);
                        coords[1] = (float) (this.y + this.height);
                        break;
                    case 3:
                        coords[0] = (float) this.x;
                        coords[1] = (float) (this.y + this.height);
                        break;
                    case 4:
                        coords[0] = (float) this.x;
                        coords[1] = (float) this.y;
                        break;
                }
            }
            if (this.t != null) {
                this.t.transform(coords, 0, coords, 0, 1);
            }
            return type;
        }
    }

    public void setRect(Rectangle2D r) {
        setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public void setFrame(double x, double y, double width, double height) {
        setRect(x, y, width, height);
    }

    public Rectangle2D getBounds2D() {
        return (Rectangle2D) clone();
    }

    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        double rx1 = getX();
        double ry1 = getY();
        double rx2 = rx1 + getWidth();
        double ry2 = ry1 + getHeight();
        return ((rx1 <= x1 && x1 <= rx2 && ry1 <= y1 && y1 <= ry2) || (rx1 <= x2 && x2 <= rx2 && ry1 <= y2 && y2 <= ry2)
                || Line2D.linesIntersect(rx1, ry1, rx2, ry2, x1, y1, x2, y2)
                || Line2D.linesIntersect(rx2, ry1, rx1, ry2, x1, y1, x2, y2));
    }

    public boolean intersectsLine(Line2D l) {
        return intersectsLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }

    public int outcode(Point2D p) {
        return outcode(p.getX(), p.getY());
    }

    public boolean contains(double x, double y) {
        if (isEmpty()) {
            return false;
        }

        double x1 = getX();
        double y1 = getY();
        double x2 = x1 + getWidth();
        double y2 = y1 + getHeight();

        return (x1 <= x && x < x2 && y1 <= y && y < y2);
    }

    public boolean intersects(double x, double y, double width, double height) {
        if (isEmpty() || width <= 0.0D || height <= 0.0D) {
            return false;
        }

        double x1 = getX();
        double y1 = getY();
        double x2 = x1 + getWidth();
        double y2 = y1 + getHeight();

        return (x + width > x1 && x < x2 && y + height > y1 && y < y2);
    }

    public boolean contains(double x, double y, double width, double height) {
        if (isEmpty() || width <= 0.0D || height <= 0.0D) {
            return false;
        }

        double x1 = getX();
        double y1 = getY();
        double x2 = x1 + getWidth();
        double y2 = y1 + getHeight();

        return (x1 <= x && x + width <= x2 && y1 <= y && y + height <= y2);
    }

    public static void intersect(Rectangle2D src1, Rectangle2D src2, Rectangle2D dst) {
        double x1 = Math.max(src1.getMinX(), src2.getMinX());
        double y1 = Math.max(src1.getMinY(), src2.getMinY());
        double x2 = Math.min(src1.getMaxX(), src2.getMaxX());
        double y2 = Math.min(src1.getMaxY(), src2.getMaxY());
        dst.setFrame(x1, y1, x2 - x1, y2 - y1);
    }

    public static void union(Rectangle2D src1, Rectangle2D src2, Rectangle2D dst) {
        double x1 = Math.min(src1.getMinX(), src2.getMinX());
        double y1 = Math.min(src1.getMinY(), src2.getMinY());
        double x2 = Math.max(src1.getMaxX(), src2.getMaxX());
        double y2 = Math.max(src1.getMaxY(), src2.getMaxY());
        dst.setFrame(x1, y1, x2 - x1, y2 - y1);
    }

    public void add(double x, double y) {
        double x1 = Math.min(getMinX(), x);
        double y1 = Math.min(getMinY(), y);
        double x2 = Math.max(getMaxX(), x);
        double y2 = Math.max(getMaxY(), y);
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    public void add(Point2D p) {
        add(p.getX(), p.getY());
    }

    public void add(Rectangle2D r) {
        union(this, r, this);
    }

    public PathIterator getPathIterator(AffineTransform t) {
        return new Iterator(this, t);
    }

    public PathIterator getPathIterator(AffineTransform t, double flatness) {
        return new Iterator(this, t);
    }

    public int hashCode() {
        HashCode hash = new HashCode();
        hash.append(getX());
        hash.append(getY());
        hash.append(getWidth());
        hash.append(getHeight());
        return hash.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Rectangle2D) {
            Rectangle2D r = (Rectangle2D) obj;
            return (getX() == r.getX()
                    && getY() == r.getY()
                    && getWidth() == r.getWidth()
                    && getHeight() == r.getHeight());
        }
        return false;
    }
}
