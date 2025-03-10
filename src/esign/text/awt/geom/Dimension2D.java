package esign.text.awt.geom;

public abstract class Dimension2D
        implements Cloneable {

    public abstract double getWidth();

    public abstract double getHeight();

    public abstract void setSize(double paramDouble1, double paramDouble2);

    public void setSize(Dimension2D d) {
        setSize(d.getWidth(), d.getHeight());
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}
