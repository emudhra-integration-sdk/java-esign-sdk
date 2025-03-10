package esign.text.pdf.parser;

import esign.text.BaseColor;
import esign.text.pdf.DocumentFont;
import esign.text.pdf.PdfString;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TextRenderInfo {

    private final PdfString string;
    private String text = null;
    private final Matrix textToUserSpaceTransformMatrix;
    private final GraphicsState gs;
    private Float unscaledWidth = null;
    private double[] fontMatrix = null;

    private final Collection<MarkedContentInfo> markedContentInfos;

    TextRenderInfo(PdfString string, GraphicsState gs, Matrix textMatrix, Collection<MarkedContentInfo> markedContentInfo) {
        this.string = string;
        this.textToUserSpaceTransformMatrix = textMatrix.multiply(gs.ctm);
        this.gs = gs;
        this.markedContentInfos = new ArrayList<MarkedContentInfo>(markedContentInfo);
        this.fontMatrix = gs.font.getFontMatrix();
    }

    private TextRenderInfo(TextRenderInfo parent, PdfString string, float horizontalOffset) {
        this.string = string;
        this.textToUserSpaceTransformMatrix = (new Matrix(horizontalOffset, 0.0F)).multiply(parent.textToUserSpaceTransformMatrix);
        this.gs = parent.gs;
        this.markedContentInfos = parent.markedContentInfos;
        this.fontMatrix = this.gs.font.getFontMatrix();
    }

    public String getText() {
        if (this.text == null) {
            this.text = decode(this.string);
        }
        return this.text;
    }

    public PdfString getPdfString() {
        return this.string;
    }

    public boolean hasMcid(int mcid) {
        return hasMcid(mcid, false);
    }

    public boolean hasMcid(int mcid, boolean checkTheTopmostLevelOnly) {
        if (checkTheTopmostLevelOnly) {
            if (this.markedContentInfos instanceof ArrayList) {
                Integer infoMcid = getMcid();
                return (infoMcid != null) ? ((infoMcid.intValue() == mcid)) : false;
            }
        } else {
            for (MarkedContentInfo info : this.markedContentInfos) {
                if (info.hasMcid()
                        && info.getMcid() == mcid) {
                    return true;
                }
            }
        }
        return false;
    }

    public Integer getMcid() {
        if (this.markedContentInfos instanceof ArrayList) {
            ArrayList<MarkedContentInfo> mci = (ArrayList<MarkedContentInfo>) this.markedContentInfos;
            MarkedContentInfo info = (mci.size() > 0) ? mci.get(mci.size() - 1) : null;
            return (info != null && info.hasMcid()) ? Integer.valueOf(info.getMcid()) : null;
        }
        return null;
    }

    float getUnscaledWidth() {
        if (this.unscaledWidth == null) {
            this.unscaledWidth = Float.valueOf(getPdfStringWidth(this.string, false));
        }
        return this.unscaledWidth.floatValue();
    }

    public LineSegment getBaseline() {
        return getUnscaledBaselineWithOffset(0.0F + this.gs.rise).transformBy(this.textToUserSpaceTransformMatrix);
    }

    public LineSegment getUnscaledBaseline() {
        return getUnscaledBaselineWithOffset(0.0F + this.gs.rise);
    }

    public LineSegment getAscentLine() {
        float ascent = this.gs.getFont().getFontDescriptor(1, this.gs.getFontSize());
        return getUnscaledBaselineWithOffset(ascent + this.gs.rise).transformBy(this.textToUserSpaceTransformMatrix);
    }

    public LineSegment getDescentLine() {
        float descent = this.gs.getFont().getFontDescriptor(3, this.gs.getFontSize());
        return getUnscaledBaselineWithOffset(descent + this.gs.rise).transformBy(this.textToUserSpaceTransformMatrix);
    }

    private LineSegment getUnscaledBaselineWithOffset(float yOffset) {
        String unicodeStr = this.string.toUnicodeString();

        float correctedUnscaledWidth = getUnscaledWidth() - (this.gs.characterSpacing + ((unicodeStr.length() > 0 && unicodeStr.charAt(unicodeStr.length() - 1) == ' ') ? this.gs.wordSpacing : 0.0F)) * this.gs.horizontalScaling;

        return new LineSegment(new Vector(0.0F, yOffset, 1.0F), new Vector(correctedUnscaledWidth, yOffset, 1.0F));
    }

    public DocumentFont getFont() {
        return (DocumentFont) this.gs.getFont();
    }

    public float getRise() {
        if (this.gs.rise == 0.0F) {
            return 0.0F;
        }

        return convertHeightFromTextSpaceToUserSpace(this.gs.rise);
    }

    private float convertWidthFromTextSpaceToUserSpace(float width) {
        LineSegment textSpace = new LineSegment(new Vector(0.0F, 0.0F, 1.0F), new Vector(width, 0.0F, 1.0F));
        LineSegment userSpace = textSpace.transformBy(this.textToUserSpaceTransformMatrix);
        return userSpace.getLength();
    }

    private float convertHeightFromTextSpaceToUserSpace(float height) {
        LineSegment textSpace = new LineSegment(new Vector(0.0F, 0.0F, 1.0F), new Vector(0.0F, height, 1.0F));
        LineSegment userSpace = textSpace.transformBy(this.textToUserSpaceTransformMatrix);
        return userSpace.getLength();
    }

    public float getSingleSpaceWidth() {
        return convertWidthFromTextSpaceToUserSpace(getUnscaledFontSpaceWidth());
    }

    public int getTextRenderMode() {
        return this.gs.renderMode;
    }

    public BaseColor getFillColor() {
        return this.gs.fillColor;
    }

    public BaseColor getStrokeColor() {
        return this.gs.strokeColor;
    }

    private float getUnscaledFontSpaceWidth() {
        char charToUse = ' ';
        if (this.gs.font.getWidth(charToUse) == 0) {
            charToUse = ' ';
        }
        return getStringWidth(String.valueOf(charToUse));
    }

    private float getStringWidth(String string) {
        float totalWidth = 0.0F;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            float w = this.gs.font.getWidth(c) / 1000.0F;
            float wordSpacing = (c == ' ') ? this.gs.wordSpacing : 0.0F;
            totalWidth += (w * this.gs.fontSize + this.gs.characterSpacing + wordSpacing) * this.gs.horizontalScaling;
        }
        return totalWidth;
    }

    private float getPdfStringWidth(PdfString string, boolean singleCharString) {
        if (singleCharString) {
            float[] widthAndWordSpacing = getWidthAndWordSpacing(string, singleCharString);
            return (widthAndWordSpacing[0] * this.gs.fontSize + this.gs.characterSpacing + widthAndWordSpacing[1]) * this.gs.horizontalScaling;
        }
        float totalWidth = 0.0F;
        for (PdfString str : splitString(string)) {
            totalWidth += getPdfStringWidth(str, true);
        }
        return totalWidth;
    }

    public List<TextRenderInfo> getCharacterRenderInfos() {
        List<TextRenderInfo> rslt = new ArrayList<TextRenderInfo>(this.string.length());
        PdfString[] strings = splitString(this.string);
        float totalWidth = 0.0F;
        for (int i = 0; i < strings.length; i++) {
            float[] widthAndWordSpacing = getWidthAndWordSpacing(strings[i], true);
            TextRenderInfo subInfo = new TextRenderInfo(this, strings[i], totalWidth);
            rslt.add(subInfo);
            totalWidth += (widthAndWordSpacing[0] * this.gs.fontSize + this.gs.characterSpacing + widthAndWordSpacing[1]) * this.gs.horizontalScaling;
        }
        for (TextRenderInfo tri : rslt) {
            tri.getUnscaledWidth();
        }
        return rslt;
    }

    private float[] getWidthAndWordSpacing(PdfString string, boolean singleCharString) {
        if (!singleCharString) {
            throw new UnsupportedOperationException();
        }
        float[] result = new float[2];
        String decoded = decode(string);
        result[0] = (float) (this.gs.font.getWidth(getCharCode(decoded)) * this.fontMatrix[0]);
        result[1] = decoded.equals(" ") ? this.gs.wordSpacing : 0.0F;
        return result;
    }

    private String decode(PdfString in) {
        byte[] bytes = in.getBytes();
        return this.gs.font.decode(bytes, 0, bytes.length);
    }

    private int getCharCode(String string) {
        try {
            byte[] b = string.getBytes("UTF-16BE");
            int value = 0;
            for (int i = 0; i < b.length - 1; i++) {
                value += b[i] & 0xFF;
                value <<= 8;
            }
            if (b.length > 0) {
                value += b[b.length - 1] & 0xFF;
            }
            return value;
        } catch (UnsupportedEncodingException unsupportedEncodingException) {

            return 0;
        }
    }

    private PdfString[] splitString(PdfString string) {
        List<PdfString> strings = new ArrayList<PdfString>();
        String stringValue = string.toString();
        for (int i = 0; i < stringValue.length(); i++) {
            PdfString newString = new PdfString(stringValue.substring(i, i + 1), string.getEncoding());
            String text = decode(newString);
            if (text.length() == 0 && i < stringValue.length() - 1) {
                newString = new PdfString(stringValue.substring(i, i + 2), string.getEncoding());
                i++;
            }
            strings.add(newString);
        }
        return strings.<PdfString>toArray(new PdfString[strings.size()]);
    }
}
