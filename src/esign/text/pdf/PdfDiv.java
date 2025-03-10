package esign.text.pdf;

import esign.text.awt.geom.AffineTransform;
import esign.text.AccessibleElementId;
import esign.text.BaseColor;
import esign.text.Chunk;
import esign.text.DocumentException;
import esign.text.Element;
import esign.text.ElementListener;
import esign.text.Image;
import esign.text.Rectangle;
import esign.text.api.Spaceable;
import esign.text.pdf.interfaces.IAccessibleElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PdfDiv
        implements Element, Spaceable, IAccessibleElement {

    private ArrayList<Element> content;

    public enum FloatType {
        NONE, LEFT, RIGHT;
    }

    public enum PositionType {
        STATIC, ABSOLUTE, FIXED, RELATIVE;
    }

    public enum DisplayType {
        NONE, BLOCK, INLINE, INLINE_BLOCK, INLINE_TABLE, LIST_ITEM, RUN_IN, TABLE, TABLE_CAPTION, TABLE_CELL, TABLE_COLUMN_GROUP, TABLE_COLUMN, TABLE_FOOTER_GROUP,
        TABLE_HEADER_GROUP, TABLE_ROW, TABLE_ROW_GROUP;
    }

    public enum BorderTopStyle {
        DOTTED, DASHED, SOLID, DOUBLE, GROOVE, RIDGE, INSET, OUTSET;
    }

    private Float left = null;

    private Float top = null;

    private Float right = null;

    private Float bottom = null;

    private Float width = null;

    private Float height = null;

    private Float percentageHeight = null;

    private Float percentageWidth = null;

    private float contentWidth = 0.0F;

    private float contentHeight = 0.0F;

    private int textAlignment = -1;

    private float paddingLeft = 0.0F;

    private float paddingRight = 0.0F;

    private float paddingTop = 0.0F;

    private float paddingBottom = 0.0F;

    private FloatType floatType = FloatType.NONE;

    private PositionType position = PositionType.STATIC;

    private DisplayType display;

    private FloatLayout floatLayout = null;

    private BorderTopStyle borderTopStyle;

    private float yLine;

    protected int runDirection = 1;

    private boolean keepTogether;

    protected PdfName role = PdfName.DIV;
    protected HashMap<PdfName, PdfObject> accessibleAttributes = null;
    protected AccessibleElementId id = new AccessibleElementId();

    public float getContentWidth() {
        return this.contentWidth;
    }

    public void setContentWidth(float contentWidth) {
        this.contentWidth = contentWidth;
    }

    public float getContentHeight() {
        return this.contentHeight;
    }

    public void setContentHeight(float contentHeight) {
        this.contentHeight = contentHeight;
    }

    public float getActualHeight() {
        return (this.height != null && this.height.floatValue() >= this.contentHeight) ? this.height.floatValue() : this.contentHeight;
    }

    public float getActualWidth() {
        return (this.width != null && this.width.floatValue() >= this.contentWidth) ? this.width.floatValue() : this.contentWidth;
    }

    public Float getPercentageHeight() {
        return this.percentageHeight;
    }

    public void setPercentageHeight(Float percentageHeight) {
        this.percentageHeight = percentageHeight;
    }

    public Float getPercentageWidth() {
        return this.percentageWidth;
    }

    public void setPercentageWidth(Float percentageWidth) {
        this.percentageWidth = percentageWidth;
    }

    public DisplayType getDisplay() {
        return this.display;
    }

    public void setDisplay(DisplayType display) {
        this.display = display;
    }

    public BaseColor getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(BaseColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setBackgroundImage(Image image) {
        this.backgroundImage = image;
    }

    public void setBackgroundImage(Image image, float width, float height) {
        this.backgroundImage = image;
        this.backgroundImageWidth = Float.valueOf(width);
        this.backgroundImageHeight = Float.valueOf(height);
    }

    public float getYLine() {
        return this.yLine;
    }

    public int getRunDirection() {
        return this.runDirection;
    }

    public void setRunDirection(int runDirection) {
        this.runDirection = runDirection;
    }

    public boolean getKeepTogether() {
        return this.keepTogether;
    }

    public void setKeepTogether(boolean keepTogether) {
        this.keepTogether = keepTogether;
    }

    private BaseColor backgroundColor = null;

    private Image backgroundImage;

    private Float backgroundImageWidth;

    private Float backgroundImageHeight;

    protected float spacingBefore;

    protected float spacingAfter;

    public PdfDiv() {
        this.content = new ArrayList<Element>();
        this.keepTogether = false;
    }

    public List<Chunk> getChunks() {
        return new ArrayList<Chunk>();
    }

    public int type() {
        return 37;
    }

    public boolean isContent() {
        return true;
    }

    public boolean isNestable() {
        return true;
    }

    public boolean process(ElementListener listener) {
        try {
            return listener.add(this);
        } catch (DocumentException de) {
            return false;
        }
    }

    public void setSpacingBefore(float spacing) {
        this.spacingBefore = spacing;
    }

    public void setSpacingAfter(float spacing) {
        this.spacingAfter = spacing;
    }

    public float getSpacingBefore() {
        return this.spacingBefore;
    }

    public float getSpacingAfter() {
        return this.spacingAfter;
    }

    public int getTextAlignment() {
        return this.textAlignment;
    }

    public void setTextAlignment(int textAlignment) {
        this.textAlignment = textAlignment;
    }

    public void addElement(Element element) {
        this.content.add(element);
    }

    public Float getLeft() {
        return this.left;
    }

    public void setLeft(Float left) {
        this.left = left;
    }

    public Float getRight() {
        return this.right;
    }

    public void setRight(Float right) {
        this.right = right;
    }

    public Float getTop() {
        return this.top;
    }

    public void setTop(Float top) {
        this.top = top;
    }

    public Float getBottom() {
        return this.bottom;
    }

    public void setBottom(Float bottom) {
        this.bottom = bottom;
    }

    public Float getWidth() {
        return this.width;
    }

    public void setWidth(Float width) {
        this.width = width;
    }

    public Float getHeight() {
        return this.height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public float getPaddingLeft() {
        return this.paddingLeft;
    }

    public void setPaddingLeft(float paddingLeft) {
        this.paddingLeft = paddingLeft;
    }

    public float getPaddingRight() {
        return this.paddingRight;
    }

    public void setPaddingRight(float paddingRight) {
        this.paddingRight = paddingRight;
    }

    public float getPaddingTop() {
        return this.paddingTop;
    }

    public void setPaddingTop(float paddingTop) {
        this.paddingTop = paddingTop;
    }

    public float getPaddingBottom() {
        return this.paddingBottom;
    }

    public void setPaddingBottom(float paddingBottom) {
        this.paddingBottom = paddingBottom;
    }

    public FloatType getFloatType() {
        return this.floatType;
    }

    public void setFloatType(FloatType floatType) {
        this.floatType = floatType;
    }

    public PositionType getPosition() {
        return this.position;
    }

    public void setPosition(PositionType position) {
        this.position = position;
    }

    public ArrayList<Element> getContent() {
        return this.content;
    }

    public void setContent(ArrayList<Element> content) {
        this.content = content;
    }

    public BorderTopStyle getBorderTopStyle() {
        return this.borderTopStyle;
    }

    public void setBorderTopStyle(BorderTopStyle borderTopStyle) {
        this.borderTopStyle = borderTopStyle;
    }

    public int layout(PdfContentByte canvas, boolean useAscender, boolean simulate, float llx, float lly, float urx, float ury) throws DocumentException {
        float leftX = Math.min(llx, urx);
        float maxY = Math.max(lly, ury);
        float minY = Math.min(lly, ury);
        float rightX = Math.max(llx, urx);
        this.yLine = maxY;
        boolean contentCutByFixedHeight = false;

        if (this.width != null && this.width.floatValue() > 0.0F) {
            if (this.width.floatValue() < rightX - leftX) {
                rightX = leftX + this.width.floatValue();
            } else if (this.width.floatValue() > rightX - leftX) {
                return 2;
            }
        } else if (this.percentageWidth != null) {
            this.contentWidth = (rightX - leftX) * this.percentageWidth.floatValue();
            rightX = leftX + this.contentWidth;
        } else if (this.percentageWidth == null
                && this.floatType == FloatType.NONE && (this.display == null || this.display == DisplayType.BLOCK || this.display == DisplayType.LIST_ITEM || this.display == DisplayType.RUN_IN)) {

            this.contentWidth = rightX - leftX;
        }

        if (this.height != null && this.height.floatValue() > 0.0F) {
            if (this.height.floatValue() < maxY - minY) {
                minY = maxY - this.height.floatValue();
                contentCutByFixedHeight = true;
            } else if (this.height.floatValue() > maxY - minY) {
                return 2;
            }
        } else if (this.percentageHeight != null) {
            if (this.percentageHeight.floatValue() < 1.0D) {
                contentCutByFixedHeight = true;
            }
            this.contentHeight = (maxY - minY) * this.percentageHeight.floatValue();
            minY = maxY - this.contentHeight;
        }

        if (!simulate && this.position == PositionType.RELATIVE) {
            Float translationX = null;
            if (this.left != null) {
                translationX = this.left;
            } else if (this.right != null) {
                translationX = Float.valueOf(-this.right.floatValue());
            } else {
                translationX = Float.valueOf(0.0F);
            }

            Float translationY = null;
            if (this.top != null) {
                translationY = Float.valueOf(-this.top.floatValue());
            } else if (this.bottom != null) {
                translationY = this.bottom;
            } else {
                translationY = Float.valueOf(0.0F);
            }
            canvas.saveState();
            canvas.transform(new AffineTransform(1.0F, 0.0F, 0.0F, 1.0F, translationX.floatValue(), translationY.floatValue()));
        }

        if (!simulate && (this.backgroundColor != null || this.backgroundImage != null) && getActualWidth() > 0.0F && getActualHeight() > 0.0F) {
            float backgroundWidth = getActualWidth();
            float backgroundHeight = getActualHeight();
            if (this.width != null) {
                backgroundWidth = (this.width.floatValue() > 0.0F) ? this.width.floatValue() : 0.0F;
            }

            if (this.height != null) {
                backgroundHeight = (this.height.floatValue() > 0.0F) ? this.height.floatValue() : 0.0F;
            }
            if (backgroundWidth > 0.0F && backgroundHeight > 0.0F) {
                Rectangle background = new Rectangle(leftX, maxY - backgroundHeight, backgroundWidth + leftX, maxY);
                if (this.backgroundColor != null) {
                    background.setBackgroundColor(this.backgroundColor);
                    PdfArtifact artifact = new PdfArtifact();
                    canvas.openMCBlock(artifact);
                    canvas.rectangle(background);
                    canvas.closeMCBlock(artifact);
                }
                if (this.backgroundImage != null) {
                    if (this.backgroundImageWidth == null) {
                        this.backgroundImage.scaleToFit(background);
                    } else {
                        this.backgroundImage.scaleAbsolute(this.backgroundImageWidth.floatValue(), this.backgroundImageHeight.floatValue());
                    }
                    this.backgroundImage.setAbsolutePosition(background.getLeft(), background.getBottom());
                    canvas.openMCBlock((IAccessibleElement) this.backgroundImage);
                    canvas.addImage(this.backgroundImage);
                    canvas.closeMCBlock((IAccessibleElement) this.backgroundImage);
                }
            }
        }

        if (this.percentageWidth == null) {
            this.contentWidth = 0.0F;
        }
        if (this.percentageHeight == null) {
            this.contentHeight = 0.0F;
        }

        minY += this.paddingBottom;
        leftX += this.paddingLeft;
        rightX -= this.paddingRight;

        this.yLine -= this.paddingTop;

        int status = 1;

        if (!this.content.isEmpty()) {
            if (this.floatLayout == null) {
                ArrayList<Element> floatingElements = new ArrayList<Element>(this.content);
                this.floatLayout = new FloatLayout(floatingElements, useAscender);
                this.floatLayout.setRunDirection(this.runDirection);
            }

            this.floatLayout.setSimpleColumn(leftX, minY, rightX, this.yLine);
            if (getBorderTopStyle() != null) {
                this.floatLayout.compositeColumn.setIgnoreSpacingBefore(false);
            }

            status = this.floatLayout.layout(canvas, simulate);
            this.yLine = this.floatLayout.getYLine();
            if (this.percentageWidth == null && this.contentWidth < this.floatLayout.getFilledWidth()) {
                this.contentWidth = this.floatLayout.getFilledWidth();
            }
        }

        if (!simulate && this.position == PositionType.RELATIVE) {
            canvas.restoreState();
        }

        this.yLine -= this.paddingBottom;
        if (this.percentageHeight == null) {
            this.contentHeight = maxY - this.yLine;
        }

        if (this.percentageWidth == null) {
            this.contentWidth += this.paddingLeft + this.paddingRight;
        }

        return contentCutByFixedHeight ? 1 : status;
    }

    public PdfObject getAccessibleAttribute(PdfName key) {
        if (this.accessibleAttributes != null) {
            return this.accessibleAttributes.get(key);
        }
        return null;
    }

    public void setAccessibleAttribute(PdfName key, PdfObject value) {
        if (this.accessibleAttributes == null) {
            this.accessibleAttributes = new HashMap<PdfName, PdfObject>();
        }
        this.accessibleAttributes.put(key, value);
    }

    public HashMap<PdfName, PdfObject> getAccessibleAttributes() {
        return this.accessibleAttributes;
    }

    public PdfName getRole() {
        return this.role;
    }

    public void setRole(PdfName role) {
        this.role = role;
    }

    public AccessibleElementId getId() {
        return this.id;
    }

    public void setId(AccessibleElementId id) {
        this.id = id;
    }

    public boolean isInline() {
        return false;
    }
}
