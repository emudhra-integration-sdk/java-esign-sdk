package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.Chunk;
import esign.text.DocumentException;
import esign.text.Font;
import esign.text.Phrase;
import esign.text.Rectangle;
import java.io.IOException;
import java.util.ArrayList;

public class TextField
        extends BaseField {

    private String defaultText;
    private String[] choices;
    private String[] choiceExports;
    private ArrayList<Integer> choiceSelections = new ArrayList<Integer>();

    private int topFirst;

    private int visibleTopChoice = -1;

    private float extraMarginLeft;

    private float extraMarginTop;

    private ArrayList<BaseFont> substitutionFonts;

    private BaseFont extensionFont;

    public TextField(PdfWriter writer, Rectangle box, String fieldName) {
        super(writer, box, fieldName);
    }

    private static boolean checkRTL(String text) {
        if (text == null || text.length() == 0) {
            return false;
        }
        char[] cc = text.toCharArray();
        for (int k = 0; k < cc.length; k++) {
            int c = cc[k];
            if (c >= 1424 && c < 1920) {
                return true;
            }
        }
        return false;
    }

    private static void changeFontSize(Phrase p, float size) {
        for (int k = 0; k < p.size(); k++) {
            ((Chunk) p.get(k)).getFont().setSize(size);
        }
    }

    private Phrase composePhrase(String text, BaseFont ufont, BaseColor color, float fontSize) {
        Phrase phrase = null;
        if (this.extensionFont == null && (this.substitutionFonts == null || this.substitutionFonts.isEmpty())) {
            phrase = new Phrase(new Chunk(text, new Font(ufont, fontSize, 0, color)));
        } else {
            FontSelector fs = new FontSelector();
            fs.addFont(new Font(ufont, fontSize, 0, color));
            if (this.extensionFont != null) {
                fs.addFont(new Font(this.extensionFont, fontSize, 0, color));
            }
            if (this.substitutionFonts != null) {
                for (int k = 0; k < this.substitutionFonts.size(); k++) {
                    fs.addFont(new Font(this.substitutionFonts.get(k), fontSize, 0, color));
                }
            }
            phrase = fs.process(text);
        }
        return phrase;
    }

    public static String removeCRLF(String text) {
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            char[] p = text.toCharArray();
            StringBuffer sb = new StringBuffer(p.length);
            for (int k = 0; k < p.length; k++) {
                char c = p[k];
                if (c == '\n') {
                    sb.append(' ');
                } else if (c == '\r') {
                    sb.append(' ');
                    if (k < p.length - 1 && p[k + 1] == '\n') {
                        k++;
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        return text;
    }

    public static String obfuscatePassword(String text) {
        char[] pchar = new char[text.length()];
        for (int i = 0; i < text.length(); i++) {
            pchar[i] = '*';
        }
        return new String(pchar);
    }

    public PdfAppearance getAppearance() throws IOException, DocumentException {
        String ptext;
        PdfAppearance app = getBorderAppearance();
        app.beginVariableText();
        if (this.text == null || this.text.length() == 0) {
            app.endVariableText();
            return app;
        }

        boolean borderExtra = (this.borderStyle == 2 || this.borderStyle == 3);
        float h = this.box.getHeight() - this.borderWidth * 2.0F - this.extraMarginTop;
        float bw2 = this.borderWidth;
        if (borderExtra) {
            h -= this.borderWidth * 2.0F;
            bw2 *= 2.0F;
        }
        float offsetX = Math.max(bw2, 1.0F);
        float offX = Math.min(bw2, offsetX);
        app.saveState();
        app.rectangle(offX, offX, this.box.getWidth() - 2.0F * offX, this.box.getHeight() - 2.0F * offX);
        app.clip();
        app.newPath();

        if ((this.options & 0x2000) != 0) {
            ptext = obfuscatePassword(this.text);
        } else if ((this.options & 0x1000) == 0) {
            ptext = removeCRLF(this.text);
        } else {
            ptext = this.text;
        }
        BaseFont ufont = getRealFont();
        BaseColor fcolor = (this.textColor == null) ? GrayColor.GRAYBLACK : this.textColor;
        int rtl = checkRTL(ptext) ? 2 : 1;
        float usize = this.fontSize;
        Phrase phrase = composePhrase(ptext, ufont, fcolor, usize);
        if ((this.options & 0x1000) != 0) {
            float width = this.box.getWidth() - 4.0F * offsetX - this.extraMarginLeft;
            float factor = ufont.getFontDescriptor(8, 1.0F) - ufont.getFontDescriptor(6, 1.0F);
            ColumnText ct = new ColumnText(null);
            if (usize == 0.0F) {
                usize = h / factor;
                if (usize > 4.0F) {
                    if (usize > 12.0F) {
                        usize = 12.0F;
                    }
                    float step = Math.max((usize - 4.0F) / 10.0F, 0.2F);
                    ct.setSimpleColumn(0.0F, -h, width, 0.0F);
                    ct.setAlignment(this.alignment);
                    ct.setRunDirection(rtl);
                    for (; usize > 4.0F; usize -= step) {
                        ct.setYLine(0.0F);
                        changeFontSize(phrase, usize);
                        ct.setText(phrase);
                        ct.setLeading(factor * usize);
                        int status = ct.go(true);
                        if ((status & 0x2) == 0) {
                            break;
                        }
                    }
                }
                if (usize < 4.0F) {
                    usize = 4.0F;
                }
            }
            changeFontSize(phrase, usize);
            ct.setCanvas(app);
            float leading = usize * factor;
            float offsetY = offsetX + h - ufont.getFontDescriptor(8, usize);
            ct.setSimpleColumn(this.extraMarginLeft + 2.0F * offsetX, -20000.0F, this.box.getWidth() - 2.0F * offsetX, offsetY + leading);
            ct.setLeading(leading);
            ct.setAlignment(this.alignment);
            ct.setRunDirection(rtl);
            ct.setText(phrase);
            ct.go();
        } else {

            if (usize == 0.0F) {
                float maxCalculatedSize = h / (ufont.getFontDescriptor(7, 1.0F) - ufont.getFontDescriptor(6, 1.0F));
                changeFontSize(phrase, 1.0F);
                float wd = ColumnText.getWidth(phrase, rtl, 0);
                if (wd == 0.0F) {
                    usize = maxCalculatedSize;
                } else {
                    usize = Math.min(maxCalculatedSize, (this.box.getWidth() - this.extraMarginLeft - 4.0F * offsetX) / wd);
                }
                if (usize < 4.0F) {
                    usize = 4.0F;
                }
            }
            changeFontSize(phrase, usize);
            float offsetY = offX + (this.box.getHeight() - 2.0F * offX - ufont.getFontDescriptor(1, usize)) / 2.0F;
            if (offsetY < offX) {
                offsetY = offX;
            }
            if (offsetY - offX < -ufont.getFontDescriptor(3, usize)) {
                float ny = -ufont.getFontDescriptor(3, usize) + offX;
                float dy = this.box.getHeight() - offX - ufont.getFontDescriptor(1, usize);
                offsetY = Math.min(ny, Math.max(offsetY, dy));
            }
            if ((this.options & 0x1000000) != 0 && this.maxCharacterLength > 0) {
                int textLen = Math.min(this.maxCharacterLength, ptext.length());
                int position = 0;
                if (this.alignment == 2) {
                    position = this.maxCharacterLength - textLen;
                } else if (this.alignment == 1) {
                    position = (this.maxCharacterLength - textLen) / 2;
                }
                float step = (this.box.getWidth() - this.extraMarginLeft) / this.maxCharacterLength;
                float start = step / 2.0F + position * step;
                if (this.textColor == null) {
                    app.setGrayFill(0.0F);
                } else {
                    app.setColorFill(this.textColor);
                }
                app.beginText();
                for (int k = 0; k < phrase.size(); k++) {
                    Chunk ck = (Chunk) phrase.get(k);
                    BaseFont bf = ck.getFont().getBaseFont();
                    app.setFontAndSize(bf, usize);
                    StringBuffer sb = ck.append("");
                    for (int j = 0; j < sb.length(); j++) {
                        String c = sb.substring(j, j + 1);
                        float wd = bf.getWidthPoint(c, usize);
                        app.setTextMatrix(this.extraMarginLeft + start - wd / 2.0F, offsetY - this.extraMarginTop);
                        app.showText(c);
                        start += step;
                    }
                }
                app.endText();
            } else {
                float x;

                switch (this.alignment) {
                    case 2:
                        x = this.extraMarginLeft + this.box.getWidth() - 2.0F * offsetX;
                        break;
                    case 1:
                        x = this.extraMarginLeft + this.box.getWidth() / 2.0F;
                        break;
                    default:
                        x = this.extraMarginLeft + 2.0F * offsetX;
                        break;
                }
                ColumnText.showTextAligned(app, this.alignment, phrase, x, offsetY - this.extraMarginTop, 0.0F, rtl, 0);
            }
        }
        app.restoreState();
        app.endVariableText();
        return app;
    }

    PdfAppearance getListAppearance() throws IOException, DocumentException {
        PdfAppearance app = getBorderAppearance();
        if (this.choices == null || this.choices.length == 0) {
            return app;
        }
        app.beginVariableText();

        int topChoice = getTopChoice();

        BaseFont ufont = getRealFont();
        float usize = this.fontSize;
        if (usize == 0.0F) {
            usize = 12.0F;
        }
        boolean borderExtra = (this.borderStyle == 2 || this.borderStyle == 3);
        float h = this.box.getHeight() - this.borderWidth * 2.0F;
        float offsetX = this.borderWidth;
        if (borderExtra) {
            h -= this.borderWidth * 2.0F;
            offsetX *= 2.0F;
        }

        float leading = ufont.getFontDescriptor(8, usize) - ufont.getFontDescriptor(6, usize);
        int maxFit = (int) (h / leading) + 1;
        int first = 0;
        int last = 0;
        first = topChoice;
        last = first + maxFit;
        if (last > this.choices.length) {
            last = this.choices.length;
        }
        this.topFirst = first;
        app.saveState();
        app.rectangle(offsetX, offsetX, this.box.getWidth() - 2.0F * offsetX, this.box.getHeight() - 2.0F * offsetX);
        app.clip();
        app.newPath();
        BaseColor fcolor = (this.textColor == null) ? GrayColor.GRAYBLACK : this.textColor;

        app.setColorFill(new BaseColor(10, 36, 106));
        for (int curVal = 0; curVal < this.choiceSelections.size(); curVal++) {
            int curChoice = ((Integer) this.choiceSelections.get(curVal)).intValue();

            if (curChoice >= first && curChoice <= last) {
                app.rectangle(offsetX, offsetX + h - (curChoice - first + 1) * leading, this.box.getWidth() - 2.0F * offsetX, leading);
                app.fill();
            }
        }
        float xp = offsetX * 2.0F;
        float yp = offsetX + h - ufont.getFontDescriptor(8, usize);
        for (int idx = first; idx < last; idx++, yp -= leading) {
            String ptext = this.choices[idx];
            int rtl = checkRTL(ptext) ? 2 : 1;
            ptext = removeCRLF(ptext);

            BaseColor textCol = this.choiceSelections.contains(Integer.valueOf(idx)) ? GrayColor.GRAYWHITE : fcolor;
            Phrase phrase = composePhrase(ptext, ufont, textCol, usize);
            ColumnText.showTextAligned(app, 0, phrase, xp, yp, 0.0F, rtl, 0);
        }
        app.restoreState();
        app.endVariableText();
        return app;
    }

    public PdfFormField getTextField() throws IOException, DocumentException {
        if (this.maxCharacterLength <= 0) {
            this.options &= 0xFEFFFFFF;
        }
        if ((this.options & 0x1000000) != 0) {
            this.options &= 0xFFFFEFFF;
        }
        PdfFormField field = PdfFormField.createTextField(this.writer, false, false, this.maxCharacterLength);
        field.setWidget(this.box, PdfAnnotation.HIGHLIGHT_INVERT);
        switch (this.alignment) {
            case 1:
                field.setQuadding(1);
                break;
            case 2:
                field.setQuadding(2);
                break;
        }
        if (this.rotation != 0) {
            field.setMKRotation(this.rotation);
        }
        if (this.fieldName != null) {
            field.setFieldName(this.fieldName);
            if (!"".equals(this.text)) {
                field.setValueAsString(this.text);
            }
            if (this.defaultText != null) {
                field.setDefaultValueAsString(this.defaultText);
            }
            if ((this.options & 0x1) != 0) {
                field.setFieldFlags(1);
            }
            if ((this.options & 0x2) != 0) {
                field.setFieldFlags(2);
            }
            if ((this.options & 0x1000) != 0) {
                field.setFieldFlags(4096);
            }
            if ((this.options & 0x800000) != 0) {
                field.setFieldFlags(8388608);
            }
            if ((this.options & 0x2000) != 0) {
                field.setFieldFlags(8192);
            }
            if ((this.options & 0x100000) != 0) {
                field.setFieldFlags(1048576);
            }
            if ((this.options & 0x400000) != 0) {
                field.setFieldFlags(4194304);
            }
            if ((this.options & 0x1000000) != 0) {
                field.setFieldFlags(16777216);
            }
        }
        field.setBorderStyle(new PdfBorderDictionary(this.borderWidth, this.borderStyle, new PdfDashPattern(3.0F)));
        PdfAppearance tp = getAppearance();
        field.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, tp);
        PdfAppearance da = (PdfAppearance) tp.getDuplicate();
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

            case 2:
                return field;
            case 3:
                field.setFlags(36);
        }
        field.setFlags(4);
        return field;
    }

    public PdfFormField getComboField() throws IOException, DocumentException {
        return getChoiceField(false);
    }

    public PdfFormField getListField() throws IOException, DocumentException {
        return getChoiceField(true);
    }

    private int getTopChoice() {
        if (this.choiceSelections == null || this.choiceSelections.size() == 0) {
            return 0;
        }

        Integer firstValue = this.choiceSelections.get(0);

        if (firstValue == null) {
            return 0;
        }

        int topChoice = 0;
        if (this.choices != null) {
            if (this.visibleTopChoice != -1) {
                return this.visibleTopChoice;
            }

            topChoice = firstValue.intValue();
            topChoice = Math.min(topChoice, this.choices.length);
            topChoice = Math.max(0, topChoice);
        }
        return topChoice;
    }

    protected PdfFormField getChoiceField(boolean isList) throws IOException, DocumentException {
        PdfAppearance tp;
        this.options &= 0xFEFFEFFF;
        String[] uchoices = this.choices;
        if (uchoices == null) {
            uchoices = new String[0];
        }
        int topChoice = getTopChoice();

        if (uchoices.length > 0 && topChoice >= 0) {
            this.text = uchoices[topChoice];
        }
        if (this.text == null) {
            this.text = "";
        }
        PdfFormField field = null;
        String[][] mix = (String[][]) null;

        if (this.choiceExports == null) {
            if (isList) {
                field = PdfFormField.createList(this.writer, uchoices, topChoice);
            } else {
                field = PdfFormField.createCombo(this.writer, ((this.options & 0x40000) != 0), uchoices, topChoice);
            }
        } else {
            mix = new String[uchoices.length][2];
            for (int k = 0; k < mix.length; k++) {
                mix[k][1] = uchoices[k];
                mix[k][0] = uchoices[k];
            }
            int top = Math.min(uchoices.length, this.choiceExports.length);
            for (int i = 0; i < top; i++) {
                if (this.choiceExports[i] != null) {
                    mix[i][0] = this.choiceExports[i];
                }
            }
            if (isList) {
                field = PdfFormField.createList(this.writer, mix, topChoice);
            } else {
                field = PdfFormField.createCombo(this.writer, ((this.options & 0x40000) != 0), mix, topChoice);
            }
        }
        field.setWidget(this.box, PdfAnnotation.HIGHLIGHT_INVERT);
        if (this.rotation != 0) {
            field.setMKRotation(this.rotation);
        }
        if (this.fieldName != null) {
            field.setFieldName(this.fieldName);
            if (uchoices.length > 0) {
                if (mix != null) {
                    if (this.choiceSelections.size() < 2) {
                        field.setValueAsString(mix[topChoice][0]);
                        field.setDefaultValueAsString(mix[topChoice][0]);
                    } else {
                        writeMultipleValues(field, mix);
                    }

                } else if (this.choiceSelections.size() < 2) {
                    field.setValueAsString(this.text);
                    field.setDefaultValueAsString(this.text);
                } else {
                    writeMultipleValues(field, (String[][]) null);
                }
            }

            if ((this.options & 0x1) != 0) {
                field.setFieldFlags(1);
            }
            if ((this.options & 0x2) != 0) {
                field.setFieldFlags(2);
            }
            if ((this.options & 0x400000) != 0) {
                field.setFieldFlags(4194304);
            }
            if ((this.options & 0x200000) != 0) {
                field.setFieldFlags(2097152);
            }
        }
        field.setBorderStyle(new PdfBorderDictionary(this.borderWidth, this.borderStyle, new PdfDashPattern(3.0F)));

        if (isList) {
            tp = getListAppearance();
            if (this.topFirst > 0) {
                field.put(PdfName.TI, new PdfNumber(this.topFirst));
            }
        } else {
            tp = getAppearance();
        }
        field.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, tp);
        PdfAppearance da = (PdfAppearance) tp.getDuplicate();
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

            case 2:
                return field;
            case 3:
                field.setFlags(36);
        }
        field.setFlags(4);
        return field;
    }

    private void writeMultipleValues(PdfFormField field, String[][] mix) {
        PdfArray indexes = new PdfArray();
        PdfArray values = new PdfArray();
        for (int i = 0; i < this.choiceSelections.size(); i++) {
            int idx = ((Integer) this.choiceSelections.get(i)).intValue();
            indexes.add(new PdfNumber(idx));

            if (mix != null) {
                values.add(new PdfString(mix[idx][0]));
            } else if (this.choices != null) {
                values.add(new PdfString(this.choices[idx]));
            }
        }
        field.put(PdfName.V, values);
        field.put(PdfName.I, indexes);
    }

    public String getDefaultText() {
        return this.defaultText;
    }

    public void setDefaultText(String defaultText) {
        this.defaultText = defaultText;
    }

    public String[] getChoices() {
        return this.choices;
    }

    public void setChoices(String[] choices) {
        this.choices = choices;
    }

    public String[] getChoiceExports() {
        return this.choiceExports;
    }

    public void setChoiceExports(String[] choiceExports) {
        this.choiceExports = choiceExports;
    }

    public int getChoiceSelection() {
        return getTopChoice();
    }

    public ArrayList<Integer> getChoiceSelections() {
        return this.choiceSelections;
    }

    public void setVisibleTopChoice(int visibleTopChoice) {
        if (visibleTopChoice < 0) {
            return;
        }

        if (this.choices != null
                && visibleTopChoice < this.choices.length) {
            this.visibleTopChoice = visibleTopChoice;
        }
    }

    public int getVisibleTopChoice() {
        return this.visibleTopChoice;
    }

    public void setChoiceSelection(int choiceSelection) {
        this.choiceSelections = new ArrayList<Integer>();
        this.choiceSelections.add(Integer.valueOf(choiceSelection));
    }

    public void addChoiceSelection(int selection) {
        if ((this.options & 0x200000) != 0) {
            this.choiceSelections.add(Integer.valueOf(selection));
        }
    }

    public void setChoiceSelections(ArrayList<Integer> selections) {
        if (selections != null) {
            this.choiceSelections = new ArrayList<Integer>(selections);
            if (this.choiceSelections.size() > 1 && (this.options & 0x200000) == 0) {
                while (this.choiceSelections.size() > 1) {
                    this.choiceSelections.remove(1);
                }
            }
        } else {

            this.choiceSelections.clear();
        }
    }

    int getTopFirst() {
        return this.topFirst;
    }

    public void setExtraMargin(float extraMarginLeft, float extraMarginTop) {
        this.extraMarginLeft = extraMarginLeft;
        this.extraMarginTop = extraMarginTop;
    }

    public ArrayList<BaseFont> getSubstitutionFonts() {
        return this.substitutionFonts;
    }

    public void setSubstitutionFonts(ArrayList<BaseFont> substitutionFonts) {
        this.substitutionFonts = substitutionFonts;
    }

    public BaseFont getExtensionFont() {
        return this.extensionFont;
    }

    public void setExtensionFont(BaseFont extensionFont) {
        this.extensionFont = extensionFont;
    }
}
