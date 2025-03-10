package esign.text.awt.geom;

import esign.text.awt.geom.misc.HashCode;
import esign.text.awt.geom.misc.Messages;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class AffineTransform
        implements Cloneable, Serializable {

    private static final long serialVersionUID = 1330973210523860834L;
    public static final int TYPE_IDENTITY = 0;
    public static final int TYPE_TRANSLATION = 1;
    public static final int TYPE_UNIFORM_SCALE = 2;
    public static final int TYPE_GENERAL_SCALE = 4;
    public static final int TYPE_QUADRANT_ROTATION = 8;
    public static final int TYPE_GENERAL_ROTATION = 16;
    public static final int TYPE_GENERAL_TRANSFORM = 32;
    public static final int TYPE_FLIP = 64;
    public static final int TYPE_MASK_SCALE = 6;
    public static final int TYPE_MASK_ROTATION = 24;
    static final int TYPE_UNKNOWN = -1;
    static final double ZERO = 1.0E-10D;
    double m00;
    double m10;
    double m01;
    double m11;
    double m02;
    double m12;
    transient int type;

    public AffineTransform() {
        this.type = 0;
        this.m00 = this.m11 = 1.0D;
        this.m10 = this.m01 = this.m02 = this.m12 = 0.0D;
    }

    public AffineTransform(AffineTransform t) {
        this.type = t.type;
        this.m00 = t.m00;
        this.m10 = t.m10;
        this.m01 = t.m01;
        this.m11 = t.m11;
        this.m02 = t.m02;
        this.m12 = t.m12;
    }

    public AffineTransform(float m00, float m10, float m01, float m11, float m02, float m12) {
        this.type = -1;
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
    }

    public AffineTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        this.type = -1;
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
    }

    public AffineTransform(float[] matrix) {
        this.type = -1;
        this.m00 = matrix[0];
        this.m10 = matrix[1];
        this.m01 = matrix[2];
        this.m11 = matrix[3];
        if (matrix.length > 4) {
            this.m02 = matrix[4];
            this.m12 = matrix[5];
        }
    }

    public AffineTransform(double[] matrix) {
        this.type = -1;
        this.m00 = matrix[0];
        this.m10 = matrix[1];
        this.m01 = matrix[2];
        this.m11 = matrix[3];
        if (matrix.length > 4) {
            this.m02 = matrix[4];
            this.m12 = matrix[5];
        }
    }

    public int getType() {
        if (this.type != -1) {
            return this.type;
        }

        int type = 0;

        if (this.m00 * this.m01 + this.m10 * this.m11 != 0.0D) {
            type |= 0x20;
            return type;
        }

        if (this.m02 != 0.0D || this.m12 != 0.0D) {
            type |= 0x1;
        } else if (this.m00 == 1.0D && this.m11 == 1.0D && this.m01 == 0.0D && this.m10 == 0.0D) {
            type = 0;
            return type;
        }

        if (this.m00 * this.m11 - this.m01 * this.m10 < 0.0D) {
            type |= 0x40;
        }

        double dx = this.m00 * this.m00 + this.m10 * this.m10;
        double dy = this.m01 * this.m01 + this.m11 * this.m11;
        if (dx != dy) {
            type |= 0x4;
        } else if (dx != 1.0D) {
            type |= 0x2;
        }

        if ((this.m00 == 0.0D && this.m11 == 0.0D) || (this.m10 == 0.0D && this.m01 == 0.0D && (this.m00 < 0.0D || this.m11 < 0.0D))) {

            type |= 0x8;
        } else if (this.m01 != 0.0D || this.m10 != 0.0D) {
            type |= 0x10;
        }

        return type;
    }

    public double getScaleX() {
        return this.m00;
    }

    public double getScaleY() {
        return this.m11;
    }

    public double getShearX() {
        return this.m01;
    }

    public double getShearY() {
        return this.m10;
    }

    public double getTranslateX() {
        return this.m02;
    }

    public double getTranslateY() {
        return this.m12;
    }

    public boolean isIdentity() {
        return (getType() == 0);
    }

    public void getMatrix(double[] matrix) {
        matrix[0] = this.m00;
        matrix[1] = this.m10;
        matrix[2] = this.m01;
        matrix[3] = this.m11;
        if (matrix.length > 4) {
            matrix[4] = this.m02;
            matrix[5] = this.m12;
        }
    }

    public double getDeterminant() {
        return this.m00 * this.m11 - this.m01 * this.m10;
    }

    public void setTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        this.type = -1;
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
    }

    public void setTransform(AffineTransform t) {
        this.type = t.type;
        setTransform(t.m00, t.m10, t.m01, t.m11, t.m02, t.m12);
    }

    public void setToIdentity() {
        this.type = 0;
        this.m00 = this.m11 = 1.0D;
        this.m10 = this.m01 = this.m02 = this.m12 = 0.0D;
    }

    public void setToTranslation(double mx, double my) {
        this.m00 = this.m11 = 1.0D;
        this.m01 = this.m10 = 0.0D;
        this.m02 = mx;
        this.m12 = my;
        if (mx == 0.0D && my == 0.0D) {
            this.type = 0;
        } else {
            this.type = 1;
        }
    }

    public void setToScale(double scx, double scy) {
        this.m00 = scx;
        this.m11 = scy;
        this.m10 = this.m01 = this.m02 = this.m12 = 0.0D;
        if (scx != 1.0D || scy != 1.0D) {
            this.type = -1;
        } else {
            this.type = 0;
        }
    }

    public void setToShear(double shx, double shy) {
        this.m00 = this.m11 = 1.0D;
        this.m02 = this.m12 = 0.0D;
        this.m01 = shx;
        this.m10 = shy;
        if (shx != 0.0D || shy != 0.0D) {
            this.type = -1;
        } else {
            this.type = 0;
        }
    }

    public void setToRotation(double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        if (Math.abs(cos) < 1.0E-10D) {
            cos = 0.0D;
            sin = (sin > 0.0D) ? 1.0D : -1.0D;
        } else if (Math.abs(sin) < 1.0E-10D) {
            sin = 0.0D;
            cos = (cos > 0.0D) ? 1.0D : -1.0D;
        }
        this.m00 = this.m11 = cos;
        this.m01 = -sin;
        this.m10 = sin;
        this.m02 = this.m12 = 0.0D;
        this.type = -1;
    }

    public void setToRotation(double angle, double px, double py) {
        setToRotation(angle);
        this.m02 = px * (1.0D - this.m00) + py * this.m10;
        this.m12 = py * (1.0D - this.m00) - px * this.m10;
        this.type = -1;
    }

    public static AffineTransform getTranslateInstance(double mx, double my) {
        AffineTransform t = new AffineTransform();
        t.setToTranslation(mx, my);
        return t;
    }

    public static AffineTransform getScaleInstance(double scx, double scY) {
        AffineTransform t = new AffineTransform();
        t.setToScale(scx, scY);
        return t;
    }

    public static AffineTransform getShearInstance(double shx, double shy) {
        AffineTransform m = new AffineTransform();
        m.setToShear(shx, shy);
        return m;
    }

    public static AffineTransform getRotateInstance(double angle) {
        AffineTransform t = new AffineTransform();
        t.setToRotation(angle);
        return t;
    }

    public static AffineTransform getRotateInstance(double angle, double x, double y) {
        AffineTransform t = new AffineTransform();
        t.setToRotation(angle, x, y);
        return t;
    }

    public void translate(double mx, double my) {
        concatenate(getTranslateInstance(mx, my));
    }

    public void scale(double scx, double scy) {
        concatenate(getScaleInstance(scx, scy));
    }

    public void shear(double shx, double shy) {
        concatenate(getShearInstance(shx, shy));
    }

    public void rotate(double angle) {
        concatenate(getRotateInstance(angle));
    }

    public void rotate(double angle, double px, double py) {
        concatenate(getRotateInstance(angle, px, py));
    }

    AffineTransform multiply(AffineTransform t1, AffineTransform t2) {
        return new AffineTransform(t1.m00 * t2.m00 + t1.m10 * t2.m01, t1.m00 * t2.m10 + t1.m10 * t2.m11, t1.m01 * t2.m00 + t1.m11 * t2.m01, t1.m01 * t2.m10 + t1.m11 * t2.m11, t1.m02 * t2.m00 + t1.m12 * t2.m01 + t2.m02, t1.m02 * t2.m10 + t1.m12 * t2.m11 + t2.m12);
    }

    public void concatenate(AffineTransform t) {
        setTransform(multiply(t, this));
    }

    public void preConcatenate(AffineTransform t) {
        setTransform(multiply(this, t));
    }

    public AffineTransform createInverse() throws NoninvertibleTransformException {
        double det = getDeterminant();
        if (Math.abs(det) < 1.0E-10D) {
            throw new NoninvertibleTransformException(Messages.getString("awt.204"));
        }
        return new AffineTransform(this.m11 / det, -this.m10 / det, -this.m01 / det, this.m00 / det, (this.m01 * this.m12 - this.m11 * this.m02) / det, (this.m10 * this.m02 - this.m00 * this.m12) / det);
    }

    public Point2D transform(Point2D src, Point2D dst) {
        if (dst == null) {
            if (src instanceof Point2D.Double) {
                dst = new Point2D.Double();
            } else {
                dst = new Point2D.Float();
            }
        }

        double x = src.getX();
        double y = src.getY();

        dst.setLocation(x * this.m00 + y * this.m01 + this.m02, x * this.m10 + y * this.m11 + this.m12);
        return dst;
    }

    public void transform(Point2D[] src, int srcOff, Point2D[] dst, int dstOff, int length) {
        while (--length >= 0) {
            Point2D srcPoint = src[srcOff++];
            double x = srcPoint.getX();
            double y = srcPoint.getY();
            Point2D dstPoint = dst[dstOff];
            if (dstPoint == null) {
                if (srcPoint instanceof Point2D.Double) {
                    dstPoint = new Point2D.Double();
                } else {
                    dstPoint = new Point2D.Float();
                }
            }
            dstPoint.setLocation(x * this.m00 + y * this.m01 + this.m02, x * this.m10 + y * this.m11 + this.m12);
            dst[dstOff++] = dstPoint;
        }
    }

    public void transform(double[] src, int srcOff, double[] dst, int dstOff, int length) {
        int step = 2;
        if (src == dst && srcOff < dstOff && dstOff < srcOff + length * 2) {
            srcOff = srcOff + length * 2 - 2;
            dstOff = dstOff + length * 2 - 2;
            step = -2;
        }
        while (--length >= 0) {
            double x = src[srcOff + 0];
            double y = src[srcOff + 1];
            dst[dstOff + 0] = x * this.m00 + y * this.m01 + this.m02;
            dst[dstOff + 1] = x * this.m10 + y * this.m11 + this.m12;
            srcOff += step;
            dstOff += step;
        }
    }

    public void transform(float[] src, int srcOff, float[] dst, int dstOff, int length) {
        int step = 2;
        if (src == dst && srcOff < dstOff && dstOff < srcOff + length * 2) {
            srcOff = srcOff + length * 2 - 2;
            dstOff = dstOff + length * 2 - 2;
            step = -2;
        }
        while (--length >= 0) {
            float x = src[srcOff + 0];
            float y = src[srcOff + 1];
            dst[dstOff + 0] = (float) (x * this.m00 + y * this.m01 + this.m02);
            dst[dstOff + 1] = (float) (x * this.m10 + y * this.m11 + this.m12);
            srcOff += step;
            dstOff += step;
        }
    }

    public void transform(float[] src, int srcOff, double[] dst, int dstOff, int length) {
        while (--length >= 0) {
            float x = src[srcOff++];
            float y = src[srcOff++];
            dst[dstOff++] = x * this.m00 + y * this.m01 + this.m02;
            dst[dstOff++] = x * this.m10 + y * this.m11 + this.m12;
        }
    }

    public void transform(double[] src, int srcOff, float[] dst, int dstOff, int length) {
        while (--length >= 0) {
            double x = src[srcOff++];
            double y = src[srcOff++];
            dst[dstOff++] = (float) (x * this.m00 + y * this.m01 + this.m02);
            dst[dstOff++] = (float) (x * this.m10 + y * this.m11 + this.m12);
        }
    }

    public Point2D deltaTransform(Point2D src, Point2D dst) {
        if (dst == null) {
            if (src instanceof Point2D.Double) {
                dst = new Point2D.Double();
            } else {
                dst = new Point2D.Float();
            }
        }

        double x = src.getX();
        double y = src.getY();

        dst.setLocation(x * this.m00 + y * this.m01, x * this.m10 + y * this.m11);
        return dst;
    }

    public void deltaTransform(double[] src, int srcOff, double[] dst, int dstOff, int length) {
        while (--length >= 0) {
            double x = src[srcOff++];
            double y = src[srcOff++];
            dst[dstOff++] = x * this.m00 + y * this.m01;
            dst[dstOff++] = x * this.m10 + y * this.m11;
        }
    }

    public Point2D inverseTransform(Point2D src, Point2D dst) throws NoninvertibleTransformException {
        double det = getDeterminant();
        if (Math.abs(det) < 1.0E-10D) {
            throw new NoninvertibleTransformException(Messages.getString("awt.204"));
        }

        if (dst == null) {
            if (src instanceof Point2D.Double) {
                dst = new Point2D.Double();
            } else {
                dst = new Point2D.Float();
            }
        }

        double x = src.getX() - this.m02;
        double y = src.getY() - this.m12;

        dst.setLocation((x * this.m11 - y * this.m01) / det, (y * this.m00 - x * this.m10) / det);
        return dst;
    }

    public void inverseTransform(double[] src, int srcOff, double[] dst, int dstOff, int length) throws NoninvertibleTransformException {
        double det = getDeterminant();
        if (Math.abs(det) < 1.0E-10D) {
            throw new NoninvertibleTransformException(Messages.getString("awt.204"));
        }

        while (--length >= 0) {
            double x = src[srcOff++] - this.m02;
            double y = src[srcOff++] - this.m12;
            dst[dstOff++] = (x * this.m11 - y * this.m01) / det;
            dst[dstOff++] = (y * this.m00 - x * this.m10) / det;
        }
    }

    public void inverseTransform(float[] src, int srcOff, float[] dst, int dstOff, int length) throws NoninvertibleTransformException {
        float det = (float) getDeterminant();
        if (Math.abs(det) < 1.0E-10D) {
            throw new NoninvertibleTransformException(Messages.getString("awt.204"));
        }

        while (--length >= 0) {
            float x = src[srcOff++] - (float) this.m02;
            float y = src[srcOff++] - (float) this.m12;
            dst[dstOff++] = (x * (float) this.m11 - y * (float) this.m01) / det;
            dst[dstOff++] = (y * (float) this.m00 - x * (float) this.m10) / det;
        }
    }

    public Shape createTransformedShape(Shape src) {
        if (src == null) {
            return null;
        }
        if (src instanceof GeneralPath) {
            return ((GeneralPath) src).createTransformedShape(this);
        }
        PathIterator path = src.getPathIterator(this);
        GeneralPath dst = new GeneralPath(path.getWindingRule());
        dst.append(path, false);
        return dst;
    }

    public String toString() {
        return getClass().getName() + "[[" + this.m00 + ", " + this.m01 + ", " + this.m02 + "], [" + this.m10 + ", " + this.m11 + ", " + this.m12 + "]]";
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
        hash.append(this.m00);
        hash.append(this.m01);
        hash.append(this.m02);
        hash.append(this.m10);
        hash.append(this.m11);
        hash.append(this.m12);
        return hash.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AffineTransform) {
            AffineTransform t = (AffineTransform) obj;
            return (this.m00 == t.m00 && this.m01 == t.m01 && this.m02 == t.m02 && this.m10 == t.m10 && this.m11 == t.m11 && this.m12 == t.m12);
        }

        return false;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.type = -1;
    }
}
