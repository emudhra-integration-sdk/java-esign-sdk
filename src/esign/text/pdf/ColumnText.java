package esign.text.pdf;

import esign.text.Chunk;
import esign.text.DocumentException;
import esign.text.Element;
import esign.text.ExceptionConverter;
import esign.text.Font;
import esign.text.Image;
import esign.text.List;
import esign.text.ListBody;
import esign.text.ListItem;
import esign.text.ListLabel;
import esign.text.Paragraph;
import esign.text.Phrase;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import esign.text.pdf.draw.DrawInterface;
import esign.text.pdf.interfaces.IAccessibleElement;
import java.util.ArrayList;
import java.util.LinkedList;
//import java.util.List;
import java.util.Stack;

public class ColumnText {

    private final Logger LOGGER = LoggerFactory.getLogger(ColumnText.class);

    public static final int AR_NOVOWEL = 1;

    public static final int AR_COMPOSEDTASHKEEL = 4;

    public static final int AR_LIG = 8;

    public static final int DIGITS_EN2AN = 32;

    public static final int DIGITS_AN2EN = 64;

    public static final int DIGITS_EN2AN_INIT_LR = 96;

    public static final int DIGITS_EN2AN_INIT_AL = 128;

    public static final int DIGIT_TYPE_AN = 0;

    public static final int DIGIT_TYPE_AN_EXTENDED = 256;

    protected int runDirection = 1;

    public static final float GLOBAL_SPACE_CHAR_RATIO = 0.0F;

    public static final int START_COLUMN = 0;

    public static final int NO_MORE_TEXT = 1;

    public static final int NO_MORE_COLUMN = 2;

    protected static final int LINE_STATUS_OK = 0;

    protected static final int LINE_STATUS_OFFLIMITS = 1;

    protected static final int LINE_STATUS_NOLINE = 2;

    protected float maxY;

    protected float minY;

    protected float leftX;

    protected float rightX;

    protected int alignment = 0;

    protected ArrayList<float[]> leftWall;

    protected ArrayList<float[]> rightWall;

    protected BidiLine bidiLine;

    protected boolean isWordSplit;

    protected float yLine;

    protected float lastX;

    protected float currentLeading = 16.0F;

    protected float fixedLeading = 16.0F;

    protected float multipliedLeading = 0.0F;

    protected PdfContentByte canvas;

    protected PdfContentByte[] canvases;

    protected int lineStatus;

    protected float indent = 0.0F;

    protected float followingIndent = 0.0F;

    protected float rightIndent = 0.0F;

    protected float extraParagraphSpace = 0.0F;

    protected float rectangularWidth = -1.0F;

    protected boolean rectangularMode = false;

    private float spaceCharRatio = 0.0F;

    private boolean lastWasNewline = true;

    private boolean repeatFirstLineIndent = true;

    private int linesWritten;

    private float firstLineY;

    private boolean firstLineYDone = false;

    private int arabicOptions = 0;

    protected float descender;

    protected boolean composite = false;

    protected ColumnText compositeColumn;

    protected LinkedList<Element> compositeElements;

    protected int listIdx = 0;

    protected int rowIdx = 0;

    private int splittedRow = -2;

    protected Phrase waitPhrase;

    private boolean useAscender = false;

    private float filledWidth;

    private boolean adjustFirstLine = true;

    private boolean inheritGraphicState = false;

    private boolean ignoreSpacingBefore = true;

    public ColumnText(PdfContentByte canvas) {
        this.canvas = canvas;
    }

    public static ColumnText duplicate(ColumnText org) {
        ColumnText ct = new ColumnText(null);
        ct.setACopy(org);
        return ct;
    }

    public ColumnText setACopy(ColumnText org) {
        if (org != null) {
            setSimpleVars(org);
            if (org.bidiLine != null) {
                this.bidiLine = new BidiLine(org.bidiLine);
            }
        }
        return this;
    }

    protected void setSimpleVars(ColumnText org) {
        this.maxY = org.maxY;
        this.minY = org.minY;
        this.alignment = org.alignment;
        this.leftWall = null;
        if (org.leftWall != null) {
            this.leftWall = (ArrayList) new ArrayList<float[]>(org.leftWall);
        }
        this.rightWall = null;
        if (org.rightWall != null) {
            this.rightWall = (ArrayList) new ArrayList<float[]>(org.rightWall);
        }
        this.yLine = org.yLine;
        this.currentLeading = org.currentLeading;
        this.fixedLeading = org.fixedLeading;
        this.multipliedLeading = org.multipliedLeading;
        this.canvas = org.canvas;
        this.canvases = org.canvases;
        this.lineStatus = org.lineStatus;
        this.indent = org.indent;
        this.followingIndent = org.followingIndent;
        this.rightIndent = org.rightIndent;
        this.extraParagraphSpace = org.extraParagraphSpace;
        this.rectangularWidth = org.rectangularWidth;
        this.rectangularMode = org.rectangularMode;
        this.spaceCharRatio = org.spaceCharRatio;
        this.lastWasNewline = org.lastWasNewline;
        this.repeatFirstLineIndent = org.repeatFirstLineIndent;
        this.linesWritten = org.linesWritten;
        this.arabicOptions = org.arabicOptions;
        this.runDirection = org.runDirection;
        this.descender = org.descender;
        this.composite = org.composite;
        this.splittedRow = org.splittedRow;
        if (org.composite) {
            this.compositeElements = new LinkedList<Element>();
            for (Element element : org.compositeElements) {
                if (element instanceof PdfPTable) {
                    this.compositeElements.add(new PdfPTable((PdfPTable) element));
                    continue;
                }
                this.compositeElements.add(element);
            }
            if (org.compositeColumn != null) {
                this.compositeColumn = duplicate(org.compositeColumn);
            }
        }
        this.listIdx = org.listIdx;
        this.rowIdx = org.rowIdx;
        this.firstLineY = org.firstLineY;
        this.leftX = org.leftX;
        this.rightX = org.rightX;
        this.firstLineYDone = org.firstLineYDone;
        this.waitPhrase = org.waitPhrase;
        this.useAscender = org.useAscender;
        this.filledWidth = org.filledWidth;
        this.adjustFirstLine = org.adjustFirstLine;
        this.inheritGraphicState = org.inheritGraphicState;
        this.ignoreSpacingBefore = org.ignoreSpacingBefore;
    }

