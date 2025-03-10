package esign.text.awt.geom;

import java.io.Serializable;

public class Rectangle
        extends Rectangle2D
        implements Shape, Serializable {

    private static final long serialVersionUID = -4345857070255674764L;
    public double x;
    public double y;
    public double width;
    public double height;

    public Rectangle() {
        setBounds(0, 0, 0, 0);
    }

    public Rectangle(Point p) {
        setBounds(p.x, p.y, 0.0D, 0.0D);
    }

    public Rectangle(Point p, Dimension d) {
        setBounds(p.x, p.y, d.width, d.height);
    }

    public Rectangle(double x, double y, double width, double height) {
        setBounds(x, y, width, height);
    }

    public Rectangle(int width, int height) {
        setBounds(0, 0, width, height);
    }

    public Rectangle(Rectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }

    public Rectangle(esign.text.Rectangle r) {
        r.normalize();
        setBounds(r.getLeft(), r.getBottom(), r.getWidth(), r.getHeight());
    }

    public Rectangle(Dimension d) {
        setBounds(0.0D, 0.0D, d.width, d.height);
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getHeight() {
        return this.height;
    }

    public double getWidth() {
        return this.width;
    }

    public boolean isEmpty() {
        return (this.width <= 0.0D || this.height <= 0.0D);
    }

    public Dimension getSize() {
        return new Dimension(this.width, this.height);
    }

    public void setSize(int mx, int my) {
        setSize(mx, my);
    }

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }

    public Point getLocation() {
        return new Point(this.x, this.y);
    }

    public void setLocation(int mx, int my) {
        setLocation(mx, my);
    }

    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setLocation(Point p) {
        setLocation(p.x, p.y);
    }

    public void setRect(double x, double y, double width, double height) {
        int x1 = (int) Math.floor(x);
        int y1 = (int) Math.floor(y);
        int x2 = (int) Math.ceil(x + width);
        int y2 = (int) Math.ceil(y + height);
        setBounds(x1, y1, x2 - x1, y2 - y1);
    }

    public Rectangle getBounds() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    public Rectangle2D getBounds2D() {
        return getBounds();
    }

    public void setBounds(int x, int y, int width, int height) {
        setBounds(x, y, width, height);
    }

    public void setBounds(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = width;
    }

    public void setBounds(Rectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }

    public void grow(int mx, int my) {
        translate(mx, my);
    }

    public void grow(double dx, double dy) {
        this.x -= dx;
        this.y -= dy;
        this.width += dx + dx;
        this.height += dy + dy;
    }

    public void translate(int mx, int my) {
        translate(mx, my);
    }

    public void translate(double mx, double my) {
        this.x += mx;
        this.y += my;
    }

    public void add(int px, int py) {
        add(px, py);
    }

    public void add(double px, double py) {
        double x1 = Math.min(this.x, px);
        double x2 = Math.max(this.x + this.width, px);
        double y1 = Math.min(this.y, py);
        double y2 = Math.max(this.y + this.height, py);
        setBounds(x1, y1, x2 - x1, y2 - y1);
    }

    public void add(Point p) {
        add(p.x, p.y);
    }

    public void add(Rectangle r) {
        double x1 = Math.min(this.x, r.x);
        double x2 = Math.max(this.x + this.width, r.x + r.width);
        double y1 = Math.min(this.y, r.y);
        double y2 = Math.max(this.y + this.height, r.y + r.height);
        setBounds(x1, y1, x2 - x1, y2 - y1);
    }

    public boolean contains(int px, int py) {
        return contains(px, py);
    }

    public boolean contains(double px, double py) {
        if (isEmpty()) {
            return false;
        }
        if (px < this.x || py < this.y) {
            return false;
        }
        px -= this.x;
        py -= this.y;
        return (px < this.width && py < this.height);
    }

    public boolean contains(Point p) {
        return contains(p.x, p.y);
    }

    public boolean contains(int rx, int ry, int rw, int rh) {
        return (contains(rx, ry) && contains(rx + rw - 1, ry + rh - 1));
    }

    public boolean contains(double rx, double ry, double rw, double rh) {
        return (contains(rx, ry) && contains(rx + rw - 0.01D, ry + rh - 0.01D));
    }

    public boolean contains(Rectangle r) {
        return contains(r.x, r.y, r.width, r.height);
    }

    public Rectangle2D createIntersection(Rectangle2D r) {
        if (r instanceof Rectangle) {
            return intersection((Rectangle) r);
        }
        Rectangle2D dst = new Rectangle2D.Double();
        Rectangle2D.intersect(this, r, dst);
        return dst;
    }

    public Rectangle intersection(Rectangle r) {
        double x1 = Math.max(this.x, r.x);
        double y1 = Math.max(this.y, r.y);
        double x2 = Math.min(this.x + this.width, r.x + r.width);
        double y2 = Math.min(this.y + this.height, r.y + r.height);
        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }

    public boolean intersects(Rectangle r) {
        return !intersection(r).isEmpty();
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

    public Rectangle2D createUnion(Rectangle2D r) {
        if (r instanceof Rectangle) {
            return union((Rectangle) r);
        }
        Rectangle2D dst = new Rectangle2D.Double();
        Rectangle2D.union(this, r, dst);
        return dst;
    }

    public Rectangle union(Rectangle r) {
        Rectangle dst = new Rectangle(this);
        dst.add(r);
        return dst;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Rectangle) {
            Rectangle r = (Rectangle) obj;
            return (r.x == this.x && r.y == this.y && r.width == this.width && r.height == this.height);
        }
        return false;
    }

    public String toString() {
        return getClass().getName() + "[x=" + this.x + ",y=" + this.y + ",width=" + this.width + ",height=" + this.height + "]";
    }
}
