package esign.text.pdf;

public class GrayColor
        extends ExtendedColor {

    private static final long serialVersionUID = -6571835680819282746L;
    private float gray;
    public static final GrayColor GRAYBLACK = new GrayColor(0.0F);
    public static final GrayColor GRAYWHITE = new GrayColor(1.0F);

    public GrayColor(int intGray) {
        this(intGray / 255.0F);
    }

    public GrayColor(float floatGray) {
        super(1, floatGray, floatGray, floatGray);
        this.gray = normalize(floatGray);
    }

    public float getGray() {
        return this.gray;
    }

    public boolean equals(Object obj) {
        return (obj instanceof GrayColor && ((GrayColor) obj).gray == this.gray);
    }

    public int hashCode() {
        return Float.floatToIntBits(this.gray);
    }
}
