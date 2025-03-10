package esign.text.pdf;

import esign.text.Chunk;
import esign.text.Image;
import esign.text.ListItem;
import esign.text.TabStop;
import java.util.ArrayList;
import java.util.Iterator;

public class PdfLine {

    protected ArrayList<PdfChunk> line;
    protected float left;
    protected float width;
    protected int alignment;
    protected float height;
    protected boolean newlineSplit = false;
    protected float originalWidth;
    protected boolean isRTL = false;
    protected ListItem listItem = null;

    protected TabStop tabStop = null;

    protected float tabStopAnchorPosition = Float.NaN;

    protected float tabPosition = Float.NaN;

    PdfLine(float left, float right, int alignment, float height) {
        this.left = left;
        this.width = right - left;
        this.originalWidth = this.width;
        this.alignment = alignment;
        this.height = height;
        this.line = new ArrayList<PdfChunk>();
    }

    PdfLine(float left, float originalWidth, float remainingWidth, int alignment, boolean newlineSplit, ArrayList<PdfChunk> line, boolean isRTL) {
        this.left = left;
        this.originalWidth = originalWidth;
        this.width = remainingWidth;
        this.alignment = alignment;
        this.line = line;
        this.newlineSplit = newlineSplit;
        this.isRTL = isRTL;
    }

    PdfChunk add(PdfChunk chunk, float currentLeading) {
        if (chunk != null && !chunk.toString().equals("")) {
            if (!chunk.toString().equals(" ") && (this.height < currentLeading || this.line.isEmpty())) {
                this.height = currentLeading;
            }
        }
        return add(chunk);
    }

    PdfChunk add(PdfChunk chunk) {
        if (chunk == null || chunk.toString().equals("")) {
            return null;
        }

        PdfChunk overflow = chunk.split(this.width);
        this.newlineSplit = (chunk.isNewlineSplit() || overflow == null);
        if (chunk.isTab()) {
            Object[] tab = (Object[]) chunk.getAttribute("TAB");
            if (chunk.isAttribute("TABSETTINGS")) {
                boolean isWhiteSpace = ((Boolean) tab[1]).booleanValue();
                if (!isWhiteSpace || !this.line.isEmpty()) {
                    flush();
                    this.tabStopAnchorPosition = Float.NaN;
                    this.tabStop = PdfChunk.getTabStop(chunk, this.originalWidth - this.width);
                    if (this.tabStop.getPosition() > this.originalWidth) {
                        if (isWhiteSpace) {
                            overflow = null;
                        } else if (Math.abs(this.originalWidth - this.width) < 0.001D) {
                            addToLine(chunk);
                            overflow = null;
                        } else {
                            overflow = chunk;
                        }
                        this.width = 0.0F;
                    } else {
                        chunk.setTabStop(this.tabStop);
                        if (!this.isRTL && this.tabStop.getAlignment() == TabStop.Alignment.LEFT) {
                            this.width = this.originalWidth - this.tabStop.getPosition();
                            this.tabStop = null;
                            this.tabPosition = Float.NaN;
                        } else {
                            this.tabPosition = this.originalWidth - this.width;
                        }
                        addToLine(chunk);
                    }
                } else {
                    return null;
                }
            } else {
                Float tabStopPosition = Float.valueOf(((Float) tab[1]).floatValue());
                boolean newline = ((Boolean) tab[2]).booleanValue();
                if (newline && tabStopPosition.floatValue() < this.originalWidth - this.width) {
                    return chunk;
                }
                chunk.adjustLeft(this.left);
                this.width = this.originalWidth - tabStopPosition.floatValue();
                addToLine(chunk);
            }

        } else if (chunk.length() > 0 || chunk.isImage()) {
            if (overflow != null) {
                chunk.trimLastSpace();
            }
            this.width -= chunk.width();
            addToLine(chunk);
        } else {

            if (this.line.size() < 1) {
                chunk = overflow;
                overflow = chunk.truncate(this.width);
                this.width -= chunk.width();
                if (chunk.length() > 0) {
                    addToLine(chunk);
                    return overflow;
                }

                if (overflow != null) {
                    addToLine(overflow);
                }
                return null;
            }

            this.width += ((PdfChunk) this.line.get(this.line.size() - 1)).trimLastSpace();
        }
        return overflow;
    }

