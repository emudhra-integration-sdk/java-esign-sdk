package esign.text.awt.geom;

import esign.text.awt.geom.misc.HashCode;
import java.io.Serializable;

public class Dimension
        extends Dimension2D
        implements Serializable {

    private static final long serialVersionUID = 4723952579491349524L;
    public double width;
    public double height;

    public Dimension(Dimension d) {
        this(d.width, d.height);
    }

    public Dimension() {
        this(0, 0);
    }

    public Dimension(double width, double height) {
        setSize(width, height);
    }

    public Dimension(int width, int height) {
        setSize(width, height);
    }

    public int hashCode() {
        HashCode hash = new HashCode();
        hash.append(this.width);
        hash.append(this.height);
        return hash.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Dimension) {
            Dimension d = (Dimension) obj;
            return (d.width == this.width && d.height == this.height);
        }
        return false;
    }

    public String toString() {
        return getClass().getName() + "[width=" + this.width + ",height=" + this.height + "]";
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }

    public void setSize(double width, double height) {
        setSize((int) Math.ceil(width), (int) Math.ceil(height));
    }

    public Dimension getSize() {
        return new Dimension(this.width, this.height);
    }

    public double getHeight() {
        return this.height;
    }

    public double getWidth() {
        return this.width;
    }
}
