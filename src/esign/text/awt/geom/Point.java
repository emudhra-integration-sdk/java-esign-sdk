package esign.text.awt.geom;

import java.io.Serializable;

public class Point
        extends Point2D
        implements Serializable {

    private static final long serialVersionUID = -5276940640259749850L;
    public double x;
    public double y;

    public Point() {
        setLocation(0, 0);
    }

    public Point(int x, int y) {
        setLocation(x, y);
    }

    public Point(double x, double y) {
        setLocation(x, y);
    }

    public Point(Point p) {
        setLocation(p.x, p.y);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Point) {
            Point p = (Point) obj;
            return (this.x == p.x && this.y == p.y);
        }
        return false;
    }

    public String toString() {
        return getClass().getName() + "[x=" + this.x + ",y=" + this.y + "]";
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public Point getLocation() {
        return new Point(this.x, this.y);
    }

    public void setLocation(Point p) {
        setLocation(p.x, p.y);
    }

    public void setLocation(int x, int y) {
        setLocation(x, y);
    }

    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void move(int x, int y) {
        move(x, y);
    }

    public void move(double x, double y) {
        setLocation(x, y);
    }

    public void translate(int dx, int dy) {
        translate(dx, dy);
    }

    public void translate(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }
}
