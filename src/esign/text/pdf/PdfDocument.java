package esign.text.pdf;

import esign.text.AccessibleElementId;
import esign.text.Anchor;
import esign.text.Annotation;
import esign.text.BaseColor;
import esign.text.Chunk;
import esign.text.Document;
import esign.text.DocumentException;
import esign.text.Element;
import esign.text.ElementListener;
import esign.text.ExceptionConverter;
import esign.text.Font;
import esign.text.Image;
import esign.text.List;
import esign.text.ListItem;
import esign.text.ListLabel;
import esign.text.MarkedObject;
import esign.text.MarkedSection;
import esign.text.Meta;
import esign.text.Paragraph;
import esign.text.Phrase;
import esign.text.Rectangle;
import esign.text.Section;
import esign.text.TabSettings;
import esign.text.TabStop;
import esign.text.Version;
import esign.text.api.WriterOperation;
import esign.text.error_messages.MessageLocalization;
import esign.text.io.TempFileCache;
import esign.text.pdf.collection.PdfCollection;
import esign.text.pdf.draw.DrawInterface;
import esign.text.pdf.interfaces.IAccessibleElement;
import esign.text.pdf.internal.PdfAnnotationsImp;
import esign.text.pdf.internal.PdfViewerPreferencesImp;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

public class PdfDocument
        extends Document {

    protected PdfWriter writer;
    private HashMap<AccessibleElementId, PdfStructureElement> structElements;
    private TempFileCache externalCache;
    private HashMap<AccessibleElementId, TempFileCache.ObjectPosition> externallyStoredStructElements;
    private HashMap<AccessibleElementId, AccessibleElementId> elementsParents;
    private boolean isToUseExternalCache;
    protected boolean openMCDocument;
    protected HashMap<Object, int[]> structParentIndices;
    protected HashMap<Object, Integer> markPoints;
    protected PdfContentByte text;
    protected PdfContentByte graphics;
    protected float leading;
    protected int alignment;
    protected float currentHeight;
    protected boolean isSectionTitle;
    protected PdfAction anchorAction;
    protected TabSettings tabSettings;
    private Stack<Float> leadingStack;
    private PdfBody body;
    protected int textEmptySize;
    protected float nextMarginLeft;
    protected float nextMarginRight;
    protected float nextMarginTop;
    protected float nextMarginBottom;
    protected boolean firstPageEvent;
    protected PdfLine line;
    protected ArrayList<PdfLine> lines;
    protected int lastElementType;
    static final String hangingPunctuation = ".,;:'";
    protected Indentation indentation;
    protected PdfInfo info;
    protected PdfOutline rootOutline;
    protected PdfOutline currentOutline;
    protected PdfViewerPreferencesImp viewerPreferences;
    protected PdfPageLabels pageLabels;
    protected TreeMap<String, Destination> localDestinations;
    int jsCounter;
    protected HashMap<String, PdfObject> documentLevelJS;

    public static class PdfInfo
            extends PdfDictionary {

        PdfInfo() {
            addProducer();
            addCreationDate();
        }

        PdfInfo(String author, String title, String subject) {
            this();
            addTitle(title);
            addSubject(subject);
            addAuthor(author);
        }

        void addTitle(String title) {
            put(PdfName.TITLE, new PdfString(title, "UnicodeBig"));
        }

        void addSubject(String subject) {
            put(PdfName.SUBJECT, new PdfString(subject, "UnicodeBig"));
        }

        void addKeywords(String keywords) {
            put(PdfName.KEYWORDS, new PdfString(keywords, "UnicodeBig"));
        }

        void addAuthor(String author) {
            put(PdfName.AUTHOR, new PdfString(author, "UnicodeBig"));
        }

        void addCreator(String creator) {
            put(PdfName.CREATOR, new PdfString(creator, "UnicodeBig"));
        }

        void addProducer() {
            put(PdfName.PRODUCER, new PdfString(Version.getInstance().getVersion()));
        }

        void addCreationDate() {
            PdfString date = new PdfDate();
            put(PdfName.CREATIONDATE, date);
            put(PdfName.MODDATE, date);
        }

        void addkey(String key, String value) {
            if (key.equals("Producer") || key.equals("CreationDate")) {
                return;
            }
            put(new PdfName(key), new PdfString(value, "UnicodeBig"));
        }
    }

    static class PdfCatalog
            extends PdfDictionary {

        PdfWriter writer;

        PdfCatalog(PdfIndirectReference pages, PdfWriter writer) {
            super(CATALOG);
            this.writer = writer;
            put(PdfName.PAGES, pages);
        }

        void addNames(TreeMap<String, PdfDocument.Destination> localDestinations, HashMap<String, PdfObject> documentLevelJS, HashMap<String, PdfObject> documentFileAttachment, PdfWriter writer) {
            if (localDestinations.isEmpty() && documentLevelJS.isEmpty() && documentFileAttachment.isEmpty()) {
                return;
            }
            try {
                PdfDictionary names = new PdfDictionary();
                if (!localDestinations.isEmpty()) {
                    HashMap<String, PdfObject> destmap = new HashMap<String, PdfObject>();
                    for (Map.Entry<String, PdfDocument.Destination> entry : localDestinations.entrySet()) {
                        String name = entry.getKey();
                        PdfDocument.Destination dest = entry.getValue();
                        if (dest.destination == null) {
                            continue;
                        }
                        destmap.put(name, dest.reference);
                    }
                    if (destmap.size() > 0) {
                        names.put(PdfName.DESTS, writer.addToBody(PdfNameTree.writeTree(destmap, writer)).getIndirectReference());
                    }
                }
                if (!documentLevelJS.isEmpty()) {
                    PdfDictionary tree = PdfNameTree.writeTree(documentLevelJS, writer);
                    names.put(PdfName.JAVASCRIPT, writer.addToBody(tree).getIndirectReference());
                }
                if (!documentFileAttachment.isEmpty()) {
                    names.put(PdfName.EMBEDDEDFILES, writer.addToBody(PdfNameTree.writeTree(documentFileAttachment, writer)).getIndirectReference());
                }
                if (names.size() > 0) {
                    put(PdfName.NAMES, writer.addToBody(names).getIndirectReference());
                }
            } catch (IOException e) {
                throw new ExceptionConverter(e);
            }
        }

        void setOpenAction(PdfAction action) {
            put(PdfName.OPENACTION, action);
        }

        void setAdditionalActions(PdfDictionary actions) {
            try {
                put(PdfName.AA, this.writer.addToBody(actions).getIndirectReference());
            } catch (Exception e) {
                throw new ExceptionConverter(e);
            }
        }
    }

    public PdfDocument() {
        this.structElements = new HashMap<AccessibleElementId, PdfStructureElement>();

        this.externallyStoredStructElements = new HashMap<AccessibleElementId, TempFileCache.ObjectPosition>();
        this.elementsParents = new HashMap<AccessibleElementId, AccessibleElementId>();
        this.isToUseExternalCache = false;

        this.openMCDocument = false;

        this.structParentIndices = (HashMap) new HashMap<Object, int[]>();

        this.markPoints = new HashMap<Object, Integer>();

        this.leading = 0.0F;

        this.alignment = 0;

        this.currentHeight = 0.0F;

        this.isSectionTitle = false;

        this.anchorAction = null;

        this.leadingStack = new Stack<Float>();

        this.firstPageEvent = true;

        this.line = null;

        this.lines = new ArrayList<PdfLine>();

        this.lastElementType = -1;

        this.indentation = new Indentation();

        this.info = new PdfInfo();

        this.viewerPreferences = new PdfViewerPreferencesImp();

        this.localDestinations = new TreeMap<String, Destination>();

        this.documentLevelJS = new HashMap<String, PdfObject>();

        this.documentFileAttachment = new HashMap<String, PdfObject>();

        this.nextPageSize = null;

        this.thisBoxSize = new HashMap<String, PdfRectangle>();

        this.boxSize = new HashMap<String, PdfRectangle>();

        this.pageEmpty = true;

        this.pageAA = null;

        this.strictImageSequence = false;

        this.imageEnd = -1.0F;

        this.imageWait = null;

        this.floatingElements = new ArrayList<Element>();
        addProducer();
        addCreationDate();
    }

    public void addWriter(PdfWriter writer) throws DocumentException {
        if (this.writer == null) {
            this.writer = writer;
            this.annotationsImp = new PdfAnnotationsImp(writer);
            return;
        }
        throw new DocumentException(MessageLocalization.getComposedMessage("you.can.only.add.a.writer.to.a.pdfdocument.once", new Object[0]));
    }

    public float getLeading() {
        return this.leading;
    }

    void setLeading(float leading) {
        this.leading = leading;
    }

    protected void pushLeading() {
        this.leadingStack.push(Float.valueOf(this.leading));
    }

    protected void popLeading() {
        this.leading = ((Float) this.leadingStack.pop()).floatValue();
        if (this.leadingStack.size() > 0) {
            this.leading = ((Float) this.leadingStack.peek()).floatValue();
        }
    }

    public TabSettings getTabSettings() {
        return this.tabSettings;
    }

    public void setTabSettings(TabSettings tabSettings) {
        this.tabSettings = tabSettings;
    }

    public boolean add(Element element) throws DocumentException {
        if (this.writer != null && this.writer.isPaused()) {
            return false;
        }
        try {
            PdfChunk chunk;
            Anchor anchor;
            Annotation annot;
            TabSettings backupTabSettings;
            Section section;
            List list;
            ListItem listItem;
            Rectangle rectangle;
            PdfPTable ptable;
            DrawInterface zh;
            MarkedObject mo;
            PdfChunk overflow;
            String url;
            Rectangle rect;
            Paragraph paragraph;
            PdfPageEvent pageEvent;
            PdfAnnotation an;
            PdfPageEvent pdfPageEvent1;
            boolean hasTitle;
            if (element.type() != 37) {
                flushFloatingElements();
            }
            switch (element.type()) {
                case 0:
                    this.info.addkey(((Meta) element).getName(), ((Meta) element).getContent());
                    this.lastElementType = element.type();
                    return true;
                case 1:
                    this.info.addTitle(((Meta) element).getContent());
                    this.lastElementType = element.type();
                    return true;
                case 2:
                    this.info.addSubject(((Meta) element).getContent());
                    this.lastElementType = element.type();
                    return true;
                case 3:
                    this.info.addKeywords(((Meta) element).getContent());
                    this.lastElementType = element.type();
                    return true;
                case 4:
                    this.info.addAuthor(((Meta) element).getContent());
                    this.lastElementType = element.type();
                    return true;
                case 7:
                    this.info.addCreator(((Meta) element).getContent());
                    this.lastElementType = element.type();
                    return true;
                case 8:
                    setLanguage(((Meta) element).getContent());
                    this.lastElementType = element.type();
                    return true;
                case 5:
                    this.info.addProducer();
                    this.lastElementType = element.type();
                    return true;
                case 6:
                    this.info.addCreationDate();
                    this.lastElementType = element.type();
                    return true;
                case 10:
                    if (this.line == null) {
                        carriageReturn();
                    }
                    chunk = new PdfChunk((Chunk) element, this.anchorAction, this.tabSettings);
                    while ((overflow = this.line.add(chunk, this.leading)) != null) {
                        carriageReturn();
                        boolean newlineSplit = chunk.isNewlineSplit();
                        chunk = overflow;
                        if (!newlineSplit) {
                            chunk.trimFirstSpace();
                        }
                    }
                    this.pageEmpty = false;
                    if (chunk.isAttribute("NEWPAGE")) {
                        newPage();
                    }
                    this.lastElementType = element.type();
                    return true;
                case 17:
                    anchor = (Anchor) element;
                    url = anchor.getReference();
                    this.leading = anchor.getLeading();
                    pushLeading();
                    if (url != null) {
                        this.anchorAction = new PdfAction(url);
                    }
                    element.process((ElementListener) this);
                    this.anchorAction = null;
                    popLeading();
                    this.lastElementType = element.type();
                    return true;
                case 29:
                    if (this.line == null) {
                        carriageReturn();
                    }
                    annot = (Annotation) element;
                    rect = new Rectangle(0.0F, 0.0F);
                    if (this.line != null) {
                        rect = new Rectangle(annot.llx(indentRight() - this.line.widthLeft()), annot.ury(indentTop() - this.currentHeight - 20.0F), annot.urx(indentRight() - this.line.widthLeft() + 20.0F), annot.lly(indentTop() - this.currentHeight));
                    }
                    an = PdfAnnotationsImp.convertAnnotation(this.writer, annot, rect);
                    this.annotationsImp.addPlainAnnotation(an);
                    this.pageEmpty = false;
                    this.lastElementType = element.type();
                    return true;
                case 11:
                    backupTabSettings = this.tabSettings;
                    if (((Phrase) element).getTabSettings() != null) {
                        this.tabSettings = ((Phrase) element).getTabSettings();
                    }
                    this.leading = ((Phrase) element).getTotalLeading();
                    pushLeading();
                    element.process((ElementListener) this);
                    this.tabSettings = backupTabSettings;
                    popLeading();
                    this.lastElementType = element.type();
                    return true;
                case 12:
                    backupTabSettings = this.tabSettings;
                    if (((Phrase) element).getTabSettings() != null) {
                        this.tabSettings = ((Phrase) element).getTabSettings();
                    }
                    paragraph = (Paragraph) element;
                    if (isTagged(this.writer)) {
                        flushLines();
                        this.text.openMCBlock((IAccessibleElement) paragraph);
                    }
                    addSpacing(paragraph.getSpacingBefore(), this.leading, paragraph.getFont());
                    this.alignment = paragraph.getAlignment();
                    this.leading = paragraph.getTotalLeading();
                    pushLeading();
                    carriageReturn();
                    if (this.currentHeight + calculateLineHeight() > indentTop() - indentBottom()) {
                        newPage();
                    }
                    this.indentation.indentLeft += paragraph.getIndentationLeft();
                    this.indentation.indentRight += paragraph.getIndentationRight();
                    carriageReturn();
                    pdfPageEvent1 = this.writer.getPageEvent();
                    if (pdfPageEvent1 != null && !this.isSectionTitle) {
                        pdfPageEvent1.onParagraph(this.writer, this, indentTop() - this.currentHeight);
                    }
                    if (paragraph.getKeepTogether()) {
                        carriageReturn();
                        PdfPTable table = new PdfPTable(1);
                        table.setKeepTogether(paragraph.getKeepTogether());
                        table.setWidthPercentage(100.0F);
                        PdfPCell cell = new PdfPCell();
                        cell.addElement((Element) paragraph);
                        cell.setBorder(0);
                        cell.setPadding(0.0F);
                        table.addCell(cell);
                        this.indentation.indentLeft -= paragraph.getIndentationLeft();
                        this.indentation.indentRight -= paragraph.getIndentationRight();
                        add((Element) table);
                        this.indentation.indentLeft += paragraph.getIndentationLeft();
                        this.indentation.indentRight += paragraph.getIndentationRight();
                    } else {
                        this.line.setExtraIndent(paragraph.getFirstLineIndent());
                        float oldHeight = this.currentHeight;
                        element.process((ElementListener) this);
                        carriageReturn();
                        if (oldHeight != this.currentHeight || this.lines.size() > 0) {
                            addSpacing(paragraph.getSpacingAfter(), paragraph.getTotalLeading(), paragraph.getFont(), true);
                        }
                    }
                    if (pdfPageEvent1 != null && !this.isSectionTitle) {
                        pdfPageEvent1.onParagraphEnd(this.writer, this, indentTop() - this.currentHeight);
                    }
                    this.alignment = 0;
                    if (this.floatingElements != null && this.floatingElements.size() != 0) {
                        flushFloatingElements();
                    }
                    this.indentation.indentLeft -= paragraph.getIndentationLeft();
                    this.indentation.indentRight -= paragraph.getIndentationRight();
                    carriageReturn();
                    this.tabSettings = backupTabSettings;
                    popLeading();
                    if (isTagged(this.writer)) {
                        flushLines();
                        this.text.closeMCBlock((IAccessibleElement) paragraph);
                    }
                    this.lastElementType = element.type();
                    return true;
                case 13:
                case 16:
                    section = (Section) element;
                    pageEvent = this.writer.getPageEvent();
                    hasTitle = (section.isNotAddedYet() && section.getTitle() != null);
                    if (section.isTriggerNewPage()) {
                        newPage();
                    }
                    if (hasTitle) {
                        float fith = indentTop() - this.currentHeight;
                        int rotation = this.pageSize.getRotation();
                        if (rotation == 90 || rotation == 180) {
                            fith = this.pageSize.getHeight() - fith;
                        }
                        PdfDestination destination = new PdfDestination(2, fith);
                        while (this.currentOutline.level() >= section.getDepth()) {
                            this.currentOutline = this.currentOutline.parent();
                        }
                        PdfOutline outline = new PdfOutline(this.currentOutline, destination, section.getBookmarkTitle(), section.isBookmarkOpen());
                        this.currentOutline = outline;
                    }
                    carriageReturn();
                    this.indentation.sectionIndentLeft += section.getIndentationLeft();
                    this.indentation.sectionIndentRight += section.getIndentationRight();
                    if (section.isNotAddedYet() && pageEvent != null) {
                        if (element.type() == 16) {
                            pageEvent.onChapter(this.writer, this, indentTop() - this.currentHeight, section.getTitle());
                        } else {
                            pageEvent.onSection(this.writer, this, indentTop() - this.currentHeight, section.getDepth(), section.getTitle());
                        }
                    }
                    if (hasTitle) {
                        this.isSectionTitle = true;
                        add((Element) section.getTitle());
                        this.isSectionTitle = false;
                    }
                    this.indentation.sectionIndentLeft += section.getIndentation();
                    element.process((ElementListener) this);
                    flushLines();
                    this.indentation.sectionIndentLeft -= section.getIndentationLeft() + section.getIndentation();
                    this.indentation.sectionIndentRight -= section.getIndentationRight();
                    if (section.isComplete() && pageEvent != null) {
                        if (element.type() == 16) {
                            pageEvent.onChapterEnd(this.writer, this, indentTop() - this.currentHeight);
                        } else {
                            pageEvent.onSectionEnd(this.writer, this, indentTop() - this.currentHeight);
                        }
                    }
                    this.lastElementType = element.type();
                    return true;
                case 14:
                    list = (List) element;
                    if (isTagged(this.writer)) {
                        flushLines();
                        this.text.openMCBlock((IAccessibleElement) list);
                    }
                    if (list.isAlignindent()) {
                        list.normalizeIndentation();
                    }
                    this.indentation.listIndentLeft += list.getIndentationLeft();
                    this.indentation.indentRight += list.getIndentationRight();
                    element.process((ElementListener) this);
                    this.indentation.listIndentLeft -= list.getIndentationLeft();
                    this.indentation.indentRight -= list.getIndentationRight();
                    carriageReturn();
                    if (isTagged(this.writer)) {
                        flushLines();
                        this.text.closeMCBlock((IAccessibleElement) list);
                    }
                    this.lastElementType = element.type();
                    return true;
                case 15:
                    listItem = (ListItem) element;
                    if (isTagged(this.writer)) {
                        flushLines();
                        this.text.openMCBlock((IAccessibleElement) listItem);
                    }
                    addSpacing(listItem.getSpacingBefore(), this.leading, listItem.getFont());
                    this.alignment = listItem.getAlignment();
                    this.indentation.listIndentLeft += listItem.getIndentationLeft();
                    this.indentation.indentRight += listItem.getIndentationRight();
                    this.leading = listItem.getTotalLeading();
                    pushLeading();
                    carriageReturn();
                    this.line.setListItem(listItem);
                    element.process((ElementListener) this);
                    addSpacing(listItem.getSpacingAfter(), listItem.getTotalLeading(), listItem.getFont(), true);
                    if (this.line.hasToBeJustified()) {
                        this.line.resetAlignment();
                    }
                    carriageReturn();
                    this.indentation.listIndentLeft -= listItem.getIndentationLeft();
                    this.indentation.indentRight -= listItem.getIndentationRight();
                    popLeading();
                    if (isTagged(this.writer)) {
                        flushLines();
                        this.text.closeMCBlock((IAccessibleElement) listItem.getListBody());
                        this.text.closeMCBlock((IAccessibleElement) listItem);
                    }
                    this.lastElementType = element.type();
                    return true;
                case 30:
                    rectangle = (Rectangle) element;
                    this.graphics.rectangle(rectangle);
                    this.pageEmpty = false;
                    this.lastElementType = element.type();
                    return true;
                case 23:
                    ptable = (PdfPTable) element;
                    if (ptable.size() > ptable.getHeaderRows()) {
                        ensureNewLine();
                        flushLines();
                        addPTable(ptable);
                        this.pageEmpty = false;
                        newLine();
                    }
                    this.lastElementType = element.type();
                    return true;
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                    if (isTagged(this.writer) && !((Image) element).isImgTemplate()) {
                        flushLines();
                        this.text.openMCBlock((IAccessibleElement) element);
                    }
                    add((Image) element);
                    if (isTagged(this.writer) && !((Image) element).isImgTemplate()) {
                        flushLines();
                        this.text.closeMCBlock((IAccessibleElement) element);
                    }
                    this.lastElementType = element.type();
                    return true;
                case 55:
                    zh = (DrawInterface) element;
                    zh.draw(this.graphics, indentLeft(), indentBottom(), indentRight(), indentTop(), indentTop() - this.currentHeight - ((this.leadingStack.size() > 0) ? this.leading : 0.0F));
                    this.pageEmpty = false;
                    this.lastElementType = element.type();
                    return true;
                case 50:
                    if (element instanceof MarkedSection) {
                        MarkedObject markedObject = ((MarkedSection) element).getTitle();
                        if (markedObject != null) {
                            markedObject.process((ElementListener) this);
                        }
                    }
                    mo = (MarkedObject) element;
                    mo.process((ElementListener) this);
                    this.lastElementType = element.type();
                    return true;
                case 666:
                    if (null != this.writer) {
                        ((WriterOperation) element).write(this.writer, this);
                    }
                    this.lastElementType = element.type();
                    return true;
                case 37:
                    ensureNewLine();
                    flushLines();
                    addDiv((PdfDiv) element);
                    this.pageEmpty = false;
                    this.lastElementType = element.type();
                    return true;
                case 38:
                    this.body = (PdfBody) element;
                    this.graphics.rectangle(this.body);
                    break;
            }
            return false;
        } catch (Exception e) {
            throw new DocumentException(e);
        }
    }

    public void open() {
        if (!this.open) {
            super.open();
            this.writer.open();
            this.rootOutline = new PdfOutline(this.writer);
            this.currentOutline = this.rootOutline;
        }
        try {
            if (isTagged(this.writer)) {
                this.openMCDocument = true;
            }
            initPage();
        } catch (DocumentException de) {
            throw new ExceptionConverter(de);
        }
    }

    public void close() {
        if (this.close) {
            return;
        }
        try {
            if (isTagged(this.writer)) {
                flushFloatingElements();
                flushLines();
                this.writer.flushAcroFields();
                this.writer.flushTaggedObjects();
                if (isPageEmpty()) {
                    int pageReferenceCount = this.writer.pageReferences.size();
                    if (pageReferenceCount > 0 && this.writer.currentPageNumber == pageReferenceCount) {
                        this.writer.pageReferences.remove(pageReferenceCount - 1);
                    }
                }
            } else {
                this.writer.flushAcroFields();
            }
            if (this.imageWait != null) {
                newPage();
            }
            endPage();
            if (isTagged(this.writer)) {
                this.writer.getDirectContent().closeMCBlock((IAccessibleElement) this);
            }
            if (this.annotationsImp.hasUnusedAnnotations()) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("not.all.annotations.could.be.added.to.the.document.the.document.doesn.t.have.enough.pages", new Object[0]));
            }
            PdfPageEvent pageEvent = this.writer.getPageEvent();
            if (pageEvent != null) {
                pageEvent.onCloseDocument(this.writer, this);
            }
            super.close();
            this.writer.addLocalDestinations(this.localDestinations);
            calculateOutlineCount();
            writeOutlines();
        } catch (Exception e) {
            throw ExceptionConverter.convertException(e);
        }
        this.writer.close();
    }

    public void setXmpMetadata(byte[] xmpMetadata) throws IOException {
        PdfStream xmp = new PdfStream(xmpMetadata);
        xmp.put(PdfName.TYPE, PdfName.METADATA);
        xmp.put(PdfName.SUBTYPE, PdfName.XML);
        PdfEncryption crypto = this.writer.getEncryption();
        if (crypto != null && !crypto.isMetadataEncrypted()) {
            PdfArray ar = new PdfArray();
            ar.add(PdfName.CRYPT);
            xmp.put(PdfName.FILTER, ar);
        }
        this.writer.addPageDictEntry(PdfName.METADATA, this.writer.addToBody(xmp).getIndirectReference());
    }

    public boolean newPage() {
        if (isPageEmpty()) {
            setNewPageSizeAndMargins();
            return false;
        }
        if (!this.open || this.close) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("the.document.is.not.open", new Object[0]));
        }
        ArrayList<IAccessibleElement> savedMcBlocks = endPage();
        super.newPage();
        this.indentation.imageIndentLeft = 0.0F;
        this.indentation.imageIndentRight = 0.0F;
        try {
            if (isTagged(this.writer)) {
                flushStructureElementsOnNewPage();
                this.writer.getDirectContentUnder().restoreMCBlocks(savedMcBlocks);
            }
            initPage();
            if (this.body != null && this.body.getBackgroundColor() != null) {
                this.graphics.rectangle(this.body);
            }
        } catch (DocumentException de) {
            throw new ExceptionConverter(de);
        }
        return true;
    }

    protected ArrayList<IAccessibleElement> endPage() {
        if (isPageEmpty()) {
            return null;
        }
        ArrayList<IAccessibleElement> savedMcBlocks = null;
        try {
            flushFloatingElements();
        } catch (DocumentException de) {
            throw new ExceptionConverter(de);
        }
        this.lastElementType = -1;
        PdfPageEvent pageEvent = this.writer.getPageEvent();
        if (pageEvent != null) {
            pageEvent.onEndPage(this.writer, this);
        }
        try {
            flushLines();
            int rotation = this.pageSize.getRotation();
            if (this.writer.isPdfIso()) {
                if (this.thisBoxSize.containsKey("art") && this.thisBoxSize.containsKey("trim")) {
                    throw new PdfXConformanceException(MessageLocalization.getComposedMessage("only.one.of.artbox.or.trimbox.can.exist.in.the.page", new Object[0]));
                }
                if (!this.thisBoxSize.containsKey("art") && !this.thisBoxSize.containsKey("trim")) {
                    if (this.thisBoxSize.containsKey("crop")) {
                        this.thisBoxSize.put("trim", this.thisBoxSize.get("crop"));
                    } else {
                        this.thisBoxSize.put("trim", new PdfRectangle(this.pageSize, this.pageSize.getRotation()));
                    }
                }
            }
            this.pageResources.addDefaultColorDiff(this.writer.getDefaultColorspace());
            if (this.writer.isRgbTransparencyBlending()) {
                PdfDictionary dcs = new PdfDictionary();
                dcs.put(PdfName.CS, PdfName.DEVICERGB);
                this.pageResources.addDefaultColorDiff(dcs);
            }
            PdfDictionary resources = this.pageResources.getResources();
            PdfPage page = new PdfPage(new PdfRectangle(this.pageSize, rotation), this.thisBoxSize, resources, rotation);
            if (isTagged(this.writer)) {
                page.put(PdfName.TABS, PdfName.S);
            } else {
                page.put(PdfName.TABS, this.writer.getTabs());
            }
            page.putAll(this.writer.getPageDictEntries());
            this.writer.resetPageDictEntries();
            if (this.pageAA != null) {
                page.put(PdfName.AA, this.writer.addToBody(this.pageAA).getIndirectReference());
                this.pageAA = null;
            }
            if (this.annotationsImp.hasUnusedAnnotations()) {
                PdfArray array = this.annotationsImp.rotateAnnotations(this.writer, this.pageSize);
                if (array.size() != 0) {
                    page.put(PdfName.ANNOTS, array);
                }
            }
            if (isTagged(this.writer)) {
                page.put(PdfName.STRUCTPARENTS, new PdfNumber(getStructParentIndex(this.writer.getCurrentPage())));
            }
            if (this.text.size() > this.textEmptySize || isTagged(this.writer)) {
                this.text.endText();
            } else {
                this.text = null;
            }
            if (isTagged(this.writer)) {
                savedMcBlocks = this.writer.getDirectContent().saveMCBlocks();
            }
            this.writer.add(page, new PdfContents(this.writer.getDirectContentUnder(), this.graphics, !isTagged(this.writer) ? this.text : null, this.writer.getDirectContent(), this.pageSize));
            this.annotationsImp.resetAnnotations();
            this.writer.resetContent();
        } catch (DocumentException de) {
            throw new ExceptionConverter(de);
        } catch (IOException ioe) {
            throw new ExceptionConverter(ioe);
        }
        return savedMcBlocks;
    }

    public boolean setPageSize(Rectangle pageSize) {
        if (this.writer != null && this.writer.isPaused()) {
            return false;
        }
        this.nextPageSize = new Rectangle(pageSize);
        return true;
    }

    public boolean setMargins(float marginLeft, float marginRight, float marginTop, float marginBottom) {
        if (this.writer != null && this.writer.isPaused()) {
            return false;
        }
        this.nextMarginLeft = marginLeft;
        this.nextMarginRight = marginRight;
        this.nextMarginTop = marginTop;
        this.nextMarginBottom = marginBottom;
        return true;
    }

    public boolean setMarginMirroring(boolean MarginMirroring) {
        if (this.writer != null && this.writer.isPaused()) {
            return false;
        }
        return super.setMarginMirroring(MarginMirroring);
    }

    public boolean setMarginMirroringTopBottom(boolean MarginMirroringTopBottom) {
        if (this.writer != null && this.writer.isPaused()) {
            return false;
        }
        return super.setMarginMirroringTopBottom(MarginMirroringTopBottom);
    }

    public void setPageCount(int pageN) {
        if (this.writer != null && this.writer.isPaused()) {
            return;
        }
        super.setPageCount(pageN);
    }

    public void resetPageCount() {
        if (this.writer != null && this.writer.isPaused()) {
            return;
        }
        super.resetPageCount();
    }

    protected void initPage() throws DocumentException {
        this.pageN++;
        this.pageResources = new PageResources();
        if (isTagged(this.writer)) {
            this.graphics = this.writer.getDirectContentUnder().getDuplicate();
            (this.writer.getDirectContent()).duplicatedFrom = this.graphics;
        } else {
            this.graphics = new PdfContentByte(this.writer);
        }
        setNewPageSizeAndMargins();
        this.imageEnd = -1.0F;
        this.indentation.imageIndentRight = 0.0F;
        this.indentation.imageIndentLeft = 0.0F;
        this.indentation.indentBottom = 0.0F;
        this.indentation.indentTop = 0.0F;
        this.currentHeight = 0.0F;
        this.thisBoxSize = new HashMap<String, PdfRectangle>(this.boxSize);
        if (this.pageSize.getBackgroundColor() != null || this.pageSize.hasBorders() || this.pageSize.getBorderColor() != null) {
            add((Element) this.pageSize);
        }
        float oldleading = this.leading;
        int oldAlignment = this.alignment;
        this.pageEmpty = true;
        try {
            if (this.imageWait != null) {
                add(this.imageWait);
                this.imageWait = null;
            }
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
        this.leading = oldleading;
        this.alignment = oldAlignment;
        carriageReturn();
        PdfPageEvent pageEvent = this.writer.getPageEvent();
        if (pageEvent != null) {
            if (this.firstPageEvent) {
                pageEvent.onOpenDocument(this.writer, this);
            }
            pageEvent.onStartPage(this.writer, this);
        }
        this.firstPageEvent = false;
    }

    protected void newLine() throws DocumentException {
        this.lastElementType = -1;
        carriageReturn();
        if (this.lines != null && !this.lines.isEmpty()) {
            this.lines.add(this.line);
            this.currentHeight += this.line.height();
        }
        this.line = new PdfLine(indentLeft(), indentRight(), this.alignment, this.leading);
    }

    protected float calculateLineHeight() {
        float tempHeight = this.line.height();
        if (tempHeight != this.leading) {
            tempHeight += this.leading;
        }
        return tempHeight;
    }

    protected void carriageReturn() {
        if (this.lines == null) {
            this.lines = new ArrayList<PdfLine>();
        }
        if (this.line != null && this.line.size() > 0) {
            if (this.currentHeight + calculateLineHeight() > indentTop() - indentBottom()) {
                if (this.currentHeight != 0.0F) {
                    PdfLine overflowLine = this.line;
                    this.line = null;
                    newPage();
                    this.line = overflowLine;
                    overflowLine.left = indentLeft();
                }
            }
            this.currentHeight += this.line.height();
            this.lines.add(this.line);
            this.pageEmpty = false;
        }
        if (this.imageEnd > -1.0F && this.currentHeight > this.imageEnd) {
            this.imageEnd = -1.0F;
            this.indentation.imageIndentRight = 0.0F;
            this.indentation.imageIndentLeft = 0.0F;
        }
        this.line = new PdfLine(indentLeft(), indentRight(), this.alignment, this.leading);
    }

    public float getVerticalPosition(boolean ensureNewLine) {
        if (ensureNewLine) {
            ensureNewLine();
        }
        return top() - this.currentHeight - this.indentation.indentTop;
    }

    protected void ensureNewLine() {
        try {
            if (this.lastElementType == 11 || this.lastElementType == 10) {
                newLine();
                flushLines();
            }
        } catch (DocumentException ex) {
            throw new ExceptionConverter(ex);
        }
    }

    protected float flushLines() throws DocumentException {
        if (this.lines == null) {
            return 0.0F;
        }
        if (this.line != null && this.line.size() > 0) {
            this.lines.add(this.line);
            this.line = new PdfLine(indentLeft(), indentRight(), this.alignment, this.leading);
        }
        if (this.lines.isEmpty()) {
            return 0.0F;
        }
        Object[] currentValues = new Object[2];
        PdfFont currentFont = null;
        float displacement = 0.0F;
        Float lastBaseFactor = new Float(0.0F);
        currentValues[1] = lastBaseFactor;
        for (PdfLine l : this.lines) {
            float moveTextX = l.indentLeft() - indentLeft() + this.indentation.indentLeft + this.indentation.listIndentLeft + this.indentation.sectionIndentLeft;
            this.text.moveText(moveTextX, -l.height());
            l.flush();
            if (l.listSymbol() != null) {
                ListLabel lbl = null;
                Chunk symbol = l.listSymbol();
                if (isTagged(this.writer)) {
                    lbl = l.listItem().getListLabel();
                    this.graphics.openMCBlock((IAccessibleElement) lbl);
                    symbol = new Chunk(symbol);
                    symbol.setRole(null);
                }
                ColumnText.showTextAligned(this.graphics, 0, new Phrase(symbol), this.text.getXTLM() - l.listIndent(), this.text.getYTLM(), 0.0F);
                if (lbl != null) {
                    this.graphics.closeMCBlock((IAccessibleElement) lbl);
                }
            }
            currentValues[0] = currentFont;
            if (isTagged(this.writer) && l.listItem() != null) {
                this.text.openMCBlock((IAccessibleElement) l.listItem().getListBody());
            }
            writeLineToContent(l, this.text, this.graphics, currentValues, this.writer.getSpaceCharRatio());
            currentFont = (PdfFont) currentValues[0];
            displacement += l.height();
            this.text.moveText(-moveTextX, 0.0F);
        }
        this.lines = new ArrayList<PdfLine>();
        return displacement;
    }

    float writeLineToContent(PdfLine line, PdfContentByte text, PdfContentByte graphics, Object[] currentValues, float ratio) throws DocumentException {
        PdfFont currentFont = (PdfFont) currentValues[0];
        float lastBaseFactor = ((Float) currentValues[1]).floatValue();
        float hangingCorrection = 0.0F;
        float hScale = 1.0F;
        float lastHScale = Float.NaN;
        float baseWordSpacing = 0.0F;
        float baseCharacterSpacing = 0.0F;
        float glueWidth = 0.0F;
        float lastX = text.getXTLM() + line.getOriginalWidth();
        int numberOfSpaces = line.numberOfSpaces();
        int lineLen = line.getLineLengthUtf32();
        boolean isJustified = (line.hasToBeJustified() && (numberOfSpaces != 0 || lineLen > 1));
        int separatorCount = line.getSeparatorCount();
        if (separatorCount > 0) {
            glueWidth = line.widthLeft() / separatorCount;
        } else if (isJustified && separatorCount == 0) {
            if (line.isNewlineSplit() && line.widthLeft() >= lastBaseFactor * (ratio * numberOfSpaces + lineLen - 1.0F)) {
                if (line.isRTL()) {
                    text.moveText(line.widthLeft() - lastBaseFactor * (ratio * numberOfSpaces + lineLen - 1.0F), 0.0F);
                }
                baseWordSpacing = ratio * lastBaseFactor;
                baseCharacterSpacing = lastBaseFactor;
            } else {
                float width = line.widthLeft();
                PdfChunk last = line.getChunk(line.size() - 1);
                if (last != null) {
                    String s = last.toString();
                    char c;
                    if (s.length() > 0 && ".,;:'".indexOf(c = s.charAt(s.length() - 1)) >= 0) {
                        float oldWidth = width;
                        width += last.font().width(c) * 0.4F;
                        hangingCorrection = width - oldWidth;
                    }
                }
                float baseFactor = width / (ratio * numberOfSpaces + lineLen - 1.0F);
                baseWordSpacing = ratio * baseFactor;
                baseCharacterSpacing = baseFactor;
                lastBaseFactor = baseFactor;
            }
        } else if (line.alignment == 0 || line.alignment == -1) {
            lastX -= line.widthLeft();
        }
        int lastChunkStroke = line.getLastStrokeChunk();
        int chunkStrokeIdx = 0;
        float xMarker = text.getXTLM();
        float baseXMarker = xMarker;
        float yMarker = text.getYTLM();
        boolean adjustMatrix = false;
        float tabPosition = 0.0F;
        boolean isMCBlockOpened = false;
        for (Iterator<PdfChunk> j = line.iterator(); j.hasNext();) {
            float ascender, descender;
            PdfChunk chunk = j.next();
            if (isTagged(this.writer) && chunk.accessibleElement != null) {
                text.openMCBlock(chunk.accessibleElement);
                isMCBlockOpened = true;
            }
            BaseColor color = chunk.color();
            float fontSize = chunk.font().size();
            if (chunk.isImage()) {
                ascender = chunk.height();
                fontSize = chunk.height();
                descender = 0.0F;
            } else {
                ascender = chunk.font().getFont().getFontDescriptor(1, fontSize);
                descender = chunk.font().getFont().getFontDescriptor(3, fontSize);
            }
            hScale = 1.0F;
            if (chunkStrokeIdx <= lastChunkStroke) {
                float f;
                if (isJustified) {
                    f = chunk.getWidthCorrected(baseCharacterSpacing, baseWordSpacing);
                } else {
                    f = chunk.width();
                }
                if (chunk.isStroked()) {
                    PdfChunk nextChunk = line.getChunk(chunkStrokeIdx + 1);
                    if (chunk.isSeparator()) {
                        f = glueWidth;
                        Object[] sep = (Object[]) chunk.getAttribute("SEPARATOR");
                        DrawInterface di = (DrawInterface) sep[0];
                        Boolean vertical = (Boolean) sep[1];
                        if (vertical.booleanValue()) {
                            di.draw(graphics, baseXMarker, yMarker + descender, baseXMarker + line.getOriginalWidth(), ascender - descender, yMarker);
                        } else {
                            di.draw(graphics, xMarker, yMarker + descender, xMarker + f, ascender - descender, yMarker);
                        }
                    }
                    if (chunk.isTab()) {
                        if (chunk.isAttribute("TABSETTINGS")) {
                            TabStop tabStop = chunk.getTabStop();
                            if (tabStop != null) {
                                tabPosition = tabStop.getPosition() + baseXMarker;
                                if (tabStop.getLeader() != null) {
                                    tabStop.getLeader().draw(graphics, xMarker, yMarker + descender, tabPosition, ascender - descender, yMarker);
                                }
                            } else {
                                tabPosition = xMarker;
                            }
                        } else {
                            Object[] tab = (Object[]) chunk.getAttribute("TAB");
                            DrawInterface di = (DrawInterface) tab[0];
                            tabPosition = ((Float) tab[1]).floatValue() + ((Float) tab[3]).floatValue();
                            if (tabPosition > xMarker) {
                                di.draw(graphics, xMarker, yMarker + descender, tabPosition, ascender - descender, yMarker);
                            }
                        }
                        float tmp = xMarker;
                        xMarker = tabPosition;
                        tabPosition = tmp;
                    }
                    if (chunk.isAttribute("BACKGROUND")) {
                        Object[] bgr = (Object[]) chunk.getAttribute("BACKGROUND");
                        if (bgr[0] != null) {
                            boolean inText = graphics.getInText();
                            if (inText && isTagged(this.writer)) {
                                graphics.endText();
                            }
                            graphics.saveState();
                            float subtract = lastBaseFactor;
                            if (nextChunk != null && nextChunk.isAttribute("BACKGROUND")) {
                                subtract = 0.0F;
                            }
                            if (nextChunk == null) {
                                subtract += hangingCorrection;
                            }
                            BaseColor c = (BaseColor) bgr[0];
                            graphics.setColorFill(c);
                            float[] extra = (float[]) bgr[1];
                            graphics.rectangle(xMarker - extra[0], yMarker + descender - extra[1] + chunk.getTextRise(), f - subtract + extra[0] + extra[2], ascender - descender + extra[1] + extra[3]);
                            graphics.fill();
                            graphics.setGrayFill(0.0F);
                            graphics.restoreState();
                            if (inText && isTagged(this.writer)) {
                                graphics.beginText(true);
                            }
                        }
                    }
                    if (chunk.isAttribute("UNDERLINE")) {
                        boolean inText = graphics.getInText();
                        if (inText && isTagged(this.writer)) {
                            graphics.endText();
                        }
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute("UNDERLINE")) {
                            subtract = 0.0F;
                        }
                        if (nextChunk == null) {
                            subtract += hangingCorrection;
                        }
                        Object[][] unders = (Object[][]) chunk.getAttribute("UNDERLINE");
                        BaseColor scolor = null;
                        for (int k = 0; k < unders.length; k++) {
                            Object[] obj = unders[k];
                            scolor = (BaseColor) obj[0];
                            float[] ps = (float[]) obj[1];
                            if (scolor == null) {
                                scolor = color;
                            }
                            if (scolor != null) {
                                graphics.setColorStroke(scolor);
                            }
                            graphics.setLineWidth(ps[0] + chunk.font().size() * ps[1]);
                            float shift = ps[2] + chunk.font().size() * ps[3];
                            int cap2 = (int) ps[4];
                            if (cap2 != 0) {
                                graphics.setLineCap(cap2);
                            }
                            graphics.moveTo(xMarker, yMarker + shift);
                            graphics.lineTo(xMarker + f - subtract, yMarker + shift);
                            graphics.stroke();
                            if (scolor != null) {
                                graphics.resetGrayStroke();
                            }
                            if (cap2 != 0) {
                                graphics.setLineCap(0);
                            }
                        }
                        graphics.setLineWidth(1.0F);
                        if (inText && isTagged(this.writer)) {
                            graphics.beginText(true);
                        }
                    }
                    if (chunk.isAttribute("ACTION")) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute("ACTION")) {
                            subtract = 0.0F;
                        }
                        if (nextChunk == null) {
                            subtract += hangingCorrection;
                        }
                        PdfAnnotation annot = null;
                        if (chunk.isImage()) {
                            annot = this.writer.createAnnotation(xMarker, yMarker + chunk.getImageOffsetY(), xMarker + f - subtract, yMarker + chunk.getImageHeight() + chunk.getImageOffsetY(), (PdfAction) chunk.getAttribute("ACTION"), (PdfName) null);
                        } else {
                            annot = this.writer.createAnnotation(xMarker, yMarker + descender + chunk.getTextRise(), xMarker + f - subtract, yMarker + ascender + chunk.getTextRise(), (PdfAction) chunk.getAttribute("ACTION"), (PdfName) null);
                        }
                        text.addAnnotation(annot, true);
                        if (isTagged(this.writer) && chunk.accessibleElement != null) {
                            PdfStructureElement strucElem = getStructElement(chunk.accessibleElement.getId());
                            if (strucElem != null) {
                                int structParent = getStructParentIndex(annot);
                                annot.put(PdfName.STRUCTPARENT, new PdfNumber(structParent));
                                strucElem.setAnnotation(annot, this.writer.getCurrentPage());
                                this.writer.getStructureTreeRoot().setAnnotationMark(structParent, strucElem.getReference());
                            }
                        }
                    }
                    if (chunk.isAttribute("REMOTEGOTO")) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute("REMOTEGOTO")) {
                            subtract = 0.0F;
                        }
                        if (nextChunk == null) {
                            subtract += hangingCorrection;
                        }
                        Object[] obj = (Object[]) chunk.getAttribute("REMOTEGOTO");
                        String filename = (String) obj[0];
                        if (obj[1] instanceof String) {
                            remoteGoto(filename, (String) obj[1], xMarker, yMarker + descender + chunk.getTextRise(), xMarker + f - subtract, yMarker + ascender + chunk.getTextRise());
                        } else {
                            remoteGoto(filename, ((Integer) obj[1]).intValue(), xMarker, yMarker + descender + chunk.getTextRise(), xMarker + f - subtract, yMarker + ascender + chunk.getTextRise());
                        }
                    }
                    if (chunk.isAttribute("LOCALGOTO")) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute("LOCALGOTO")) {
                            subtract = 0.0F;
                        }
                        if (nextChunk == null) {
                            subtract += hangingCorrection;
                        }
                        localGoto((String) chunk.getAttribute("LOCALGOTO"), xMarker, yMarker, xMarker + f - subtract, yMarker + fontSize);
                    }
                    if (chunk.isAttribute("LOCALDESTINATION")) {
                        localDestination((String) chunk.getAttribute("LOCALDESTINATION"), new PdfDestination(0, xMarker, yMarker + fontSize, 0.0F));
                    }
                    if (chunk.isAttribute("GENERICTAG")) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute("GENERICTAG")) {
                            subtract = 0.0F;
                        }
                        if (nextChunk == null) {
                            subtract += hangingCorrection;
                        }
                        Rectangle rect = new Rectangle(xMarker, yMarker, xMarker + f - subtract, yMarker + fontSize);
                        PdfPageEvent pev = this.writer.getPageEvent();
                        if (pev != null) {
                            pev.onGenericTag(this.writer, this, rect, (String) chunk.getAttribute("GENERICTAG"));
                        }
                    }
                    if (chunk.isAttribute("PDFANNOTATION")) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute("PDFANNOTATION")) {
                            subtract = 0.0F;
                        }
                        if (nextChunk == null) {
                            subtract += hangingCorrection;
                        }
                        PdfAnnotation annot = PdfFormField.shallowDuplicate((PdfAnnotation) chunk.getAttribute("PDFANNOTATION"));
                        annot.put(PdfName.RECT, new PdfRectangle(xMarker, yMarker + descender, xMarker + f - subtract, yMarker + ascender));
                        text.addAnnotation(annot, true);
                    }
                    float[] params = (float[]) chunk.getAttribute("SKEW");
                    Float hs = (Float) chunk.getAttribute("HSCALE");
                    if (params != null || hs != null) {
                        float b = 0.0F, c = 0.0F;
                        if (params != null) {
                            b = params[0];
                            c = params[1];
                        }
                        if (hs != null) {
                            hScale = hs.floatValue();
                        }
                        text.setTextMatrix(hScale, b, c, 1.0F, xMarker, yMarker);
                    }
                    if (!isJustified) {
                        if (chunk.isAttribute("WORD_SPACING")) {
                            Float ws = (Float) chunk.getAttribute("WORD_SPACING");
                            text.setWordSpacing(ws.floatValue());
                        }
                        if (chunk.isAttribute("CHAR_SPACING")) {
                            Float cs = (Float) chunk.getAttribute("CHAR_SPACING");
                            text.setCharacterSpacing(cs.floatValue());
                        }
                    }
                    if (chunk.isImage()) {
                        Image image = chunk.getImage();
                        f = chunk.getImageWidth();
                        float[] matrix = image.matrix(chunk.getImageScalePercentage());
                        matrix[4] = xMarker + chunk.getImageOffsetX() - matrix[4];
                        matrix[5] = yMarker + chunk.getImageOffsetY() - matrix[5];
                        boolean wasIntext = false;
                        if (graphics.getInText() && !(image instanceof esign.text.ImgTemplate)) {
                            wasIntext = true;
                            graphics.endText();
                        }
                        graphics.addImage(image, matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5], false, isMCBlockOpened);
                        if (wasIntext) {
                            graphics.beginText(true);
                        }
                        text.moveText(xMarker + lastBaseFactor + chunk.getImageWidth() - text.getXTLM(), 0.0F);
                    }
                }
                xMarker += f;
                chunkStrokeIdx++;
            }
            if (!chunk.isImage() && chunk.font().compareTo(currentFont) != 0) {
                currentFont = chunk.font();
                text.setFontAndSize(currentFont.getFont(), currentFont.size());
            }
            float rise = 0.0F;
            Object[] textRender = (Object[]) chunk.getAttribute("TEXTRENDERMODE");
            int tr = 0;
            float strokeWidth = 1.0F;
            BaseColor strokeColor = null;
            Float fr = (Float) chunk.getAttribute("SUBSUPSCRIPT");
            if (textRender != null) {
                tr = ((Integer) textRender[0]).intValue() & 0x3;
                if (tr != 0) {
                    text.setTextRenderingMode(tr);
                }
                if (tr == 1 || tr == 2) {
                    strokeWidth = ((Float) textRender[1]).floatValue();
                    if (strokeWidth != 1.0F) {
                        text.setLineWidth(strokeWidth);
                    }
                    strokeColor = (BaseColor) textRender[2];
                    if (strokeColor == null) {
                        strokeColor = color;
                    }
                    if (strokeColor != null) {
                        text.setColorStroke(strokeColor);
                    }
                }
            }
            if (fr != null) {
                rise = fr.floatValue();
            }
            if (color != null) {
                text.setColorFill(color);
            }
            if (rise != 0.0F) {
                text.setTextRise(rise);
            }
            if (chunk.isImage()) {
                adjustMatrix = true;
            } else if (chunk.isHorizontalSeparator()) {
                PdfTextArray array = new PdfTextArray();
                array.add(-glueWidth * 1000.0F / chunk.font.size() / hScale);
                text.showText(array);
            } else if (chunk.isTab() && tabPosition != xMarker) {
                PdfTextArray array = new PdfTextArray();
                array.add((tabPosition - xMarker) * 1000.0F / chunk.font.size() / hScale);
                text.showText(array);
            } else if (isJustified && numberOfSpaces > 0 && chunk.isSpecialEncoding()) {
                if (hScale != lastHScale) {
                    lastHScale = hScale;
                    text.setWordSpacing(baseWordSpacing / hScale);
                    text.setCharacterSpacing(baseCharacterSpacing / hScale + text.getCharacterSpacing());
                }
                String s = chunk.toString();
                int idx = s.indexOf(' ');
                if (idx < 0) {
                    text.showText(s);
                } else {
                    float spaceCorrection = -baseWordSpacing * 1000.0F / chunk.font.size() / hScale;
                    PdfTextArray textArray = new PdfTextArray(s.substring(0, idx));
                    int lastIdx = idx;
                    while ((idx = s.indexOf(' ', lastIdx + 1)) >= 0) {
                        textArray.add(spaceCorrection);
                        textArray.add(s.substring(lastIdx, idx));
                        lastIdx = idx;
                    }
                    textArray.add(spaceCorrection);
                    textArray.add(s.substring(lastIdx));
                    text.showText(textArray);
                }
            } else {
                if (isJustified && hScale != lastHScale) {
                    lastHScale = hScale;
                    text.setWordSpacing(baseWordSpacing / hScale);
                    text.setCharacterSpacing(baseCharacterSpacing / hScale + text.getCharacterSpacing());
                }
                text.showText(chunk.toString());
            }
            if (rise != 0.0F) {
                text.setTextRise(0.0F);
            }
            if (color != null) {
                text.resetRGBColorFill();
            }
            if (tr != 0) {
                text.setTextRenderingMode(0);
            }
            if (strokeColor != null) {
                text.resetRGBColorStroke();
            }
            if (strokeWidth != 1.0F) {
                text.setLineWidth(1.0F);
            }
            if (chunk.isAttribute("SKEW") || chunk.isAttribute("HSCALE")) {
                adjustMatrix = true;
                text.setTextMatrix(xMarker, yMarker);
            }
            if (!isJustified) {
                if (chunk.isAttribute("CHAR_SPACING")) {
                    text.setCharacterSpacing(baseCharacterSpacing);
                }
                if (chunk.isAttribute("WORD_SPACING")) {
                    text.setWordSpacing(baseWordSpacing);
                }
            }
            if (isTagged(this.writer) && chunk.accessibleElement != null) {
                text.closeMCBlock(chunk.accessibleElement);
            }
        }
        if (isJustified) {
            text.setWordSpacing(0.0F);
            text.setCharacterSpacing(0.0F);
            if (line.isNewlineSplit()) {
                lastBaseFactor = 0.0F;
            }
        }
        if (adjustMatrix) {
            text.moveText(baseXMarker - text.getXTLM(), 0.0F);
        }
        currentValues[0] = currentFont;
        currentValues[1] = new Float(lastBaseFactor);
        return lastX;
    }

    public static class Indentation {

        float indentLeft = 0.0F;
        float sectionIndentLeft = 0.0F;
        float listIndentLeft = 0.0F;
        float imageIndentLeft = 0.0F;
        float indentRight = 0.0F;
        float sectionIndentRight = 0.0F;
        float imageIndentRight = 0.0F;
        float indentTop = 0.0F;
        float indentBottom = 0.0F;
    }

    protected float indentLeft() {
        return left(this.indentation.indentLeft + this.indentation.listIndentLeft + this.indentation.imageIndentLeft + this.indentation.sectionIndentLeft);
    }

    protected float indentRight() {
        return right(this.indentation.indentRight + this.indentation.sectionIndentRight + this.indentation.imageIndentRight);
    }

    protected float indentTop() {
        return top(this.indentation.indentTop);
    }

    float indentBottom() {
        return bottom(this.indentation.indentBottom);
    }

    protected void addSpacing(float extraspace, float oldleading, Font f) {
        addSpacing(extraspace, oldleading, f, false);
    }

    protected void addSpacing(float extraspace, float oldleading, Font f, boolean spacingAfter) {
        if (extraspace == 0.0F) {
            return;
        }
        if (this.pageEmpty) {
            return;
        }
        float height = spacingAfter ? extraspace : calculateLineHeight();
        if (this.currentHeight + height > indentTop() - indentBottom()) {
            newPage();
            return;
        }
        this.leading = extraspace;
        carriageReturn();
        if (f.isUnderlined() || f.isStrikethru()) {
            f = new Font(f);
            int style = f.getStyle();
            style &= 0xFFFFFFFB;
            style &= 0xFFFFFFF7;
            f.setStyle(style);
        }
        Chunk space = new Chunk(" ", f);
        if (spacingAfter && this.pageEmpty) {
            space = new Chunk("", f);
        }
        space.process((ElementListener) this);
        carriageReturn();
        this.leading = oldleading;
    }

    PdfInfo getInfo() {
        return this.info;
    }

    PdfCatalog getCatalog(PdfIndirectReference pages) {
        PdfCatalog catalog = new PdfCatalog(pages, this.writer);
        if (this.rootOutline.getKids().size() > 0) {
            catalog.put(PdfName.PAGEMODE, PdfName.USEOUTLINES);
            catalog.put(PdfName.OUTLINES, this.rootOutline.indirectReference());
        }
        this.writer.getPdfVersion().addToCatalog(catalog);
        this.viewerPreferences.addToCatalog(catalog);
        if (this.pageLabels != null) {
            catalog.put(PdfName.PAGELABELS, this.pageLabels.getDictionary(this.writer));
        }
        catalog.addNames(this.localDestinations, getDocumentLevelJS(), this.documentFileAttachment, this.writer);
        if (this.openActionName != null) {
            PdfAction action = getLocalGotoAction(this.openActionName);
            catalog.setOpenAction(action);
        } else if (this.openActionAction != null) {
            catalog.setOpenAction(this.openActionAction);
        }
        if (this.additionalActions != null) {
            catalog.setAdditionalActions(this.additionalActions);
        }
        if (this.collection != null) {
            catalog.put(PdfName.COLLECTION, (PdfObject) this.collection);
        }
        if (this.annotationsImp.hasValidAcroForm()) {
            try {
                catalog.put(PdfName.ACROFORM, this.writer.addToBody(this.annotationsImp.getAcroForm()).getIndirectReference());
            } catch (IOException e) {
                throw new ExceptionConverter(e);
            }
        }
        if (this.language != null) {
            catalog.put(PdfName.LANG, this.language);
        }
        return catalog;
    }

    void addOutline(PdfOutline outline, String name) {
        localDestination(name, outline.getPdfDestination());
    }

    public PdfOutline getRootOutline() {
        return this.rootOutline;
    }

    void calculateOutlineCount() {
        if (this.rootOutline.getKids().size() == 0) {
            return;
        }
        traverseOutlineCount(this.rootOutline);
    }

    void traverseOutlineCount(PdfOutline outline) {
        ArrayList<PdfOutline> kids = outline.getKids();
        PdfOutline parent = outline.parent();
        if (kids.isEmpty()) {
            if (parent != null) {
                parent.setCount(parent.getCount() + 1);
            }
        } else {
            for (int k = 0; k < kids.size(); k++) {
                traverseOutlineCount(kids.get(k));
            }
            if (parent != null) {
                if (outline.isOpen()) {
                    parent.setCount(outline.getCount() + parent.getCount() + 1);
                } else {
                    parent.setCount(parent.getCount() + 1);
                    outline.setCount(-outline.getCount());
                }
            }
        }
    }

    void writeOutlines() throws IOException {
        if (this.rootOutline.getKids().size() == 0) {
            return;
        }
        outlineTree(this.rootOutline);
        this.writer.addToBody(this.rootOutline, this.rootOutline.indirectReference());
    }

    void outlineTree(PdfOutline outline) throws IOException {
        outline.setIndirectReference(this.writer.getPdfIndirectReference());
        if (outline.parent() != null) {
            outline.put(PdfName.PARENT, outline.parent().indirectReference());
        }
        ArrayList<PdfOutline> kids = outline.getKids();
        int size = kids.size();
        int k;
        for (k = 0; k < size; k++) {
            outlineTree(kids.get(k));
        }
        for (k = 0; k < size; k++) {
            if (k > 0) {
                ((PdfOutline) kids.get(k)).put(PdfName.PREV, ((PdfOutline) kids.get(k - 1)).indirectReference());
            }
            if (k < size - 1) {
                ((PdfOutline) kids.get(k)).put(PdfName.NEXT, ((PdfOutline) kids.get(k + 1)).indirectReference());
            }
        }
        if (size > 0) {
            outline.put(PdfName.FIRST, ((PdfOutline) kids.get(0)).indirectReference());
            outline.put(PdfName.LAST, ((PdfOutline) kids.get(size - 1)).indirectReference());
        }
        for (k = 0; k < size; k++) {
            PdfOutline kid = kids.get(k);
            this.writer.addToBody(kid, kid.indirectReference());
        }
    }

    void setViewerPreferences(int preferences) {
        this.viewerPreferences.setViewerPreferences(preferences);
    }

    void addViewerPreference(PdfName key, PdfObject value) {
        this.viewerPreferences.addViewerPreference(key, value);
    }

    void setPageLabels(PdfPageLabels pageLabels) {
        this.pageLabels = pageLabels;
    }

    public PdfPageLabels getPageLabels() {
        return this.pageLabels;
    }

    void localGoto(String name, float llx, float lly, float urx, float ury) {
        PdfAction action = getLocalGotoAction(name);
        this.annotationsImp.addPlainAnnotation(this.writer.createAnnotation(llx, lly, urx, ury, action, (PdfName) null));
    }

    void remoteGoto(String filename, String name, float llx, float lly, float urx, float ury) {
        this.annotationsImp.addPlainAnnotation(this.writer.createAnnotation(llx, lly, urx, ury, new PdfAction(filename, name), (PdfName) null));
    }

    void remoteGoto(String filename, int page, float llx, float lly, float urx, float ury) {
        addAnnotation(this.writer.createAnnotation(llx, lly, urx, ury, new PdfAction(filename, page), (PdfName) null));
    }

    void setAction(PdfAction action, float llx, float lly, float urx, float ury) {
        addAnnotation(this.writer.createAnnotation(llx, lly, urx, ury, action, (PdfName) null));
    }

    PdfAction getLocalGotoAction(String name) {
        PdfAction action;
        Destination dest = this.localDestinations.get(name);
        if (dest == null) {
            dest = new Destination();
        }
        if (dest.action == null) {
            if (dest.reference == null) {
                dest.reference = this.writer.getPdfIndirectReference();
            }
            action = new PdfAction(dest.reference);
            dest.action = action;
            this.localDestinations.put(name, dest);
        } else {
            action = dest.action;
        }
        return action;
    }

    private void addDiv(PdfDiv div) throws DocumentException {
        if (this.floatingElements == null) {
            this.floatingElements = new ArrayList<Element>();
        }
        this.floatingElements.add(div);
    }

    boolean localDestination(String name, PdfDestination destination) {
        Destination dest = this.localDestinations.get(name);
        if (dest == null) {
            dest = new Destination();
        }
        if (dest.destination != null) {
            return false;
        }
        dest.destination = destination;
        this.localDestinations.put(name, dest);
        if (!destination.hasPage()) {
            destination.addPage(this.writer.getCurrentPage());
        }
        return true;
    }
    protected static final DecimalFormat SIXTEEN_DIGITS = new DecimalFormat("0000000000000000");
    protected HashMap<String, PdfObject> documentFileAttachment;
    protected String openActionName;
    protected PdfAction openActionAction;
    protected PdfDictionary additionalActions;
    protected PdfCollection collection;
    PdfAnnotationsImp annotationsImp;
    protected PdfString language;
    protected Rectangle nextPageSize;
    protected HashMap<String, PdfRectangle> thisBoxSize;
    protected HashMap<String, PdfRectangle> boxSize;
    private boolean pageEmpty;
    protected PdfDictionary pageAA;
    protected PageResources pageResources;
    protected boolean strictImageSequence;
    protected float imageEnd;
    protected Image imageWait;
    private ArrayList<Element> floatingElements;

    void addJavaScript(PdfAction js) {
        if (js.get(PdfName.JS) == null) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("only.javascript.actions.are.allowed", new Object[0]));
        }
        try {
            this.documentLevelJS.put(SIXTEEN_DIGITS.format(this.jsCounter++), this.writer.addToBody(js).getIndirectReference());
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    void addJavaScript(String name, PdfAction js) {
        if (js.get(PdfName.JS) == null) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("only.javascript.actions.are.allowed", new Object[0]));
        }
        try {
            this.documentLevelJS.put(name, this.writer.addToBody(js).getIndirectReference());
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    HashMap<String, PdfObject> getDocumentLevelJS() {
        return this.documentLevelJS;
    }

    void addFileAttachment(String description, PdfFileSpecification fs) throws IOException {
        if (description == null) {
            PdfString desc = (PdfString) fs.get(PdfName.DESC);
            if (desc == null) {
                description = "";
            } else {
                description = PdfEncodings.convertToString(desc.getBytes(), null);
            }
        }
        fs.addDescription(description, true);
        if (description.length() == 0) {
            description = "Unnamed";
        }
        String fn = PdfEncodings.convertToString((new PdfString(description, "UnicodeBig")).getBytes(), null);
        int k = 0;
        while (this.documentFileAttachment.containsKey(fn)) {
            k++;
            fn = PdfEncodings.convertToString((new PdfString(description + " " + k, "UnicodeBig")).getBytes(), null);
        }
        this.documentFileAttachment.put(fn, fs.getReference());
    }

    HashMap<String, PdfObject> getDocumentFileAttachment() {
        return this.documentFileAttachment;
    }

    void setOpenAction(String name) {
        this.openActionName = name;
        this.openActionAction = null;
    }

    void setOpenAction(PdfAction action) {
        this.openActionAction = action;
        this.openActionName = null;
    }

    void addAdditionalAction(PdfName actionType, PdfAction action) {
        if (this.additionalActions == null) {
            this.additionalActions = new PdfDictionary();
        }
        if (action == null) {
            this.additionalActions.remove(actionType);
        } else {
            this.additionalActions.put(actionType, action);
        }
        if (this.additionalActions.size() == 0) {
            this.additionalActions = null;
        }
    }

    public void setCollection(PdfCollection collection) {
        this.collection = collection;
    }

    PdfAcroForm getAcroForm() {
        return this.annotationsImp.getAcroForm();
    }

    void setSigFlags(int f) {
        this.annotationsImp.setSigFlags(f);
    }

    void addCalculationOrder(PdfFormField formField) {
        this.annotationsImp.addCalculationOrder(formField);
    }

    void addAnnotation(PdfAnnotation annot) {
        this.pageEmpty = false;
        this.annotationsImp.addAnnotation(annot);
    }

    void setLanguage(String language) {
        this.language = new PdfString(language);
    }

    void setCropBoxSize(Rectangle crop) {
        setBoxSize("crop", crop);
    }

    void setBoxSize(String boxName, Rectangle size) {
        if (size == null) {
            this.boxSize.remove(boxName);
        } else {
            this.boxSize.put(boxName, new PdfRectangle(size));
        }
    }

    protected void setNewPageSizeAndMargins() {
        this.pageSize = this.nextPageSize;
        if (this.marginMirroring && (getPageNumber() & 0x1) == 0) {
            this.marginRight = this.nextMarginLeft;
            this.marginLeft = this.nextMarginRight;
        } else {
            this.marginLeft = this.nextMarginLeft;
            this.marginRight = this.nextMarginRight;
        }
        if (this.marginMirroringTopBottom && (getPageNumber() & 0x1) == 0) {
            this.marginTop = this.nextMarginBottom;
            this.marginBottom = this.nextMarginTop;
        } else {
            this.marginTop = this.nextMarginTop;
            this.marginBottom = this.nextMarginBottom;
        }
        if (!isTagged(this.writer)) {
            this.text = new PdfContentByte(this.writer);
            this.text.reset();
        } else {
            this.text = this.graphics;
        }
        this.text.beginText();
        this.text.moveText(left(), top());
        if (isTagged(this.writer)) {
            this.textEmptySize = this.text.size();
        }
    }

    Rectangle getBoxSize(String boxName) {
        PdfRectangle r = this.thisBoxSize.get(boxName);
        if (r != null) {
            return r.getRectangle();
        }
        return null;
    }

    void setPageEmpty(boolean pageEmpty) {
        this.pageEmpty = pageEmpty;
    }

    boolean isPageEmpty() {
        if (isTagged(this.writer)) {
            return (this.writer == null || (this.writer.getDirectContent().size(false) == 0 && this.writer.getDirectContentUnder().size(false) == 0 && this.text.size(false) - this.textEmptySize == 0 && (this.pageEmpty || this.writer.isPaused())));
        }
        return (this.writer == null || (this.writer.getDirectContent().size() == 0 && this.writer.getDirectContentUnder().size() == 0 && (this.pageEmpty || this.writer.isPaused())));
    }

    void setDuration(int seconds) {
        if (seconds > 0) {
            this.writer.addPageDictEntry(PdfName.DUR, new PdfNumber(seconds));
        }
    }

    void setTransition(PdfTransition transition) {
        this.writer.addPageDictEntry(PdfName.TRANS, transition.getTransitionDictionary());
    }

    void setPageAction(PdfName actionType, PdfAction action) {
        if (this.pageAA == null) {
            this.pageAA = new PdfDictionary();
        }
        this.pageAA.put(actionType, action);
    }

    void setThumbnail(Image image) throws PdfException, DocumentException {
        this.writer.addPageDictEntry(PdfName.THUMB, this.writer.getImageReference(this.writer.addDirectImageSimple(image)));
    }

    PageResources getPageResources() {
        return this.pageResources;
    }

    boolean isStrictImageSequence() {
        return this.strictImageSequence;
    }

    void setStrictImageSequence(boolean strictImageSequence) {
        this.strictImageSequence = strictImageSequence;
    }

    public void clearTextWrap() {
        float tmpHeight = this.imageEnd - this.currentHeight;
        if (this.line != null) {
            tmpHeight += this.line.height();
        }
        if (this.imageEnd > -1.0F && tmpHeight > 0.0F) {
            carriageReturn();
            this.currentHeight += tmpHeight;
        }
    }

    public int getStructParentIndex(Object obj) {
        int[] i = this.structParentIndices.get(obj);
        if (i == null) {
            i = new int[]{this.structParentIndices.size(), 0};
            this.structParentIndices.put(obj, i);
        }
        return i[0];
    }

    public int getNextMarkPoint(Object obj) {
        int[] i = this.structParentIndices.get(obj);
        if (i == null) {
            i = new int[]{this.structParentIndices.size(), 0};
            this.structParentIndices.put(obj, i);
        }
        int markPoint = i[1];
        i[1] = i[1] + 1;
        return markPoint;
    }

    public int[] getStructParentIndexAndNextMarkPoint(Object obj) {
        int[] i = this.structParentIndices.get(obj);
        if (i == null) {
            i = new int[]{this.structParentIndices.size(), 0};
            this.structParentIndices.put(obj, i);
        }
        int markPoint = i[1];
        i[1] = i[1] + 1;
        return new int[]{i[0], markPoint};
    }

    protected void add(Image image) throws PdfException, DocumentException {
        if (image.hasAbsoluteY()) {
            this.graphics.addImage(image);
            this.pageEmpty = false;
            return;
        }
        if (this.currentHeight != 0.0F && indentTop() - this.currentHeight - image.getScaledHeight() < indentBottom()) {
            if (!this.strictImageSequence && this.imageWait == null) {
                this.imageWait = image;
                return;
            }
            newPage();
            if (this.currentHeight != 0.0F && indentTop() - this.currentHeight - image.getScaledHeight() < indentBottom()) {
                this.imageWait = image;
                return;
            }
        }
        this.pageEmpty = false;
        if (image == this.imageWait) {
            this.imageWait = null;
        }
        boolean textwrap = ((image.getAlignment() & 0x4) == 4 && (image.getAlignment() & 0x1) != 1);
        boolean underlying = ((image.getAlignment() & 0x8) == 8);
        float diff = this.leading / 2.0F;
        if (textwrap) {
            diff += this.leading;
        }
        float lowerleft = indentTop() - this.currentHeight - image.getScaledHeight() - diff;
        float[] mt = image.matrix();
        float startPosition = indentLeft() - mt[4];
        if ((image.getAlignment() & 0x2) == 2) {
            startPosition = indentRight() - image.getScaledWidth() - mt[4];
        }
        if ((image.getAlignment() & 0x1) == 1) {
            startPosition = indentLeft() + (indentRight() - indentLeft() - image.getScaledWidth()) / 2.0F - mt[4];
        }
        if (image.hasAbsoluteX()) {
            startPosition = image.getAbsoluteX();
        }
        if (textwrap) {
            if (this.imageEnd < 0.0F || this.imageEnd < this.currentHeight + image.getScaledHeight() + diff) {
                this.imageEnd = this.currentHeight + image.getScaledHeight() + diff;
            }
            if ((image.getAlignment() & 0x2) == 2) {
                this.indentation.imageIndentRight += image.getScaledWidth() + image.getIndentationLeft();
            } else {
                this.indentation.imageIndentLeft += image.getScaledWidth() + image.getIndentationRight();
            }
        } else if ((image.getAlignment() & 0x2) == 2) {
            startPosition -= image.getIndentationRight();
        } else if ((image.getAlignment() & 0x1) == 1) {
            startPosition += image.getIndentationLeft() - image.getIndentationRight();
        } else {
            startPosition += image.getIndentationLeft();
        }
        this.graphics.addImage(image, mt[0], mt[1], mt[2], mt[3], startPosition, lowerleft - mt[5]);
        if (!textwrap && !underlying) {
            this.currentHeight += image.getScaledHeight() + diff;
            flushLines();
            this.text.moveText(0.0F, -(image.getScaledHeight() + diff));
            newLine();
        }
    }

    void addPTable(PdfPTable ptable) throws DocumentException {
        ColumnText ct = new ColumnText(isTagged(this.writer) ? this.text : this.writer.getDirectContent());
        ct.setRunDirection(ptable.getRunDirection());
        if (ptable.getKeepTogether() && !fitsPage(ptable, 0.0F) && this.currentHeight > 0.0F) {
            newPage();
            if (isTagged(this.writer)) {
                ct.setCanvas(this.text);
            }
        }
        if (this.currentHeight == 0.0F) {
            ct.setAdjustFirstLine(false);
        }
        ct.addElement((Element) ptable);
        boolean he = ptable.isHeadersInEvent();
        ptable.setHeadersInEvent(true);
        int loop = 0;
        while (true) {
            ct.setSimpleColumn(indentLeft(), indentBottom(), indentRight(), indentTop() - this.currentHeight);
            int status = ct.go();
            if ((status & 0x1) != 0) {
                if (isTagged(this.writer)) {
                    this.text.setTextMatrix(indentLeft(), ct.getYLine());
                } else {
                    this.text.moveText(0.0F, ct.getYLine() - indentTop() + this.currentHeight);
                }
                this.currentHeight = indentTop() - ct.getYLine();
                break;
            }
            if (indentTop() - this.currentHeight == ct.getYLine()) {
                loop++;
            } else {
                loop = 0;
            }
            if (loop == 3) {
                throw new DocumentException(MessageLocalization.getComposedMessage("infinite.table.loop", new Object[0]));
            }
            this.currentHeight = indentTop() - ct.getYLine();
            newPage();
            if (isTagged(this.writer)) {
                ct.setCanvas(this.text);
            }
        }
        ptable.setHeadersInEvent(he);
    }

    private void flushFloatingElements() throws DocumentException {
        if (this.floatingElements != null && !this.floatingElements.isEmpty()) {
            ArrayList<Element> cachedFloatingElements = this.floatingElements;
            this.floatingElements = null;
            FloatLayout fl = new FloatLayout(cachedFloatingElements, false);
            int loop = 0;
            while (true) {
                float left = indentLeft();
                fl.setSimpleColumn(indentLeft(), indentBottom(), indentRight(), indentTop() - this.currentHeight);
                try {
                    int status = fl.layout(isTagged(this.writer) ? this.text : this.writer.getDirectContent(), false);
                    if ((status & 0x1) != 0) {
                        if (isTagged(this.writer)) {
                            this.text.setTextMatrix(indentLeft(), fl.getYLine());
                        } else {
                            this.text.moveText(0.0F, fl.getYLine() - indentTop() + this.currentHeight);
                        }
                        this.currentHeight = indentTop() - fl.getYLine();
                        break;
                    }
                } catch (Exception exc) {
                    return;
                }
                if (indentTop() - this.currentHeight == fl.getYLine() || isPageEmpty()) {
                    loop++;
                } else {
                    loop = 0;
                }
                if (loop == 2) {
                    return;
                }
                newPage();
            }
        }
    }

    boolean fitsPage(PdfPTable table, float margin) {
        if (!table.isLockedWidth()) {
            float totalWidth = (indentRight() - indentLeft()) * table.getWidthPercentage() / 100.0F;
            table.setTotalWidth(totalWidth);
        }

        ensureNewLine();
        Float spaceNeeded = Float.valueOf(table.isSkipFirstHeader() ? (table.getTotalHeight() - table.getHeaderHeight()) : table.getTotalHeight());
        return (spaceNeeded.floatValue() + ((this.currentHeight > 0.0F) ? table.spacingBefore() : 0.0F) <= indentTop() - this.currentHeight - indentBottom() - margin);
    }

    private static boolean isTagged(PdfWriter writer) {
        return (writer != null && writer.isTagged());
    }

    private PdfLine getLastLine() {
        if (this.lines.size() > 0) {
            return this.lines.get(this.lines.size() - 1);
        }
        return null;
    }

    public class Destination {

        public PdfAction action;

        public PdfIndirectReference reference;
        public PdfDestination destination;
    }

    protected void useExternalCache(TempFileCache externalCache) {
        this.isToUseExternalCache = true;
        this.externalCache = externalCache;
    }

    protected void saveStructElement(AccessibleElementId id, PdfStructureElement element) {
        this.structElements.put(id, element);
    }

    protected PdfStructureElement getStructElement(AccessibleElementId id) {
        return getStructElement(id, true);
    }

    protected PdfStructureElement getStructElement(AccessibleElementId id, boolean toSaveFetchedElement) {
        PdfStructureElement element = this.structElements.get(id);
        if (this.isToUseExternalCache && element == null) {
            TempFileCache.ObjectPosition pos = this.externallyStoredStructElements.get(id);
            if (pos != null) {
                try {
                    element = (PdfStructureElement) this.externalCache.get(pos);
                    element.setStructureTreeRoot(this.writer.getStructureTreeRoot());
                    element.setStructureElementParent(getStructElement(this.elementsParents.get(element.getElementId()), toSaveFetchedElement));

                    if (toSaveFetchedElement) {
                        this.externallyStoredStructElements.remove(id);
                        this.structElements.put(id, element);
                    }
                } catch (IOException e) {
                    throw new ExceptionConverter(e);
                } catch (ClassNotFoundException e) {
                    throw new ExceptionConverter(e);
                }
            }
        }
        return element;
    }

    protected void flushStructureElementsOnNewPage() {
        if (!this.isToUseExternalCache) {
            return;
        }
        Iterator<Map.Entry<AccessibleElementId, PdfStructureElement>> iterator = this.structElements.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<AccessibleElementId, PdfStructureElement> entry = iterator.next();
            if (((PdfStructureElement) entry.getValue()).getStructureType().equals(PdfName.DOCUMENT)) {
                continue;
            }
            try {
                PdfStructureElement el = entry.getValue();
                PdfDictionary parentDict = el.getParent();
                PdfStructureElement parent = null;
                if (parentDict instanceof PdfStructureElement) {
                    parent = (PdfStructureElement) parentDict;
                }
                if (parent != null) {
                    this.elementsParents.put(entry.getKey(), parent.getElementId());
                }

                TempFileCache.ObjectPosition pos = this.externalCache.put(el);
                this.externallyStoredStructElements.put(entry.getKey(), pos);
                iterator.remove();
            } catch (IOException e) {
                throw new ExceptionConverter(e);
            }
        }
    }

    public Set<AccessibleElementId> getStructElements() {
        Set<AccessibleElementId> elements = new HashSet<AccessibleElementId>();
        elements.addAll(this.externallyStoredStructElements.keySet());
        elements.addAll(this.structElements.keySet());
        return elements;
    }
}
