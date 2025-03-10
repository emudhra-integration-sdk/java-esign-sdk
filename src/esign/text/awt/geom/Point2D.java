package esign.text.awt.geom;

import esign.text.awt.geom.misc.HashCode;

public abstract class Point2D
        implements Cloneable {

    public abstract double getX();

    public abstract double getY();

    public abstract void setLocation(double paramDouble1, double paramDouble2);

    public static class Float
            extends Point2D {

        public float x;
        public float y;

        public Float() {
        }

        public Float(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public void setLocation(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public void setLocation(double x, double y) {
            this.x = (float) x;
            this.y = (float) y;
        }

        public String toString() {
            return getClass().getName() + "[x=" + this.x + ",y=" + this.y + "]";
        }
    }

    public static class Double
            extends Point2D {

        public double x;
        public double y;

        public Double() {
        }

        public Double(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public void setLocation(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public String toString() {
            return getClass().getName() + "[x=" + this.x + ",y=" + this.y + "]";
        }
    }

    public void setLocation(Point2D p) {
        setLocation(p.getX(), p.getY());
    }

    public static double distanceSq(double x1, double y1, double x2, double y2) {
        x2 -= x1;
        y2 -= y1;
        return x2 * x2 + y2 * y2;
    }

    public double distanceSq(double px, double py) {
        return distanceSq(getX(), getY(), px, py);
    }

    public double distanceSq(Point2D p) {
        return distanceSq(getX(), getY(), p.getX(), p.getY());
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(distanceSq(x1, y1, x2, y2));
    }

    public double distance(double px, double py) {
        return Math.sqrt(distanceSq(px, py));
    }

    public double distance(Point2D p) {
        return Math.sqrt(distanceSq(p));
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    public int hashCode() {
        HashCode hash = new HashCode();
        hash.append(getX());
        hash.append(getY());
        return hash.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Point2D) {
            Point2D p = (Point2D) obj;
            return (getX() == p.getX() && getY() == p.getY());
        }
        return false;
    }
}
