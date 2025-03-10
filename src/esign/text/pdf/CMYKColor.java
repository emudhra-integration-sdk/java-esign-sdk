package esign.text.pdf;

public class CMYKColor
        extends ExtendedColor {

    private static final long serialVersionUID = 5940378778276468452L;
    float cyan;
    float magenta;
    float yellow;
    float black;

    public CMYKColor(int intCyan, int intMagenta, int intYellow, int intBlack) {
        this(intCyan / 255.0F, intMagenta / 255.0F, intYellow / 255.0F, intBlack / 255.0F);
    }

    public CMYKColor(float floatCyan, float floatMagenta, float floatYellow, float floatBlack) {
        super(2, 1.0F - floatCyan - floatBlack, 1.0F - floatMagenta - floatBlack, 1.0F - floatYellow - floatBlack);
        this.cyan = normalize(floatCyan);
        this.magenta = normalize(floatMagenta);
        this.yellow = normalize(floatYellow);
        this.black = normalize(floatBlack);
    }

    public float getCyan() {
        return this.cyan;
    }

    public float getMagenta() {
        return this.magenta;
    }

    public float getYellow() {
        return this.yellow;
    }

    public float getBlack() {
        return this.black;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof CMYKColor)) {
            return false;
        }
        CMYKColor c2 = (CMYKColor) obj;
        return (this.cyan == c2.cyan && this.magenta == c2.magenta && this.yellow == c2.yellow && this.black == c2.black);
    }

    public int hashCode() {
        return Float.floatToIntBits(this.cyan) ^ Float.floatToIntBits(this.magenta) ^ Float.floatToIntBits(this.yellow) ^ Float.floatToIntBits(this.black);
    }
}
