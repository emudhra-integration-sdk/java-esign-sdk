package esign.text.pdf;

import esign.text.DocListener;
import esign.text.Document;
import esign.text.DocumentException;
import esign.text.ExceptionConverter;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import esign.text.exceptions.BadPasswordException;
import esign.text.log.Counter;
import esign.text.log.CounterFactory;
import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class PdfCopy
        extends PdfWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfCopy.class);

    static class IndirectReferences {

        PdfIndirectReference theRef;

        boolean hasCopied;

        IndirectReferences(PdfIndirectReference ref) {
            this.theRef = ref;
            this.hasCopied = false;
        }

        void setCopied() {
            this.hasCopied = true;
        }

        void setNotCopied() {
            this.hasCopied = false;
        }

        boolean getCopied() {
            return this.hasCopied;
        }

        PdfIndirectReference getRef() {
            return this.theRef;
        }

        public String toString() {
            String ext = "";
            if (this.hasCopied) {
                ext = ext + " Copied";
            }
            return getRef() + ext;
        }
    }

    protected static Counter COUNTER = CounterFactory.getCounter(PdfCopy.class);
    protected HashMap<RefKey, IndirectReferences> indirects;
    protected HashMap<PdfReader, HashMap<RefKey, IndirectReferences>> indirectMap;

    protected Counter getCounter() {
        return COUNTER;
    }

    protected HashMap<PdfObject, PdfObject> parentObjects;
    protected HashSet<PdfObject> disableIndirects;
    protected PdfReader reader;
    protected int[] namePtr = new int[]{0};

    private boolean rotateContents = true;
    protected PdfArray fieldArray;
    protected HashSet<PdfTemplate> fieldTemplates;
    private PdfStructTreeController structTreeController = null;
    private int currentStructArrayNumber = 0;

    protected PRIndirectReference structTreeRootReference;

    protected LinkedHashMap<RefKey, PdfIndirectObject> indirectObjects;

    protected ArrayList<PdfIndirectObject> savedObjects;

    protected ArrayList<ImportedPage> importedPages;

    protected boolean updateRootKids = false;

    private static final PdfName annotId = new PdfName("iTextAnnotId");
    private static int annotIdCnt = 0;

    protected boolean mergeFields = false;
    private boolean needAppearances = false;
    private boolean hasSignature;
    private PdfIndirectReference acroForm;
    private HashMap<PdfArray, ArrayList<Integer>> tabOrder;
    private ArrayList<Object> calculationOrderRefs;
    private PdfDictionary resources;
    protected ArrayList<AcroFields> fields;
    private ArrayList<String> calculationOrder;
    private HashMap<String, Object> fieldTree;
    private HashMap<Integer, PdfIndirectObject> unmergedMap;
    private HashMap<RefKey, PdfIndirectObject> unmergedIndirectRefsMap;
    private HashMap<Integer, PdfIndirectObject> mergedMap;
    private HashSet<PdfIndirectObject> mergedSet;
    private boolean mergeFieldsInternalCall = false;
    private static final PdfName iTextTag = new PdfName("_iTextTag_");
    private static final Integer zero = Integer.valueOf(0);
    private HashSet<Object> mergedRadioButtons = new HashSet();
    private HashMap<Object, PdfString> mergedTextFields = new HashMap<Object, PdfString>();

    private HashSet<PdfReader> readersWithImportedStructureTreeRootKids = new HashSet<PdfReader>();

    protected static class ImportedPage {

        int pageNumber;
        PdfReader reader;
        PdfArray mergedFields;
        PdfIndirectReference annotsIndirectReference;

        ImportedPage(PdfReader reader, int pageNumber, boolean keepFields) {
            this.pageNumber = pageNumber;
            this.reader = reader;
            if (keepFields) {
                this.mergedFields = new PdfArray();
            }
        }

        public boolean equals(Object o) {
            if (!(o instanceof ImportedPage)) {
                return false;
            }
            ImportedPage other = (ImportedPage) o;
            return (this.pageNumber == other.pageNumber && this.reader.equals(other.reader));
        }

        public String toString() {
            return Integer.toString(this.pageNumber);
        }
    }

    public PdfCopy(Document document, OutputStream os) throws DocumentException {
        super(new PdfDocument(), os);
        document.addDocListener((DocListener) this.pdf);
        this.pdf.addWriter(this);
        this.indirectMap = new HashMap<PdfReader, HashMap<RefKey, IndirectReferences>>();
        this.parentObjects = new HashMap<PdfObject, PdfObject>();
        this.disableIndirects = new HashSet<PdfObject>();

        this.indirectObjects = new LinkedHashMap<RefKey, PdfIndirectObject>();
        this.savedObjects = new ArrayList<PdfIndirectObject>();
        this.importedPages = new ArrayList<ImportedPage>();
    }

    public void setPageEvent(PdfPageEvent event) {
        throw new UnsupportedOperationException();
    }

    public boolean isRotateContents() {
        return this.rotateContents;
    }

    public void setRotateContents(boolean rotateContents) {
        this.rotateContents = rotateContents;
    }

    public void setMergeFields() {
        this.mergeFields = true;
        this.resources = new PdfDictionary();
        this.fields = new ArrayList<AcroFields>();
        this.calculationOrder = new ArrayList<String>();
        this.fieldTree = new LinkedHashMap<String, Object>();
        this.unmergedMap = new HashMap<Integer, PdfIndirectObject>();
        this.unmergedIndirectRefsMap = new HashMap<RefKey, PdfIndirectObject>();
        this.mergedMap = new HashMap<Integer, PdfIndirectObject>();
        this.mergedSet = new HashSet<PdfIndirectObject>();
    }

    public PdfImportedPage getImportedPage(PdfReader reader, int pageNumber) {
        if (this.mergeFields && !this.mergeFieldsInternalCall) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("1.method.cannot.be.used.in.mergeFields.mode.please.use.addDocument", new Object[]{"getImportedPage"}));
        }
        if (this.mergeFields) {
            ImportedPage newPage = new ImportedPage(reader, pageNumber, this.mergeFields);
            this.importedPages.add(newPage);
        }
        if (this.structTreeController != null) {
            this.structTreeController.reader = null;
        }
        this.disableIndirects.clear();
        this.parentObjects.clear();
        return getImportedPageImpl(reader, pageNumber);
    }

    public PdfImportedPage getImportedPage(PdfReader reader, int pageNumber, boolean keepTaggedPdfStructure) throws BadPdfFormatException {
        if (this.mergeFields && !this.mergeFieldsInternalCall) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("1.method.cannot.be.used.in.mergeFields.mode.please.use.addDocument", new Object[]{"getImportedPage"}));
        }
        this.updateRootKids = false;
        if (!keepTaggedPdfStructure) {
            if (this.mergeFields) {
                ImportedPage importedPage = new ImportedPage(reader, pageNumber, this.mergeFields);
                this.importedPages.add(importedPage);
            }
            return getImportedPageImpl(reader, pageNumber);
        }
        if (this.structTreeController != null) {
            if (reader != this.structTreeController.reader) {
                this.structTreeController.setReader(reader);
            }
        } else {
            this.structTreeController = new PdfStructTreeController(reader, this);
        }
        ImportedPage newPage = new ImportedPage(reader, pageNumber, this.mergeFields);
        switch (checkStructureTreeRootKids(newPage)) {
            case -1:
                clearIndirects(reader);
                this.updateRootKids = true;
                break;
            case 0:
                this.updateRootKids = false;
                break;
            case 1:
                this.updateRootKids = true;
                break;
        }
        this.importedPages.add(newPage);

        this.disableIndirects.clear();
        this.parentObjects.clear();
        return getImportedPageImpl(reader, pageNumber);
    }

    private void clearIndirects(PdfReader reader) {
        HashMap<RefKey, IndirectReferences> currIndirects = this.indirectMap.get(reader);
        ArrayList<RefKey> forDelete = new ArrayList<RefKey>();
        for (Map.Entry<RefKey, IndirectReferences> entry : currIndirects.entrySet()) {
            PdfIndirectReference iRef = ((IndirectReferences) entry.getValue()).theRef;
            RefKey key = new RefKey(iRef);
            PdfIndirectObject iobj = this.indirectObjects.get(key);
            if (iobj == null) {
                forDelete.add(entry.getKey());
                continue;
            }
            if (iobj.object.isArray() || iobj.object.isDictionary() || iobj.object.isStream()) {
                forDelete.add(entry.getKey());
            }
        }

        for (RefKey key : forDelete) {
            currIndirects.remove(key);
        }
    }

    private int checkStructureTreeRootKids(ImportedPage newPage) {
        if (this.importedPages.size() == 0) {
            return 1;
        }
        boolean readerExist = false;
        for (ImportedPage page : this.importedPages) {
            if (page.reader.equals(newPage.reader)) {
                readerExist = true;

                break;
            }
        }

        if (!readerExist) {
            return 1;
        }

        ImportedPage lastPage = this.importedPages.get(this.importedPages.size() - 1);
        boolean equalReader = lastPage.reader.equals(newPage.reader);

        if (equalReader && newPage.pageNumber > lastPage.pageNumber) {
            if (this.readersWithImportedStructureTreeRootKids.contains(newPage.reader)) {
                return 0;
            }
            return 1;
        }

        return -1;
    }

    protected void structureTreeRootKidsForReaderImported(PdfReader reader) {
        this.readersWithImportedStructureTreeRootKids.add(reader);
    }

    protected void fixStructureTreeRoot(HashSet<RefKey> activeKeys, HashSet<PdfName> activeClassMaps) {
        HashMap<PdfName, PdfObject> newClassMap = new HashMap<PdfName, PdfObject>(activeClassMaps.size());
        for (PdfName key : activeClassMaps) {
            PdfObject cm = this.structureTreeRoot.classes.get(key);
            if (cm != null) {
                newClassMap.put(key, cm);
            }

        }
        this.structureTreeRoot.classes = newClassMap;

        PdfArray kids = this.structureTreeRoot.getAsArray(PdfName.K);
        if (kids != null) {
            for (int i = 0; i < kids.size(); i++) {
                PdfIndirectReference iref = (PdfIndirectReference) kids.getPdfObject(i);
                RefKey key = new RefKey(iref);
                if (!activeKeys.contains(key)) {
                    kids.remove(i--);
                }

            }
        }
    }

    protected PdfImportedPage getImportedPageImpl(PdfReader reader, int pageNumber) {
        if (this.currentPdfReaderInstance != null) {
            if (this.currentPdfReaderInstance.getReader() != reader) {

                this.currentPdfReaderInstance = getPdfReaderInstance(reader);
            }
        } else {

            this.currentPdfReaderInstance = getPdfReaderInstance(reader);
        }

        return this.currentPdfReaderInstance.getImportedPage(pageNumber);
    }

    protected PdfIndirectReference copyIndirect(PRIndirectReference in, boolean keepStructure, boolean directRootKids) throws IOException, BadPdfFormatException {
        PdfIndirectReference theRef;
        RefKey key = new RefKey(in);
        IndirectReferences iRef = this.indirects.get(key);
        PdfObject obj = PdfReader.getPdfObjectRelease(in);
        if (keepStructure && directRootKids
                && obj instanceof PdfDictionary) {
            PdfDictionary dict = (PdfDictionary) obj;
            if (dict.contains(PdfName.PG)) {
                return null;
            }
        }
        if (iRef != null) {
            theRef = iRef.getRef();
            if (iRef.getCopied()) {
                return theRef;
            }
        } else {

            theRef = this.body.getPdfIndirectReference();
            iRef = new IndirectReferences(theRef);
            this.indirects.put(key, iRef);
        }

        if (obj != null && obj.isDictionary()) {
            PdfObject type = PdfReader.getPdfObjectRelease(((PdfDictionary) obj).get(PdfName.TYPE));
            if (type != null) {
                if (PdfName.PAGE.equals(type)) {
                    return theRef;
                }
                if (PdfName.CATALOG.equals(type)) {
                    LOGGER.warn(MessageLocalization.getComposedMessage("make.copy.of.catalog.dictionary.is.forbidden", new Object[0]));
                    return null;
                }
            }
        }
        iRef.setCopied();
        if (obj != null) {
            this.parentObjects.put(obj, in);
        }
        PdfObject res = copyObject(obj, keepStructure, directRootKids);
        if (this.disableIndirects.contains(obj)) {
            iRef.setNotCopied();
        }
        if (res != null) {

            addToBody(res, theRef);
            return theRef;
        }

        this.indirects.remove(key);
        return null;
    }

    protected PdfIndirectReference copyIndirect(PRIndirectReference in) throws IOException, BadPdfFormatException {
        return copyIndirect(in, false, false);
    }

    protected PdfDictionary copyDictionary(PdfDictionary in, boolean keepStruct, boolean directRootKids) throws IOException, BadPdfFormatException {
        PdfDictionary out = new PdfDictionary(in.size());
        PdfObject type = PdfReader.getPdfObjectRelease(in.get(PdfName.TYPE));

        if (keepStruct) {

            if (directRootKids && in.contains(PdfName.PG)) {

                PdfObject curr = in;
                this.disableIndirects.add(curr);
                while (this.parentObjects.containsKey(curr) && !this.disableIndirects.contains(curr)) {
                    curr = this.parentObjects.get(curr);
                    this.disableIndirects.add(curr);
                }
                return null;
            }

            PdfName structType = in.getAsName(PdfName.S);
            this.structTreeController.addRole(structType);
            this.structTreeController.addClass(in);
        }
        if (this.structTreeController != null && this.structTreeController.reader != null && (in.contains(PdfName.STRUCTPARENTS) || in.contains(PdfName.STRUCTPARENT))) {
            PdfName key = PdfName.STRUCTPARENT;
            if (in.contains(PdfName.STRUCTPARENTS)) {
                key = PdfName.STRUCTPARENTS;
            }
            PdfObject value = in.get(key);
            out.put(key, new PdfNumber(this.currentStructArrayNumber));
            this.structTreeController.copyStructTreeForPage((PdfNumber) value, this.currentStructArrayNumber++);
        }
        for (PdfName element : in.getKeys()) {
            PdfObject res;
            PdfName key = element;
            PdfObject value = in.get(key);
            if (this.structTreeController != null && this.structTreeController.reader != null && (key.equals(PdfName.STRUCTPARENTS) || key.equals(PdfName.STRUCTPARENT))) {
                continue;
            }
            if (PdfName.PAGE.equals(type)) {
                if (!key.equals(PdfName.B) && !key.equals(PdfName.PARENT)) {
                    this.parentObjects.put(value, in);
                    res = copyObject(value, keepStruct, directRootKids);
                    if (res != null) {
                        out.put(key, res);
                    }
                }
                continue;
            }
            if (this.tagged && value.isIndirect() && isStructTreeRootReference((PRIndirectReference) value)) {
                res = this.structureTreeRoot.getReference();
            } else {
                res = copyObject(value, keepStruct, directRootKids);
            }
            if (res != null) {
                out.put(key, res);
            }
        }

        return out;
    }

    protected PdfDictionary copyDictionary(PdfDictionary in) throws IOException, BadPdfFormatException {
        return copyDictionary(in, false, false);
    }

    protected PdfStream copyStream(PRStream in) throws IOException, BadPdfFormatException {
        PRStream out = new PRStream(in, null);

        for (PdfName element : in.getKeys()) {
            PdfName key = element;
            PdfObject value = in.get(key);
            this.parentObjects.put(value, in);
            PdfObject res = copyObject(value);
            if (res != null) {
                out.put(key, res);
            }
        }
        return out;
    }

    protected PdfArray copyArray(PdfArray in, boolean keepStruct, boolean directRootKids) throws IOException, BadPdfFormatException {
        PdfArray out = new PdfArray(in.size());

        for (Iterator<PdfObject> i = in.listIterator(); i.hasNext();) {
            PdfObject value = i.next();
            this.parentObjects.put(value, in);
            PdfObject res = copyObject(value, keepStruct, directRootKids);
            if (res != null) {
                out.add(res);
            }
        }
        return out;
    }

    protected PdfArray copyArray(PdfArray in) throws IOException, BadPdfFormatException {
        return copyArray(in, false, false);
    }

    protected PdfObject copyObject(PdfObject in, boolean keepStruct, boolean directRootKids) throws IOException, BadPdfFormatException {
        if (in == null) {
            return PdfNull.PDFNULL;
        }
        switch (in.type) {
            case 6:
                return copyDictionary((PdfDictionary) in, keepStruct, directRootKids);
            case 10:
                if (!keepStruct && !directRootKids) {
                    return copyIndirect((PRIndirectReference) in);
                }
                return copyIndirect((PRIndirectReference) in, keepStruct, directRootKids);
            case 5:
                return copyArray((PdfArray) in, keepStruct, directRootKids);
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 8:
                return in;
            case 7:
                return copyStream((PRStream) in);
        }

        if (in.type < 0) {
            String lit = ((PdfLiteral) in).toString();
            if (lit.equals("true") || lit.equals("false")) {
                return new PdfBoolean(lit);
            }
            return new PdfLiteral(lit);
        }
        System.out.println("CANNOT COPY type " + in.type);
        return null;
    }

    protected PdfObject copyObject(PdfObject in) throws IOException, BadPdfFormatException {
        return copyObject(in, false, false);
    }

    protected int setFromIPage(PdfImportedPage iPage) {
        int pageNum = iPage.getPageNumber();
        PdfReaderInstance inst = this.currentPdfReaderInstance = iPage.getPdfReaderInstance();
        this.reader = inst.getReader();
        setFromReader(this.reader);
        return pageNum;
    }

    protected void setFromReader(PdfReader reader) {
        this.reader = reader;
        this.indirects = this.indirectMap.get(reader);
        if (this.indirects == null) {
            this.indirects = new HashMap<RefKey, IndirectReferences>();
            this.indirectMap.put(reader, this.indirects);
        }
    }

    public void addPage(PdfImportedPage iPage) throws IOException, BadPdfFormatException {
        if (this.mergeFields && !this.mergeFieldsInternalCall) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("1.method.cannot.be.used.in.mergeFields.mode.please.use.addDocument", new Object[]{"addPage"}));
        }

        int pageNum = setFromIPage(iPage);
        PdfDictionary thePage = this.reader.getPageN(pageNum);
        PRIndirectReference origRef = this.reader.getPageOrigRef(pageNum);
        this.reader.releasePage(pageNum);
        RefKey key = new RefKey(origRef);

        IndirectReferences iRef = this.indirects.get(key);
        if (iRef != null && !iRef.getCopied()) {
            this.pageReferences.add(iRef.getRef());
            iRef.setCopied();
        }
        PdfIndirectReference pageRef = getCurrentPage();
        if (iRef == null) {
            iRef = new IndirectReferences(pageRef);
            this.indirects.put(key, iRef);
        }
        iRef.setCopied();
        if (this.tagged) {
            this.structTreeRootReference = (PRIndirectReference) this.reader.getCatalog().get(PdfName.STRUCTTREEROOT);
        }
        PdfDictionary newPage = copyDictionary(thePage);
        if (this.mergeFields) {
            ImportedPage importedPage = this.importedPages.get(this.importedPages.size() - 1);
            importedPage.annotsIndirectReference = this.body.getPdfIndirectReference();
            newPage.put(PdfName.ANNOTS, importedPage.annotsIndirectReference);
        }
        this.root.addPage(newPage);
        iPage.setCopied();
        this.currentPageNumber++;
        this.pdf.setPageCount(this.currentPageNumber);
        this.structTreeRootReference = null;
    }

    public void addPage(Rectangle rect, int rotation) throws DocumentException {
        if (this.mergeFields && !this.mergeFieldsInternalCall) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("1.method.cannot.be.used.in.mergeFields.mode.please.use.addDocument", new Object[]{"addPage"}));
        }
        PdfRectangle mediabox = new PdfRectangle(rect, rotation);
        PageResources resources = new PageResources();
        PdfPage page = new PdfPage(mediabox, new HashMap<String, PdfRectangle>(), resources.getResources(), 0);
        page.put(PdfName.TABS, getTabs());
        this.root.addPage(page);
        this.currentPageNumber++;
        this.pdf.setPageCount(this.currentPageNumber);
    }

    public void addDocument(PdfReader reader, List<Integer> pagesToKeep) throws DocumentException, IOException {
        if (this.indirectMap.containsKey(reader)) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("document.1.has.already.been.added", new Object[]{reader.toString()}));
        }
        reader.selectPages(pagesToKeep, false);
        addDocument(reader);
    }

    public void copyDocumentFields(PdfReader reader) throws DocumentException, IOException {
        if (!this.document.isOpen()) {
            throw new DocumentException(MessageLocalization.getComposedMessage("the.document.is.not.open.yet.you.can.only.add.meta.information", new Object[0]));
        }

        if (this.indirectMap.containsKey(reader)) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("document.1.has.already.been.added", new Object[]{reader.toString()}));
        }

        if (!reader.isOpenedWithFullPermissions()) {
            throw new BadPasswordException(MessageLocalization.getComposedMessage("pdfreader.not.opened.with.owner.password", new Object[0]));
        }
        if (!this.mergeFields) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("1.method.can.be.only.used.in.mergeFields.mode.please.use.addDocument", new Object[]{"copyDocumentFields"}));
        }
        this.indirects = new HashMap<RefKey, IndirectReferences>();
        this.indirectMap.put(reader, this.indirects);

        reader.consolidateNamedDestinations();
        reader.shuffleSubsetNames();
        if (this.tagged && PdfStructTreeController.checkTagged(reader)) {
            this.structTreeRootReference = (PRIndirectReference) reader.getCatalog().get(PdfName.STRUCTTREEROOT);
            if (this.structTreeController != null) {
                if (reader != this.structTreeController.reader) {
                    this.structTreeController.setReader(reader);
                }
            } else {
                this.structTreeController = new PdfStructTreeController(reader, this);
            }
        }

        List<PdfObject> annotationsToBeCopied = new ArrayList<PdfObject>();

        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            PdfDictionary page = reader.getPageNRelease(i);
            if (page != null && page.contains(PdfName.ANNOTS)) {
                PdfArray annots = page.getAsArray(PdfName.ANNOTS);
                if (annots != null && annots.size() > 0) {
                    if (this.importedPages.size() < i) {
                        throw new DocumentException(MessageLocalization.getComposedMessage("there.are.not.enough.imported.pages.for.copied.fields", new Object[0]));
                    }
                    ((HashMap<RefKey, IndirectReferences>) this.indirectMap.get(reader)).put(new RefKey(reader.pageRefs.getPageOrigRef(i)), new IndirectReferences(this.pageReferences.get(i - 1)));
                    for (int j = 0; j < annots.size(); j++) {
                        PdfDictionary annot = annots.getAsDict(j);
                        if (annot != null) {
                            annot.put(annotId, new PdfNumber(++annotIdCnt));
                            annotationsToBeCopied.add(annots.getPdfObject(j));
                        }
                    }
                }
            }
        }

        for (PdfObject annot : annotationsToBeCopied) {
            copyObject(annot);
        }

        if (this.tagged && this.structTreeController != null) {
            this.structTreeController.attachStructTreeRootKids(null);
        }
        AcroFields acro = reader.getAcroFields();
        boolean needapp = !acro.isGenerateAppearances();
        if (needapp) {
            this.needAppearances = true;
        }
        this.fields.add(acro);
        updateCalculationOrder(reader);
        this.structTreeRootReference = null;
    }

    public void addDocument(PdfReader reader) throws DocumentException, IOException {
        if (!this.document.isOpen()) {
            throw new DocumentException(MessageLocalization.getComposedMessage("the.document.is.not.open.yet.you.can.only.add.meta.information", new Object[0]));
        }
        if (this.indirectMap.containsKey(reader)) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("document.1.has.already.been.added", new Object[]{reader.toString()}));
        }
        if (!reader.isOpenedWithFullPermissions()) {
            throw new BadPasswordException(MessageLocalization.getComposedMessage("pdfreader.not.opened.with.owner.password", new Object[0]));
        }
        if (this.mergeFields) {
            reader.consolidateNamedDestinations();
            reader.shuffleSubsetNames();
            for (int j = 1; j <= reader.getNumberOfPages(); j++) {
                PdfDictionary page = reader.getPageNRelease(j);
                if (page != null && page.contains(PdfName.ANNOTS)) {
                    PdfArray annots = page.getAsArray(PdfName.ANNOTS);
                    if (annots != null) {
                        for (int k = 0; k < annots.size(); k++) {
                            PdfDictionary annot = annots.getAsDict(k);
                            if (annot != null) {
                                annot.put(annotId, new PdfNumber(++annotIdCnt));
                            }
                        }
                    }
                }
            }
            AcroFields acro = reader.getAcroFields();

            boolean needapp = !acro.isGenerateAppearances();
            if (needapp) {
                this.needAppearances = true;
            }
            this.fields.add(acro);
            updateCalculationOrder(reader);
        }
        boolean tagged = (this.tagged && PdfStructTreeController.checkTagged(reader));
        this.mergeFieldsInternalCall = true;
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            addPage(getImportedPage(reader, i, tagged));
        }
        this.mergeFieldsInternalCall = false;
    }

    public PdfIndirectObject addToBody(PdfObject object, PdfIndirectReference ref) throws IOException {
        return addToBody(object, ref, false);
    }

    public PdfIndirectObject addToBody(PdfObject object, PdfIndirectReference ref, boolean formBranching) throws IOException {
        PdfIndirectObject iobj;
        if (formBranching) {
            updateReferences(object);
        }

        if ((this.tagged || this.mergeFields) && this.indirectObjects != null && (object.isArray() || object.isDictionary() || object.isStream() || object.isNull())) {
            RefKey key = new RefKey(ref);
            PdfIndirectObject obj = this.indirectObjects.get(key);
            if (obj == null) {
                obj = new PdfIndirectObject(ref, object, this);
                this.indirectObjects.put(key, obj);
            }
            iobj = obj;
        } else {
            iobj = super.addToBody(object, ref);
        }
        if (this.mergeFields && object.isDictionary()) {
            PdfNumber annotId = ((PdfDictionary) object).getAsNumber(PdfCopy.annotId);
            if (annotId != null) {
                if (formBranching) {
                    this.mergedMap.put(Integer.valueOf(annotId.intValue()), iobj);
                    this.mergedSet.add(iobj);
                } else {
                    this.unmergedMap.put(Integer.valueOf(annotId.intValue()), iobj);
                    this.unmergedIndirectRefsMap.put(new RefKey(iobj.number, iobj.generation), iobj);
                }
            }
        }
        return iobj;
    }

    protected void cacheObject(PdfIndirectObject iobj) {
        if ((this.tagged || this.mergeFields) && this.indirectObjects != null) {
            this.savedObjects.add(iobj);
            RefKey key = new RefKey(iobj.number, iobj.generation);
            if (!this.indirectObjects.containsKey(key)) {
                this.indirectObjects.put(key, iobj);
            }

        }
    }

    protected void flushTaggedObjects() throws IOException {

        try {
            fixTaggedStructure();
        } catch (ClassCastException classCastException) {
        } finally {
            flushIndirectObjects();
        }

    }

    protected void flushAcroFields() throws IOException, BadPdfFormatException {
        if (this.mergeFields) {

            try {
                for (ImportedPage page : this.importedPages) {
                    PdfDictionary pageDict = page.reader.getPageN(page.pageNumber);
                    if (pageDict != null) {
                        PdfArray pageFields = pageDict.getAsArray(PdfName.ANNOTS);
                        if (pageFields == null || pageFields.size() == 0) {
                            continue;
                        }
                        for (AcroFields.Item items : page.reader.getAcroFields().getFields().values()) {
                            for (PdfIndirectReference ref : items.widget_refs) {
                                pageFields.arrayList.remove(ref);
                            }
                        }
                        this.indirects = this.indirectMap.get(page.reader);
                        for (PdfObject ref : pageFields.arrayList) {
                            page.mergedFields.add(copyObject(ref));
                        }
                    }
                }
                for (PdfReader reader : this.indirectMap.keySet()) {
                    reader.removeFields();
                }
                mergeFields();
                createAcroForms();
            } catch (ClassCastException classCastException) {
            } finally {
                if (!this.tagged) {
                    flushIndirectObjects();
                }
            }

        }
    }

    protected void fixTaggedStructure() throws IOException {
        HashMap<Integer, PdfIndirectReference> numTree = this.structureTreeRoot.getNumTree();
        HashSet<RefKey> activeKeys = new HashSet<RefKey>();
        ArrayList<PdfIndirectReference> actives = new ArrayList<PdfIndirectReference>();
        int pageRefIndex = 0;

        if (this.mergeFields && this.acroForm != null) {
            actives.add(this.acroForm);
            activeKeys.add(new RefKey(this.acroForm));
        }
        for (PdfIndirectReference page : this.pageReferences) {
            actives.add(page);
            activeKeys.add(new RefKey(page));
        }

        for (int i = numTree.size() - 1; i >= 0; i--) {
            PdfIndirectReference currNum = numTree.get(Integer.valueOf(i));
            if (currNum != null) {

                RefKey numKey = new RefKey(currNum);
                PdfObject obj = ((PdfIndirectObject) this.indirectObjects.get(numKey)).object;
                if (obj.isDictionary()) {
                    boolean addActiveKeys = false;
                    if (this.pageReferences.contains(((PdfDictionary) obj).get(PdfName.PG))) {
                        addActiveKeys = true;
                    } else {
                        PdfDictionary k = PdfStructTreeController.getKDict((PdfDictionary) obj);
                        if (k != null && this.pageReferences.contains(k.get(PdfName.PG))) {
                            addActiveKeys = true;
                        }
                    }
                    if (addActiveKeys) {
                        activeKeys.add(numKey);
                        actives.add(currNum);
                    } else {
                        numTree.remove(Integer.valueOf(i));
                    }
                } else if (obj.isArray()) {
                    activeKeys.add(numKey);
                    actives.add(currNum);
                    PdfArray currNums = (PdfArray) obj;
                    PdfIndirectReference currPage = this.pageReferences.get(pageRefIndex++);
                    actives.add(currPage);
                    activeKeys.add(new RefKey(currPage));
                    PdfIndirectReference prevKid = null;
                    for (int j = 0; j < currNums.size(); j++) {
                        PdfIndirectReference currKid = (PdfIndirectReference) currNums.getDirectObject(j);
                        if (!currKid.equals(prevKid)) {
                            RefKey kidKey = new RefKey(currKid);
                            activeKeys.add(kidKey);
                            actives.add(currKid);

                            PdfIndirectObject iobj = this.indirectObjects.get(kidKey);
                            if (iobj.object.isDictionary()) {
                                PdfDictionary dict = (PdfDictionary) iobj.object;
                                PdfIndirectReference pg = (PdfIndirectReference) dict.get(PdfName.PG);

                                if (pg != null && !this.pageReferences.contains(pg) && !pg.equals(currPage)) {
                                    dict.put(PdfName.PG, currPage);
                                    PdfArray kids = dict.getAsArray(PdfName.K);
                                    if (kids != null) {
                                        PdfObject firstKid = kids.getDirectObject(0);
                                        if (firstKid.isNumber()) {
                                            kids.remove(0);
                                        }
                                    }
                                }
                            }
                            prevKid = currKid;
                        }
                    }
                }
            }
        }
        HashSet<PdfName> activeClassMaps = new HashSet<PdfName>();

        findActives(actives, activeKeys, activeClassMaps);

        ArrayList<PdfIndirectReference> newRefs = findActiveParents(activeKeys);

        fixPgKey(newRefs, activeKeys);

        fixStructureTreeRoot(activeKeys, activeClassMaps);

        for (Map.Entry<RefKey, PdfIndirectObject> entry : this.indirectObjects.entrySet()) {
            if (!activeKeys.contains(entry.getKey())) {
                entry.setValue(null);
                continue;
            }
            if (((PdfIndirectObject) entry.getValue()).object.isArray()) {
                removeInactiveReferences((PdfArray) ((PdfIndirectObject) entry.getValue()).object, activeKeys);
                continue;
            }
            if (((PdfIndirectObject) entry.getValue()).object.isDictionary()) {
                PdfObject kids = ((PdfDictionary) ((PdfIndirectObject) entry.getValue()).object).get(PdfName.K);
                if (kids != null && kids.isArray()) {
                    removeInactiveReferences((PdfArray) kids, activeKeys);
                }
            }
        }
    }

    private void removeInactiveReferences(PdfArray array, HashSet<RefKey> activeKeys) {
        for (int i = 0; i < array.size(); i++) {
            PdfObject obj = array.getPdfObject(i);
            if ((obj.type() == 0 && !activeKeys.contains(new RefKey((PdfIndirectReference) obj))) || (obj
                    .isDictionary() && containsInactivePg((PdfDictionary) obj, activeKeys))) {
                array.remove(i--);
            }
        }
    }

    private boolean containsInactivePg(PdfDictionary dict, HashSet<RefKey> activeKeys) {
        PdfObject pg = dict.get(PdfName.PG);
        if (pg != null && !activeKeys.contains(new RefKey((PdfIndirectReference) pg))) {
            return true;
        }
        return false;
    }

    private ArrayList<PdfIndirectReference> findActiveParents(HashSet<RefKey> activeKeys) {
        ArrayList<PdfIndirectReference> newRefs = new ArrayList<PdfIndirectReference>();
        ArrayList<RefKey> tmpActiveKeys = new ArrayList<RefKey>(activeKeys);
        for (int i = 0; i < tmpActiveKeys.size(); i++) {
            PdfIndirectObject iobj = this.indirectObjects.get(tmpActiveKeys.get(i));
            if (iobj != null && iobj.object.isDictionary()) {
                PdfObject parent = ((PdfDictionary) iobj.object).get(PdfName.P);
                if (parent != null && parent.type() == 0) {
                    RefKey key = new RefKey((PdfIndirectReference) parent);
                    if (!activeKeys.contains(key)) {
                        activeKeys.add(key);
                        tmpActiveKeys.add(key);
                        newRefs.add((PdfIndirectReference) parent);
                    }
                }
            }
        }
        return newRefs;
    }

    private void fixPgKey(ArrayList<PdfIndirectReference> newRefs, HashSet<RefKey> activeKeys) {
        for (PdfIndirectReference iref : newRefs) {
            PdfIndirectObject iobj = this.indirectObjects.get(new RefKey(iref));
            if (iobj == null || !iobj.object.isDictionary()) {
                continue;
            }
            PdfDictionary dict = (PdfDictionary) iobj.object;
            PdfObject pg = dict.get(PdfName.PG);
            if (pg == null || activeKeys.contains(new RefKey((PdfIndirectReference) pg))) {
                continue;
            }
            PdfArray kids = dict.getAsArray(PdfName.K);
            if (kids == null) {
                continue;
            }
            for (int i = 0; i < kids.size(); i++) {
                PdfObject obj = kids.getPdfObject(i);
                if (obj.type() != 0) {
                    kids.remove(i--);
                } else {
                    PdfIndirectObject kid = this.indirectObjects.get(new RefKey((PdfIndirectReference) obj));
                    if (kid != null && kid.object.isDictionary()) {
                        PdfObject kidPg = ((PdfDictionary) kid.object).get(PdfName.PG);
                        if (kidPg != null && activeKeys.contains(new RefKey((PdfIndirectReference) kidPg))) {
                            dict.put(PdfName.PG, kidPg);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void findActives(ArrayList<PdfIndirectReference> actives, HashSet<RefKey> activeKeys, HashSet<PdfName> activeClassMaps) {
        for (int i = 0; i < actives.size(); i++) {
            RefKey key = new RefKey(actives.get(i));
            PdfIndirectObject iobj = this.indirectObjects.get(key);
            if (iobj != null && iobj.object != null) {
                switch (iobj.object.type()) {
                    case 0:
                        findActivesFromReference((PdfIndirectReference) iobj.object, actives, activeKeys);
                        break;
                    case 5:
                        findActivesFromArray((PdfArray) iobj.object, actives, activeKeys, activeClassMaps);
                        break;
                    case 6:
                    case 7:
                        findActivesFromDict((PdfDictionary) iobj.object, actives, activeKeys, activeClassMaps);
                        break;
                }
            }
        }
    }

    private void findActivesFromReference(PdfIndirectReference iref, ArrayList<PdfIndirectReference> actives, HashSet<RefKey> activeKeys) {
        RefKey key = new RefKey(iref);
        PdfIndirectObject iobj = this.indirectObjects.get(key);
        if (iobj != null && iobj.object.isDictionary() && containsInactivePg((PdfDictionary) iobj.object, activeKeys)) {
            return;
        }
        if (!activeKeys.contains(key)) {
            activeKeys.add(key);
            actives.add(iref);
        }
    }

    private void findActivesFromArray(PdfArray array, ArrayList<PdfIndirectReference> actives, HashSet<RefKey> activeKeys, HashSet<PdfName> activeClassMaps) {
        for (PdfObject obj : array) {
            switch (obj.type()) {
                case 0:
                    findActivesFromReference((PdfIndirectReference) obj, actives, activeKeys);

                case 5:
                    findActivesFromArray((PdfArray) obj, actives, activeKeys, activeClassMaps);

                case 6:
                case 7:
                    findActivesFromDict((PdfDictionary) obj, actives, activeKeys, activeClassMaps);
            }
        }
    }

    private void findActivesFromDict(PdfDictionary dict, ArrayList<PdfIndirectReference> actives, HashSet<RefKey> activeKeys, HashSet<PdfName> activeClassMaps) {
        if (containsInactivePg(dict, activeKeys)) {
            return;
        }
        for (PdfName key : dict.getKeys()) {
            PdfObject obj = dict.get(key);
            if (key.equals(PdfName.P)) {
                continue;
            }
            if (key.equals(PdfName.C)) {
                if (obj.isArray()) {
                    for (PdfObject cm : (PdfArray) obj) {
                        if (cm.isName()) {
                            activeClassMaps.add((PdfName) cm);
                        }
                    }
                    continue;
                }
                if (obj.isName()) {
                    activeClassMaps.add((PdfName) obj);
                }
                continue;
            }
            switch (obj.type()) {
                case 0:
                    findActivesFromReference((PdfIndirectReference) obj, actives, activeKeys);

                case 5:
                    findActivesFromArray((PdfArray) obj, actives, activeKeys, activeClassMaps);

                case 6:
                case 7:
                    findActivesFromDict((PdfDictionary) obj, actives, activeKeys, activeClassMaps);
            }
        }
    }

    protected void flushIndirectObjects() throws IOException {
        for (PdfIndirectObject iobj : this.savedObjects) {
            this.indirectObjects.remove(new RefKey(iobj.number, iobj.generation));
        }
        HashSet<RefKey> inactives = new HashSet<RefKey>();
        for (Map.Entry<RefKey, PdfIndirectObject> entry : this.indirectObjects.entrySet()) {
            if (entry.getValue() != null) {
                writeObjectToBody(entry.getValue());
                continue;
            }
            inactives.add(entry.getKey());
        }
        ArrayList<PdfWriter.PdfBody.PdfCrossReference> pdfCrossReferences = new ArrayList<PdfWriter.PdfBody.PdfCrossReference>(this.body.xrefs);
        for (PdfWriter.PdfBody.PdfCrossReference cr : pdfCrossReferences) {
            RefKey key = new RefKey(cr.getRefnum(), 0);
            if (inactives.contains(key)) {
                this.body.xrefs.remove(cr);
            }
        }
        this.indirectObjects = null;
    }

    private void writeObjectToBody(PdfIndirectObject object) throws IOException {
        boolean skipWriting = false;
        if (this.mergeFields) {
            updateAnnotationReferences(object.object);
            if (object.object.isDictionary() || object.object.isStream()) {
                PdfDictionary dictionary = (PdfDictionary) object.object;
                if (this.unmergedIndirectRefsMap.containsKey(new RefKey(object.number, object.generation))) {
                    PdfNumber annotId = dictionary.getAsNumber(PdfCopy.annotId);
                    if (annotId != null && this.mergedMap.containsKey(Integer.valueOf(annotId.intValue()))) {
                        skipWriting = true;
                    }
                }
                if (this.mergedSet.contains(object)) {
                    PdfNumber annotId = dictionary.getAsNumber(PdfCopy.annotId);
                    if (annotId != null) {
                        PdfIndirectObject unmerged = this.unmergedMap.get(Integer.valueOf(annotId.intValue()));
                        if (unmerged != null && unmerged.object.isDictionary()) {
                            PdfNumber structParent = ((PdfDictionary) unmerged.object).getAsNumber(PdfName.STRUCTPARENT);
                            if (structParent != null) {
                                dictionary.put(PdfName.STRUCTPARENT, structParent);
                            }
                        }
                    }
                }
            }
        }
        if (!skipWriting) {
            PdfDictionary dictionary = null;
            PdfNumber annotId = null;
            if (this.mergeFields && object.object.isDictionary()) {
                dictionary = (PdfDictionary) object.object;
                annotId = dictionary.getAsNumber(PdfCopy.annotId);
                if (annotId != null) {
                    dictionary.remove(PdfCopy.annotId);
                }
            }
            this.body.add(object.object, object.number, object.generation, true);
            if (annotId != null) {
                dictionary.put(PdfCopy.annotId, annotId);
            }
        }
    }

    private void updateAnnotationReferences(PdfObject obj) {
        if (obj.isArray()) {
            PdfArray array = (PdfArray) obj;
            for (int i = 0; i < array.size(); i++) {
                PdfObject o = array.getPdfObject(i);
                if (o != null && o.type() == 0) {
                    PdfIndirectObject entry = this.unmergedIndirectRefsMap.get(new RefKey((PdfIndirectReference) o));
                    if (entry != null
                            && entry.object.isDictionary()) {
                        PdfNumber annotId = ((PdfDictionary) entry.object).getAsNumber(PdfCopy.annotId);
                        if (annotId != null) {
                            PdfIndirectObject merged = this.mergedMap.get(Integer.valueOf(annotId.intValue()));
                            if (merged != null) {
                                array.set(i, merged.getIndirectReference());
                            }
                        }
                    }
                } else {

                    updateAnnotationReferences(o);
                }
            }
        } else if (obj.isDictionary() || obj.isStream()) {
            PdfDictionary dictionary = (PdfDictionary) obj;
            for (PdfName key : dictionary.getKeys()) {
                PdfObject o = dictionary.get(key);
                if (o != null && o.type() == 0) {
                    PdfIndirectObject entry = this.unmergedIndirectRefsMap.get(new RefKey((PdfIndirectReference) o));
                    if (entry != null
                            && entry.object.isDictionary()) {
                        PdfNumber annotId = ((PdfDictionary) entry.object).getAsNumber(PdfCopy.annotId);
                        if (annotId != null) {
                            PdfIndirectObject merged = this.mergedMap.get(Integer.valueOf(annotId.intValue()));
                            if (merged != null) {
                                dictionary.put(key, merged.getIndirectReference());
                            }
                        }
                    }
                    continue;
                }
                updateAnnotationReferences(o);
            }
        }
    }

    private void updateCalculationOrder(PdfReader reader) {
        PdfDictionary catalog = reader.getCatalog();
        PdfDictionary acro = catalog.getAsDict(PdfName.ACROFORM);
        if (acro == null) {
            return;
        }
        PdfArray co = acro.getAsArray(PdfName.CO);
        if (co == null || co.size() == 0) {
            return;
        }
        AcroFields af = reader.getAcroFields();
        for (int k = 0; k < co.size(); k++) {
            PdfObject obj = co.getPdfObject(k);
            if (obj != null && obj.isIndirect()) {

                String name = getCOName(reader, (PRIndirectReference) obj);
                if (af.getFieldItem(name) != null) {

                    name = "." + name;
                    if (!this.calculationOrder.contains(name)) {
                        this.calculationOrder.add(name);
                    }
                }
            }
        }
    }

    private static String getCOName(PdfReader reader, PRIndirectReference ref) {
        String name = "";
        while (ref != null) {
            PdfObject obj = PdfReader.getPdfObject(ref);
            if (obj == null || obj.type() != 6) {
                break;
            }
            PdfDictionary dic = (PdfDictionary) obj;
            PdfString t = dic.getAsString(PdfName.T);
            if (t != null) {
                name = t.toUnicodeString() + "." + name;
            }
            ref = (PRIndirectReference) dic.get(PdfName.PARENT);
        }
        if (name.endsWith(".")) {
            name = name.substring(0, name.length() - 2);
        }
        return name;
    }

    private void mergeFields() {
        int pageOffset = 0;
        for (int k = 0; k < this.fields.size(); k++) {
            AcroFields af = this.fields.get(k);
            Map<String, AcroFields.Item> fd = af.getFields();
            if (pageOffset < this.importedPages.size() && ((ImportedPage) this.importedPages.get(pageOffset)).reader == af.reader) {
                addPageOffsetToField(fd, pageOffset);
                pageOffset += af.reader.getNumberOfPages();
            }
            mergeWithMaster(fd);
        }
    }

    private void addPageOffsetToField(Map<String, AcroFields.Item> fd, int pageOffset) {
        if (pageOffset == 0) {
            return;
        }
        for (AcroFields.Item item : fd.values()) {
            for (int k = 0; k < item.size(); k++) {
                int p = item.getPage(k).intValue();
                item.forcePage(k, p + pageOffset);
            }
        }
    }

    private void mergeWithMaster(Map<String, AcroFields.Item> fd) {
        for (Map.Entry<String, AcroFields.Item> entry : fd.entrySet()) {
            String name = entry.getKey();
            mergeField(name, entry.getValue());
        }
    }

    private void mergeField(String name, AcroFields.Item item) {
        String s;
        Map<Object, Object> obj;
        HashMap<String, Object> map = this.fieldTree;
        StringTokenizer tk = new StringTokenizer(name, ".");
        if (!tk.hasMoreTokens()) {
            return;
        }
        while (true) {
            s = tk.nextToken();
            obj = (HashMap<Object, Object>) map.get(s);
            if (tk.hasMoreTokens()) {
                if (obj == null) {
                    obj = (HashMap<Object, Object>) new LinkedHashMap<Object, Object>();
                    map.put(s, obj);
                    map = (HashMap) obj;
                    continue;
                }
                if (obj instanceof HashMap) {
                    map = (HashMap) obj;
                    continue;
                }
                return;
            }
            break;
        }
        if (obj instanceof HashMap) {
            return;
        }
        PdfDictionary merged = item.getMerged(0);
        if (obj == null) {
            PdfDictionary field = new PdfDictionary();
            if (PdfName.SIG.equals(merged.get(PdfName.FT))) {
                this.hasSignature = true;
            }
            for (PdfName element : merged.getKeys()) {
                PdfName key = element;
                if (fieldKeys.contains(key)) {
                    field.put(key, merged.get(key));
                }
            }
            ArrayList<Object> list = new ArrayList();
            list.add(field);
            createWidgets(list, item);
            map.put(s, list);
        } else {

            ArrayList<Object> list = (ArrayList) obj;
            PdfDictionary field = (PdfDictionary) list.get(0);
            PdfName type1 = (PdfName) field.get(PdfName.FT);
            PdfName type2 = (PdfName) merged.get(PdfName.FT);
            if (type1 == null || !type1.equals(type2)) {
                return;
            }
            int flag1 = 0;
            PdfObject f1 = field.get(PdfName.FF);
            if (f1 != null && f1.isNumber()) {
                flag1 = ((PdfNumber) f1).intValue();
            }
            int flag2 = 0;
            PdfObject f2 = merged.get(PdfName.FF);
            if (f2 != null && f2.isNumber()) {
                flag2 = ((PdfNumber) f2).intValue();
            }
            if (type1.equals(PdfName.BTN)) {
                if (((flag1 ^ flag2) & 0x10000) != 0) {
                    return;
                }
                if ((flag1 & 0x10000) == 0 && ((flag1 ^ flag2) & 0x8000) != 0) {
                    return;
                }
            } else if (type1.equals(PdfName.CH) && ((flag1 ^ flag2) & 0x20000) != 0) {
                return;
            }
            createWidgets(list, item);
        }
    }

    private void createWidgets(ArrayList<Object> list, AcroFields.Item item) {
        for (int k = 0; k < item.size(); k++) {
            list.add(item.getPage(k));
            PdfDictionary merged = item.getMerged(k);
            PdfObject dr = merged.get(PdfName.DR);
            if (dr != null) {
                PdfFormField.mergeResources(this.resources, (PdfDictionary) PdfReader.getPdfObject(dr));
            }
            PdfDictionary widget = new PdfDictionary();
            for (PdfName element : merged.getKeys()) {
                PdfName key = element;
                if (widgetKeys.contains(key)) {
                    widget.put(key, merged.get(key));
                }
            }
            widget.put(iTextTag, new PdfNumber(item.getTabOrder(k).intValue() + 1));
            list.add(widget);
        }
    }

    private PdfObject propagate(PdfObject obj) throws IOException {
        if (obj == null) {
            return new PdfNull();
        }
        if (obj.isArray()) {
            PdfArray a = (PdfArray) obj;
            for (int i = 0; i < a.size(); i++) {
                a.set(i, propagate(a.getPdfObject(i)));
            }
            return a;
        }
        if (obj.isDictionary() || obj.isStream()) {
            PdfDictionary d = (PdfDictionary) obj;
            for (PdfName key : d.getKeys()) {
                d.put(key, propagate(d.get(key)));
            }
            return d;
        }
        if (obj.isIndirect()) {
            obj = PdfReader.getPdfObject(obj);
            return addToBody(propagate(obj)).getIndirectReference();
        }
        return obj;
    }

    private void createAcroForms() throws IOException, BadPdfFormatException {
        if (this.fieldTree.isEmpty()) {

            for (ImportedPage importedPage : this.importedPages) {
                if (importedPage.mergedFields.size() > 0) {
                    addToBody(importedPage.mergedFields, importedPage.annotsIndirectReference);
                }
            }
            return;
        }
        PdfDictionary form = new PdfDictionary();
        form.put(PdfName.DR, propagate(this.resources));

        if (this.needAppearances) {
            form.put(PdfName.NEEDAPPEARANCES, PdfBoolean.PDFTRUE);
        }
        form.put(PdfName.DA, new PdfString("/Helv 0 Tf 0 g "));
        this.tabOrder = new HashMap<PdfArray, ArrayList<Integer>>();
        this.calculationOrderRefs = new ArrayList(this.calculationOrder);
        form.put(PdfName.FIELDS, branchForm(this.fieldTree, (PdfIndirectReference) null, ""));
        if (this.hasSignature) {
            form.put(PdfName.SIGFLAGS, new PdfNumber(3));
        }
        PdfArray co = new PdfArray();
        for (int k = 0; k < this.calculationOrderRefs.size(); k++) {
            Object obj = this.calculationOrderRefs.get(k);
            if (obj instanceof PdfIndirectReference) {
                co.add((PdfIndirectReference) obj);
            }
        }
        if (co.size() > 0) {
            form.put(PdfName.CO, co);
        }
        this.acroForm = addToBody(form).getIndirectReference();
        for (ImportedPage importedPage : this.importedPages) {
            addToBody(importedPage.mergedFields, importedPage.annotsIndirectReference);
        }
    }

    private void updateReferences(PdfObject obj) {
        if (obj.isDictionary() || obj.isStream()) {
            PdfDictionary dictionary = (PdfDictionary) obj;
            for (PdfName key : dictionary.getKeys()) {
                PdfObject o = dictionary.get(key);
                if (o.isIndirect()) {
                    PdfReader reader = ((PRIndirectReference) o).getReader();
                    HashMap<RefKey, IndirectReferences> indirects = this.indirectMap.get(reader);
                    IndirectReferences indRef = indirects.get(new RefKey((PRIndirectReference) o));
                    if (indRef != null) {
                        dictionary.put(key, indRef.getRef());
                    }
                    continue;
                }
                updateReferences(o);
            }

        } else if (obj.isArray()) {
            PdfArray array = (PdfArray) obj;
            for (int i = 0; i < array.size(); i++) {
                PdfObject o = array.getPdfObject(i);
                if (o.isIndirect()) {
                    PdfReader reader = ((PRIndirectReference) o).getReader();
                    HashMap<RefKey, IndirectReferences> indirects = this.indirectMap.get(reader);
                    IndirectReferences indRef = indirects.get(new RefKey((PRIndirectReference) o));
                    if (indRef != null) {
                        array.set(i, indRef.getRef());
                    }
                } else {
                    updateReferences(o);
                }
            }
        }
    }

    private PdfArray branchForm(HashMap<String, Object> level, PdfIndirectReference parent, String fname) throws IOException, BadPdfFormatException {
        PdfArray arr = new PdfArray();
        for (Map.Entry<String, Object> entry : level.entrySet()) {
            String name = entry.getKey();
            Object obj = entry.getValue();
            PdfIndirectReference ind = getPdfIndirectReference();
            PdfDictionary dic = new PdfDictionary();
            if (parent != null) {
                dic.put(PdfName.PARENT, parent);
            }
            dic.put(PdfName.T, new PdfString(name, "UnicodeBig"));
            String fname2 = fname + "." + name;
            int coidx = this.calculationOrder.indexOf(fname2);
            if (coidx >= 0) {
                this.calculationOrderRefs.set(coidx, ind);
            }
            if (obj instanceof HashMap) {
                dic.put(PdfName.KIDS, branchForm((HashMap<String, Object>) obj, ind, fname2));
                arr.add(ind);
                addToBody(dic, ind, true);
                continue;
            }
            ArrayList<Object> list = (ArrayList<Object>) obj;
            dic.mergeDifferent((PdfDictionary) list.get(0));
            if (list.size() == 3) {
                dic.mergeDifferent((PdfDictionary) list.get(2));
                int page = ((Integer) list.get(1)).intValue();
                PdfArray annots = ((ImportedPage) this.importedPages.get(page - 1)).mergedFields;
                PdfNumber nn = (PdfNumber) dic.get(iTextTag);
                dic.remove(iTextTag);
                dic.put(PdfName.TYPE, PdfName.ANNOT);
                adjustTabOrder(annots, ind, nn);
            } else {
                PdfDictionary field = (PdfDictionary) list.get(0);
                PdfArray kids = new PdfArray();
                for (int k = 1; k < list.size(); k += 2) {
                    int page = ((Integer) list.get(k)).intValue();
                    PdfArray annots = ((ImportedPage) this.importedPages.get(page - 1)).mergedFields;
                    PdfDictionary widget = new PdfDictionary();
                    widget.merge((PdfDictionary) list.get(k + 1));
                    widget.put(PdfName.PARENT, ind);
                    PdfNumber nn = (PdfNumber) widget.get(iTextTag);
                    widget.remove(iTextTag);
                    if (isTextField(field)) {
                        PdfString v = field.getAsString(PdfName.V);
                        PdfObject ap = widget.getDirectObject(PdfName.AP);
                        if (v != null && ap != null) {
                            if (!this.mergedTextFields.containsKey(list)) {
                                this.mergedTextFields.put(list, v);
                            } else {
                                try {
                                    TextField tx = new TextField(this, null, null);
                                    ((AcroFields) this.fields.get(0)).decodeGenericDictionary(widget, tx);
                                    Rectangle box = PdfReader.getNormalizedRectangle(widget.getAsArray(PdfName.RECT));
                                    if (tx.getRotation() == 90 || tx.getRotation() == 270) {
                                        box = box.rotate();
                                    }
                                    tx.setBox(box);
                                    tx.setText(((PdfString) this.mergedTextFields.get(list)).toUnicodeString());
                                    PdfAppearance app = tx.getAppearance();
                                    ((PdfDictionary) ap).put(PdfName.N, app.getIndirectReference());
                                } catch (DocumentException documentException) {
                                }
                            }

                        }
                    } else if (isCheckButton(field)) {
                        PdfName v = field.getAsName(PdfName.V);
                        PdfName as = widget.getAsName(PdfName.AS);
                        if (v != null && as != null) {
                            widget.put(PdfName.AS, v);
                        }
                    } else if (isRadioButton(field)) {
                        PdfName v = field.getAsName(PdfName.V);
                        PdfName as = widget.getAsName(PdfName.AS);
                        if (v != null && as != null && !as.equals(getOffStateName(widget))) {
                            if (!this.mergedRadioButtons.contains(list)) {
                                this.mergedRadioButtons.add(list);
                                widget.put(PdfName.AS, v);
                            } else {
                                widget.put(PdfName.AS, getOffStateName(widget));
                            }
                        }
                    }
                    widget.put(PdfName.TYPE, PdfName.ANNOT);
                    PdfIndirectReference wref = addToBody(widget, getPdfIndirectReference(), true).getIndirectReference();
                    adjustTabOrder(annots, wref, nn);
                    kids.add(wref);
                }
                dic.put(PdfName.KIDS, kids);
            }
            arr.add(ind);
            addToBody(dic, ind, true);
        }

        return arr;
    }

    private void adjustTabOrder(PdfArray annots, PdfIndirectReference ind, PdfNumber nn) {
        int v = nn.intValue();
        ArrayList<Integer> t = this.tabOrder.get(annots);
        if (t == null) {
            t = new ArrayList<Integer>();
            int size = annots.size() - 1;
            for (int k = 0; k < size; k++) {
                t.add(zero);
            }
            t.add(Integer.valueOf(v));
            this.tabOrder.put(annots, t);
            annots.add(ind);
        } else {

            int size = t.size() - 1;
            for (int k = size; k >= 0; k--) {
                if (((Integer) t.get(k)).intValue() <= v) {
                    t.add(k + 1, Integer.valueOf(v));
                    annots.add(k + 1, ind);
                    size = -2;
                    break;
                }
            }
            if (size != -2) {
                t.add(0, Integer.valueOf(v));
                annots.add(0, ind);
            }
        }
    }

    protected PdfDictionary getCatalog(PdfIndirectReference rootObj) {
        try {
            PdfDictionary theCat = this.pdf.getCatalog(rootObj);
            buildStructTreeRootForTagged(theCat);
            if (this.fieldArray != null) {
                addFieldResources(theCat);
            } else if (this.mergeFields && this.acroForm != null) {
                theCat.put(PdfName.ACROFORM, this.acroForm);
            }
            return theCat;
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    protected boolean isStructTreeRootReference(PdfIndirectReference prRef) {
        if (prRef == null || this.structTreeRootReference == null) {
            return false;
        }
        return (prRef.number == this.structTreeRootReference.number && prRef.generation == this.structTreeRootReference.generation);
    }

    private void addFieldResources(PdfDictionary catalog) throws IOException {
        if (this.fieldArray == null) {
            return;
        }
        PdfDictionary acroForm = new PdfDictionary();
        catalog.put(PdfName.ACROFORM, acroForm);
        acroForm.put(PdfName.FIELDS, this.fieldArray);
        acroForm.put(PdfName.DA, new PdfString("/Helv 0 Tf 0 g "));
        if (this.fieldTemplates.isEmpty()) {
            return;
        }
        PdfDictionary dr = new PdfDictionary();
        acroForm.put(PdfName.DR, dr);
        for (PdfTemplate template : this.fieldTemplates) {
            PdfFormField.mergeResources(dr, (PdfDictionary) template.getResources());
        }

        PdfDictionary fonts = dr.getAsDict(PdfName.FONT);
        if (fonts == null) {
            fonts = new PdfDictionary();
            dr.put(PdfName.FONT, fonts);
        }
        if (!fonts.contains(PdfName.HELV)) {
            PdfDictionary dic = new PdfDictionary(PdfName.FONT);
            dic.put(PdfName.BASEFONT, PdfName.HELVETICA);
            dic.put(PdfName.ENCODING, PdfName.WIN_ANSI_ENCODING);
            dic.put(PdfName.NAME, PdfName.HELV);
            dic.put(PdfName.SUBTYPE, PdfName.TYPE1);
            fonts.put(PdfName.HELV, addToBody(dic).getIndirectReference());
        }
        if (!fonts.contains(PdfName.ZADB)) {
            PdfDictionary dic = new PdfDictionary(PdfName.FONT);
            dic.put(PdfName.BASEFONT, PdfName.ZAPFDINGBATS);
            dic.put(PdfName.NAME, PdfName.ZADB);
            dic.put(PdfName.SUBTYPE, PdfName.TYPE1);
            fonts.put(PdfName.ZADB, addToBody(dic).getIndirectReference());
        }
    }

    public void close() {
        if (this.open) {
            this.pdf.close();
            super.close();
        }
    }

    public PdfIndirectReference add(PdfOutline outline) {
        return null;
    }

    public void addAnnotation(PdfAnnotation annot) {
    }

    PdfIndirectReference add(PdfPage page, PdfContents contents) throws PdfException {
        return null;
    }

    public void freeReader(PdfReader reader) throws IOException {
        if (this.mergeFields) {
            throw new UnsupportedOperationException(MessageLocalization.getComposedMessage("it.is.not.possible.to.free.reader.in.merge.fields.mode", new Object[0]));
        }
        PdfArray array = reader.trailer.getAsArray(PdfName.ID);
        if (array != null) {
            this.originalFileID = array.getAsString(0).getBytes();
        }
        this.indirectMap.remove(reader);

        this.currentPdfReaderInstance = null;

        super.freeReader(reader);
    }

    protected PdfName getOffStateName(PdfDictionary widget) {
        return PdfName.Off;
    }

    protected static final HashSet<PdfName> widgetKeys = new HashSet<PdfName>();
    protected static final HashSet<PdfName> fieldKeys = new HashSet<PdfName>();

    static {
        widgetKeys.add(PdfName.SUBTYPE);
        widgetKeys.add(PdfName.CONTENTS);
        widgetKeys.add(PdfName.RECT);
        widgetKeys.add(PdfName.NM);
        widgetKeys.add(PdfName.M);
        widgetKeys.add(PdfName.F);
        widgetKeys.add(PdfName.BS);
        widgetKeys.add(PdfName.BORDER);
        widgetKeys.add(PdfName.AP);
        widgetKeys.add(PdfName.AS);
        widgetKeys.add(PdfName.C);
        widgetKeys.add(PdfName.A);
        widgetKeys.add(PdfName.STRUCTPARENT);
        widgetKeys.add(PdfName.OC);
        widgetKeys.add(PdfName.H);
        widgetKeys.add(PdfName.MK);
        widgetKeys.add(PdfName.DA);
        widgetKeys.add(PdfName.Q);
        widgetKeys.add(PdfName.P);
        widgetKeys.add(PdfName.TYPE);
        widgetKeys.add(annotId);
        fieldKeys.add(PdfName.AA);
        fieldKeys.add(PdfName.FT);
        fieldKeys.add(PdfName.TU);
        fieldKeys.add(PdfName.TM);
        fieldKeys.add(PdfName.FF);
        fieldKeys.add(PdfName.V);
        fieldKeys.add(PdfName.DV);
        fieldKeys.add(PdfName.DS);
        fieldKeys.add(PdfName.RV);
        fieldKeys.add(PdfName.OPT);
        fieldKeys.add(PdfName.MAXLEN);
        fieldKeys.add(PdfName.TI);
        fieldKeys.add(PdfName.I);
        fieldKeys.add(PdfName.LOCK);
        fieldKeys.add(PdfName.SV);
    }

    static Integer getFlags(PdfDictionary field) {
        PdfName type = field.getAsName(PdfName.FT);
        if (!PdfName.BTN.equals(type)) {
            return null;
        }
        PdfNumber flags = field.getAsNumber(PdfName.FF);
        if (flags == null) {
            return null;
        }
        return Integer.valueOf(flags.intValue());
    }

    static boolean isCheckButton(PdfDictionary field) {
        Integer flags = getFlags(field);
        return (flags == null || ((flags.intValue() & 0x10000) == 0 && (flags.intValue() & 0x8000) == 0));
    }

    static boolean isRadioButton(PdfDictionary field) {
        Integer flags = getFlags(field);
        return (flags != null && (flags.intValue() & 0x10000) == 0 && (flags.intValue() & 0x8000) != 0);
    }

    static boolean isTextField(PdfDictionary field) {
        PdfName type = field.getAsName(PdfName.FT);
        return PdfName.TX.equals(type);
    }

    public PageStamp createPageStamp(PdfImportedPage iPage) {
        int pageNum = iPage.getPageNumber();
        PdfReader reader = iPage.getPdfReaderInstance().getReader();
        if (isTagged()) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("creating.page.stamp.not.allowed.for.tagged.reader", new Object[0]));
        }
        PdfDictionary pageN = reader.getPageN(pageNum);
        return new PageStamp(reader, pageN, this);
    }

    public static class PageStamp {

        PdfDictionary pageN;
        PdfCopy.StampContent under;
        PdfCopy.StampContent over;
        PageResources pageResources;
        PdfReader reader;
        PdfCopy cstp;

        PageStamp(PdfReader reader, PdfDictionary pageN, PdfCopy cstp) {
            this.pageN = pageN;
            this.reader = reader;
            this.cstp = cstp;
        }

        public PdfContentByte getUnderContent() {
            if (this.under == null) {
                if (this.pageResources == null) {
                    this.pageResources = new PageResources();
                    PdfDictionary resources = this.pageN.getAsDict(PdfName.RESOURCES);
                    this.pageResources.setOriginalResources(resources, this.cstp.namePtr);
                }
                this.under = new PdfCopy.StampContent(this.cstp, this.pageResources);
            }
            return this.under;
        }

        public PdfContentByte getOverContent() {
            if (this.over == null) {
                if (this.pageResources == null) {
                    this.pageResources = new PageResources();
                    PdfDictionary resources = this.pageN.getAsDict(PdfName.RESOURCES);
                    this.pageResources.setOriginalResources(resources, this.cstp.namePtr);
                }
                this.over = new PdfCopy.StampContent(this.cstp, this.pageResources);
            }
            return this.over;
        }

        public void alterContents() throws IOException {
            if (this.over == null && this.under == null) {
                return;
            }
            PdfArray ar = null;
            PdfObject content = PdfReader.getPdfObject(this.pageN.get(PdfName.CONTENTS), this.pageN);
            if (content == null) {
                ar = new PdfArray();
                this.pageN.put(PdfName.CONTENTS, ar);
            } else if (content.isArray()) {
                ar = (PdfArray) content;
            } else if (content.isStream()) {
                ar = new PdfArray();
                ar.add(this.pageN.get(PdfName.CONTENTS));
                this.pageN.put(PdfName.CONTENTS, ar);
            } else {
                ar = new PdfArray();
                this.pageN.put(PdfName.CONTENTS, ar);
            }
            ByteBuffer out = new ByteBuffer();
            if (this.under != null) {
                out.append(PdfContents.SAVESTATE);
                applyRotation(this.pageN, out);
                out.append(this.under.getInternalBuffer());
                out.append(PdfContents.RESTORESTATE);
            }
            if (this.over != null) {
                out.append(PdfContents.SAVESTATE);
            }
            PdfStream stream = new PdfStream(out.toByteArray());
            stream.flateCompress(this.cstp.getCompressionLevel());
            PdfIndirectReference ref1 = this.cstp.addToBody(stream).getIndirectReference();
            ar.addFirst(ref1);
            out.reset();
            if (this.over != null) {
                out.append(' ');
                out.append(PdfContents.RESTORESTATE);
                out.append(PdfContents.SAVESTATE);
                applyRotation(this.pageN, out);
                out.append(this.over.getInternalBuffer());
                out.append(PdfContents.RESTORESTATE);
                stream = new PdfStream(out.toByteArray());
                stream.flateCompress(this.cstp.getCompressionLevel());
                ar.add(this.cstp.addToBody(stream).getIndirectReference());
            }
            this.pageN.put(PdfName.RESOURCES, this.pageResources.getResources());
        }

        void applyRotation(PdfDictionary pageN, ByteBuffer out) {
            if (!this.cstp.rotateContents) {
                return;
            }
            Rectangle page = this.reader.getPageSizeWithRotation(pageN);
            int rotation = page.getRotation();
            switch (rotation) {
                case 90:
                    out.append(PdfContents.ROTATE90);
                    out.append(page.getTop());
                    out.append(' ').append('0').append(PdfContents.ROTATEFINAL);
                    break;
                case 180:
                    out.append(PdfContents.ROTATE180);
                    out.append(page.getRight());
                    out.append(' ');
                    out.append(page.getTop());
                    out.append(PdfContents.ROTATEFINAL);
                    break;
                case 270:
                    out.append(PdfContents.ROTATE270);
                    out.append('0').append(' ');
                    out.append(page.getRight());
                    out.append(PdfContents.ROTATEFINAL);
                    break;
            }
        }

        private void addDocumentField(PdfIndirectReference ref) {
            if (this.cstp.fieldArray == null) {
                this.cstp.fieldArray = new PdfArray();
            }
            this.cstp.fieldArray.add(ref);
        }

        private void expandFields(PdfFormField field, ArrayList<PdfAnnotation> allAnnots) {
            allAnnots.add(field);
            ArrayList<PdfFormField> kids = field.getKids();
            if (kids != null) {
                for (PdfFormField f : kids) {
                    expandFields(f, allAnnots);
                }
            }
        }

        public void addAnnotation(PdfAnnotation annot) {
            try {
                ArrayList<PdfAnnotation> allAnnots = new ArrayList<PdfAnnotation>();
                if (annot.isForm()) {
                    PdfFormField field = (PdfFormField) annot;
                    if (field.getParent() != null) {
                        return;
                    }
                    expandFields(field, allAnnots);
                    if (this.cstp.fieldTemplates == null) {
                        this.cstp.fieldTemplates = new HashSet<PdfTemplate>();
                    }
                } else {
                    allAnnots.add(annot);
                }
                for (int k = 0; k < allAnnots.size(); k++) {
                    annot = allAnnots.get(k);
                    if (annot.isForm()) {
                        if (!annot.isUsed()) {
                            HashSet<PdfTemplate> templates = annot.getTemplates();
                            if (templates != null) {
                                this.cstp.fieldTemplates.addAll(templates);
                            }
                        }
                        PdfFormField field = (PdfFormField) annot;
                        if (field.getParent() == null) {
                            addDocumentField(field.getIndirectReference());
                        }
                    }
                    if (annot.isAnnotation()) {
                        PdfObject pdfobj = PdfReader.getPdfObject(this.pageN.get(PdfName.ANNOTS), this.pageN);
                        PdfArray annots = null;
                        if (pdfobj == null || !pdfobj.isArray()) {
                            annots = new PdfArray();
                            this.pageN.put(PdfName.ANNOTS, annots);
                        } else {

                            annots = (PdfArray) pdfobj;
                        }
                        annots.add(annot.getIndirectReference());
                        if (!annot.isUsed()) {
                            PdfRectangle rect = (PdfRectangle) annot.get(PdfName.RECT);
                            if (rect != null && (rect.left() != 0.0F || rect.right() != 0.0F || rect.top() != 0.0F || rect.bottom() != 0.0F)) {
                                int rotation = this.reader.getPageRotation(this.pageN);
                                Rectangle pageSize = this.reader.getPageSizeWithRotation(this.pageN);
                                switch (rotation) {
                                    case 90:
                                        annot.put(PdfName.RECT, new PdfRectangle(pageSize
                                                .getTop() - rect.bottom(), rect
                                                .left(), pageSize
                                                        .getTop() - rect.top(), rect
                                                .right()));
                                        break;
                                    case 180:
                                        annot.put(PdfName.RECT, new PdfRectangle(pageSize
                                                .getRight() - rect.left(), pageSize
                                                .getTop() - rect.bottom(), pageSize
                                                .getRight() - rect.right(), pageSize
                                                .getTop() - rect.top()));
                                        break;
                                    case 270:
                                        annot.put(PdfName.RECT, new PdfRectangle(rect
                                                .bottom(), pageSize
                                                        .getRight() - rect.left(), rect
                                                .top(), pageSize
                                                        .getRight() - rect.right()));
                                        break;
                                }
                            }
                        }
                    }
                    if (!annot.isUsed()) {
                        annot.setUsed();
                        this.cstp.addToBody(annot, annot.getIndirectReference());
                    }

                }
            } catch (IOException e) {
                throw new ExceptionConverter(e);
            }
        }
    }

    public static class StampContent
            extends PdfContentByte {

        PageResources pageResources;

        StampContent(PdfWriter writer, PageResources pageResources) {
            super(writer);
            this.pageResources = pageResources;
        }

        public PdfContentByte getDuplicate() {
            return new StampContent(this.writer, this.pageResources);
        }

        PageResources getPageResources() {
            return this.pageResources;
        }
    }
}