    private void addToLine(PdfChunk chunk) {
        if (chunk.changeLeading) {
            float f;
            if (chunk.isImage()) {
                Image img = chunk.getImage();

                f = chunk.getImageHeight() + chunk.getImageOffsetY() + img.getBorderWidthTop() + img.getSpacingBefore();
            } else {

                f = chunk.getLeading();
            }
            if (f > this.height) {
                this.height = f;
            }
        }
        if (this.tabStop != null && this.tabStop.getAlignment() == TabStop.Alignment.ANCHOR && Float.isNaN(this.tabStopAnchorPosition)) {
            String value = chunk.toString();
            int anchorIndex = value.indexOf(this.tabStop.getAnchorChar());
            if (anchorIndex != -1) {
                float subWidth = chunk.width(value.substring(anchorIndex, value.length()));
                this.tabStopAnchorPosition = this.originalWidth - this.width - subWidth;
            }
        }
        this.line.add(chunk);
    }

    public int size() {
        return this.line.size();
    }

    public Iterator<PdfChunk> iterator() {
        return this.line.iterator();
    }

    float height() {
        return this.height;
    }

    float indentLeft() {
        if (this.isRTL) {
            switch (this.alignment) {
                case 1:
                    return this.left + this.width / 2.0F;
                case 2:
                    return this.left;
                case 3:
                    return this.left + (hasToBeJustified() ? 0.0F : this.width);
            }

            return this.left + this.width;
        }
        if (getSeparatorCount() <= 0) {
            switch (this.alignment) {
                case 2:
                    return this.left + this.width;
                case 1:
                    return this.left + this.width / 2.0F;
            }
        }
        return this.left;
    }

    public boolean hasToBeJustified() {
        return (((this.alignment == 3 && !this.newlineSplit) || this.alignment == 8) && this.width != 0.0F);
    }

    public void resetAlignment() {
        if (this.alignment == 3) {
            this.alignment = 0;
        }
    }

    void setExtraIndent(float extra) {
        this.left += extra;
        this.width -= extra;
        this.originalWidth -= extra;
    }

    float widthLeft() {
        return this.width;
    }

    int numberOfSpaces() {
        int numberOfSpaces = 0;
        for (PdfChunk pdfChunk : this.line) {
            String tmp = pdfChunk.toString();
            int length = tmp.length();
            for (int i = 0; i < length; i++) {
                if (tmp.charAt(i) == ' ') {
                    numberOfSpaces++;
                }
            }
        }
        return numberOfSpaces;
    }

    public void setListItem(ListItem listItem) {
        this.listItem = listItem;
    }

    public Chunk listSymbol() {
        return (this.listItem != null) ? this.listItem.getListSymbol() : null;
    }

    public float listIndent() {
        return (this.listItem != null) ? this.listItem.getIndentationLeft() : 0.0F;
    }

    public ListItem listItem() {
        return this.listItem;
    }

    public String toString() {
        StringBuffer tmp = new StringBuffer();
        for (PdfChunk pdfChunk : this.line) {
            tmp.append(pdfChunk.toString());
        }
        return tmp.toString();
    }

    public int getLineLengthUtf32() {
        int total = 0;
        for (PdfChunk element : this.line) {
            total += ((PdfChunk) element).lengthUtf32();
        }
        return total;
    }

    public boolean isNewlineSplit() {
        return (this.newlineSplit && this.alignment != 8);
    }

