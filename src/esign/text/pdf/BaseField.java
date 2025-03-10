package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.DocumentException;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public abstract class BaseField {

    public static final float BORDER_WIDTH_THIN = 1.0F;
    public static final float BORDER_WIDTH_MEDIUM = 2.0F;
    public static final float BORDER_WIDTH_THICK = 3.0F;
    public static final int VISIBLE = 0;
    public static final int HIDDEN = 1;
    public static final int VISIBLE_BUT_DOES_NOT_PRINT = 2;
    public static final int HIDDEN_BUT_PRINTABLE = 3;
    public static final int READ_ONLY = 1;
    public static final int REQUIRED = 2;
    public static final int MULTILINE = 4096;
    public static final int DO_NOT_SCROLL = 8388608;
    public static final int PASSWORD = 8192;
    public static final int FILE_SELECTION = 1048576;
    public static final int DO_NOT_SPELL_CHECK = 4194304;
    public static final int EDIT = 262144;
    public static final int MULTISELECT = 2097152;
    public static final int COMB = 16777216;
    protected float borderWidth = 1.0F;
    protected int borderStyle = 0;
    protected BaseColor borderColor;
    protected BaseColor backgroundColor;
    protected BaseColor textColor;
    protected BaseFont font;
    protected float fontSize = 0.0F;
    protected int alignment = 0;

    protected PdfWriter writer;

    protected String text;
    protected Rectangle box;
    protected int rotation = 0;

    protected int visibility;

    protected String fieldName;

    protected int options;

    protected int maxCharacterLength;

    private static final HashMap<PdfName, Integer> fieldKeys = new HashMap<PdfName, Integer>();

    static {
        fieldKeys.putAll(PdfCopyFieldsImp.fieldKeys);
        fieldKeys.put(PdfName.T, Integer.valueOf(1));
    }

    public BaseField(PdfWriter writer, Rectangle box, String fieldName) {
        this.writer = writer;
        setBox(box);
        this.fieldName = fieldName;
    }

    protected BaseFont getRealFont() throws IOException, DocumentException {
        if (this.font == null) {
            return BaseFont.createFont("Helvetica", "Cp1252", false);
        }
        return this.font;
    }

    protected PdfAppearance getBorderAppearance() {
        PdfAppearance app = PdfAppearance.createAppearance(this.writer, this.box.getWidth(), this.box.getHeight());
        switch (this.rotation) {
            case 90:
                app.setMatrix(0.0F, 1.0F, -1.0F, 0.0F, this.box.getHeight(), 0.0F);
                break;
            case 180:
                app.setMatrix(-1.0F, 0.0F, 0.0F, -1.0F, this.box.getWidth(), this.box.getHeight());
                break;
            case 270:
                app.setMatrix(0.0F, -1.0F, 1.0F, 0.0F, 0.0F, this.box.getWidth());
                break;
        }
        app.saveState();

        if (this.backgroundColor != null) {
            app.setColorFill(this.backgroundColor);
            app.rectangle(0.0F, 0.0F, this.box.getWidth(), this.box.getHeight());
            app.fill();
        }

        if (this.borderStyle == 4) {
            if (this.borderWidth != 0.0F && this.borderColor != null) {
                app.setColorStroke(this.borderColor);
                app.setLineWidth(this.borderWidth);
                app.moveTo(0.0F, this.borderWidth / 2.0F);
                app.lineTo(this.box.getWidth(), this.borderWidth / 2.0F);
                app.stroke();
            }

        } else if (this.borderStyle == 2) {
            if (this.borderWidth != 0.0F && this.borderColor != null) {
                app.setColorStroke(this.borderColor);
                app.setLineWidth(this.borderWidth);
                app.rectangle(this.borderWidth / 2.0F, this.borderWidth / 2.0F, this.box.getWidth() - this.borderWidth, this.box.getHeight() - this.borderWidth);
                app.stroke();
            }

            BaseColor actual = this.backgroundColor;
            if (actual == null) {
                actual = BaseColor.WHITE;
            }
            app.setGrayFill(1.0F);
            drawTopFrame(app);
            app.setColorFill(actual.darker());
            drawBottomFrame(app);
        } else if (this.borderStyle == 3) {
            if (this.borderWidth != 0.0F && this.borderColor != null) {
                app.setColorStroke(this.borderColor);
                app.setLineWidth(this.borderWidth);
                app.rectangle(this.borderWidth / 2.0F, this.borderWidth / 2.0F, this.box.getWidth() - this.borderWidth, this.box.getHeight() - this.borderWidth);
                app.stroke();
            }

            app.setGrayFill(0.5F);
            drawTopFrame(app);
            app.setGrayFill(0.75F);
            drawBottomFrame(app);

        } else if (this.borderWidth != 0.0F && this.borderColor != null) {
            if (this.borderStyle == 1) {
                app.setLineDash(3.0F, 0.0F);
            }
            app.setColorStroke(this.borderColor);
            app.setLineWidth(this.borderWidth);
            app.rectangle(this.borderWidth / 2.0F, this.borderWidth / 2.0F, this.box.getWidth() - this.borderWidth, this.box.getHeight() - this.borderWidth);
            app.stroke();
            if ((this.options & 0x1000000) != 0 && this.maxCharacterLength > 1) {
                float step = this.box.getWidth() / this.maxCharacterLength;
                float yb = this.borderWidth / 2.0F;
                float yt = this.box.getHeight() - this.borderWidth / 2.0F;
                for (int k = 1; k < this.maxCharacterLength; k++) {
                    float x = step * k;
                    app.moveTo(x, yb);
                    app.lineTo(x, yt);
                }
                app.stroke();
            }
        }

        app.restoreState();
        return app;
    }

    protected static ArrayList<String> getHardBreaks(String text) {
        ArrayList<String> arr = new ArrayList<String>();
        char[] cs = text.toCharArray();
        int len = cs.length;
        StringBuffer buf = new StringBuffer();
        for (int k = 0; k < len; k++) {
            char c = cs[k];
            if (c == '\r') {
                if (k + 1 < len && cs[k + 1] == '\n') {
                    k++;
                }
                arr.add(buf.toString());
                buf = new StringBuffer();
            } else if (c == '\n') {
                arr.add(buf.toString());
                buf = new StringBuffer();
            } else {

                buf.append(c);
            }
        }
        arr.add(buf.toString());
        return arr;
    }

    protected static void trimRight(StringBuffer buf) {
        int len = buf.length();
        while (true) {
            if (len == 0) {
                return;
            }
            if (buf.charAt(--len) != ' ') {
                return;
            }
            buf.setLength(len);
        }
    }

    protected static ArrayList<String> breakLines(ArrayList<String> breaks, BaseFont font, float fontSize, float width) {
        ArrayList<String> lines = new ArrayList<String>();
        StringBuffer buf = new StringBuffer();
        for (int ck = 0; ck < breaks.size(); ck++) {
            buf.setLength(0);
            float w = 0.0F;
            char[] cs = ((String) breaks.get(ck)).toCharArray();
            int len = cs.length;

            int state = 0;
            int lastspace = -1;
            char c = Character.MIN_VALUE;
            int refk = 0;
            for (int k = 0; k < len; k++) {
                c = cs[k];
                switch (state) {
                    case 0:
                        w += font.getWidthPoint(c, fontSize);
                        buf.append(c);
                        if (w > width) {
                            w = 0.0F;
                            if (buf.length() > 1) {
                                k--;
                                buf.setLength(buf.length() - 1);
                            }
                            lines.add(buf.toString());
                            buf.setLength(0);
                            refk = k;
                            if (c == ' ') {
                                state = 2;
                                break;
                            }
                            state = 1;
                            break;
                        }
                        if (c != ' ') {
                            state = 1;
                        }
                        break;
                    case 1:
                        w += font.getWidthPoint(c, fontSize);
                        buf.append(c);
                        if (c == ' ') {
                            lastspace = k;
                        }
                        if (w > width) {
                            w = 0.0F;
                            if (lastspace >= 0) {
                                k = lastspace;
                                buf.setLength(lastspace - refk);
                                trimRight(buf);
                                lines.add(buf.toString());
                                buf.setLength(0);
                                refk = k;
                                lastspace = -1;
                                state = 2;
                                break;
                            }
                            if (buf.length() > 1) {
                                k--;
                                buf.setLength(buf.length() - 1);
                            }
                            lines.add(buf.toString());
                            buf.setLength(0);
                            refk = k;
                            if (c == ' ') {
                                state = 2;
                            }
                        }
                        break;
                    case 2:
                        if (c != ' ') {
                            w = 0.0F;
                            k--;
                            state = 1;
                        }
                        break;
                }
            }
            trimRight(buf);
            lines.add(buf.toString());
        }
        return lines;
    }

    private void drawTopFrame(PdfAppearance app) {
        app.moveTo(this.borderWidth, this.borderWidth);
        app.lineTo(this.borderWidth, this.box.getHeight() - this.borderWidth);
        app.lineTo(this.box.getWidth() - this.borderWidth, this.box.getHeight() - this.borderWidth);
        app.lineTo(this.box.getWidth() - 2.0F * this.borderWidth, this.box.getHeight() - 2.0F * this.borderWidth);
        app.lineTo(2.0F * this.borderWidth, this.box.getHeight() - 2.0F * this.borderWidth);
        app.lineTo(2.0F * this.borderWidth, 2.0F * this.borderWidth);
        app.lineTo(this.borderWidth, this.borderWidth);
        app.fill();
    }

    private void drawBottomFrame(PdfAppearance app) {
        app.moveTo(this.borderWidth, this.borderWidth);
        app.lineTo(this.box.getWidth() - this.borderWidth, this.borderWidth);
        app.lineTo(this.box.getWidth() - this.borderWidth, this.box.getHeight() - this.borderWidth);
        app.lineTo(this.box.getWidth() - 2.0F * this.borderWidth, this.box.getHeight() - 2.0F * this.borderWidth);
        app.lineTo(this.box.getWidth() - 2.0F * this.borderWidth, 2.0F * this.borderWidth);
        app.lineTo(2.0F * this.borderWidth, 2.0F * this.borderWidth);
        app.lineTo(this.borderWidth, this.borderWidth);
        app.fill();
    }

    public float getBorderWidth() {
        return this.borderWidth;
    }

    public void setBorderWidth(float borderWidth) {
        this.borderWidth = borderWidth;
    }

    public int getBorderStyle() {
        return this.borderStyle;
    }

    public void setBorderStyle(int borderStyle) {
        this.borderStyle = borderStyle;
    }

    public BaseColor getBorderColor() {
        return this.borderColor;
    }

    public void setBorderColor(BaseColor borderColor) {
        this.borderColor = borderColor;
    }

    public BaseColor getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(BaseColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public BaseColor getTextColor() {
        return this.textColor;
    }

    public void setTextColor(BaseColor textColor) {
        this.textColor = textColor;
    }

    public BaseFont getFont() {
        return this.font;
    }

    public void setFont(BaseFont font) {
        this.font = font;
    }

    public float getFontSize() {
        return this.fontSize;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public int getAlignment() {
        return this.alignment;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Rectangle getBox() {
        return this.box;
    }

    public void setBox(Rectangle box) {
        if (box == null) {
            this.box = null;
        } else {

            this.box = new Rectangle(box);
            this.box.normalize();
        }
    }

    public int getRotation() {
        return this.rotation;
    }

    public void setRotation(int rotation) {
        if (rotation % 90 != 0) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("rotation.must.be.a.multiple.of.90", new Object[0]));
        }
        rotation %= 360;
        if (rotation < 0) {
            rotation += 360;
        }
        this.rotation = rotation;
    }

    public void setRotationFromPage(Rectangle page) {
        setRotation(page.getRotation());
    }

    public int getVisibility() {
        return this.visibility;
    }

    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public int getOptions() {
        return this.options;
    }

    public void setOptions(int options) {
        this.options = options;
    }

    public int getMaxCharacterLength() {
        return this.maxCharacterLength;
    }

    public void setMaxCharacterLength(int maxCharacterLength) {
        this.maxCharacterLength = maxCharacterLength;
    }

    public PdfWriter getWriter() {
        return this.writer;
    }

    public void setWriter(PdfWriter writer) {
        this.writer = writer;
    }

    public static void moveFields(PdfDictionary from, PdfDictionary to) {
        for (Iterator<PdfName> i = from.getKeys().iterator(); i.hasNext();) {
            PdfName key = i.next();
            if (fieldKeys.containsKey(key)) {
                if (to != null) {
                    to.put(key, from.get(key));
                }
                i.remove();
            }
        }
    }
}
