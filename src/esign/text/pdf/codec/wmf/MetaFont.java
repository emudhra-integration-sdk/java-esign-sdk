package esign.text.pdf.codec.wmf;

import esign.text.Document;
import esign.text.ExceptionConverter;
import esign.text.Font;
import esign.text.FontFactory;
import esign.text.pdf.BaseFont;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MetaFont
        extends MetaObject {

    static final String[] fontNames = new String[]{"Courier", "Courier-Bold", "Courier-Oblique", "Courier-BoldOblique", "Helvetica", "Helvetica-Bold", "Helvetica-Oblique", "Helvetica-BoldOblique", "Times-Roman", "Times-Bold", "Times-Italic", "Times-BoldItalic", "Symbol", "ZapfDingbats"};

    static final int MARKER_BOLD = 1;

    static final int MARKER_ITALIC = 2;

    static final int MARKER_COURIER = 0;

    static final int MARKER_HELVETICA = 4;

    static final int MARKER_TIMES = 8;

    static final int MARKER_SYMBOL = 12;

    static final int DEFAULT_PITCH = 0;
    static final int FIXED_PITCH = 1;
    static final int VARIABLE_PITCH = 2;
    static final int FF_DONTCARE = 0;
    static final int FF_ROMAN = 1;
    static final int FF_SWISS = 2;
    static final int FF_MODERN = 3;
    static final int FF_SCRIPT = 4;
    static final int FF_DECORATIVE = 5;
    static final int BOLDTHRESHOLD = 600;
    static final int nameSize = 32;
    static final int ETO_OPAQUE = 2;
    static final int ETO_CLIPPED = 4;
    int height;
    float angle;
    int bold;
    int italic;
    boolean underline;
    boolean strikeout;
    int charset;
    int pitchAndFamily;
    String faceName = "arial";
    BaseFont font = null;

    public MetaFont() {
        this.type = 3;
    }

    public void init(InputMeta in) throws IOException {
        this.height = Math.abs(in.readShort());
        in.skip(2);
        this.angle = (float) (in.readShort() / 1800.0D * Math.PI);
        in.skip(2);
        this.bold = (in.readShort() >= 600) ? 1 : 0;
        this.italic = (in.readByte() != 0) ? 2 : 0;
        this.underline = (in.readByte() != 0);
        this.strikeout = (in.readByte() != 0);
        this.charset = in.readByte();
        in.skip(3);
        this.pitchAndFamily = in.readByte();
        byte[] name = new byte[32];
        int k;
        for (k = 0; k < 32; k++) {
            int c = in.readByte();
            if (c == 0) {
                break;
            }
            name[k] = (byte) c;
        }
        try {
            this.faceName = new String(name, 0, k, "Cp1252");
        } catch (UnsupportedEncodingException e) {
            this.faceName = new String(name, 0, k);
        }
        this.faceName = this.faceName.toLowerCase();
    }

    public BaseFont getFont() {
        String fontName;
        if (this.font != null) {
            return this.font;
        }
        Font ff2 = FontFactory.getFont(this.faceName, "Cp1252", true, 10.0F, ((this.italic != 0) ? 2 : 0) | ((this.bold != 0) ? 1 : 0));
        this.font = ff2.getBaseFont();
        if (this.font != null) {
            return this.font;
        }
        if (this.faceName.indexOf("courier") != -1 || this.faceName.indexOf("terminal") != -1 || this.faceName
                .indexOf("fixedsys") != -1) {
            fontName = fontNames[0 + this.italic + this.bold];
        } else if (this.faceName.indexOf("ms sans serif") != -1 || this.faceName.indexOf("arial") != -1 || this.faceName
                .indexOf("system") != -1) {
            fontName = fontNames[4 + this.italic + this.bold];
        } else if (this.faceName.indexOf("arial black") != -1) {
            fontName = fontNames[4 + this.italic + 1];
        } else if (this.faceName.indexOf("times") != -1 || this.faceName.indexOf("ms serif") != -1 || this.faceName
                .indexOf("roman") != -1) {
            fontName = fontNames[8 + this.italic + this.bold];
        } else if (this.faceName.indexOf("symbol") != -1) {
            fontName = fontNames[12];
        } else {

            int pitch = this.pitchAndFamily & 0x3;
            int family = this.pitchAndFamily >> 4 & 0x7;
            switch (family) {
                case 3:
                    fontName = fontNames[0 + this.italic + this.bold];
                    break;
                case 1:
                    fontName = fontNames[8 + this.italic + this.bold];
                    break;
                case 2:
                case 4:
                case 5:
                    fontName = fontNames[4 + this.italic + this.bold];
                    break;

                default:
                    switch (pitch) {
                        case 1:
                            fontName = fontNames[0 + this.italic + this.bold];
                            break;
                    }
                    fontName = fontNames[4 + this.italic + this.bold];
                    break;
            }

        }
        try {
            this.font = BaseFont.createFont(fontName, "Cp1252", false);
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }

        return this.font;
    }

    public float getAngle() {
        return this.angle;
    }

    public boolean isUnderline() {
        return this.underline;
    }

    public boolean isStrikeout() {
        return this.strikeout;
    }

    public float getFontSize(MetaState state) {
        return Math.abs(state.transformY(this.height) - state.transformY(0)) * Document.wmfFontCorrection;
    }
}