    public int getLastStrokeChunk() {
        int lastIdx = this.line.size() - 1;
        for (; lastIdx >= 0; lastIdx--) {
            PdfChunk chunk = this.line.get(lastIdx);
            if (chunk.isStroked()) {
                break;
            }
        }
        return lastIdx;
    }

    public PdfChunk getChunk(int idx) {
        if (idx < 0 || idx >= this.line.size()) {
            return null;
        }
        return this.line.get(idx);
    }

    public float getOriginalWidth() {
        return this.originalWidth;
    }

    float[] getMaxSize(float fixedLeading, float multipliedLeading) {
        float normal_leading = 0.0F;
        float image_leading = -10000.0F;

        for (int k = 0; k < this.line.size(); k++) {
            PdfChunk chunk = this.line.get(k);
            if (chunk.isImage()) {
                Image img = chunk.getImage();
                if (chunk.changeLeading()) {
                    float height = chunk.getImageHeight() + chunk.getImageOffsetY() + img.getSpacingBefore();
                    image_leading = Math.max(height, image_leading);
                }

            } else if (chunk.changeLeading()) {
                normal_leading = Math.max(chunk.getLeading(), normal_leading);
            } else {
                normal_leading = Math.max(fixedLeading + multipliedLeading * chunk.font().size(), normal_leading);
            }
        }
        return new float[]{(normal_leading > 0.0F) ? normal_leading : fixedLeading, image_leading};
    }

    boolean isRTL() {
        return this.isRTL;
    }

    int getSeparatorCount() {
        int s = 0;

        for (PdfChunk element : this.line) {
            PdfChunk ck = element;
            if (ck.isTab()) {
                if (ck.isAttribute("TABSETTINGS")) {
                    continue;
                }
                return -1;
            }
            if (ck.isHorizontalSeparator()) {
                s++;
            }
        }
        return s;
    }

    public float getWidthCorrected(float charSpacing, float wordSpacing) {
        float total = 0.0F;
        for (int k = 0; k < this.line.size(); k++) {
            PdfChunk ck = this.line.get(k);
            total += ck.getWidthCorrected(charSpacing, wordSpacing);
        }
        return total;
    }

    public float getAscender() {
        float ascender = 0.0F;
        for (int k = 0; k < this.line.size(); k++) {
            PdfChunk ck = this.line.get(k);
            if (ck.isImage()) {
                ascender = Math.max(ascender, ck.getImageHeight() + ck.getImageOffsetY());
            } else {
                PdfFont font = ck.font();
                float textRise = ck.getTextRise();
                ascender = Math.max(ascender, ((textRise > 0.0F) ? textRise : 0.0F) + font.getFont().getFontDescriptor(1, font.size()));
            }
        }
        return ascender;
    }

    public float getDescender() {
        float descender = 0.0F;
        for (int k = 0; k < this.line.size(); k++) {
            PdfChunk ck = this.line.get(k);
            if (ck.isImage()) {
                descender = Math.min(descender, ck.getImageOffsetY());
            } else {
                PdfFont font = ck.font();
                float textRise = ck.getTextRise();
                descender = Math.min(descender, ((textRise < 0.0F) ? textRise : 0.0F) + font.getFont().getFontDescriptor(3, font.size()));
            }
        }
        return descender;
    }

    public void flush() {
        if (this.tabStop != null) {
            float textWidth = this.originalWidth - this.width - this.tabPosition;
            float tabStopPosition = this.tabStop.getPosition(this.tabPosition, this.originalWidth - this.width, this.tabStopAnchorPosition);
            this.width = this.originalWidth - tabStopPosition - textWidth;
            if (this.width < 0.0F) {
                tabStopPosition += this.width;
            }
            if (!this.isRTL) {
                this.tabStop.setPosition(tabStopPosition);
            } else {
                this.tabStop.setPosition(this.originalWidth - this.width - this.tabPosition);
            }
            this.tabStop = null;
            this.tabPosition = Float.NaN;
        }
    }
}
