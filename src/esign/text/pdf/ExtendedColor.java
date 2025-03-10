package esign.text.pdf;

import esign.text.BaseColor;

public abstract class ExtendedColor
        extends BaseColor {

    private static final long serialVersionUID = 2722660170712380080L;
    public static final int TYPE_RGB = 0;
    public static final int TYPE_GRAY = 1;
    public static final int TYPE_CMYK = 2;
    public static final int TYPE_SEPARATION = 3;
    public static final int TYPE_PATTERN = 4;
    public static final int TYPE_SHADING = 5;
    public static final int TYPE_DEVICEN = 6;
    public static final int TYPE_LAB = 7;
    protected int type;

    public ExtendedColor(int type) {
        super(0, 0, 0);
        this.type = type;
    }

    public ExtendedColor(int type, float red, float green, float blue) {
        super(normalize(red), normalize(green), normalize(blue));
        this.type = type;
    }

    public ExtendedColor(int type, int red, int green, int blue, int alpha) {
        super(normalize(red / 255.0F), normalize(green / 255.0F), normalize(blue / 255.0F), normalize(alpha / 255.0F));
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    public static int getType(BaseColor color) {
        if (color instanceof ExtendedColor) {
            return ((ExtendedColor) color).getType();
        }
        return 0;
    }

    static final float normalize(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        if (value > 1.0F) {
            return 1.0F;
        }
        return value;
    }
}
