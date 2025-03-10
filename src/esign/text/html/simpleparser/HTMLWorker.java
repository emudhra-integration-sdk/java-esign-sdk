package esign.text.html.simpleparser;

import esign.text.Chunk;
import esign.text.DocListener;
import esign.text.DocumentException;
import esign.text.Element;
import esign.text.ExceptionConverter;
import esign.text.FontProvider;
import esign.text.Image;
import esign.text.List;
import esign.text.ListItem;
import esign.text.Paragraph;
import esign.text.Phrase;
import esign.text.Rectangle;
import esign.text.TextElementArray;
import esign.text.html.HtmlUtilities;
import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import esign.text.pdf.PdfPCell;
import esign.text.pdf.PdfPTable;
import esign.text.pdf.draw.LineSeparator;
import esign.text.xml.simpleparser.SimpleXMLDocHandler;
import esign.text.xml.simpleparser.SimpleXMLParser;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
//import java.util.List;
import java.util.Map;
import java.util.Stack;

@Deprecated
public class HTMLWorker
        implements SimpleXMLDocHandler, DocListener {

    private static Logger LOGGER = LoggerFactory.getLogger(HTMLWorker.class);

    protected DocListener document;

    protected Map<String, HTMLTagProcessor> tags;

    private StyleSheet style = new StyleSheet();
    protected Stack<Element> stack;
    protected Paragraph currentParagraph;
    private final ChainedProperties chain;
    public static final String IMG_PROVIDER = "img_provider";
    public static final String IMG_PROCESSOR = "img_interface";
    public static final String IMG_STORE = "img_static";
    public static final String IMG_BASEURL = "img_baseurl";
    public static final String FONT_PROVIDER = "font_factory";
    public static final String LINK_PROVIDER = "alink_interface";
    private Map<String, Object> providers;
    private final ElementFactory factory;
    private final Stack<boolean[]> tableState;
    private boolean pendingTR;
    private boolean pendingTD;
    private boolean pendingLI;
    private boolean insidePRE;
    protected boolean skipText;
    protected java.util.List<Element> objectList;

    public HTMLWorker(DocListener document) {
        this(document, null, null);
    }

    public void setSupportedTags(Map<String, HTMLTagProcessor> tags) {
        if (tags == null) {
            tags = new HTMLTagProcessors();
        }
        this.tags = tags;
    }

    public void setStyleSheet(StyleSheet style) {
        if (style == null) {
            style = new StyleSheet();
        }
        this.style = style;
    }

    public void startDocument() {
        HashMap<String, String> attrs = new HashMap<String, String>();
        this.style.applyStyle("body", attrs);
        this.chain.addToChain("body", attrs);
    }

    public void startElement(String tag, Map<String, String> attrs) {
        HTMLTagProcessor htmlTag = this.tags.get(tag);
        if (htmlTag == null) {
            return;
        }
        this.style.applyStyle(tag, attrs);
        StyleSheet.resolveStyleAttribute(attrs, this.chain);
        try {
            htmlTag.startElement(this, tag, attrs);
        } catch (DocumentException e) {
            throw new ExceptionConverter(e);
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    public void text(String content) {
        if (this.skipText) {
            return;
        }
        if (this.currentParagraph == null) {
            this.currentParagraph = createParagraph();
        }
        if (!this.insidePRE) {
            if (content.trim().length() == 0 && content.indexOf(' ') < 0) {
                return;
            }
            content = HtmlUtilities.eliminateWhiteSpace(content);
        }
        Chunk chunk = createChunk(content);
        this.currentParagraph.add((Element) chunk);
    }

    public void parse(Reader reader) throws IOException {
        LOGGER.info("Please note, there is a more extended version of the HTMLWorker available in the iText XMLWorker");
        SimpleXMLParser.parse(this, null, reader, true);
    }

    public void endElement(String tag) {
        HTMLTagProcessor htmlTag = this.tags.get(tag);
        if (htmlTag == null) {
            return;
        }
        try {
            htmlTag.endElement(this, tag);
        } catch (DocumentException e) {
            throw new ExceptionConverter(e);
        }
    }

    public void endDocument() {
        try {
            for (int k = 0; k < this.stack.size(); k++) {
                this.document.add(this.stack.elementAt(k));
            }
            if (this.currentParagraph != null) {
                this.document.add((Element) this.currentParagraph);
            }
            this.currentParagraph = null;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public void newLine() {
        if (this.currentParagraph == null) {
            this.currentParagraph = new Paragraph();
        }
        this.currentParagraph.add((Element) createChunk("\n"));
    }

    public HTMLWorker(DocListener document, Map<String, HTMLTagProcessor> tags, StyleSheet style) {
        this.stack = new Stack<Element>();

        this.chain = new ChainedProperties();

        this.providers = new HashMap<String, Object>();

        this.factory = new ElementFactory();

        this.tableState = new Stack<boolean[]>();

        this.pendingTR = false;

        this.pendingTD = false;

        this.pendingLI = false;

        this.insidePRE = false;

        this.skipText = false;
        this.document = document;
        setSupportedTags(tags);
        setStyleSheet(style);
    }

    public void carriageReturn() throws DocumentException {
        if (this.currentParagraph == null) {
            return;
        }
        if (this.stack.empty()) {
            this.document.add((Element) this.currentParagraph);
        } else {
            Element obj = this.stack.pop();
            if (obj instanceof TextElementArray) {
                TextElementArray current = (TextElementArray) obj;
                current.add((Element) this.currentParagraph);
            }
            this.stack.push(obj);
        }
        this.currentParagraph = null;
    }

    public void flushContent() {
        pushToStack((Element) this.currentParagraph);
        this.currentParagraph = new Paragraph();
    }

    public void pushToStack(Element element) {
        if (element != null) {
            this.stack.push(element);
        }
    }

    public void pushTableState() {
        this.tableState.push(new boolean[]{this.pendingTR, this.pendingTD});
    }

    public void updateChain(String tag, Map<String, String> attrs) {
        this.chain.addToChain(tag, attrs);
    }

    public void updateChain(String tag) {
        this.chain.removeChain(tag);
    }

    public void setProviders(Map<String, Object> providers) {
        if (providers == null) {
            return;
        }
        this.providers = providers;
        FontProvider ff = null;
        if (providers != null) {
            ff = (FontProvider) providers.get("font_factory");
        }
        if (ff != null) {
            this.factory.setFontProvider(ff);
        }
    }

    public Chunk createChunk(String content) {
        return this.factory.createChunk(content, this.chain);
    }

    public Paragraph createParagraph() {
        return this.factory.createParagraph(this.chain);
    }

    public List createList(String tag) {
        return this.factory.createList(tag, this.chain);
    }

    public ListItem createListItem() {
        return this.factory.createListItem(this.chain);
    }

    public LineSeparator createLineSeparator(Map<String, String> attrs) {
        return this.factory.createLineSeparator(attrs, this.currentParagraph.getLeading() / 2.0F);
    }

    public Image createImage(Map<String, String> attrs) throws DocumentException, IOException {
        String src = attrs.get("src");
        if (src == null) {
            return null;
        }
        Image img = this.factory.createImage(src, attrs, this.chain, this.document, (ImageProvider) this.providers.get("img_provider"), (ImageStore) this.providers.get("img_static"), (String) this.providers.get("img_baseurl"));
        return img;
    }

    public void popTableState() {
        boolean[] state = this.tableState.pop();
        this.pendingTR = state[0];
        this.pendingTD = state[1];
    }

    public CellWrapper createCell(String tag) {
        return new CellWrapper(tag, this.chain);
    }

    public void processLink() {
        if (this.currentParagraph == null) {
            this.currentParagraph = new Paragraph();
        }
        LinkProcessor i = (LinkProcessor) this.providers.get("alink_interface");
        if (i == null || !i.process(this.currentParagraph, this.chain)) {
            String href = this.chain.getProperty("href");
            if (href != null) {
                for (Chunk ck : this.currentParagraph.getChunks()) {
                    ck.setAnchor(href);
                }
            }
        }
        if (this.stack.isEmpty()) {
            Paragraph tmp = new Paragraph(new Phrase((Phrase) this.currentParagraph));
            this.currentParagraph = tmp;
        } else {
            Paragraph tmp = (Paragraph) this.stack.pop();
            tmp.add((Element) new Phrase((Phrase) this.currentParagraph));
            this.currentParagraph = tmp;
        }
    }

    public void processList() throws DocumentException {
        if (this.stack.empty()) {
            return;
        }
        Element obj = this.stack.pop();
        if (!(obj instanceof List)) {
            this.stack.push(obj);
            return;
        }
        if (this.stack.empty()) {
            this.document.add(obj);
        } else {
            ((TextElementArray) this.stack.peek()).add(obj);
        }
    }

    public boolean isPendingTR() {
        return this.pendingTR;
    }

    public void processListItem() throws DocumentException {
        if (this.stack.empty()) {
            return;
        }
        Element obj = this.stack.pop();
        if (!(obj instanceof ListItem)) {
            this.stack.push(obj);
            return;
        }
        if (this.stack.empty()) {
            this.document.add(obj);
            return;
        }
        ListItem item = (ListItem) obj;
        Element list = this.stack.pop();
        if (!(list instanceof List)) {
            this.stack.push(list);
            return;
        }
        ((List) list).add((Element) item);
        item.adjustListSymbolFont();
        this.stack.push(list);
    }

    public void processImage(Image img, Map<String, String> attrs) throws DocumentException {
        ImageProcessor processor = (ImageProcessor) this.providers.get("img_interface");
        if (processor == null || !processor.process(img, attrs, this.chain, this.document)) {
            String align = attrs.get("align");
            if (align != null) {
                carriageReturn();
            }
            if (this.currentParagraph == null) {
                this.currentParagraph = createParagraph();
            }
            this.currentParagraph.add((Element) new Chunk(img, 0.0F, 0.0F, true));
            this.currentParagraph.setAlignment(HtmlUtilities.alignmentValue(align));
            if (align != null) {
                carriageReturn();
            }
        }
    }

    public void setPendingTR(boolean pendingTR) {
        this.pendingTR = pendingTR;
    }

    public void processTable() throws DocumentException {
        TableWrapper table = (TableWrapper) this.stack.pop();
        PdfPTable tb = table.createTable();
        tb.setSplitRows(true);
        if (this.stack.empty()) {
            this.document.add((Element) tb);
        } else {
            ((TextElementArray) this.stack.peek()).add((Element) tb);
        }
    }

    public boolean isPendingTD() {
        return this.pendingTD;
    }

    public void processRow() {
        ArrayList<PdfPCell> row = new ArrayList<PdfPCell>();
        ArrayList<Float> cellWidths = new ArrayList<Float>();
        boolean percentage = false;
        float totalWidth = 0.0F;
        int zeroWidth = 0;
        TableWrapper table = null;
        while (true) {
            Element obj = this.stack.pop();
            if (obj instanceof CellWrapper) {
                CellWrapper cell = (CellWrapper) obj;
                float width = cell.getWidth();
                cellWidths.add(new Float(width));
                percentage |= cell.isPercentage();
                if (width == 0.0F) {
                    zeroWidth++;
                } else {
                    totalWidth += width;
                }
                row.add(cell.getCell());
            }
            if (obj instanceof TableWrapper) {
                table = (TableWrapper) obj;
                table.addRow(row);
                if (cellWidths.size() > 0) {
                    totalWidth = 100.0F - totalWidth;
                    Collections.reverse(cellWidths);
                    float[] widths = new float[cellWidths.size()];
                    boolean hasZero = false;
                    for (int i = 0; i < widths.length; i++) {
                        widths[i] = ((Float) cellWidths.get(i)).floatValue();
                        if (widths[i] == 0.0F && percentage && zeroWidth > 0) {
                            widths[i] = totalWidth / zeroWidth;
                        }
                        if (widths[i] == 0.0F) {
                            hasZero = true;
                            break;
                        }
                    }
                    if (!hasZero) {
                        table.setColWidths(widths);
                    }
                }
                this.stack.push(table);
                return;
            }
        }
    }

    public void setPendingTD(boolean pendingTD) {
        this.pendingTD = pendingTD;
    }

    public boolean isPendingLI() {
        return this.pendingLI;
    }

    public void setPendingLI(boolean pendingLI) {
        this.pendingLI = pendingLI;
    }

    public boolean isInsidePRE() {
        return this.insidePRE;
    }

    public void setInsidePRE(boolean insidePRE) {
        this.insidePRE = insidePRE;
    }

    public boolean isSkipText() {
        return this.skipText;
    }

    public void setSkipText(boolean skipText) {
        this.skipText = skipText;
    }

    public static java.util.List<Element> parseToList(Reader reader, StyleSheet style) throws IOException {
        return parseToList(reader, style, null);
    }

    public static java.util.List<Element> parseToList(Reader reader, StyleSheet style, HashMap<String, Object> providers) throws IOException {
        return parseToList(reader, style, null, providers);
    }

    public static java.util.List<Element> parseToList(Reader reader, StyleSheet style, Map<String, HTMLTagProcessor> tags, HashMap<String, Object> providers) throws IOException {
        HTMLWorker worker = new HTMLWorker(null, tags, style);
        worker.document = worker;
        worker.setProviders(providers);
        worker.objectList = new ArrayList<Element>();
        worker.parse(reader);
        return worker.objectList;
    }

    public boolean add(Element element) throws DocumentException {
        this.objectList.add(element);
        return true;
    }

    public void close() {
    }

    public boolean newPage() {
        return true;
    }

    public void open() {
    }

    public void resetPageCount() {
    }

    public boolean setMarginMirroring(boolean marginMirroring) {
        return false;
    }

    public boolean setMarginMirroringTopBottom(boolean marginMirroring) {
        return false;
    }

    public boolean setMargins(float marginLeft, float marginRight, float marginTop, float marginBottom) {
        return true;
    }

    public void setPageCount(int pageN) {
    }

    public boolean setPageSize(Rectangle pageSize) {
        return true;
    }

    @Deprecated
    public void setInterfaceProps(HashMap<String, Object> providers) {
        setProviders(providers);
    }

    @Deprecated
    public Map<String, Object> getInterfaceProps() {
        return this.providers;
    }
}
