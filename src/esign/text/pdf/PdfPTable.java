package esign.text.pdf;

import esign.text.AccessibleElementId;
import esign.text.Chunk;
import esign.text.DocumentException;
import esign.text.Element;
import esign.text.ElementListener;
import esign.text.Image;
import esign.text.LargeElement;
import esign.text.Phrase;
import esign.text.Rectangle;
import esign.text.api.Spaceable;
import esign.text.error_messages.MessageLocalization;
import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import esign.text.pdf.events.PdfPTableEventForwarder;
import esign.text.pdf.interfaces.IAccessibleElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdfPTable
        implements LargeElement, Spaceable, IAccessibleElement {

    private final Logger LOGGER = LoggerFactory.getLogger(PdfPTable.class);

    public static final int BASECANVAS = 0;

    public static final int BACKGROUNDCANVAS = 1;

    public static final int LINECANVAS = 2;

    public static final int TEXTCANVAS = 3;

    protected ArrayList<PdfPRow> rows = new ArrayList<PdfPRow>();
    protected float totalHeight = 0.0F;

    protected PdfPCell[] currentRow;

    protected int currentColIdx = 0;
    protected PdfPCell defaultCell = new PdfPCell((Phrase) null);
    protected float totalWidth = 0.0F;

    protected float[] relativeWidths;

    protected float[] absoluteWidths;

    protected PdfPTableEvent tableEvent;

    protected int headerRows;

    protected float widthPercentage = 80.0F;

    private int horizontalAlignment = 1;

    private boolean skipFirstHeader = false;

    private boolean skipLastFooter = false;

    protected boolean isColspan = false;

    protected int runDirection = 1;

    private boolean lockedWidth = false;

    private boolean splitRows = true;

    protected float spacingBefore;

    protected float spacingAfter;

    protected float paddingTop;

    private boolean[] extendLastRow = new boolean[]{false, false};

    private boolean headersInEvent;

    private boolean splitLate = true;

    private boolean keepTogether;

    protected boolean complete = true;

    private int footerRows;

    protected boolean rowCompleted = true;

    protected boolean loopCheck = true;

    protected boolean rowsNotChecked = true;

    protected PdfName role = PdfName.TABLE;
    protected HashMap<PdfName, PdfObject> accessibleAttributes = null;
    protected AccessibleElementId id = new AccessibleElementId();
    private PdfPTableHeader header = null;
    private PdfPTableBody body = null;
    private PdfPTableFooter footer = null;

    private int numberOfWrittenRows;

    protected PdfPTable() {
    }

    public PdfPTable(float[] relativeWidths) {
        if (relativeWidths == null) {
            throw new NullPointerException(MessageLocalization.getComposedMessage("the.widths.array.in.pdfptable.constructor.can.not.be.null", new Object[0]));
        }
        if (relativeWidths.length == 0) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.widths.array.in.pdfptable.constructor.can.not.have.zero.length", new Object[0]));
        }
        this.relativeWidths = new float[relativeWidths.length];
        System.arraycopy(relativeWidths, 0, this.relativeWidths, 0, relativeWidths.length);
        this.absoluteWidths = new float[relativeWidths.length];
        calculateWidths();
        this.currentRow = new PdfPCell[this.absoluteWidths.length];
        this.keepTogether = false;
    }

    public PdfPTable(int numColumns) {
        if (numColumns <= 0) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.number.of.columns.in.pdfptable.constructor.must.be.greater.than.zero", new Object[0]));
        }
        this.relativeWidths = new float[numColumns];
        for (int k = 0; k < numColumns; k++) {
            this.relativeWidths[k] = 1.0F;
        }
        this.absoluteWidths = new float[this.relativeWidths.length];
        calculateWidths();
        this.currentRow = new PdfPCell[this.absoluteWidths.length];
        this.keepTogether = false;
    }

    public PdfPTable(PdfPTable table) {
        copyFormat(table);
        int k;
        for (k = 0; k < this.currentRow.length
                && table.currentRow[k] != null; k++) {

            this.currentRow[k] = new PdfPCell(table.currentRow[k]);
        }
        for (k = 0; k < table.rows.size(); k++) {
            PdfPRow row = table.rows.get(k);
            if (row != null) {
                row = new PdfPRow(row);
            }
            this.rows.add(row);
        }
    }

    public void init() {
        this.LOGGER.info("Initialize row and cell heights");

        for (PdfPRow row : getRows()) {
            if (row == null) {
                continue;
            }
            row.calculated = false;
            for (PdfPCell cell : row.getCells()) {
                if (cell != null) {
                    cell.setCalculatedHeight(0.0F);
                }
            }
        }
    }

    public static PdfPTable shallowCopy(PdfPTable table) {
        PdfPTable nt = new PdfPTable();
        nt.copyFormat(table);
        return nt;
    }

    protected void copyFormat(PdfPTable sourceTable) {
        this.rowsNotChecked = sourceTable.rowsNotChecked;
        this.relativeWidths = new float[sourceTable.getNumberOfColumns()];
        this.absoluteWidths = new float[sourceTable.getNumberOfColumns()];
        System.arraycopy(sourceTable.relativeWidths, 0, this.relativeWidths, 0, getNumberOfColumns());
        System.arraycopy(sourceTable.absoluteWidths, 0, this.absoluteWidths, 0, getNumberOfColumns());
        this.totalWidth = sourceTable.totalWidth;
        this.totalHeight = sourceTable.totalHeight;
        this.currentColIdx = 0;
        this.tableEvent = sourceTable.tableEvent;
        this.runDirection = sourceTable.runDirection;
        if (sourceTable.defaultCell instanceof PdfPHeaderCell) {
            this.defaultCell = new PdfPHeaderCell((PdfPHeaderCell) sourceTable.defaultCell);
        } else {
            this.defaultCell = new PdfPCell(sourceTable.defaultCell);
        }
        this.currentRow = new PdfPCell[sourceTable.currentRow.length];
        this.isColspan = sourceTable.isColspan;
        this.splitRows = sourceTable.splitRows;
        this.spacingAfter = sourceTable.spacingAfter;
        this.spacingBefore = sourceTable.spacingBefore;
        this.headerRows = sourceTable.headerRows;
        this.footerRows = sourceTable.footerRows;
        this.lockedWidth = sourceTable.lockedWidth;
        this.extendLastRow = sourceTable.extendLastRow;
        this.headersInEvent = sourceTable.headersInEvent;
        this.widthPercentage = sourceTable.widthPercentage;
        this.splitLate = sourceTable.splitLate;
        this.skipFirstHeader = sourceTable.skipFirstHeader;
        this.skipLastFooter = sourceTable.skipLastFooter;
        this.horizontalAlignment = sourceTable.horizontalAlignment;
        this.keepTogether = sourceTable.keepTogether;
        this.complete = sourceTable.complete;
        this.loopCheck = sourceTable.loopCheck;
        this.id = sourceTable.id;
        this.role = sourceTable.role;
        if (sourceTable.accessibleAttributes != null) {
            this.accessibleAttributes = new HashMap<PdfName, PdfObject>(sourceTable.accessibleAttributes);
        }
        this.header = sourceTable.getHeader();
        this.body = sourceTable.getBody();
        this.footer = sourceTable.getFooter();
    }

    public void setWidths(float[] relativeWidths) throws DocumentException {
        if (relativeWidths.length != getNumberOfColumns()) {
            throw new DocumentException(MessageLocalization.getComposedMessage("wrong.number.of.columns", new Object[0]));
        }
        this.relativeWidths = new float[relativeWidths.length];
        System.arraycopy(relativeWidths, 0, this.relativeWidths, 0, relativeWidths.length);
        this.absoluteWidths = new float[relativeWidths.length];
        this.totalHeight = 0.0F;
        calculateWidths();
        calculateHeights();
    }

    public void setWidths(int[] relativeWidths) throws DocumentException {
        float[] tb = new float[relativeWidths.length];
        for (int k = 0; k < relativeWidths.length; k++) {
            tb[k] = relativeWidths[k];
        }
        setWidths(tb);
    }

    protected void calculateWidths() {
        if (this.totalWidth <= 0.0F) {
            return;
        }
        float total = 0.0F;
        int numCols = getNumberOfColumns();
        int k;
        for (k = 0; k < numCols; k++) {
            total += this.relativeWidths[k];
        }
        for (k = 0; k < numCols; k++) {
            this.absoluteWidths[k] = this.totalWidth * this.relativeWidths[k] / total;
        }
    }

    public void setTotalWidth(float totalWidth) {
        if (this.totalWidth == totalWidth) {
            return;
        }
        this.totalWidth = totalWidth;
        this.totalHeight = 0.0F;
        calculateWidths();
        calculateHeights();
    }

    public void setTotalWidth(float[] columnWidth) throws DocumentException {
        if (columnWidth.length != getNumberOfColumns()) {
            throw new DocumentException(MessageLocalization.getComposedMessage("wrong.number.of.columns", new Object[0]));
        }
        this.totalWidth = 0.0F;
        for (int k = 0; k < columnWidth.length; k++) {
            this.totalWidth += columnWidth[k];
        }
        setWidths(columnWidth);
    }

    public void setWidthPercentage(float[] columnWidth, Rectangle pageSize) throws DocumentException {
        if (columnWidth.length != getNumberOfColumns()) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("wrong.number.of.columns", new Object[0]));
        }
        setTotalWidth(columnWidth);
        this.widthPercentage = this.totalWidth / (pageSize.getRight() - pageSize.getLeft()) * 100.0F;
    }

    public float getTotalWidth() {
        return this.totalWidth;
    }

    public float calculateHeights() {
        if (this.totalWidth <= 0.0F) {
            return 0.0F;
        }
        this.totalHeight = 0.0F;
        for (int k = 0; k < this.rows.size(); k++) {
            this.totalHeight += getRowHeight(k, true);
        }
        return this.totalHeight;
    }

    public void resetColumnCount(int newColCount) {
        if (newColCount <= 0) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.number.of.columns.in.pdfptable.constructor.must.be.greater.than.zero", new Object[0]));
        }
        this.relativeWidths = new float[newColCount];
        for (int k = 0; k < newColCount; k++) {
            this.relativeWidths[k] = 1.0F;
        }
        this.absoluteWidths = new float[this.relativeWidths.length];
        calculateWidths();
        this.currentRow = new PdfPCell[this.absoluteWidths.length];
        this.totalHeight = 0.0F;
    }

    public PdfPCell getDefaultCell() {
        return this.defaultCell;
    }

    public PdfPCell addCell(PdfPCell cell) {
        PdfPCell ncell;
        this.rowCompleted = false;

        if (cell instanceof PdfPHeaderCell) {
            ncell = new PdfPHeaderCell((PdfPHeaderCell) cell);
        } else {
            ncell = new PdfPCell(cell);
        }

        int colspan = ncell.getColspan();
        colspan = Math.max(colspan, 1);
        colspan = Math.min(colspan, this.currentRow.length - this.currentColIdx);
        ncell.setColspan(colspan);

        if (colspan != 1) {
            this.isColspan = true;
        }
        int rdir = ncell.getRunDirection();
        if (rdir == 1) {
            ncell.setRunDirection(this.runDirection);
        }

        skipColsWithRowspanAbove();

        boolean cellAdded = false;
        if (this.currentColIdx < this.currentRow.length) {
            this.currentRow[this.currentColIdx] = ncell;
            this.currentColIdx += colspan;
            cellAdded = true;
        }

        skipColsWithRowspanAbove();

        while (this.currentColIdx >= this.currentRow.length) {
            int numCols = getNumberOfColumns();
            if (this.runDirection == 3) {
                PdfPCell[] rtlRow = new PdfPCell[numCols];
                int rev = this.currentRow.length;
                for (int k = 0; k < this.currentRow.length; k++) {
                    PdfPCell rcell = this.currentRow[k];
                    int cspan = rcell.getColspan();
                    rev -= cspan;
                    rtlRow[rev] = rcell;
                    k += cspan - 1;
                }
                this.currentRow = rtlRow;
            }
            PdfPRow row = new PdfPRow(this.currentRow);
            if (this.totalWidth > 0.0F) {
                row.setWidths(this.absoluteWidths);
                this.totalHeight += row.getMaxHeights();
            }
            this.rows.add(row);
            this.currentRow = new PdfPCell[numCols];
            this.currentColIdx = 0;
            skipColsWithRowspanAbove();
            this.rowCompleted = true;
        }

        if (!cellAdded) {
            this.currentRow[this.currentColIdx] = ncell;
            this.currentColIdx += colspan;
        }
        return ncell;
    }

    private void skipColsWithRowspanAbove() {
        int direction = 1;
        if (this.runDirection == 3) {
            direction = -1;
        }
        while (rowSpanAbove(this.rows.size(), this.currentColIdx)) {
            this.currentColIdx += direction;
        }
    }

    PdfPCell cellAt(int row, int col) {
        PdfPCell[] cells = ((PdfPRow) this.rows.get(row)).getCells();
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] != null
                    && col >= i && col < i + cells[i].getColspan()) {
                return cells[i];
            }
        }

        return null;
    }

    boolean rowSpanAbove(int currRow, int currCol) {
        if (currCol >= getNumberOfColumns() || currCol < 0 || currRow < 1) {

            return false;
        }
        int row = currRow - 1;
        PdfPRow aboveRow = this.rows.get(row);
        if (aboveRow == null) {
            return false;
        }
        PdfPCell aboveCell = cellAt(row, currCol);
        while (aboveCell == null && row > 0) {
            aboveRow = this.rows.get(--row);
            if (aboveRow == null) {
                return false;
            }
            aboveCell = cellAt(row, currCol);
        }

        int distance = currRow - row;

        if (aboveCell.getRowspan() == 1 && distance > 1) {
            int col = currCol - 1;
            aboveRow = this.rows.get(row + 1);
            distance--;
            aboveCell = aboveRow.getCells()[col];
            while (aboveCell == null && col > 0) {
                aboveCell = aboveRow.getCells()[--col];
            }
        }

        return (aboveCell != null && aboveCell.getRowspan() > distance);
    }

    public void addCell(String text) {
        addCell(new Phrase(text));
    }

    public void addCell(PdfPTable table) {
        this.defaultCell.setTable(table);
        PdfPCell newCell = addCell(this.defaultCell);
        newCell.id = new AccessibleElementId();
        this.defaultCell.setTable(null);
    }

    public void addCell(Image image) {
        this.defaultCell.setImage(image);
        PdfPCell newCell = addCell(this.defaultCell);
        newCell.id = new AccessibleElementId();
        this.defaultCell.setImage(null);
    }

    public void addCell(Phrase phrase) {
        this.defaultCell.setPhrase(phrase);
        PdfPCell newCell = addCell(this.defaultCell);
        newCell.id = new AccessibleElementId();
        this.defaultCell.setPhrase(null);
    }

    public float writeSelectedRows(int rowStart, int rowEnd, float xPos, float yPos, PdfContentByte[] canvases) {
        return writeSelectedRows(0, -1, rowStart, rowEnd, xPos, yPos, canvases);
    }

    public float writeSelectedRows(int colStart, int colEnd, int rowStart, int rowEnd, float xPos, float yPos, PdfContentByte[] canvases) {
        return writeSelectedRows(colStart, colEnd, rowStart, rowEnd, xPos, yPos, canvases, true);
    }

    public float writeSelectedRows(int colStart, int colEnd, int rowStart, int rowEnd, float xPos, float yPos, PdfContentByte[] canvases, boolean reusable) {
        if (this.totalWidth <= 0.0F) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("the.table.width.must.be.greater.than.zero", new Object[0]));
        }

        int totalRows = this.rows.size();
        if (rowStart < 0) {
            rowStart = 0;
        }
        if (rowEnd < 0) {
            rowEnd = totalRows;
        } else {
            rowEnd = Math.min(rowEnd, totalRows);
        }
        if (rowStart >= rowEnd) {
            return yPos;
        }

        int totalCols = getNumberOfColumns();
        if (colStart < 0) {
            colStart = 0;
        } else {
            colStart = Math.min(colStart, totalCols);
        }
        if (colEnd < 0) {
            colEnd = totalCols;
        } else {
            colEnd = Math.min(colEnd, totalCols);
        }

        this.LOGGER.info(String.format("Writing row %s to %s; column %s to %s", new Object[]{Integer.valueOf(rowStart), Integer.valueOf(rowEnd), Integer.valueOf(colStart), Integer.valueOf(colEnd)}));

        float yPosStart = yPos;

        PdfPTableBody currentBlock = null;
        if (this.rowsNotChecked) {
            getFittingRows(Float.MAX_VALUE, rowStart);
        }
        List<PdfPRow> rows = getRows(rowStart, rowEnd);
        int k = rowStart;
        for (PdfPRow row : rows) {
            if ((getHeader()).rows != null && (getHeader()).rows.contains(row) && currentBlock == null) {
                currentBlock = openTableBlock(getHeader(), canvases[3]);
            } else if ((getBody()).rows != null && (getBody()).rows.contains(row) && currentBlock == null) {
                currentBlock = openTableBlock(getBody(), canvases[3]);
            } else if ((getFooter()).rows != null && (getFooter()).rows.contains(row) && currentBlock == null) {
                currentBlock = openTableBlock(getFooter(), canvases[3]);
            }
            if (row != null) {
                row.writeCells(colStart, colEnd, xPos, yPos, canvases, reusable);
                yPos -= row.getMaxHeights();
            }
            if ((getHeader()).rows != null && (getHeader()).rows.contains(row) && (k == rowEnd - 1 || !(getHeader()).rows.contains(rows.get(k + 1)))) {
                currentBlock = closeTableBlock(getHeader(), canvases[3]);
            } else if ((getBody()).rows != null && (getBody()).rows.contains(row) && (k == rowEnd - 1 || !(getBody()).rows.contains(rows.get(k + 1)))) {
                currentBlock = closeTableBlock(getBody(), canvases[3]);
            } else if ((getFooter()).rows != null && (getFooter()).rows.contains(row) && (k == rowEnd - 1 || !(getFooter()).rows.contains(rows.get(k + 1)))) {
                currentBlock = closeTableBlock(getFooter(), canvases[3]);
            }
            k++;
        }

        if (this.tableEvent != null && colStart == 0 && colEnd == totalCols) {
            float[] heights = new float[rowEnd - rowStart + 1];
            heights[0] = yPosStart;
            for (k = rowStart; k < rowEnd; k++) {
                PdfPRow row = rows.get(k);
                float hr = 0.0F;
                if (row != null) {
                    hr = row.getMaxHeights();
                }
                heights[k - rowStart + 1] = heights[k - rowStart] - hr;
            }
            this.tableEvent.tableLayout(this, getEventWidths(xPos, rowStart, rowEnd, this.headersInEvent), heights, this.headersInEvent ? this.headerRows : 0, rowStart, canvases);
        }

        return yPos;
    }

    private PdfPTableBody openTableBlock(PdfPTableBody block, PdfContentByte canvas) {
        if (canvas.writer.getStandardStructElems().contains(block.getRole())) {
            canvas.openMCBlock(block);
            return block;
        }
        return null;
    }

    private PdfPTableBody closeTableBlock(PdfPTableBody block, PdfContentByte canvas) {
        if (canvas.writer.getStandardStructElems().contains(block.getRole())) {
            canvas.closeMCBlock(block);
        }
        return null;
    }

    public float writeSelectedRows(int rowStart, int rowEnd, float xPos, float yPos, PdfContentByte canvas) {
        return writeSelectedRows(0, -1, rowStart, rowEnd, xPos, yPos, canvas);
    }

    public float writeSelectedRows(int colStart, int colEnd, int rowStart, int rowEnd, float xPos, float yPos, PdfContentByte canvas) {
        return writeSelectedRows(colStart, colEnd, rowStart, rowEnd, xPos, yPos, canvas, true);
    }

    public float writeSelectedRows(int colStart, int colEnd, int rowStart, int rowEnd, float xPos, float yPos, PdfContentByte canvas, boolean reusable) {
        int totalCols = getNumberOfColumns();
        if (colStart < 0) {
            colStart = 0;
        } else {
            colStart = Math.min(colStart, totalCols);
        }

        if (colEnd < 0) {
            colEnd = totalCols;
        } else {
            colEnd = Math.min(colEnd, totalCols);
        }

        boolean clip = (colStart != 0 || colEnd != totalCols);

        if (clip) {
            float w = 0.0F;
            for (int k = colStart; k < colEnd; k++) {
                w += this.absoluteWidths[k];
            }
            canvas.saveState();
            float lx = (colStart == 0) ? 10000.0F : 0.0F;
            float rx = (colEnd == totalCols) ? 10000.0F : 0.0F;
            canvas.rectangle(xPos - lx, -10000.0F, w + lx + rx, 20000.0F);
            canvas.clip();
            canvas.newPath();
        }

        PdfContentByte[] canvases = beginWritingRows(canvas);
        float y = writeSelectedRows(colStart, colEnd, rowStart, rowEnd, xPos, yPos, canvases, reusable);
        endWritingRows(canvases);

        if (clip) {
            canvas.restoreState();
        }

        return y;
    }

    public static PdfContentByte[] beginWritingRows(PdfContentByte canvas) {
        return new PdfContentByte[]{canvas, canvas
            .getDuplicate(), canvas
            .getDuplicate(), canvas
            .getDuplicate()};
    }

    public static void endWritingRows(PdfContentByte[] canvases) {
        PdfContentByte canvas = canvases[0];
        PdfArtifact artifact = new PdfArtifact();
        canvas.openMCBlock(artifact);
        canvas.saveState();
        canvas.add(canvases[1]);
        canvas.restoreState();
        canvas.saveState();
        canvas.setLineCap(2);
        canvas.resetRGBColorStroke();
        canvas.add(canvases[2]);
        canvas.restoreState();
        canvas.closeMCBlock(artifact);
        canvas.add(canvases[3]);
    }

    public int size() {
        return this.rows.size();
    }

    public float getTotalHeight() {
        return this.totalHeight;
    }

    public float getRowHeight(int idx) {
        return getRowHeight(idx, false);
    }

    protected float getRowHeight(int idx, boolean firsttime) {
        if (this.totalWidth <= 0.0F || idx < 0 || idx >= this.rows.size()) {
            return 0.0F;
        }
        PdfPRow row = this.rows.get(idx);
        if (row == null) {
            return 0.0F;
        }
        if (firsttime) {
            row.setWidths(this.absoluteWidths);
        }
        float height = row.getMaxHeights();

        for (int i = 0; i < this.relativeWidths.length; i++) {
            if (rowSpanAbove(idx, i)) {

                int rs = 1;
                while (rowSpanAbove(idx - rs, i)) {
                    rs++;
                }
                PdfPRow tmprow = this.rows.get(idx - rs);
                PdfPCell cell = tmprow.getCells()[i];
                float tmp = 0.0F;
                if (cell != null && cell.getRowspan() == rs + 1) {
                    tmp = cell.getMaxHeight();
                    while (rs > 0) {
                        tmp -= getRowHeight(idx - rs);
                        rs--;
                    }
                }
                if (tmp > height) {
                    height = tmp;
                }
            }
        }
        row.setMaxHeights(height);
        return height;
    }

    public float getRowspanHeight(int rowIndex, int cellIndex) {
        if (this.totalWidth <= 0.0F || rowIndex < 0 || rowIndex >= this.rows.size()) {
            return 0.0F;
        }
        PdfPRow row = this.rows.get(rowIndex);
        if (row == null || cellIndex >= (row.getCells()).length) {
            return 0.0F;
        }
        PdfPCell cell = row.getCells()[cellIndex];
        if (cell == null) {
            return 0.0F;
        }
        float rowspanHeight = 0.0F;
        for (int j = 0; j < cell.getRowspan(); j++) {
            rowspanHeight += getRowHeight(rowIndex + j);
        }
        return rowspanHeight;
    }

    public boolean hasRowspan(int rowIdx) {
        if (rowIdx < this.rows.size() && getRow(rowIdx).hasRowspan()) {
            return true;
        }
        PdfPRow previousRow = (rowIdx > 0) ? getRow(rowIdx - 1) : null;
        if (previousRow != null && previousRow.hasRowspan()) {
            return true;
        }
        for (int i = 0; i < getNumberOfColumns(); i++) {
            if (rowSpanAbove(rowIdx - 1, i)) {
                return true;
            }
        }
        return false;
    }

    public void normalizeHeadersFooters() {
        if (this.footerRows > this.headerRows) {
            this.footerRows = this.headerRows;
        }
    }

    public float getHeaderHeight() {
        float total = 0.0F;
        int size = Math.min(this.rows.size(), this.headerRows);
        for (int k = 0; k < size; k++) {
            PdfPRow row = this.rows.get(k);
            if (row != null) {
                total += row.getMaxHeights();
            }
        }
        return total;
    }

    public float getFooterHeight() {
        float total = 0.0F;
        int start = Math.max(0, this.headerRows - this.footerRows);
        int size = Math.min(this.rows.size(), this.headerRows);
        for (int k = start; k < size; k++) {
            PdfPRow row = this.rows.get(k);
            if (row != null) {
                total += row.getMaxHeights();
            }
        }
        return total;
    }

    public boolean deleteRow(int rowNumber) {
        if (rowNumber < 0 || rowNumber >= this.rows.size()) {
            return false;
        }
        if (this.totalWidth > 0.0F) {
            PdfPRow row = this.rows.get(rowNumber);
            if (row != null) {
                this.totalHeight -= row.getMaxHeights();
            }
        }
        this.rows.remove(rowNumber);
        if (rowNumber < this.headerRows) {
            this.headerRows--;
            if (rowNumber >= this.headerRows - this.footerRows) {
                this.footerRows--;
            }
        }
        return true;
    }

    public boolean deleteLastRow() {
        return deleteRow(this.rows.size() - 1);
    }

    public void deleteBodyRows() {
        ArrayList<PdfPRow> rows2 = new ArrayList<PdfPRow>();
        for (int k = 0; k < this.headerRows; k++) {
            rows2.add(this.rows.get(k));
        }
        this.rows = rows2;
        this.totalHeight = 0.0F;
        if (this.totalWidth > 0.0F) {
            this.totalHeight = getHeaderHeight();
        }
    }

    public int getNumberOfColumns() {
        return this.relativeWidths.length;
    }

    public int getHeaderRows() {
        return this.headerRows;
    }

    public void setHeaderRows(int headerRows) {
        if (headerRows < 0) {
            headerRows = 0;
        }
        this.headerRows = headerRows;
    }

    public List<Chunk> getChunks() {
        return new ArrayList<Chunk>();
    }

    public int type() {
        return 23;
    }

    public boolean isContent() {
        return true;
    }

    public boolean isNestable() {
        return true;
    }

    public boolean process(ElementListener listener) {
        try {
            return listener.add((Element) this);
        } catch (DocumentException de) {
            return false;
        }
    }

    public String getSummary() {
        return getAccessibleAttribute(PdfName.SUMMARY).toString();
    }

    public void setSummary(String summary) {
        setAccessibleAttribute(PdfName.SUMMARY, new PdfString(summary));
    }

    public float getWidthPercentage() {
        return this.widthPercentage;
    }

    public void setWidthPercentage(float widthPercentage) {
        this.widthPercentage = widthPercentage;
    }

    public int getHorizontalAlignment() {
        return this.horizontalAlignment;
    }

    public void setHorizontalAlignment(int horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

    public PdfPRow getRow(int idx) {
        return this.rows.get(idx);
    }

    public ArrayList<PdfPRow> getRows() {
        return this.rows;
    }

    public int getLastCompletedRowIndex() {
        return this.rows.size() - 1;
    }

    public void setBreakPoints(int... breakPoints) {
        keepRowsTogether(0, this.rows.size());

        for (int i = 0; i < breakPoints.length; i++) {
            getRow(breakPoints[i]).setMayNotBreak(false);
        }
    }

    public void keepRowsTogether(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
            getRow(rows[i]).setMayNotBreak(true);
        }
    }

    public void keepRowsTogether(int start, int end) {
        if (start < end) {
            while (start < end) {
                getRow(start).setMayNotBreak(true);
                start++;
            }
        }
    }

    public void keepRowsTogether(int start) {
        keepRowsTogether(start, this.rows.size());
    }

    public ArrayList<PdfPRow> getRows(int start, int end) {
        ArrayList<PdfPRow> list = new ArrayList<PdfPRow>();
        if (start < 0 || end > size()) {
            return list;
        }
        for (int i = start; i < end; i++) {
            list.add(adjustCellsInRow(i, end));
        }
        return list;
    }

    protected PdfPRow adjustCellsInRow(int start, int end) {
        PdfPRow row = getRow(start);
        if (row.isAdjusted()) {
            return row;
        }
        row = new PdfPRow(row);

        PdfPCell[] cells = row.getCells();
        for (int i = 0; i < cells.length; i++) {
            PdfPCell cell = cells[i];
            if (cell != null && cell.getRowspan() != 1) {

                int stop = Math.min(end, start + cell.getRowspan());
                float extra = 0.0F;
                for (int k = start + 1; k < stop; k++) {
                    extra += getRow(k).getMaxHeights();
                }
                row.setExtraHeight(i, extra);
            }
        }
        row.setAdjusted(true);
        return row;
    }

    public void setTableEvent(PdfPTableEvent event) {
        if (event == null) {
            this.tableEvent = null;
        } else if (this.tableEvent == null) {
            this.tableEvent = event;
        } else if (this.tableEvent instanceof PdfPTableEventForwarder) {
            ((PdfPTableEventForwarder) this.tableEvent).addTableEvent(event);
        } else {
            PdfPTableEventForwarder forward = new PdfPTableEventForwarder();
            forward.addTableEvent(this.tableEvent);
            forward.addTableEvent(event);
            this.tableEvent = (PdfPTableEvent) forward;
        }
    }

    public PdfPTableEvent getTableEvent() {
        return this.tableEvent;
    }

    public float[] getAbsoluteWidths() {
        return this.absoluteWidths;
    }

    float[][] getEventWidths(float xPos, int firstRow, int lastRow, boolean includeHeaders) {
        if (includeHeaders) {
            firstRow = Math.max(firstRow, this.headerRows);
            lastRow = Math.max(lastRow, this.headerRows);
        }
        float[][] widths = new float[(includeHeaders ? this.headerRows : 0) + lastRow - firstRow][];
        if (this.isColspan) {
            int n = 0;
            if (includeHeaders) {
                for (int k = 0; k < this.headerRows; k++) {
                    PdfPRow row = this.rows.get(k);
                    if (row == null) {
                        n++;
                    } else {
                        widths[n++] = row.getEventWidth(xPos, this.absoluteWidths);
                    }
                }
            }
            for (; firstRow < lastRow; firstRow++) {
                PdfPRow row = this.rows.get(firstRow);
                if (row == null) {
                    n++;
                } else {
                    widths[n++] = row.getEventWidth(xPos, this.absoluteWidths);
                }
            }
        } else {
            int numCols = getNumberOfColumns();
            float[] width = new float[numCols + 1];
            width[0] = xPos;
            int k;
            for (k = 0; k < numCols; k++) {
                width[k + 1] = width[k] + this.absoluteWidths[k];
            }
            for (k = 0; k < widths.length; k++) {
                widths[k] = width;
            }
        }
        return widths;
    }

    public boolean isSkipFirstHeader() {
        return this.skipFirstHeader;
    }

    public boolean isSkipLastFooter() {
        return this.skipLastFooter;
    }

    public void setSkipFirstHeader(boolean skipFirstHeader) {
        this.skipFirstHeader = skipFirstHeader;
    }

    public void setSkipLastFooter(boolean skipLastFooter) {
        this.skipLastFooter = skipLastFooter;
    }

    public void setRunDirection(int runDirection) {
        switch (runDirection) {
            case 0:
            case 1:
            case 2:
            case 3:
                this.runDirection = runDirection;
                return;
        }
        throw new RuntimeException(MessageLocalization.getComposedMessage("invalid.run.direction.1", runDirection));
    }

    public int getRunDirection() {
        return this.runDirection;
    }

    public boolean isLockedWidth() {
        return this.lockedWidth;
    }

    public void setLockedWidth(boolean lockedWidth) {
        this.lockedWidth = lockedWidth;
    }

    public boolean isSplitRows() {
        return this.splitRows;
    }

    public void setSplitRows(boolean splitRows) {
        this.splitRows = splitRows;
    }

    public void setSpacingBefore(float spacing) {
        this.spacingBefore = spacing;
    }

    public void setSpacingAfter(float spacing) {
        this.spacingAfter = spacing;
    }

    public float spacingBefore() {
        return this.spacingBefore;
    }

    public float spacingAfter() {
        return this.spacingAfter;
    }

    public float getPaddingTop() {
        return this.paddingTop;
    }

    public void setPaddingTop(float paddingTop) {
        this.paddingTop = paddingTop;
    }

    public boolean isExtendLastRow() {
        return this.extendLastRow[0];
    }

    public void setExtendLastRow(boolean extendLastRows) {
        this.extendLastRow[0] = extendLastRows;
        this.extendLastRow[1] = extendLastRows;
    }

    public void setExtendLastRow(boolean extendLastRows, boolean extendFinalRow) {
        this.extendLastRow[0] = extendLastRows;
        this.extendLastRow[1] = extendFinalRow;
    }

    public boolean isExtendLastRow(boolean newPageFollows) {
        if (newPageFollows) {
            return this.extendLastRow[0];
        }
        return this.extendLastRow[1];
    }

    public boolean isHeadersInEvent() {
        return this.headersInEvent;
    }

    public void setHeadersInEvent(boolean headersInEvent) {
        this.headersInEvent = headersInEvent;
    }

    public boolean isSplitLate() {
        return this.splitLate;
    }

    public void setSplitLate(boolean splitLate) {
        this.splitLate = splitLate;
    }

    public void setKeepTogether(boolean keepTogether) {
        this.keepTogether = keepTogether;
    }

    public boolean getKeepTogether() {
        return this.keepTogether;
    }

    public int getFooterRows() {
        return this.footerRows;
    }

    public void setFooterRows(int footerRows) {
        if (footerRows < 0) {
            footerRows = 0;
        }
        this.footerRows = footerRows;
    }

    public void completeRow() {
        while (!this.rowCompleted) {
            addCell(this.defaultCell);
        }
    }

    public void flushContent() {
        deleteBodyRows();

        if (this.numberOfWrittenRows > 0) {
            setSkipFirstHeader(true);
        }
    }

    void addNumberOfRowsWritten(int numberOfWrittenRows) {
        this.numberOfWrittenRows += numberOfWrittenRows;
    }

    public boolean isComplete() {
        return this.complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public float getSpacingBefore() {
        return this.spacingBefore;
    }

    public float getSpacingAfter() {
        return this.spacingAfter;
    }

    public boolean isLoopCheck() {
        return this.loopCheck;
    }

    public void setLoopCheck(boolean loopCheck) {
        this.loopCheck = loopCheck;
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

    public PdfPTableHeader getHeader() {
        if (this.header == null) {
            this.header = new PdfPTableHeader();
        }
        return this.header;
    }

    public PdfPTableBody getBody() {
        if (this.body == null) {
            this.body = new PdfPTableBody();
        }
        return this.body;
    }

    public PdfPTableFooter getFooter() {
        if (this.footer == null) {
            this.footer = new PdfPTableFooter();
        }
        return this.footer;
    }

    public int getCellStartRowIndex(int rowIdx, int colIdx) {
        int lastRow = rowIdx;
        while (getRow(lastRow).getCells()[colIdx] == null && lastRow > 0) {
            lastRow--;
        }
        return lastRow;
    }

    public static class FittingRows {

        public final int firstRow;

        public final int lastRow;

        public final float height;

        public final float completedRowsHeight;

        private final Map<Integer, Float> correctedHeightsForLastRow;

        public FittingRows(int firstRow, int lastRow, float height, float completedRowsHeight, Map<Integer, Float> correctedHeightsForLastRow) {
            this.firstRow = firstRow;
            this.lastRow = lastRow;
            this.height = height;
            this.completedRowsHeight = completedRowsHeight;
            this.correctedHeightsForLastRow = correctedHeightsForLastRow;
        }

        public void correctLastRowChosen(PdfPTable table, int k) {
            PdfPRow row = table.getRow(k);
            Float value = this.correctedHeightsForLastRow.get(Integer.valueOf(k));
            if (value != null) {
                row.setFinalMaxHeights(value.floatValue());
            }
        }
    }

    public static class ColumnMeasurementState {

        public float height = 0.0F;

        public int rowspan = 1, colspan = 1;

        public void beginCell(PdfPCell cell, float completedRowsHeight, float rowHeight) {
            this.rowspan = cell.getRowspan();
            this.colspan = cell.getColspan();
            this.height = completedRowsHeight + Math.max(cell.hasCachedMaxHeight() ? cell.getCachedMaxHeight() : cell.getMaxHeight(), rowHeight);
        }

        public void consumeRowspan(float completedRowsHeight, float rowHeight) {
            this.rowspan--;
        }

        public boolean cellEnds() {
            return (this.rowspan == 1);
        }
    }

    public FittingRows getFittingRows(float availableHeight, int startIdx) {
        this.LOGGER.info(String.format("getFittingRows(%s, %s)", new Object[]{Float.valueOf(availableHeight), Integer.valueOf(startIdx)}));
        if (startIdx > 0 && startIdx < this.rows.size()
                && getRow(startIdx).getCells()[0] == null) {
            throw new AssertionError();
        }

        int cols = getNumberOfColumns();
        ColumnMeasurementState[] states = new ColumnMeasurementState[cols];
        for (int i = 0; i < cols; i++) {
            states[i] = new ColumnMeasurementState();
        }
        float completedRowsHeight = 0.0F;

        float totalHeight = 0.0F;
        Map<Integer, Float> correctedHeightsForLastRow = new HashMap<Integer, Float>();
        int k;
        for (k = startIdx; k < size(); k++) {
            PdfPRow row = getRow(k);
            float rowHeight = row.getMaxRowHeightsWithoutCalculating();
            float maxCompletedRowsHeight = 0.0F;
            int j = 0;
            while (j < cols) {
                PdfPCell cell = row.getCells()[j];
                ColumnMeasurementState state = states[j];
                if (cell == null) {
                    state.consumeRowspan(completedRowsHeight, rowHeight);
                } else {
                    state.beginCell(cell, completedRowsHeight, rowHeight);
                    this.LOGGER.info(String.format("Height after beginCell: %s (cell: %s)", new Object[]{Float.valueOf(state.height), Float.valueOf(cell.getCachedMaxHeight())}));
                }
                if (state.cellEnds() && state.height > maxCompletedRowsHeight) {
                    maxCompletedRowsHeight = state.height;
                }
                for (int m = 1; m < state.colspan; m++) {
                    (states[j + m]).height = state.height;
                }
                j += state.colspan;
            }

            float maxTotalHeight = 0.0F;
            for (ColumnMeasurementState state : states) {
                if (state.height > maxTotalHeight) {
                    maxTotalHeight = state.height;
                }
            }
            row.setFinalMaxHeights(maxCompletedRowsHeight - completedRowsHeight);

            float remainingHeight = availableHeight - (isSplitLate() ? maxTotalHeight : maxCompletedRowsHeight);
            if (remainingHeight < 0.0F) {
                break;
            }
            correctedHeightsForLastRow.put(Integer.valueOf(k), Float.valueOf(maxTotalHeight - completedRowsHeight));
            completedRowsHeight = maxCompletedRowsHeight;
            totalHeight = maxTotalHeight;
        }
        this.rowsNotChecked = false;
        return new FittingRows(startIdx, k - 1, totalHeight, completedRowsHeight, correctedHeightsForLastRow);
    }
}
