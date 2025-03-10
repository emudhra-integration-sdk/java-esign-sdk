package esign.text.pdf;

import esign.text.DocumentException;
import esign.text.ExceptionConverter;
import esign.text.Rectangle;
import java.io.IOException;

public class RadioCheckField
        extends BaseField {

    public static final int TYPE_CHECK = 1;
    public static final int TYPE_CIRCLE = 2;
    public static final int TYPE_CROSS = 3;
    public static final int TYPE_DIAMOND = 4;
    public static final int TYPE_SQUARE = 5;
    public static final int TYPE_STAR = 6;
    protected static String[] typeChars = new String[]{"4", "l", "8", "u", "n", "H"};

    protected int checkType;

    private String onValue;

    private boolean checked;

    public RadioCheckField(PdfWriter writer, Rectangle box, String fieldName, String onValue) {
        super(writer, box, fieldName);
        setOnValue(onValue);
        setCheckType(2);
    }

    public int getCheckType() {
        return this.checkType;
    }

    public void setCheckType(int checkType) {
        if (checkType < 1 || checkType > 6) {
            checkType = 2;
        }
        this.checkType = checkType;
        setText(typeChars[checkType - 1]);
        try {
            setFont(BaseFont.createFont("ZapfDingbats", "Cp1252", false));
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public String getOnValue() {
        return this.onValue;
    }

    public void setOnValue(String onValue) {
        this.onValue = onValue;
    }

    public boolean isChecked() {
        return this.checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public PdfAppearance getAppearance(boolean isRadio, boolean on) throws IOException, DocumentException {
        if (isRadio && this.checkType == 2) {
            return getAppearanceRadioCircle(on);
        }
        PdfAppearance app = getBorderAppearance();
        if (!on) {
            return app;
        }
        BaseFont ufont = getRealFont();
        boolean borderExtra = (this.borderStyle == 2 || this.borderStyle == 3);
        float h = this.box.getHeight() - this.borderWidth * 2.0F;
        float bw2 = this.borderWidth;
        if (borderExtra) {
            h -= this.borderWidth * 2.0F;
            bw2 *= 2.0F;
        }
        float offsetX = borderExtra ? (2.0F * this.borderWidth) : this.borderWidth;
        offsetX = Math.max(offsetX, 1.0F);
        float offX = Math.min(bw2, offsetX);
        float wt = this.box.getWidth() - 2.0F * offX;
        float ht = this.box.getHeight() - 2.0F * offX;
        float fsize = this.fontSize;
        if (fsize == 0.0F) {
            float bw = ufont.getWidthPoint(this.text, 1.0F);
            if (bw == 0.0F) {
                fsize = 12.0F;
            } else {
                fsize = wt / bw;
            }
            float nfsize = h / ufont.getFontDescriptor(1, 1.0F);
            fsize = Math.min(fsize, nfsize);
        }
        app.saveState();
        app.rectangle(offX, offX, wt, ht);
        app.clip();
        app.newPath();
        if (this.textColor == null) {
            app.resetGrayFill();
        } else {
            app.setColorFill(this.textColor);
        }
        app.beginText();
        app.setFontAndSize(ufont, fsize);
        app.setTextMatrix((this.box.getWidth() - ufont.getWidthPoint(this.text, fsize)) / 2.0F, (this.box
                .getHeight() - ufont.getAscentPoint(this.text, fsize)) / 2.0F);
        app.showText(this.text);
        app.endText();
        app.restoreState();
        return app;
    }

    public PdfAppearance getAppearanceRadioCircle(boolean on) {
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
        Rectangle box = new Rectangle(app.getBoundingBox());
        float cx = box.getWidth() / 2.0F;
        float cy = box.getHeight() / 2.0F;
        float r = (Math.min(box.getWidth(), box.getHeight()) - this.borderWidth) / 2.0F;
        if (r <= 0.0F) {
            return app;
        }
        if (this.backgroundColor != null) {
            app.setColorFill(this.backgroundColor);
            app.circle(cx, cy, r + this.borderWidth / 2.0F);
            app.fill();
        }
        if (this.borderWidth > 0.0F && this.borderColor != null) {
            app.setLineWidth(this.borderWidth);
            app.setColorStroke(this.borderColor);
            app.circle(cx, cy, r);
            app.stroke();
        }
        if (on) {
            if (this.textColor == null) {
                app.resetGrayFill();
            } else {
                app.setColorFill(this.textColor);
            }
            app.circle(cx, cy, r / 2.0F);
            app.fill();
        }
        return app;
    }

    public PdfFormField getRadioGroup(boolean noToggleToOff, boolean radiosInUnison) {
        PdfFormField field = PdfFormField.createRadioButton(this.writer, noToggleToOff);
        if (radiosInUnison) {
            field.setFieldFlags(33554432);
        }
        field.setFieldName(this.fieldName);
        if ((this.options & 0x1) != 0) {
            field.setFieldFlags(1);
        }
        if ((this.options & 0x2) != 0) {
            field.setFieldFlags(2);
        }
        field.setValueAsName(this.checked ? this.onValue : "Off");
        return field;
    }

    public PdfFormField getRadioField() throws IOException, DocumentException {
        return getField(true);
    }

    public PdfFormField getCheckField() throws IOException, DocumentException {
        return getField(false);
    }

    protected PdfFormField getField(boolean isRadio) throws IOException, DocumentException {
        PdfFormField field = null;
        if (isRadio) {
            field = PdfFormField.createEmpty(this.writer);
        } else {
            field = PdfFormField.createCheckBox(this.writer);
        }
        field.setWidget(this.box, PdfAnnotation.HIGHLIGHT_INVERT);
        if (!isRadio) {
            field.setFieldName(this.fieldName);
            if ((this.options & 0x1) != 0) {
                field.setFieldFlags(1);
            }
            if ((this.options & 0x2) != 0) {
                field.setFieldFlags(2);
            }
            field.setValueAsName(this.checked ? this.onValue : "Off");
            setCheckType(this.checkType);
        }
        if (this.text != null) {
            field.setMKNormalCaption(this.text);
        }
        if (this.rotation != 0) {
            field.setMKRotation(this.rotation);
        }
        field.setBorderStyle(new PdfBorderDictionary(this.borderWidth, this.borderStyle, new PdfDashPattern(3.0F)));
        PdfAppearance tpon = getAppearance(isRadio, true);
        PdfAppearance tpoff = getAppearance(isRadio, false);
        field.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, this.onValue, tpon);
        field.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, "Off", tpoff);
        field.setAppearanceState(this.checked ? this.onValue : "Off");
        PdfAppearance da = (PdfAppearance) tpon.getDuplicate();
        BaseFont realFont = getRealFont();
        if (realFont != null) {
            da.setFontAndSize(getRealFont(), this.fontSize);
        }
        if (this.textColor == null) {
            da.setGrayFill(0.0F);
        } else {
            da.setColorFill(this.textColor);
        }
        field.setDefaultAppearanceString(da);
        if (this.borderColor != null) {
            field.setMKBorderColor(this.borderColor);
        }
        if (this.backgroundColor != null) {
            field.setMKBackgroundColor(this.backgroundColor);
        }
        switch (this.visibility) {
            case 1:
                field.setFlags(6);

            case 2:
                return field;
            case 3:
                field.setFlags(36);
        }
        field.setFlags(4);
        return field;
    }
}
