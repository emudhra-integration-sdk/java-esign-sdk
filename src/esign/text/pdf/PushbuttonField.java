package esign.text.pdf;

import esign.text.DocumentException;
import esign.text.Image;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import java.io.IOException;

public class PushbuttonField
        extends BaseField {

    public static final int LAYOUT_LABEL_ONLY = 1;
    public static final int LAYOUT_ICON_ONLY = 2;
    public static final int LAYOUT_ICON_TOP_LABEL_BOTTOM = 3;
    public static final int LAYOUT_LABEL_TOP_ICON_BOTTOM = 4;
    public static final int LAYOUT_ICON_LEFT_LABEL_RIGHT = 5;
    public static final int LAYOUT_LABEL_LEFT_ICON_RIGHT = 6;
    public static final int LAYOUT_LABEL_OVER_ICON = 7;
    public static final int SCALE_ICON_ALWAYS = 1;
    public static final int SCALE_ICON_NEVER = 2;
    public static final int SCALE_ICON_IS_TOO_BIG = 3;
    public static final int SCALE_ICON_IS_TOO_SMALL = 4;
    private int layout = 1;

    private Image image;

    private PdfTemplate template;

    private int scaleIcon = 1;

    private boolean proportionalIcon = true;

    private float iconVerticalAdjustment = 0.5F;

    private float iconHorizontalAdjustment = 0.5F;

    private boolean iconFitToBounds;

    private PdfTemplate tp;

    private PRIndirectReference iconReference;

    public PushbuttonField(PdfWriter writer, Rectangle box, String fieldName) {
        super(writer, box, fieldName);
    }

    public int getLayout() {
        return this.layout;
    }

    public void setLayout(int layout) {
        if (layout < 1 || layout > 7) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("layout.out.of.bounds", new Object[0]));
        }
        this.layout = layout;
    }

    public Image getImage() {
        return this.image;
    }

    public void setImage(Image image) {
        this.image = image;
        this.template = null;
    }

    public PdfTemplate getTemplate() {
        return this.template;
    }

    public void setTemplate(PdfTemplate template) {
        this.template = template;
        this.image = null;
    }

    public int getScaleIcon() {
        return this.scaleIcon;
    }

    public void setScaleIcon(int scaleIcon) {
        if (scaleIcon < 1 || scaleIcon > 4) {
            scaleIcon = 1;
        }
        this.scaleIcon = scaleIcon;
    }

    public boolean isProportionalIcon() {
        return this.proportionalIcon;
    }

    public void setProportionalIcon(boolean proportionalIcon) {
        this.proportionalIcon = proportionalIcon;
    }

    public float getIconVerticalAdjustment() {
        return this.iconVerticalAdjustment;
    }

    public void setIconVerticalAdjustment(float iconVerticalAdjustment) {
        if (iconVerticalAdjustment < 0.0F) {
            iconVerticalAdjustment = 0.0F;
        } else if (iconVerticalAdjustment > 1.0F) {
            iconVerticalAdjustment = 1.0F;
        }
        this.iconVerticalAdjustment = iconVerticalAdjustment;
    }

    public float getIconHorizontalAdjustment() {
        return this.iconHorizontalAdjustment;
    }

    public void setIconHorizontalAdjustment(float iconHorizontalAdjustment) {
        if (iconHorizontalAdjustment < 0.0F) {
            iconHorizontalAdjustment = 0.0F;
        } else if (iconHorizontalAdjustment > 1.0F) {
            iconHorizontalAdjustment = 1.0F;
        }
        this.iconHorizontalAdjustment = iconHorizontalAdjustment;
    }

    private float calculateFontSize(float w, float h) throws IOException, DocumentException {
        BaseFont ufont = getRealFont();
        float fsize = this.fontSize;
        if (fsize == 0.0F) {
            float bw = ufont.getWidthPoint(this.text, 1.0F);
            if (bw == 0.0F) {
                fsize = 12.0F;
            } else {
                fsize = w / bw;
            }
            float nfsize = h / (1.0F - ufont.getFontDescriptor(3, 1.0F));
            fsize = Math.min(fsize, nfsize);
            if (fsize < 4.0F) {
                fsize = 4.0F;
            }
        }
        return fsize;
    }

    public PdfAppearance getAppearance() throws IOException, DocumentException {
        PdfAppearance app = getBorderAppearance();
        Rectangle box = new Rectangle(app.getBoundingBox());
        if ((this.text == null || this.text.length() == 0) && (this.layout == 1 || (this.image == null && this.template == null && this.iconReference == null))) {
            return app;
        }
        if (this.layout == 2 && this.image == null && this.template == null && this.iconReference == null) {
            return app;
        }
        BaseFont ufont = getRealFont();
        boolean borderExtra = (this.borderStyle == 2 || this.borderStyle == 3);
        float h = box.getHeight() - this.borderWidth * 2.0F;
        float bw2 = this.borderWidth;
        if (borderExtra) {
            h -= this.borderWidth * 2.0F;
            bw2 *= 2.0F;
        }
        float offsetX = borderExtra ? (2.0F * this.borderWidth) : this.borderWidth;
        offsetX = Math.max(offsetX, 1.0F);
        float offX = Math.min(bw2, offsetX);
        this.tp = null;
        float textX = Float.NaN;
        float textY = 0.0F;
        float fsize = this.fontSize;
        float wt = box.getWidth() - 2.0F * offX - 2.0F;
        float ht = box.getHeight() - 2.0F * offX;
        float adj = this.iconFitToBounds ? 0.0F : (offX + 1.0F);
        int nlayout = this.layout;
        if (this.image == null && this.template == null && this.iconReference == null) {
            nlayout = 1;
        }
        Rectangle iconBox = null;
        while (true) {
            float nht;
            float nw;
            switch (nlayout) {
                case 1:
                case 7:
                    if (this.text != null && this.text.length() > 0 && wt > 0.0F && ht > 0.0F) {
                        fsize = calculateFontSize(wt, ht);
                        textX = (box.getWidth() - ufont.getWidthPoint(this.text, fsize)) / 2.0F;
                        textY = (box.getHeight() - ufont.getFontDescriptor(1, fsize)) / 2.0F;
                    }
                case 2:
                    if (nlayout == 7 || nlayout == 2) {
                        iconBox = new Rectangle(box.getLeft() + adj, box.getBottom() + adj, box.getRight() - adj, box.getTop() - adj);
                    }
                    break;
                case 3:
                    if (this.text == null || this.text.length() == 0 || wt <= 0.0F || ht <= 0.0F) {
                        nlayout = 2;
                        continue;
                    }
                    nht = box.getHeight() * 0.35F - offX;
                    if (nht > 0.0F) {
                        fsize = calculateFontSize(wt, nht);
                    } else {
                        fsize = 4.0F;
                    }
                    textX = (box.getWidth() - ufont.getWidthPoint(this.text, fsize)) / 2.0F;
                    textY = offX - ufont.getFontDescriptor(3, fsize);
                    iconBox = new Rectangle(box.getLeft() + adj, textY + fsize, box.getRight() - adj, box.getTop() - adj);
                    break;
                case 4:
                    if (this.text == null || this.text.length() == 0 || wt <= 0.0F || ht <= 0.0F) {
                        nlayout = 2;
                        continue;
                    }
                    nht = box.getHeight() * 0.35F - offX;
                    if (nht > 0.0F) {
                        fsize = calculateFontSize(wt, nht);
                    } else {
                        fsize = 4.0F;
                    }
                    textX = (box.getWidth() - ufont.getWidthPoint(this.text, fsize)) / 2.0F;
                    textY = box.getHeight() - offX - fsize;
                    if (textY < offX) {
                        textY = offX;
                    }
                    iconBox = new Rectangle(box.getLeft() + adj, box.getBottom() + adj, box.getRight() - adj, textY + ufont.getFontDescriptor(3, fsize));
                    break;
                case 6:
                    if (this.text == null || this.text.length() == 0 || wt <= 0.0F || ht <= 0.0F) {
                        nlayout = 2;
                        continue;
                    }
                    nw = box.getWidth() * 0.35F - offX;
                    if (nw > 0.0F) {
                        fsize = calculateFontSize(wt, nw);
                    } else {
                        fsize = 4.0F;
                    }
                    if (ufont.getWidthPoint(this.text, fsize) >= wt) {
                        nlayout = 1;
                        fsize = this.fontSize;
                        continue;
                    }
                    textX = offX + 1.0F;
                    textY = (box.getHeight() - ufont.getFontDescriptor(1, fsize)) / 2.0F;
                    iconBox = new Rectangle(textX + ufont.getWidthPoint(this.text, fsize), box.getBottom() + adj, box.getRight() - adj, box.getTop() - adj);
                    break;
                case 5:
                    if (this.text == null || this.text.length() == 0 || wt <= 0.0F || ht <= 0.0F) {
                        nlayout = 2;
                        continue;
                    }
                    nw = box.getWidth() * 0.35F - offX;
                    if (nw > 0.0F) {
                        fsize = calculateFontSize(wt, nw);
                    } else {
                        fsize = 4.0F;
                    }
                    if (ufont.getWidthPoint(this.text, fsize) >= wt) {
                        nlayout = 1;
                        fsize = this.fontSize;
                        continue;
                    }
                    textX = box.getWidth() - ufont.getWidthPoint(this.text, fsize) - offX - 1.0F;
                    textY = (box.getHeight() - ufont.getFontDescriptor(1, fsize)) / 2.0F;
                    iconBox = new Rectangle(box.getLeft() + adj, box.getBottom() + adj, textX - 1.0F, box.getTop() - adj);
                    break;
            }
            break;
        }
        if (textY < box.getBottom() + offX) {
            textY = box.getBottom() + offX;
        }
        if (iconBox != null && (iconBox.getWidth() <= 0.0F || iconBox.getHeight() <= 0.0F)) {
            iconBox = null;
        }
        boolean haveIcon = false;
        float boundingBoxWidth = 0.0F;
        float boundingBoxHeight = 0.0F;
        PdfArray matrix = null;
        if (iconBox != null) {
            if (this.image != null) {
                this.tp = new PdfTemplate(this.writer);
                this.tp.setBoundingBox(new Rectangle((Rectangle) this.image));
                this.writer.addDirectTemplateSimple(this.tp, PdfName.FRM);
                this.tp.addImage(this.image, this.image.getWidth(), 0.0F, 0.0F, this.image.getHeight(), 0.0F, 0.0F);
                haveIcon = true;
                boundingBoxWidth = this.tp.getBoundingBox().getWidth();
                boundingBoxHeight = this.tp.getBoundingBox().getHeight();
            } else if (this.template != null) {
                this.tp = new PdfTemplate(this.writer);
                this.tp.setBoundingBox(new Rectangle(this.template.getWidth(), this.template.getHeight()));
                this.writer.addDirectTemplateSimple(this.tp, PdfName.FRM);
                this.tp.addTemplate(this.template, this.template.getBoundingBox().getLeft(), this.template.getBoundingBox().getBottom());
                haveIcon = true;
                boundingBoxWidth = this.tp.getBoundingBox().getWidth();
                boundingBoxHeight = this.tp.getBoundingBox().getHeight();
            } else if (this.iconReference != null) {
                PdfDictionary dic = (PdfDictionary) PdfReader.getPdfObject(this.iconReference);
                if (dic != null) {
                    Rectangle r2 = PdfReader.getNormalizedRectangle(dic.getAsArray(PdfName.BBOX));
                    matrix = dic.getAsArray(PdfName.MATRIX);
                    haveIcon = true;
                    boundingBoxWidth = r2.getWidth();
                    boundingBoxHeight = r2.getHeight();
                }
            }
        }
        if (haveIcon) {
            float icx = iconBox.getWidth() / boundingBoxWidth;
            float icy = iconBox.getHeight() / boundingBoxHeight;
            if (this.proportionalIcon) {
                switch (this.scaleIcon) {
                    case 3:
                        icx = Math.min(icx, icy);
                        icx = Math.min(icx, 1.0F);
                        break;
                    case 4:
                        icx = Math.min(icx, icy);
                        icx = Math.max(icx, 1.0F);
                        break;
                    case 2:
                        icx = 1.0F;
                        break;
                    default:
                        icx = Math.min(icx, icy);
                        break;
                }
                icy = icx;
            } else {

                switch (this.scaleIcon) {
                    case 3:
                        icx = Math.min(icx, 1.0F);
                        icy = Math.min(icy, 1.0F);
                        break;
                    case 4:
                        icx = Math.max(icx, 1.0F);
                        icy = Math.max(icy, 1.0F);
                        break;
                    case 2:
                        icx = icy = 1.0F;
                        break;
                }

            }
            float xpos = iconBox.getLeft() + (iconBox.getWidth() - boundingBoxWidth * icx) * this.iconHorizontalAdjustment;
            float ypos = iconBox.getBottom() + (iconBox.getHeight() - boundingBoxHeight * icy) * this.iconVerticalAdjustment;
            app.saveState();
            app.rectangle(iconBox.getLeft(), iconBox.getBottom(), iconBox.getWidth(), iconBox.getHeight());
            app.clip();
            app.newPath();
            if (this.tp != null) {
                app.addTemplate(this.tp, icx, 0.0F, 0.0F, icy, xpos, ypos);
            } else {
                float cox = 0.0F;
                float coy = 0.0F;
                if (matrix != null && matrix.size() == 6) {
                    PdfNumber nm = matrix.getAsNumber(4);
                    if (nm != null) {
                        cox = nm.floatValue();
                    }
                    nm = matrix.getAsNumber(5);
                    if (nm != null) {
                        coy = nm.floatValue();
                    }
                }
                app.addTemplateReference(this.iconReference, PdfName.FRM, icx, 0.0F, 0.0F, icy, xpos - cox * icx, ypos - coy * icy);
            }
            app.restoreState();
        }
        if (!Float.isNaN(textX)) {
            app.saveState();
            app.rectangle(offX, offX, box.getWidth() - 2.0F * offX, box.getHeight() - 2.0F * offX);
            app.clip();
            app.newPath();
            if (this.textColor == null) {
                app.resetGrayFill();
            } else {
                app.setColorFill(this.textColor);
            }
            app.beginText();
            app.setFontAndSize(ufont, fsize);
            app.setTextMatrix(textX, textY);
            app.showText(this.text);
            app.endText();
            app.restoreState();
        }
        return app;
    }

    public PdfFormField getField() throws IOException, DocumentException {
        PdfFormField field = PdfFormField.createPushButton(this.writer);
        field.setWidget(this.box, PdfAnnotation.HIGHLIGHT_INVERT);
        if (this.fieldName != null) {
            field.setFieldName(this.fieldName);
            if ((this.options & 0x1) != 0) {
                field.setFieldFlags(1);
            }
            if ((this.options & 0x2) != 0) {
                field.setFieldFlags(2);
            }
        }
        if (this.text != null) {
            field.setMKNormalCaption(this.text);
        }
        if (this.rotation != 0) {
            field.setMKRotation(this.rotation);
        }
        field.setBorderStyle(new PdfBorderDictionary(this.borderWidth, this.borderStyle, new PdfDashPattern(3.0F)));
        PdfAppearance tpa = getAppearance();
        field.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, tpa);
        PdfAppearance da = (PdfAppearance) tpa.getDuplicate();
        da.setFontAndSize(getRealFont(), this.fontSize);
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
                break;
            case 2:
                break;
            case 3:
                field.setFlags(36);
                break;
            default:
                field.setFlags(4);
                break;
        }
        if (this.tp != null) {
            field.setMKNormalIcon(this.tp);
        }
        field.setMKTextPosition(this.layout - 1);
        PdfName scale = PdfName.A;
        if (this.scaleIcon == 3) {
            scale = PdfName.B;
        } else if (this.scaleIcon == 4) {
            scale = PdfName.S;
        } else if (this.scaleIcon == 2) {
            scale = PdfName.N;
        }
        field.setMKIconFit(scale, this.proportionalIcon ? PdfName.P : PdfName.A, this.iconHorizontalAdjustment, this.iconVerticalAdjustment, this.iconFitToBounds);

        return field;
    }

    public boolean isIconFitToBounds() {
        return this.iconFitToBounds;
    }

    public void setIconFitToBounds(boolean iconFitToBounds) {
        this.iconFitToBounds = iconFitToBounds;
    }

    public PRIndirectReference getIconReference() {
        return this.iconReference;
    }

    public void setIconReference(PRIndirectReference iconReference) {
        this.iconReference = iconReference;
    }
}
