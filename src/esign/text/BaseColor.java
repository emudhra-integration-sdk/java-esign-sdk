package esign.text;

import esign.text.error_messages.MessageLocalization;

public class BaseColor {

    public static final BaseColor WHITE = new BaseColor(255, 255, 255);
    public static final BaseColor LIGHT_GRAY = new BaseColor(192, 192, 192);
    public static final BaseColor GRAY = new BaseColor(128, 128, 128);
    public static final BaseColor DARK_GRAY = new BaseColor(64, 64, 64);
    public static final BaseColor BLACK = new BaseColor(0, 0, 0);
    public static final BaseColor RED = new BaseColor(255, 0, 0);
    public static final BaseColor PINK = new BaseColor(255, 175, 175);
    public static final BaseColor ORANGE = new BaseColor(255, 200, 0);
    public static final BaseColor YELLOW = new BaseColor(255, 255, 0);
    public static final BaseColor GREEN = new BaseColor(0, 255, 0);
    public static final BaseColor MAGENTA = new BaseColor(255, 0, 255);
    public static final BaseColor CYAN = new BaseColor(0, 255, 255);
    public static final BaseColor BLUE = new BaseColor(0, 0, 255);

    private static final double FACTOR = 0.7D;

    private int value;

    public BaseColor(int red, int green, int blue, int alpha) {
        setValue(red, green, blue, alpha);
    }

    public BaseColor(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    public BaseColor(float red, float green, float blue, float alpha) {
        this((int) ((red * 255.0F) + 0.5D), (int) ((green * 255.0F) + 0.5D), (int) ((blue * 255.0F) + 0.5D), (int) ((alpha * 255.0F) + 0.5D));
    }

    public BaseColor(float red, float green, float blue) {
        this(red, green, blue, 1.0F);
    }

    public BaseColor(int argb) {
        this.value = argb;
    }

    public int getRGB() {
        return this.value;
    }

    public int getRed() {
        return getRGB() >> 16 & 0xFF;
    }

    public int getGreen() {
        return getRGB() >> 8 & 0xFF;
    }

    public int getBlue() {
        return getRGB() >> 0 & 0xFF;
    }

    public int getAlpha() {
        return getRGB() >> 24 & 0xFF;
    }

    public BaseColor brighter() {
        int r = getRed();
        int g = getGreen();
        int b = getBlue();

        int i = 3;
        if (r == 0 && g == 0 && b == 0) {
            return new BaseColor(i, i, i);
        }
        if (r > 0 && r < i) {
            r = i;
        }
        if (g > 0 && g < i) {
            g = i;
        }
        if (b > 0 && b < i) {
            b = i;
        }
        return new BaseColor(Math.min((int) (r / 0.7D), 255),
                Math.min((int) (g / 0.7D), 255),
                Math.min((int) (b / 0.7D), 255));
    }

    public BaseColor darker() {
        return new BaseColor(Math.max((int) (getRed() * 0.7D), 0),
                Math.max((int) (getGreen() * 0.7D), 0),
                Math.max((int) (getBlue() * 0.7D), 0));
    }

    public boolean equals(Object obj) {
        return (obj instanceof BaseColor && ((BaseColor) obj).value == this.value);
    }

    public int hashCode() {
        return this.value;
    }

    protected void setValue(int red, int green, int blue, int alpha) {
        validate(red);
        validate(green);
        validate(blue);
        validate(alpha);
        this.value = (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF) << 0;
    }

    private static void validate(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("color.value.outside.range.0.255", new Object[0]));
        }
    }

    public String toString() {
        return "Color value[" + Integer.toString(this.value, 16) + "]";
    }
}
