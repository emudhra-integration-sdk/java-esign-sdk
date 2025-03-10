package esign.text.pdf;

import esign.text.DocumentException;
import esign.text.Element;
import esign.text.Paragraph;
import esign.text.WritableDirectElement;
import esign.text.api.Spaceable;
import java.util.ArrayList;
import java.util.List;

public class FloatLayout {

    protected float maxY;
    protected float minY;
    protected float leftX;
    protected float rightX;
    protected float yLine;
    protected float floatLeftX;
    protected float floatRightX;
    protected float filledWidth;
    protected final ColumnText compositeColumn;
    protected final List<Element> content;
    protected final boolean useAscender;

    public float getYLine() {
        return this.yLine;
    }

    public void setYLine(float yLine) {
        this.yLine = yLine;
    }

    public float getFilledWidth() {
        return this.filledWidth;
    }

    public void setFilledWidth(float filledWidth) {
        this.filledWidth = filledWidth;
    }

    public int getRunDirection() {
        return this.compositeColumn.getRunDirection();
    }

    public void setRunDirection(int runDirection) {
        this.compositeColumn.setRunDirection(runDirection);
    }

    public FloatLayout(List<Element> elements, boolean useAscender) {
        this.compositeColumn = new ColumnText(null);
        this.compositeColumn.setUseAscender(useAscender);
        this.useAscender = useAscender;
        this.content = elements;
    }

    public void setSimpleColumn(float llx, float lly, float urx, float ury) {
        this.leftX = Math.min(llx, urx);
        this.maxY = Math.max(lly, ury);
        this.minY = Math.min(lly, ury);
        this.rightX = Math.max(llx, urx);
        this.floatLeftX = this.leftX;
        this.floatRightX = this.rightX;
        this.yLine = this.maxY;
        this.filledWidth = 0.0F;
    }

    public int layout(PdfContentByte canvas, boolean simulate) throws DocumentException {
        this.compositeColumn.setCanvas(canvas);
        int status = 1;

        ArrayList<Element> floatingElements = new ArrayList<Element>();
        List<Element> content = simulate ? new ArrayList<Element>(this.content) : this.content;

        while (!content.isEmpty()) {
            if (content.get(0) instanceof PdfDiv) {
                PdfDiv floatingElement = (PdfDiv) content.get(0);
                if (floatingElement.getFloatType() == PdfDiv.FloatType.LEFT || floatingElement.getFloatType() == PdfDiv.FloatType.RIGHT) {
                    floatingElements.add(floatingElement);
                    content.remove(0);
                    continue;
                }
                if (!floatingElements.isEmpty()) {
                    status = floatingLayout(floatingElements, simulate);
                    if ((status & 0x1) == 0) {
                        break;
                    }
                }

                content.remove(0);

                status = floatingElement.layout(canvas, this.useAscender, true, this.floatLeftX, this.minY, this.floatRightX, this.yLine);

                if (floatingElement.getKeepTogether() && (status & 0x1) == 0) {
                    if ((this.compositeColumn.getCanvas().getPdfDocument()).currentHeight > 0.0F || this.yLine != this.maxY) {
                        content.add(0, floatingElement);

                        break;
                    }
                }
                if (!simulate) {
                    canvas.openMCBlock(floatingElement);
                    status = floatingElement.layout(canvas, this.useAscender, simulate, this.floatLeftX, this.minY, this.floatRightX, this.yLine);
                    canvas.closeMCBlock(floatingElement);
                }

                if (floatingElement.getActualWidth() > this.filledWidth) {
                    this.filledWidth = floatingElement.getActualWidth();
                }
                if ((status & 0x1) == 0) {
                    content.add(0, floatingElement);
                    this.yLine = floatingElement.getYLine();
                    break;
                }
                this.yLine -= floatingElement.getActualHeight();

                continue;
            }
            floatingElements.add(content.get(0));
            content.remove(0);
        }

        if ((status & 0x1) != 0 && !floatingElements.isEmpty()) {
            status = floatingLayout(floatingElements, simulate);
        }

        content.addAll(0, floatingElements);

        return status;
    }