    private void addWaitingPhrase() {
        if (this.bidiLine == null && this.waitPhrase != null) {
            this.bidiLine = new BidiLine();
            for (Chunk c : this.waitPhrase.getChunks()) {
                this.bidiLine.addChunk(new PdfChunk(c, null, this.waitPhrase.getTabSettings()));
            }
            this.waitPhrase = null;
        }
    }

    public void addText(Phrase phrase) {
        if (phrase == null || this.composite) {
            return;
        }
        addWaitingPhrase();
        if (this.bidiLine == null) {
            this.waitPhrase = phrase;
            return;
        }
        for (Object element : phrase.getChunks()) {
            this.bidiLine.addChunk(new PdfChunk((Chunk) element, null, phrase.getTabSettings()));
        }
    }

    public void setText(Phrase phrase) {
        this.bidiLine = null;
        this.composite = false;
        this.compositeColumn = null;
        this.compositeElements = null;
        this.listIdx = 0;
        this.rowIdx = 0;
        this.splittedRow = -1;
        this.waitPhrase = phrase;
    }

    public void addText(Chunk chunk) {
        if (chunk == null || this.composite) {
            return;
        }
        addText(new Phrase(chunk));
    }

    public void addElement(Element element) {
        PdfPTable pdfPTable = null;
        Paragraph paragraph;
        if (element == null) {
            return;
        }
//         pdfPTable = element;
        if (element instanceof Image) {
            Image img = (Image) element;
            PdfPTable t = new PdfPTable(1);
            float w = img.getWidthPercentage();
            if (w == 0.0F) {
                t.setTotalWidth(img.getScaledWidth());
                t.setLockedWidth(true);
            } else {
                t.setWidthPercentage(w);
            }
            t.setSpacingAfter(img.getSpacingAfter());
            t.setSpacingBefore(img.getSpacingBefore());
            switch (img.getAlignment()) {
                case 0:
                    t.setHorizontalAlignment(0);
                    break;
                case 2:
                    t.setHorizontalAlignment(2);
                    break;
                default:
                    t.setHorizontalAlignment(1);
                    break;
            }
            PdfPCell c = new PdfPCell(img, true);
            c.setPadding(0.0F);
            c.setBorder(img.getBorder());
            c.setBorderColor(img.getBorderColor());
            c.setBorderWidth(img.getBorderWidth());
            c.setBackgroundColor(img.getBackgroundColor());
            t.addCell(c);
            pdfPTable = t;
        }
        if (element.type() == Element.CHUNK) {
            paragraph = new Paragraph((Chunk) element);
        } else if (element.type() == Element.PHRASE) {
            paragraph = new Paragraph((Phrase) element);
        } else if (element.type() == Element.PTABLE) {
            ((PdfPTable) element).init();
        }
        if (element.type() != 35 && element.type() != 12 && element.type() != 14 && element.type() != 23 && element.type() != 55 && element.type() != 37) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("element.not.allowed", new Object[0]));
        }
        if (!this.composite) {
            this.composite = true;
            this.compositeElements = new LinkedList<Element>();
            this.bidiLine = null;
            this.waitPhrase = null;
        }
        if (element.type() == Element.PARAGRAPH) {
            Paragraph p = (Paragraph) element;
            this.compositeElements.addAll(p.breakUp());
            return;
        }
        this.compositeElements.add(element);
    }

    public static boolean isAllowedElement(Element element) {
        int type = element.type();
        if (type == 10 || type == 11 || type == 37 || type == 12 || type == 14 || type == 55 || type == 23) {
            return true;
        }
        if (element instanceof Image) {
            return true;
        }
        return false;
    }

    protected ArrayList<float[]> convertColumn(float[] cLine) {
        if (cLine.length < 4) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("no.valid.column.line.found", new Object[0]));
        }
        ArrayList<float[]> cc = (ArrayList) new ArrayList<float[]>();
        for (int k = 0; k < cLine.length - 2; k += 2) {
            float x1 = cLine[k];
            float y1 = cLine[k + 1];
            float x2 = cLine[k + 2];
            float y2 = cLine[k + 3];
            if (y1 != y2) {
                float a = (x1 - x2) / (y1 - y2);
                float b = x1 - a * y1;
                float[] r = new float[4];
                r[0] = Math.min(y1, y2);
                r[1] = Math.max(y1, y2);
                r[2] = a;
                r[3] = b;
                cc.add(r);
                this.maxY = Math.max(this.maxY, r[1]);
                this.minY = Math.min(this.minY, r[0]);
            }
        }
        if (cc.isEmpty()) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("no.valid.column.line.found", new Object[0]));
        }
        return cc;
    }

    protected float findLimitsPoint(ArrayList<float[]> wall) {
        this.lineStatus = 0;
        if (this.yLine < this.minY || this.yLine > this.maxY) {
            this.lineStatus = 1;
            return 0.0F;
        }
        for (int k = 0; k < wall.size();) {
            float[] r = wall.get(k);
            if (this.yLine < r[0] || this.yLine > r[1]) {
                k++;
                continue;
            }
            return r[2] * this.yLine + r[3];
        }
        this.lineStatus = 2;
        return 0.0F;
    }

    protected float[] findLimitsOneLine() {
        float x1 = findLimitsPoint(this.leftWall);
        if (this.lineStatus == 1 || this.lineStatus == 2) {
            return null;
        }
        float x2 = findLimitsPoint(this.rightWall);
        if (this.lineStatus == 2) {
            return null;
        }
        return new float[]{x1, x2};
    }

    protected float[] findLimitsTwoLines() {
        float[] x1, x2;
        boolean repeat = false;
        while (true) {
            if (repeat && this.currentLeading == 0.0F) {
                return null;
            }
            repeat = true;
            x1 = findLimitsOneLine();
            if (this.lineStatus == 1) {
                return null;
            }
            this.yLine -= this.currentLeading;
            if (this.lineStatus == 2) {
                continue;
            }
            x2 = findLimitsOneLine();
            if (this.lineStatus == 1) {
                return null;
            }
            if (this.lineStatus == 2) {
                this.yLine -= this.currentLeading;
                continue;
            }
            if (x1[0] >= x2[1] || x2[0] >= x1[1]) {
                continue;
            }
            break;
        }
        return new float[]{x1[0], x1[1], x2[0], x2[1]};
    }

    public void setColumns(float[] leftLine, float[] rightLine) {
        this.maxY = -1.0E21F;
        this.minY = 1.0E21F;
        setYLine(Math.max(leftLine[1], leftLine[leftLine.length - 1]));
        this.rightWall = convertColumn(rightLine);
        this.leftWall = convertColumn(leftLine);
        this.rectangularWidth = -1.0F;
        this.rectangularMode = false;
    }

    public void setSimpleColumn(Phrase phrase, float llx, float lly, float urx, float ury, float leading, int alignment) {
        addText(phrase);
        setSimpleColumn(llx, lly, urx, ury, leading, alignment);
    }

    public void setSimpleColumn(float llx, float lly, float urx, float ury, float leading, int alignment) {
        setLeading(leading);
        this.alignment = alignment;
        setSimpleColumn(llx, lly, urx, ury);
    }

    public void setSimpleColumn(float llx, float lly, float urx, float ury) {
        this.leftX = Math.min(llx, urx);
        this.maxY = Math.max(lly, ury);
        this.minY = Math.min(lly, ury);
        this.rightX = Math.max(llx, urx);
        this.yLine = this.maxY;
        this.rectangularWidth = this.rightX - this.leftX;
        if (this.rectangularWidth < 0.0F) {
            this.rectangularWidth = 0.0F;
        }
        this.rectangularMode = true;
    }

    public void setSimpleColumn(Rectangle rect) {
        setSimpleColumn(rect.getLeft(), rect.getBottom(), rect.getRight(), rect.getTop());
    }

    public void setLeading(float leading) {
        this.fixedLeading = leading;
        this.multipliedLeading = 0.0F;
    }

    public void setLeading(float fixedLeading, float multipliedLeading) {
        this.fixedLeading = fixedLeading;
        this.multipliedLeading = multipliedLeading;
    }

    public float getLeading() {
        return this.fixedLeading;
    }

    public float getMultipliedLeading() {
        return this.multipliedLeading;
    }

    public void setYLine(float yLine) {
        this.yLine = yLine;
    }

    public float getYLine() {
        return this.yLine;
    }

    public int getRowsDrawn() {
        return this.rowIdx;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public int getAlignment() {
        return this.alignment;
    }

    public void setIndent(float indent) {
        setIndent(indent, true);
    }

    public void setIndent(float indent, boolean repeatFirstLineIndent) {
        this.indent = indent;
        this.lastWasNewline = true;
        this.repeatFirstLineIndent = repeatFirstLineIndent;
    }

    public float getIndent() {
        return this.indent;
    }

    public void setFollowingIndent(float indent) {
        this.followingIndent = indent;
        this.lastWasNewline = true;
    }

    public float getFollowingIndent() {
        return this.followingIndent;
    }

    public void setRightIndent(float indent) {
        this.rightIndent = indent;
        this.lastWasNewline = true;
    }

    public float getRightIndent() {
        return this.rightIndent;
    }

    public float getCurrentLeading() {
        return this.currentLeading;
    }

    public boolean getInheritGraphicState() {
        return this.inheritGraphicState;
    }

    public void setInheritGraphicState(boolean inheritGraphicState) {
        this.inheritGraphicState = inheritGraphicState;
    }

    public boolean isIgnoreSpacingBefore() {
        return this.ignoreSpacingBefore;
    }

    public void setIgnoreSpacingBefore(boolean ignoreSpacingBefore) {
        this.ignoreSpacingBefore = ignoreSpacingBefore;
    }

    public int go() throws DocumentException {
        return go(false);
    }

    public int go(boolean simulate) throws DocumentException {
        return go(simulate, null);
    }

    public int go(boolean simulate, IAccessibleElement elementToGo) throws DocumentException {
        this.isWordSplit = false;
        if (this.composite) {
            return goComposite(simulate);
        }
        ListBody lBody = null;
        if (isTagged(this.canvas) && elementToGo instanceof ListItem) {
            lBody = ((ListItem) elementToGo).getListBody();
        }
        addWaitingPhrase();
        if (this.bidiLine == null) {
            return 1;
        }
        this.descender = 0.0F;
        this.linesWritten = 0;
        this.lastX = 0.0F;
        boolean dirty = false;
        float ratio = this.spaceCharRatio;
        Object[] currentValues = new Object[2];
        PdfFont currentFont = null;
        Float lastBaseFactor = new Float(0.0F);
        currentValues[1] = lastBaseFactor;
        PdfDocument pdf = null;
        PdfContentByte graphics = null;
        PdfContentByte text = null;
        this.firstLineY = Float.NaN;
        int localRunDirection = this.runDirection;
        if (this.canvas != null) {
            graphics = this.canvas;
            pdf = this.canvas.getPdfDocument();
            if (!isTagged(this.canvas)) {
                text = this.canvas.getDuplicate(this.inheritGraphicState);
            } else {
                text = this.canvas;
            }
        } else if (!simulate) {
            throw new NullPointerException(MessageLocalization.getComposedMessage("columntext.go.with.simulate.eq.eq.false.and.text.eq.eq.null", new Object[0]));
        }
        if (!simulate) {
            if (ratio == 0.0F) {
                ratio = text.getPdfWriter().getSpaceCharRatio();
            } else if (ratio < 0.001F) {
                ratio = 0.001F;
            }
        }
        if (!this.rectangularMode) {
            float max = 0.0F;
            for (PdfChunk c : this.bidiLine.chunks) {
                max = Math.max(max, c.height());
            }
            this.currentLeading = this.fixedLeading + max * this.multipliedLeading;
        }
        float firstIndent = 0.0F;
        int status = 0;
        boolean rtl = false;
        while (true) {
            PdfLine line;
            float x1;
            firstIndent = this.lastWasNewline ? this.indent : this.followingIndent;
            if (this.rectangularMode) {
                if (this.rectangularWidth <= firstIndent + this.rightIndent) {
                    status = 2;
                    if (this.bidiLine.isEmpty()) {
                        status |= 0x1;
                    }
                    break;
                }
                if (this.bidiLine.isEmpty()) {
                    status = 1;
                    break;
                }
                line = this.bidiLine.processLine(this.leftX, this.rectangularWidth - firstIndent - this.rightIndent, this.alignment, localRunDirection, this.arabicOptions, this.minY, this.yLine, this.descender);
                this.isWordSplit |= this.bidiLine.isWordSplit();
                if (line == null) {
                    status = 1;
                    break;
                }
                float[] maxSize = line.getMaxSize(this.fixedLeading, this.multipliedLeading);
                if (isUseAscender() && Float.isNaN(this.firstLineY)) {
                    this.currentLeading = line.getAscender();
                } else {
                    this.currentLeading = Math.max(maxSize[0], maxSize[1] - this.descender);
                }
                if (this.yLine > this.maxY || this.yLine - this.currentLeading < this.minY) {
                    status = 2;
                    this.bidiLine.restore();
                    break;
                }
                this.yLine -= this.currentLeading;
                if (!simulate && !dirty) {
                    if (line.isRTL && this.canvas.isTagged()) {
                        this.canvas.beginMarkedContentSequence(PdfName.REVERSEDCHARS);
                        rtl = true;
                    }
                    text.beginText();
                    dirty = true;
                }
                if (Float.isNaN(this.firstLineY)) {
                    this.firstLineY = this.yLine;
                }
                updateFilledWidth(this.rectangularWidth - line.widthLeft());
                x1 = this.leftX;
            } else {
                float yTemp = this.yLine - this.currentLeading;
                float[] xx = findLimitsTwoLines();
                if (xx == null) {
                    status = 2;
                    if (this.bidiLine.isEmpty()) {
                        status |= 0x1;
                    }
                    this.yLine = yTemp;
                    break;
                }
                if (this.bidiLine.isEmpty()) {
                    status = 1;
                    this.yLine = yTemp;
                    break;
                }
                x1 = Math.max(xx[0], xx[2]);
                float x2 = Math.min(xx[1], xx[3]);
                if (x2 - x1 <= firstIndent + this.rightIndent) {
                    continue;
                }
                line = this.bidiLine.processLine(x1, x2 - x1 - firstIndent - this.rightIndent, this.alignment, localRunDirection, this.arabicOptions, this.minY, this.yLine, this.descender);
                if (!simulate && !dirty) {
                    if (line.isRTL && this.canvas.isTagged()) {
                        this.canvas.beginMarkedContentSequence(PdfName.REVERSEDCHARS);
                        rtl = true;
                    }
                    text.beginText();
                    dirty = true;
                }
                if (line == null) {
                    status = 1;
                    this.yLine = yTemp;
                    break;
                }
            }
            if (isTagged(this.canvas) && elementToGo instanceof ListItem
                    && !Float.isNaN(this.firstLineY) && !this.firstLineYDone) {
                if (!simulate) {
                    ListLabel lbl = ((ListItem) elementToGo).getListLabel();
                    this.canvas.openMCBlock((IAccessibleElement) lbl);
                    Chunk symbol = new Chunk(((ListItem) elementToGo).getListSymbol());
                    symbol.setRole(null);
                    showTextAligned(this.canvas, 0, new Phrase(symbol), this.leftX + lbl.getIndentation(), this.firstLineY, 0.0F);
                    this.canvas.closeMCBlock((IAccessibleElement) lbl);
                }
                this.firstLineYDone = true;
            }
            if (!simulate) {
                if (lBody != null) {
                    this.canvas.openMCBlock((IAccessibleElement) lBody);
                    lBody = null;
                }
                currentValues[0] = currentFont;
                text.setTextMatrix(x1 + (line.isRTL() ? this.rightIndent : firstIndent) + line.indentLeft(), this.yLine);
                this.lastX = pdf.writeLineToContent(line, text, graphics, currentValues, ratio);
                currentFont = (PdfFont) currentValues[0];
            }
            this.lastWasNewline = (this.repeatFirstLineIndent && line.isNewlineSplit());
            this.yLine -= line.isNewlineSplit() ? this.extraParagraphSpace : 0.0F;
            this.linesWritten++;
            this.descender = line.getDescender();
        }
        if (dirty) {
            text.endText();
            if (this.canvas != text) {
                this.canvas.add(text);
            }
            if (rtl && this.canvas.isTagged()) {
                this.canvas.endMarkedContentSequence();
            }
        }
        return status;
    }

    public boolean isWordSplit() {
        return this.isWordSplit;
    }

    public float getExtraParagraphSpace() {
        return this.extraParagraphSpace;
    }

    public void setExtraParagraphSpace(float extraParagraphSpace) {
        this.extraParagraphSpace = extraParagraphSpace;
    }

    public void clearChunks() {
        if (this.bidiLine != null) {
            this.bidiLine.clearChunks();
        }
    }

    public float getSpaceCharRatio() {
        return this.spaceCharRatio;
    }

    public void setSpaceCharRatio(float spaceCharRatio) {
        this.spaceCharRatio = spaceCharRatio;
    }

    public void setRunDirection(int runDirection) {
        if (runDirection < 0 || runDirection > 3) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("invalid.run.direction.1", runDirection));
        }
        this.runDirection = runDirection;
    }

    public int getRunDirection() {
        return this.runDirection;
    }

    public int getLinesWritten() {
        return this.linesWritten;
    }

    public float getLastX() {
        return this.lastX;
    }

    public int getArabicOptions() {
        return this.arabicOptions;
    }

    public void setArabicOptions(int arabicOptions) {
        this.arabicOptions = arabicOptions;
    }

    public float getDescender() {
        return this.descender;
    }

    public static float getWidth(Phrase phrase, int runDirection, int arabicOptions) {
        ColumnText ct = new ColumnText(null);
        ct.addText(phrase);
        ct.addWaitingPhrase();
        PdfLine line = ct.bidiLine.processLine(0.0F, 20000.0F, 0, runDirection, arabicOptions, 0.0F, 0.0F, 0.0F);
        if (line == null) {
            return 0.0F;
        }
        return 20000.0F - line.widthLeft();
    }

    public static float getWidth(Phrase phrase) {
        return getWidth(phrase, 1, 0);
    }

    public static void showTextAligned(PdfContentByte canvas, int alignment, Phrase phrase, float x, float y, float rotation, int runDirection, int arabicOptions) {
        float llx, urx;
        if (alignment != 0 && alignment != 1 && alignment != 2) {
            alignment = 0;
        }
        canvas.saveState();
        ColumnText ct = new ColumnText(canvas);
        float lly = -1.0F;
        float ury = 2.0F;
        switch (alignment) {
            case 0:
                llx = 0.0F;
                urx = 20000.0F;
                break;
            case 2:
                llx = -20000.0F;
                urx = 0.0F;
                break;
            default:
                llx = -20000.0F;
                urx = 20000.0F;
                break;
        }
        if (rotation == 0.0F) {
            llx += x;
            lly += y;
            urx += x;
            ury += y;
        } else {
            double alpha = rotation * Math.PI / 180.0D;
            float cos = (float) Math.cos(alpha);
            float sin = (float) Math.sin(alpha);
            canvas.concatCTM(cos, sin, -sin, cos, x, y);
        }
        ct.setSimpleColumn(phrase, llx, lly, urx, ury, 2.0F, alignment);
        if (runDirection == 3) {
            if (alignment == 0) {
                alignment = 2;
            } else if (alignment == 2) {
                alignment = 0;
            }
        }
        ct.setAlignment(alignment);
        ct.setArabicOptions(arabicOptions);
        ct.setRunDirection(runDirection);
        try {
            ct.go();
        } catch (DocumentException e) {
            throw new ExceptionConverter(e);
        }
        canvas.restoreState();
    }

    public static void showTextAligned(PdfContentByte canvas, int alignment, Phrase phrase, float x, float y, float rotation) {
        showTextAligned(canvas, alignment, phrase, x, y, rotation, 1, 0);
    }

    public static float fitText(Font font, String text, Rectangle rect, float maxFontSize, int runDirection) {
        try {
            ColumnText ct = null;
            int status = 0;
            if (maxFontSize <= 0.0F) {
                int cr = 0;
                int lf = 0;
                char[] t = text.toCharArray();
                for (int i = 0; i < t.length; i++) {
                    if (t[i] == '\n') {
                        lf++;
                    } else if (t[i] == '\r') {
                        cr++;
                    }
                }
                int minLines = Math.max(cr, lf) + 1;
                maxFontSize = Math.abs(rect.getHeight()) / minLines - 0.001F;
            }
            font.setSize(maxFontSize);
            Phrase ph = new Phrase(text, font);
            ct = new ColumnText(null);
            ct.setSimpleColumn(ph, rect.getLeft(), rect.getBottom(), rect.getRight(), rect.getTop(), maxFontSize, 0);
            ct.setRunDirection(runDirection);
            status = ct.go(true);
            if ((status & 0x1) != 0) {
                return maxFontSize;
            }
            float precision = 0.1F;
            float min = 0.0F;
            float max = maxFontSize;
            float size = maxFontSize;
            for (int k = 0; k < 50; k++) {
                size = (min + max) / 2.0F;
                ct = new ColumnText(null);
                font.setSize(size);
                ct.setSimpleColumn(new Phrase(text, font), rect.getLeft(), rect.getBottom(), rect.getRight(), rect.getTop(), size, 0);
                ct.setRunDirection(runDirection);
                status = ct.go(true);
                if ((status & 0x1) != 0) {
                    if (max - min < size * precision) {
                        return size;
                    }
                    min = size;
                } else {
                    max = size;
                }
            }
            return size;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    protected int goComposite(boolean simulate) throws DocumentException {
        PdfDocument pdf = null;
        if (this.canvas != null) {
            pdf = this.canvas.pdf;
        }
        if (!this.rectangularMode) {
            throw new DocumentException(MessageLocalization.getComposedMessage("irregular.columns.are.not.supported.in.composite.mode", new Object[0]));
        }
        this.linesWritten = 0;
        this.descender = 0.0F;
        boolean firstPass = true;
        boolean isRTL = (this.runDirection == 3);
        while (true) {
            if (this.compositeElements.isEmpty()) {
                return 1;
            }
            Element element = this.compositeElements.getFirst();
            if (element.type() == 12) {
                Paragraph para = (Paragraph) element;
                int status = 0;
                for (int keep = 0; keep < 2; keep++) {
                    float lastY = this.yLine;
                    boolean createHere = false;
                    if (this.compositeColumn == null) {
                        this.compositeColumn = new ColumnText(this.canvas);
                        this.compositeColumn.setAlignment(para.getAlignment());
                        this.compositeColumn.setIndent(para.getIndentationLeft() + para.getFirstLineIndent(), false);
                        this.compositeColumn.setExtraParagraphSpace(para.getExtraParagraphSpace());
                        this.compositeColumn.setFollowingIndent(para.getIndentationLeft());
                        this.compositeColumn.setRightIndent(para.getIndentationRight());
                        this.compositeColumn.setLeading(para.getLeading(), para.getMultipliedLeading());
                        this.compositeColumn.setRunDirection(this.runDirection);
                        this.compositeColumn.setArabicOptions(this.arabicOptions);
                        this.compositeColumn.setSpaceCharRatio(this.spaceCharRatio);
                        this.compositeColumn.addText((Phrase) para);
                        if (!firstPass || !this.adjustFirstLine) {
                            this.yLine -= para.getSpacingBefore();
                        }
                        createHere = true;
                    }
                    this.compositeColumn.setUseAscender(((firstPass || this.descender == 0.0F) && this.adjustFirstLine) ? this.useAscender : false);
                    this.compositeColumn.setInheritGraphicState(this.inheritGraphicState);
                    this.compositeColumn.leftX = this.leftX;
                    this.compositeColumn.rightX = this.rightX;
                    this.compositeColumn.yLine = this.yLine;
                    this.compositeColumn.rectangularWidth = this.rectangularWidth;
                    this.compositeColumn.rectangularMode = this.rectangularMode;
                    this.compositeColumn.minY = this.minY;
                    this.compositeColumn.maxY = this.maxY;
                    boolean keepCandidate = (para.getKeepTogether() && createHere && (!firstPass || !this.adjustFirstLine));
                    boolean s = (simulate || (keepCandidate && keep == 0));
                    if (isTagged(this.canvas) && !s) {
                        this.canvas.openMCBlock((IAccessibleElement) para);
                    }
                    status = this.compositeColumn.go(s);
                    if (isTagged(this.canvas) && !s) {
                        this.canvas.closeMCBlock((IAccessibleElement) para);
                    }
                    this.lastX = this.compositeColumn.getLastX();
                    updateFilledWidth(this.compositeColumn.filledWidth);
                    if ((status & 0x1) == 0 && keepCandidate) {
                        this.compositeColumn = null;
                        this.yLine = lastY;
                        return 2;
                    }
                    if (simulate || !keepCandidate) {
                        break;
                    }
                    if (keep == 0) {
                        this.compositeColumn = null;
                        this.yLine = lastY;
                    }
                }
                firstPass = false;
                if (this.compositeColumn.getLinesWritten() > 0) {
                    this.yLine = this.compositeColumn.yLine;
                    this.linesWritten += this.compositeColumn.linesWritten;
                    this.descender = this.compositeColumn.descender;
                    this.isWordSplit |= this.compositeColumn.isWordSplit();
                }
                this.currentLeading = this.compositeColumn.currentLeading;
                if ((status & 0x1) != 0) {
                    this.compositeColumn = null;
                    this.compositeElements.removeFirst();
                    this.yLine -= para.getSpacingAfter();
                }
                if ((status & 0x2) != 0) {
                    return 2;
                }
                continue;
            }
            if (element.type() == 14) {
                List list = (List) element;
                ArrayList<Element> items = list.getItems();
                ListItem item = null;
                float listIndentation = list.getIndentationLeft();
                int count = 0;
                Stack<Object[]> stack = new Stack();
                for (int k = 0; k < items.size(); k++) {
                    Object obj = items.get(k);
                    if (obj instanceof ListItem) {
                        if (count == this.listIdx) {
                            item = (ListItem) obj;
                            break;
                        }
                        count++;
                    } else if (obj instanceof List) {
                        stack.push(new Object[]{list, Integer.valueOf(k), new Float(listIndentation)});
                        list = (List) obj;
                        items = list.getItems();
                        listIndentation += list.getIndentationLeft();
                        k = -1;
                        continue;
                    }
                    while (k == items.size() - 1 && !stack.isEmpty()) {
                        Object[] objs = stack.pop();
                        list = (List) objs[0];
                        items = list.getItems();
                        k = ((Integer) objs[1]).intValue();
                        listIndentation = ((Float) objs[2]).floatValue();
                    }
                    continue;
                }
                int status = 0;
                boolean keepTogetherAndDontFit = false;
                for (int keep = 0; keep < 2; keep++) {
                    float lastY = this.yLine;
                    boolean createHere = false;
                    if (this.compositeColumn == null) {
                        if (item == null) {
                            this.listIdx = 0;
                            this.compositeElements.removeFirst();
                            break;
                        }
                        this.compositeColumn = new ColumnText(this.canvas);
                        this.compositeColumn.setUseAscender(((firstPass || this.descender == 0.0F) && this.adjustFirstLine) ? this.useAscender : false);
                        this.compositeColumn.setInheritGraphicState(this.inheritGraphicState);
                        this.compositeColumn.setAlignment(item.getAlignment());
                        this.compositeColumn.setIndent(item.getIndentationLeft() + listIndentation + item.getFirstLineIndent(), false);
                        this.compositeColumn.setExtraParagraphSpace(item.getExtraParagraphSpace());
                        this.compositeColumn.setFollowingIndent(this.compositeColumn.getIndent());
                        this.compositeColumn.setRightIndent(item.getIndentationRight() + list.getIndentationRight());
                        this.compositeColumn.setLeading(item.getLeading(), item.getMultipliedLeading());
                        this.compositeColumn.setRunDirection(this.runDirection);
                        this.compositeColumn.setArabicOptions(this.arabicOptions);
                        this.compositeColumn.setSpaceCharRatio(this.spaceCharRatio);
                        this.compositeColumn.addText((Phrase) item);
                        if (!firstPass || !this.adjustFirstLine) {
                            this.yLine -= item.getSpacingBefore();
                        }
                        createHere = true;
                    }
                    this.compositeColumn.leftX = this.leftX;
                    this.compositeColumn.rightX = this.rightX;
                    this.compositeColumn.yLine = this.yLine;
                    this.compositeColumn.rectangularWidth = this.rectangularWidth;
                    this.compositeColumn.rectangularMode = this.rectangularMode;
                    this.compositeColumn.minY = this.minY;
                    this.compositeColumn.maxY = this.maxY;
                    boolean keepCandidate = (item.getKeepTogether() && createHere && (!firstPass || !this.adjustFirstLine));
                    boolean s = (simulate || (keepCandidate && keep == 0));
                    if (isTagged(this.canvas) && !s) {
                        item.getListLabel().setIndentation(listIndentation);
                        if (list.getFirstItem() == item || (this.compositeColumn != null && this.compositeColumn.bidiLine != null)) {
                            this.canvas.openMCBlock((IAccessibleElement) list);
                        }
                        this.canvas.openMCBlock((IAccessibleElement) item);
                    }
                    status = this.compositeColumn.go(s, (IAccessibleElement) item);
                    if (isTagged(this.canvas) && !s) {
                        this.canvas.closeMCBlock((IAccessibleElement) item.getListBody());
                        this.canvas.closeMCBlock((IAccessibleElement) item);
                    }
                    this.lastX = this.compositeColumn.getLastX();
                    updateFilledWidth(this.compositeColumn.filledWidth);
                    if ((status & 0x1) == 0 && keepCandidate) {
                        keepTogetherAndDontFit = true;
                        this.compositeColumn = null;
                        this.yLine = lastY;
                    }
                    if (simulate || !keepCandidate || keepTogetherAndDontFit) {
                        break;
                    }
                    if (keep == 0) {
                        this.compositeColumn = null;
                        this.yLine = lastY;
                    }
                }
                if (isTagged(this.canvas) && !simulate && (item == null || (list.getLastItem() == item && (status & 0x1) != 0) || (status & 0x2) != 0)) {
                    this.canvas.closeMCBlock((IAccessibleElement) list);
                }
                if (keepTogetherAndDontFit) {
                    return 2;
                }
                if (item == null) {
                    continue;
                }
                firstPass = false;
                this.yLine = this.compositeColumn.yLine;
                this.linesWritten += this.compositeColumn.linesWritten;
                this.descender = this.compositeColumn.descender;
                this.currentLeading = this.compositeColumn.currentLeading;
                if (!isTagged(this.canvas)
                        && !Float.isNaN(this.compositeColumn.firstLineY) && !this.compositeColumn.firstLineYDone) {
                    if (!simulate) {
                        if (isRTL) {
                            showTextAligned(this.canvas, 2, new Phrase(item.getListSymbol()), this.compositeColumn.lastX + item.getIndentationLeft(), this.compositeColumn.firstLineY, 0.0F, this.runDirection, this.arabicOptions);
                        } else {
                            showTextAligned(this.canvas, 0, new Phrase(item.getListSymbol()), this.compositeColumn.leftX + listIndentation, this.compositeColumn.firstLineY, 0.0F);
                        }
                    }
                    this.compositeColumn.firstLineYDone = true;
                }
                if ((status & 0x1) != 0) {
                    this.compositeColumn = null;
                    this.listIdx++;
                    this.yLine -= item.getSpacingAfter();
                }
                if ((status & 0x2) != 0) {
                    return 2;
                }
                continue;
            }
            if (element.type() == 23) {
                float tableWidth;
                PdfPTable table = (PdfPTable) element;
                int backedUpRunDir = this.runDirection;
                this.runDirection = table.getRunDirection();
                isRTL = (this.runDirection == 3);
                if (table.size() <= table.getHeaderRows()) {
                    this.compositeElements.removeFirst();
                    continue;
                }
                float yTemp = this.yLine;
                yTemp += this.descender;
                if (this.rowIdx == 0 && this.adjustFirstLine) {
                    yTemp -= table.spacingBefore();
                }
                if (yTemp < this.minY || yTemp > this.maxY) {
                    return 2;
                }
                float yLineWrite = yTemp;
                float x1 = this.leftX;
                this.currentLeading = 0.0F;
                if (table.isLockedWidth()) {
                    tableWidth = table.getTotalWidth();
                    updateFilledWidth(tableWidth);
                } else {
                    tableWidth = this.rectangularWidth * table.getWidthPercentage() / 100.0F;
                    table.setTotalWidth(tableWidth);
                }
                table.normalizeHeadersFooters();
                int headerRows = table.getHeaderRows();
                int footerRows = table.getFooterRows();
                int realHeaderRows = headerRows - footerRows;
                float footerHeight = table.getFooterHeight();
                float headerHeight = table.getHeaderHeight() - footerHeight;
                boolean skipHeader = (table.isSkipFirstHeader() && this.rowIdx <= realHeaderRows && (table.isComplete() || this.rowIdx != realHeaderRows));
                if (!skipHeader) {
                    yTemp -= headerHeight;
                }
                int k = 0;
                if (this.rowIdx < headerRows) {
                    this.rowIdx = headerRows;
                }
                PdfPTable.FittingRows fittingRows = null;
                if (table.isSkipLastFooter()) {
                    fittingRows = table.getFittingRows(yTemp - this.minY, this.rowIdx);
                }
                if (!table.isSkipLastFooter() || fittingRows.lastRow < table.size() - 1) {
                    yTemp -= footerHeight;
                    fittingRows = table.getFittingRows(yTemp - this.minY, this.rowIdx);
                }
                if (yTemp < this.minY || yTemp > this.maxY) {
                    return 2;
                }
                k = fittingRows.lastRow + 1;
                yTemp -= fittingRows.height;
                this.LOGGER.info("Want to split at row " + k);
                int kTemp = k;
                while (kTemp > this.rowIdx && kTemp < table.size() && table.getRow(kTemp).isMayNotBreak()) {
                    kTemp--;
                }
                if (kTemp < table.size() - 1 && !table.getRow(kTemp).isMayNotBreak()) {
                    kTemp++;
                }
                if ((kTemp > this.rowIdx && kTemp < k) || (kTemp == headerRows && table.getRow(headerRows).isMayNotBreak() && table.isLoopCheck())) {
                    yTemp = this.minY;
                    k = kTemp;
                    table.setLoopCheck(false);
                }
                this.LOGGER.info("Will split at row " + k);
                if (table.isSplitLate() && k > 0) {
                    fittingRows.correctLastRowChosen(table, k - 1);
                }
                if (!table.isComplete()) {
                    yTemp += footerHeight;
                }
                if (!table.isSplitRows()) {
                    this.splittedRow = -1;
                    if (k == this.rowIdx) {
                        if (k == table.size()) {
                            this.compositeElements.removeFirst();
                            continue;
                        }
                        if (table.isComplete() || k != 1) {
                            table.getRows().remove(k);
                        }
                        return 2;
                    }
                } else if (table.isSplitLate() && (this.rowIdx < k || (this.splittedRow == -2 && (table
                        .getHeaderRows() == 0 || table.isSkipFirstHeader())))) {
                    this.splittedRow = -1;
                } else if (k < table.size()) {
                    yTemp -= fittingRows.completedRowsHeight - fittingRows.height;
                    float h = yTemp - this.minY;
                    PdfPRow newRow = table.getRow(k).splitRow(table, k, h);
                    if (newRow == null) {
                        this.LOGGER.info("Didn't split row!");
                        this.splittedRow = -1;
                        if (this.rowIdx == k) {
                            return 2;
                        }
                    } else {
                        if (k != this.splittedRow) {
                            this.splittedRow = k + 1;
                            table = new PdfPTable(table);
                            this.compositeElements.set(0, table);
                            ArrayList<PdfPRow> rows = table.getRows();
                            for (int i = headerRows; i < this.rowIdx; i++) {
                                rows.set(i, null);
                            }
                        }
                        yTemp = this.minY;
                        table.getRows().add(++k, newRow);
                        this.LOGGER.info("Inserting row at position " + k);
                    }
                }
                firstPass = false;
                if (!simulate) {
                    switch (table.getHorizontalAlignment()) {
                        case 2:
                            if (!isRTL) {
                                x1 += this.rectangularWidth - tableWidth;
                            }
                            break;
                        case 1:
                            x1 += (this.rectangularWidth - tableWidth) / 2.0F;
                            break;
                        default:
                            if (isRTL) {
                                x1 += this.rectangularWidth - tableWidth;
                            }
                            break;
                    }
                    PdfPTable nt = PdfPTable.shallowCopy(table);
                    ArrayList<PdfPRow> sub = nt.getRows();
                    if (!skipHeader && realHeaderRows > 0) {
                        ArrayList<PdfPRow> arrayList = table.getRows(0, realHeaderRows);
                        if (isTagged(this.canvas)) {
                            (nt.getHeader()).rows = arrayList;
                        }
                        sub.addAll(arrayList);
                    } else {
                        nt.setHeaderRows(footerRows);
                    }
                    ArrayList<PdfPRow> rows = table.getRows(this.rowIdx, k);
                    if (isTagged(this.canvas)) {
                        (nt.getBody()).rows = rows;
                    }
                    sub.addAll(rows);
                    boolean showFooter = !table.isSkipLastFooter();
                    boolean newPageFollows = false;
                    if (k < table.size()) {
                        nt.setComplete(true);
                        showFooter = true;
                        newPageFollows = true;
                    }
                    if (footerRows > 0 && nt.isComplete() && showFooter) {
                        ArrayList<PdfPRow> arrayList = table.getRows(realHeaderRows, realHeaderRows + footerRows);
                        if (isTagged(this.canvas)) {
                            (nt.getFooter()).rows = arrayList;
                        }
                        sub.addAll(arrayList);
                    } else {
                        footerRows = 0;
                    }
                    if (sub.size() > 0) {
                        float rowHeight = 0.0F;
                        int lastIdx = sub.size() - 1 - footerRows;
                        PdfPRow last = sub.get(lastIdx);
                        if (table.isExtendLastRow(newPageFollows)) {
                            rowHeight = last.getMaxHeights();
                            last.setMaxHeights(yTemp - this.minY + rowHeight);
                            yTemp = this.minY;
                        }
                        if (newPageFollows) {
                            PdfPTableEvent tableEvent = table.getTableEvent();
                            if (tableEvent instanceof PdfPTableEventSplit) {
                                ((PdfPTableEventSplit) tableEvent).splitTable(table);
                            }
                        }
                        if (this.canvases != null) {
                            if (isTagged(this.canvases[3])) {
                                this.canvases[3].openMCBlock(table);
                            }
                            nt.writeSelectedRows(0, -1, 0, -1, x1, yLineWrite, this.canvases, false);
                            if (isTagged(this.canvases[3])) {
                                this.canvases[3].closeMCBlock(table);
                            }
                        } else {
                            if (isTagged(this.canvas)) {
                                this.canvas.openMCBlock(table);
                            }
                            nt.writeSelectedRows(0, -1, 0, -1, x1, yLineWrite, this.canvas, false);
                            if (isTagged(this.canvas)) {
                                this.canvas.closeMCBlock(table);
                            }
                        }
                        if (!table.isComplete()) {
                            table.addNumberOfRowsWritten(k);
                        }
                        if (this.splittedRow == k && k < table.size()) {
                            PdfPRow splitted = table.getRows().get(k);
                            splitted.copyRowContent(nt, lastIdx);
                        } else if (k > 0 && k < table.size()) {
                            PdfPRow row = table.getRow(k);
                            row.splitRowspans(table, k - 1, nt, lastIdx);
                        }
                        if (table.isExtendLastRow(newPageFollows)) {
                            last.setMaxHeights(rowHeight);
                        }
                        if (newPageFollows) {
                            PdfPTableEvent tableEvent = table.getTableEvent();
                            if (tableEvent instanceof PdfPTableEventAfterSplit) {
                                PdfPRow row = table.getRow(k);
                                ((PdfPTableEventAfterSplit) tableEvent).afterSplitTable(table, row, k);
                            }
                        }
                    }
                } else if (table.isExtendLastRow() && this.minY > -1.07374182E9F) {
                    yTemp = this.minY;
                }
                this.yLine = yTemp;
                this.descender = 0.0F;
                this.currentLeading = 0.0F;
                if (!skipHeader && !table.isComplete()) {
                    this.yLine += footerHeight;
                }
                while (k < table.size()
                        && table.getRowHeight(k) <= 0.0F && !table.hasRowspan(k)) {
                    k++;
                }
                if (k >= table.size()) {
                    if (this.yLine - table.spacingAfter() < this.minY) {
                        this.yLine = this.minY;
                    } else {
                        this.yLine -= table.spacingAfter();
                    }
                    this.compositeElements.removeFirst();
                    this.splittedRow = -1;
                    this.rowIdx = 0;
                } else {
                    if (this.splittedRow > -1) {
                        ArrayList<PdfPRow> rows = table.getRows();
                        for (int i = this.rowIdx; i < k; i++) {
                            rows.set(i, null);
                        }
                    }
                    this.rowIdx = k;
                    return 2;
                }
                this.runDirection = backedUpRunDir;
                isRTL = (this.runDirection == 3);
                continue;
            }
            if (element.type() == 55) {
                if (!simulate) {
                    DrawInterface zh = (DrawInterface) element;
                    zh.draw(this.canvas, this.leftX, this.minY, this.rightX, this.maxY, this.yLine);
                }
                this.compositeElements.removeFirst();
                continue;
            }
            if (element.type() == 37) {
                ArrayList<Element> floatingElements = new ArrayList<Element>();
                do {
                    floatingElements.add(element);
                    this.compositeElements.removeFirst();
                    element = !this.compositeElements.isEmpty() ? this.compositeElements.getFirst() : null;
                } while (element != null && element.type() == 37);
                FloatLayout fl = new FloatLayout(floatingElements, this.useAscender);
                fl.setSimpleColumn(this.leftX, this.minY, this.rightX, this.yLine);
                fl.compositeColumn.setIgnoreSpacingBefore(isIgnoreSpacingBefore());
                int status = fl.layout(this.canvas, simulate);
                this.yLine = fl.getYLine();
                this.descender = 0.0F;
                if ((status & 0x1) == 0) {
                    this.compositeElements.addAll(floatingElements);
                    return status;
                }
                continue;
            }
            this.compositeElements.removeFirst();
        }
    }

    public PdfContentByte getCanvas() {
        return this.canvas;
    }

    public void setCanvas(PdfContentByte canvas) {
        this.canvas = canvas;
        this.canvases = null;
        if (this.compositeColumn != null) {
            this.compositeColumn.setCanvas(canvas);
        }
    }

    public void setCanvases(PdfContentByte[] canvases) {
        this.canvases = canvases;
        this.canvas = canvases[3];
        if (this.compositeColumn != null) {
            this.compositeColumn.setCanvases(canvases);
        }
    }

    public PdfContentByte[] getCanvases() {
        return this.canvases;
    }

    public boolean zeroHeightElement() {
        return (this.composite && !this.compositeElements.isEmpty() && ((Element) this.compositeElements.getFirst()).type() == 55);
    }

    public java.util.List<Element> getCompositeElements() {
        return this.compositeElements;
    }

    public boolean isUseAscender() {
        return this.useAscender;
    }

    public void setUseAscender(boolean useAscender) {
        this.useAscender = useAscender;
    }

    public static boolean hasMoreText(int status) {
        return ((status & 0x1) == 0);
    }

    public float getFilledWidth() {
        return this.filledWidth;
    }

    public void setFilledWidth(float filledWidth) {
        this.filledWidth = filledWidth;
    }

    public void updateFilledWidth(float w) {
        if (w > this.filledWidth) {
            this.filledWidth = w;
        }
    }

    public boolean isAdjustFirstLine() {
        return this.adjustFirstLine;
    }

    public void setAdjustFirstLine(boolean adjustFirstLine) {
        this.adjustFirstLine = adjustFirstLine;
    }

    private static boolean isTagged(PdfContentByte canvas) {
        return (canvas != null && canvas.pdf != null && canvas.writer != null && canvas.writer.isTagged());
    }
}
