package esign.text;

import esign.text.pdf.BaseFont;

public class Font
        implements Comparable<Font> {

    public static final int NORMAL = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final int UNDERLINE = 4;
    public static final int STRIKETHRU = 8;
    public static final int BOLDITALIC = 3;
    public static final int UNDEFINED = -1;
    public static final int DEFAULTSIZE = 12;

    public enum FontFamily {
        COURIER,
        HELVETICA,
        TIMES_ROMAN,
        SYMBOL,
        ZAPFDINGBATS,
        UNDEFINED;
    }

    public enum FontStyle {
        NORMAL("normal"), BOLD("bold"), ITALIC("italic"), OBLIQUE("oblique"), UNDERLINE("underline"),
        LINETHROUGH("line-through");

        private String code;

        FontStyle(String code) {
            this.code = code;
        }

        public String getValue() {
            return this.code;
        }
    }

    private FontFamily family = FontFamily.UNDEFINED;

    private float size = -1.0F;

    private int style = -1;

    private BaseColor color = null;

    private BaseFont baseFont = null;

    public Font(Font other) {
        this.family = other.family;
        this.size = other.size;
        this.style = other.style;
        this.color = other.color;
        this.baseFont = other.baseFont;
    }

    public Font(FontFamily family, float size, int style, BaseColor color) {
        this.family = family;
        this.size = size;
        this.style = style;
        this.color = color;
    }

    public Font(BaseFont bf, float size, int style, BaseColor color) {
        this.baseFont = bf;
        this.size = size;
        this.style = style;
        this.color = color;
    }

    public Font(BaseFont bf, float size, int style) {
        this(bf, size, style, (BaseColor) null);
    }

    public Font(BaseFont bf, float size) {
        this(bf, size, -1, (BaseColor) null);
    }

    public Font(BaseFont bf) {
        this(bf, -1.0F, -1, (BaseColor) null);
    }

    public Font(FontFamily family, float size, int style) {
        this(family, size, style, (BaseColor) null);
    }

    public Font(FontFamily family, float size) {
        this(family, size, -1, (BaseColor) null);
    }

    public Font(FontFamily family) {
        this(family, -1.0F, -1, (BaseColor) null);
    }

    public Font() {
        this(FontFamily.UNDEFINED, -1.0F, -1, (BaseColor) null);
    }

    public int compareTo(Font font) {
        if (font == null) {
            return -1;
        }

        try {
            if (this.baseFont != null && !this.baseFont.equals(font.getBaseFont())) {
                return -2;
            }
            if (this.family != font.getFamily()) {
                return 1;
            }
            if (this.size != font.getSize()) {
                return 2;
            }
            if (this.style != font.getStyle()) {
                return 3;
            }
            if (this.color == null) {
                if (font.color == null) {
                    return 0;
                }
                return 4;
            }
            if (font.color == null) {
                return 4;
            }
            if (this.color.equals(font.getColor())) {
                return 0;
            }
            return 4;
        } catch (ClassCastException cce) {
            return -3;
        }
    }

    public FontFamily getFamily() {
        return this.family;
    }

    public String getFamilyname() {
        String tmp = "unknown";
        switch (getFamily()) {
            case COURIER:
                return "Courier";
            case HELVETICA:
                return "Helvetica";
            case TIMES_ROMAN:
                return "Times-Roman";
            case SYMBOL:
                return "Symbol";
            case ZAPFDINGBATS:
                return "ZapfDingbats";
        }
        if (this.baseFont != null) {
            String[][] names = this.baseFont.getFamilyFontName();
            for (String[] name : names) {
                if ("0".equals(name[2])) {
                    return name[3];
                }
                if ("1033".equals(name[2])) {
                    tmp = name[3];
                }
                if ("".equals(name[2])) {
                    tmp = name[3];
                }
            }
        }

        return tmp;
    }

    public void setFamily(String family) {
        this.family = getFamily(family);
    }

    public static FontFamily getFamily(String family) {
        if (family.equalsIgnoreCase("Courier")) {
            return FontFamily.COURIER;
        }
        if (family.equalsIgnoreCase("Helvetica")) {
            return FontFamily.HELVETICA;
        }
        if (family.equalsIgnoreCase("Times-Roman")) {
            return FontFamily.TIMES_ROMAN;
        }
        if (family.equalsIgnoreCase("Symbol")) {
            return FontFamily.SYMBOL;
        }
        if (family.equalsIgnoreCase("ZapfDingbats")) {
            return FontFamily.ZAPFDINGBATS;
        }
        return FontFamily.UNDEFINED;
    }

    public float getSize() {
        return this.size;
    }

    public float getCalculatedSize() {
        float s = this.size;
        if (s == -1.0F) {
            s = 12.0F;
        }
        return s;
    }

    public float getCalculatedLeading(float multipliedLeading) {
        return multipliedLeading * getCalculatedSize();
    }

    public void setSize(float size) {
        this.size = size;
    }

    public int getStyle() {
        return this.style;
    }

    public int getCalculatedStyle() {
        int style = this.style;
        if (style == -1) {
            style = 0;
        }
        if (this.baseFont != null) {
            return style;
        }
        if (this.family == FontFamily.SYMBOL || this.family == FontFamily.ZAPFDINGBATS) {
            return style;
        }
        return style & 0xFFFFFFFC;
    }

    public boolean isBold() {
        if (this.style == -1) {
            return false;
        }
        return ((this.style & 0x1) == 1);
    }

    public boolean isItalic() {
        if (this.style == -1) {
            return false;
        }
        return ((this.style & 0x2) == 2);
    }

    public boolean isUnderlined() {
        if (this.style == -1) {
            return false;
        }
        return ((this.style & 0x4) == 4);
    }

    public boolean isStrikethru() {
        if (this.style == -1) {
            return false;
        }
        return ((this.style & 0x8) == 8);
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public void setStyle(String style) {
        if (this.style == -1) {
            this.style = 0;
        }
        this.style |= getStyleValue(style);
    }

    public static int getStyleValue(String style) {
        int s = 0;
        if (style.indexOf(FontStyle.NORMAL.getValue()) != -1) {
            s |= 0x0;
        }
        if (style.indexOf(FontStyle.BOLD.getValue()) != -1) {
            s |= 0x1;
        }
        if (style.indexOf(FontStyle.ITALIC.getValue()) != -1) {
            s |= 0x2;
        }
        if (style.indexOf(FontStyle.OBLIQUE.getValue()) != -1) {
            s |= 0x2;
        }
        if (style.indexOf(FontStyle.UNDERLINE.getValue()) != -1) {
            s |= 0x4;
        }
        if (style.indexOf(FontStyle.LINETHROUGH.getValue()) != -1) {
            s |= 0x8;
        }
        return s;
    }

    public BaseColor getColor() {
        return this.color;
    }

    public void setColor(BaseColor color) {
        this.color = color;
    }

    public void setColor(int red, int green, int blue) {
        this.color = new BaseColor(red, green, blue);
    }

    public BaseFont getBaseFont() {
        return this.baseFont;
    }

    public BaseFont getCalculatedBaseFont(boolean specialEncoding) {
        if (this.baseFont != null) {
            return this.baseFont;
        }
        int style = this.style;
        if (style == -1) {
            style = 0;
        }
        String fontName = "Helvetica";
        String encoding = "Cp1252";
        BaseFont cfont = null;
        switch (this.family) {
            case COURIER:
                switch (style & 0x3) {
                    case 1:
                        fontName = "Courier-Bold";
                        break;
                    case 2:
                        fontName = "Courier-Oblique";
                        break;
                    case 3:
                        fontName = "Courier-BoldOblique";
                        break;
                }

                fontName = "Courier";
                break;

            case TIMES_ROMAN:
                switch (style & 0x3) {
                    case 1:
                        fontName = "Times-Bold";
                        break;
                    case 2:
                        fontName = "Times-Italic";
                        break;
                    case 3:
                        fontName = "Times-BoldItalic";
                        break;
                }

                fontName = "Times-Roman";
                break;

            case SYMBOL:
                fontName = "Symbol";
                if (specialEncoding) {
                    encoding = "Symbol";
                }
                break;
            case ZAPFDINGBATS:
                fontName = "ZapfDingbats";
                if (specialEncoding) {
                    encoding = "ZapfDingbats";
                }
                break;
            default:
                switch (style & 0x3) {
                    case 1:
                        fontName = "Helvetica-Bold";
                        break;
                    case 2:
                        fontName = "Helvetica-Oblique";
                        break;
                    case 3:
                        fontName = "Helvetica-BoldOblique";
                        break;
                }

                fontName = "Helvetica";
                break;
        }

        try {
            cfont = BaseFont.createFont(fontName, encoding, false);
        } catch (Exception ee) {
            throw new ExceptionConverter(ee);
        }
        return cfont;
    }

    public boolean isStandardFont() {
        return (this.family == FontFamily.UNDEFINED && this.size == -1.0F && this.style == -1 && this.color == null && this.baseFont == null);
    }

    public Font difference(Font font) {
        if (font == null) {
            return this;
        }
        float dSize = font.size;
        if (dSize == -1.0F) {
            dSize = this.size;
        }

        int dStyle = -1;
        int style1 = this.style;
        int style2 = font.getStyle();
        if (style1 != -1 || style2 != -1) {
            if (style1 == -1) {
                style1 = 0;
            }
            if (style2 == -1) {
                style2 = 0;
            }
            dStyle = style1 | style2;
        }

        BaseColor dColor = font.color;
        if (dColor == null) {
            dColor = this.color;
        }

        if (font.baseFont != null) {
            return new Font(font.baseFont, dSize, dStyle, dColor);
        }
        if (font.getFamily() != FontFamily.UNDEFINED) {
            return new Font(font.family, dSize, dStyle, dColor);
        }
        if (this.baseFont != null) {
            if (dStyle == style1) {
                return new Font(this.baseFont, dSize, dStyle, dColor);
            }
            return FontFactory.getFont(getFamilyname(), dSize, dStyle, dColor);
        }

        return new Font(this.family, dSize, dStyle, dColor);
    }
}