    private int floatingLayout(List<Element> floatingElements, boolean simulate) throws DocumentException {
        int status = 1;
        float minYLine = this.yLine;
        float leftWidth = 0.0F;
        float rightWidth = 0.0F;

        ColumnText currentCompositeColumn = this.compositeColumn;
        if (simulate) {
            currentCompositeColumn = ColumnText.duplicate(this.compositeColumn);
        }

        boolean ignoreSpacingBefore = (this.maxY == this.yLine);

        while (!floatingElements.isEmpty()) {
            Element nextElement = floatingElements.get(0);
            floatingElements.remove(0);
            if (nextElement instanceof PdfDiv) {
                PdfDiv floatingElement = (PdfDiv) nextElement;
                status = floatingElement.layout(this.compositeColumn.getCanvas(), this.useAscender, true, this.floatLeftX, this.minY, this.floatRightX, this.yLine);
                if ((status & 0x1) == 0) {
                    this.yLine = minYLine;
                    this.floatLeftX = this.leftX;
                    this.floatRightX = this.rightX;
                    status = floatingElement.layout(this.compositeColumn.getCanvas(), this.useAscender, true, this.floatLeftX, this.minY, this.floatRightX, this.yLine);
                    if ((status & 0x1) == 0) {
                        floatingElements.add(0, floatingElement);
                        break;
                    }
                }
                if (floatingElement.getFloatType() == PdfDiv.FloatType.LEFT) {
                    status = floatingElement.layout(this.compositeColumn.getCanvas(), this.useAscender, simulate, this.floatLeftX, this.minY, this.floatRightX, this.yLine);
                    this.floatLeftX += floatingElement.getActualWidth();
                    leftWidth += floatingElement.getActualWidth();
                } else if (floatingElement.getFloatType() == PdfDiv.FloatType.RIGHT) {
                    status = floatingElement.layout(this.compositeColumn.getCanvas(), this.useAscender, simulate, this.floatRightX - floatingElement.getActualWidth() - 0.01F, this.minY, this.floatRightX, this.yLine);
                    this.floatRightX -= floatingElement.getActualWidth();
                    rightWidth += floatingElement.getActualWidth();
                }
                minYLine = Math.min(minYLine, this.yLine - floatingElement.getActualHeight());
            } else {
                if (this.minY > minYLine) {
                    status = 2;
                    floatingElements.add(0, nextElement);
                    if (currentCompositeColumn != null) {
                        currentCompositeColumn.setText(null);
                    }
                    break;
                }
                if (nextElement instanceof Spaceable && (!ignoreSpacingBefore || !currentCompositeColumn.isIgnoreSpacingBefore() || ((Spaceable) nextElement).getPaddingTop() != 0.0F)) {
                    this.yLine -= ((Spaceable) nextElement).getSpacingBefore();
                }
                if (simulate) {
                    if (nextElement instanceof PdfPTable) {
                        currentCompositeColumn.addElement((Element) new PdfPTable((PdfPTable) nextElement));
                    } else {
                        currentCompositeColumn.addElement(nextElement);
                    }
                } else {
                    currentCompositeColumn.addElement(nextElement);
                }

                if (this.yLine > minYLine) {
                    currentCompositeColumn.setSimpleColumn(this.floatLeftX, this.yLine, this.floatRightX, minYLine);
                } else {
                    currentCompositeColumn.setSimpleColumn(this.floatLeftX, this.yLine, this.floatRightX, this.minY);
                }
                currentCompositeColumn.setFilledWidth(0.0F);

                status = currentCompositeColumn.go(simulate);
                if (this.yLine > minYLine && (this.floatLeftX > this.leftX || this.floatRightX < this.rightX) && (status & 0x1) == 0) {
                    this.yLine = minYLine;
                    this.floatLeftX = this.leftX;
                    this.floatRightX = this.rightX;
                    if (leftWidth != 0.0F && rightWidth != 0.0F) {
                        this.filledWidth = this.rightX - this.leftX;
                    } else {
                        if (leftWidth > this.filledWidth) {
                            this.filledWidth = leftWidth;
                        }
                        if (rightWidth > this.filledWidth) {
                            this.filledWidth = rightWidth;
                        }
                    }

                    leftWidth = 0.0F;
                    rightWidth = 0.0F;
                    if (simulate && nextElement instanceof PdfPTable) {
                        currentCompositeColumn.addElement((Element) new PdfPTable((PdfPTable) nextElement));
                    }

                    currentCompositeColumn.setSimpleColumn(this.floatLeftX, this.yLine, this.floatRightX, this.minY);
                    status = currentCompositeColumn.go(simulate);
                    minYLine = currentCompositeColumn.getYLine() + currentCompositeColumn.getDescender();
                    this.yLine = minYLine;
                    if (currentCompositeColumn.getFilledWidth() > this.filledWidth) {
                        this.filledWidth = currentCompositeColumn.getFilledWidth();
                    }
                } else {
                    if (rightWidth > 0.0F) {
                        rightWidth += currentCompositeColumn.getFilledWidth();
                    } else if (leftWidth > 0.0F) {
                        leftWidth += currentCompositeColumn.getFilledWidth();
                    } else if (currentCompositeColumn.getFilledWidth() > this.filledWidth) {
                        this.filledWidth = currentCompositeColumn.getFilledWidth();
                    }
                    minYLine = Math.min(currentCompositeColumn.getYLine() + currentCompositeColumn.getDescender(), minYLine);
                    this.yLine = currentCompositeColumn.getYLine() + currentCompositeColumn.getDescender();
                }

                if ((status & 0x1) == 0) {
                    if (!simulate) {
                        floatingElements.addAll(0, currentCompositeColumn.getCompositeElements());
                        currentCompositeColumn.getCompositeElements().clear();
                        break;
                    }
                    floatingElements.add(0, nextElement);
                    currentCompositeColumn.setText(null);

                    break;
                }
                currentCompositeColumn.setText(null);
            }

            if (nextElement instanceof Paragraph) {
                Paragraph p = (Paragraph) nextElement;
                for (Element e : p) {
                    if (e instanceof WritableDirectElement) {
                        WritableDirectElement writableElement = (WritableDirectElement) e;
                        if (writableElement.getDirectElementType() == 1 && !simulate) {
                            PdfWriter writer = this.compositeColumn.getCanvas().getPdfWriter();
                            PdfDocument doc = this.compositeColumn.getCanvas().getPdfDocument();

                            float savedHeight = doc.currentHeight;
                            doc.currentHeight = doc.top() - this.yLine - doc.indentation.indentTop;
                            writableElement.write(writer, doc);
                            doc.currentHeight = savedHeight;
                        }
                    }
                }
            }

            if (ignoreSpacingBefore && nextElement.getChunks().size() == 0) {
                if (nextElement instanceof Paragraph) {
                    Paragraph p = (Paragraph) nextElement;
                    Element e = (Element) p.get(0);
                    if (e instanceof WritableDirectElement) {
                        WritableDirectElement writableElement = (WritableDirectElement) e;
                        if (writableElement.getDirectElementType() != 1) {
                            ignoreSpacingBefore = false;
                        }
                    }
                    continue;
                }
                if (nextElement instanceof Spaceable) {
                    ignoreSpacingBefore = false;
                }
                continue;
            }
            ignoreSpacingBefore = false;
        }

        if (leftWidth != 0.0F && rightWidth != 0.0F) {
            this.filledWidth = this.rightX - this.leftX;
        } else {
            if (leftWidth > this.filledWidth) {
                this.filledWidth = leftWidth;
            }
            if (rightWidth > this.filledWidth) {
                this.filledWidth = rightWidth;
            }
        }

        this.yLine = minYLine;
        this.floatLeftX = this.leftX;
        this.floatRightX = this.rightX;

        return status;
    }
}
