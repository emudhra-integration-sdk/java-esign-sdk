package esign.text.pdf;

import esign.text.DocListener;
import esign.text.Document;
import esign.text.DocumentException;
import esign.text.ExceptionConverter;
import esign.text.error_messages.MessageLocalization;
import esign.text.exceptions.BadPasswordException;
import esign.text.log.Counter;
import esign.text.log.CounterFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

@Deprecated
class PdfCopyFieldsImp
        extends PdfWriter {

    private static final PdfName iTextTag = new PdfName("_iTextTag_");
    private static final Integer zero = Integer.valueOf(0);
    ArrayList<PdfReader> readers = new ArrayList<PdfReader>();
    HashMap<PdfReader, IntHashtable> readers2intrefs = new HashMap<PdfReader, IntHashtable>();
    HashMap<PdfReader, IntHashtable> pages2intrefs = new HashMap<PdfReader, IntHashtable>();
    HashMap<PdfReader, IntHashtable> visited = new HashMap<PdfReader, IntHashtable>();
    ArrayList<AcroFields> fields = new ArrayList<AcroFields>();
    RandomAccessFileOrArray file;
    HashMap<String, Object> fieldTree = new HashMap<String, Object>();
    ArrayList<PdfIndirectReference> pageRefs = new ArrayList<PdfIndirectReference>();
    ArrayList<PdfDictionary> pageDics = new ArrayList<PdfDictionary>();
    PdfDictionary resources = new PdfDictionary();
    PdfDictionary form;
    boolean closing = false;
    Document nd;
    private HashMap<PdfArray, ArrayList<Integer>> tabOrder;
    private ArrayList<String> calculationOrder = new ArrayList<String>();
    private ArrayList<Object> calculationOrderRefs;
    private boolean hasSignature;
    private boolean needAppearances = false;
    private HashSet<Object> mergedRadioButtons = new HashSet();

    protected Counter COUNTER = CounterFactory.getCounter(PdfCopyFields.class);

    protected Counter getCounter() {
        return this.COUNTER;
    }

    PdfCopyFieldsImp(OutputStream os) throws DocumentException {
        this(os, (char) 0);
    }

    PdfCopyFieldsImp(OutputStream os, char pdfVersion) throws DocumentException {
        super(new PdfDocument(), os);
        this.pdf.addWriter(this);
        if (pdfVersion != '\000') {
            setPdfVersion(pdfVersion);
        }
        this.nd = new Document();
        this.nd.addDocListener((DocListener) this.pdf);
    }

    void addDocument(PdfReader reader, List<Integer> pagesToKeep) throws DocumentException, IOException {
        if (!this.readers2intrefs.containsKey(reader) && reader.isTampered()) {
            throw new DocumentException(MessageLocalization.getComposedMessage("the.document.was.reused", new Object[0]));
        }
        reader = new PdfReader(reader);
        reader.selectPages(pagesToKeep);
        if (reader.getNumberOfPages() == 0) {
            return;
        }
        reader.setTampered(false);
        addDocument(reader);
    }

    void addDocument(PdfReader reader) throws DocumentException, IOException {
        if (!reader.isOpenedWithFullPermissions()) {
            throw new BadPasswordException(MessageLocalization.getComposedMessage("pdfreader.not.opened.with.owner.password", new Object[0]));
        }
        openDoc();
        if (this.readers2intrefs.containsKey(reader)) {
            reader = new PdfReader(reader);
        } else {

            if (reader.isTampered()) {
                throw new DocumentException(MessageLocalization.getComposedMessage("the.document.was.reused", new Object[0]));
            }
            reader.consolidateNamedDestinations();
            reader.setTampered(true);
        }
        reader.shuffleSubsetNames();
        this.readers2intrefs.put(reader, new IntHashtable());
        this.readers.add(reader);
        int len = reader.getNumberOfPages();
        IntHashtable refs = new IntHashtable();
        for (int p = 1; p <= len; p++) {
            refs.put(reader.getPageOrigRef(p).getNumber(), 1);
            reader.releasePage(p);
        }
        this.pages2intrefs.put(reader, refs);
        this.visited.put(reader, new IntHashtable());
        AcroFields acro = reader.getAcroFields();

        boolean needapp = !acro.isGenerateAppearances();
        if (needapp) {
            this.needAppearances = true;
        }
        this.fields.add(acro);
        updateCalculationOrder(reader);
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
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    protected void updateCalculationOrder(PdfReader reader) {
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

    void propagate(PdfObject obj, PdfIndirectReference refo, boolean restricted) throws IOException {
        PdfDictionary dic;
        Iterator<PdfObject> it;
        if (obj == null) {
            return;
        }

        if (obj instanceof PdfIndirectReference) {
            return;
        }
        switch (obj.type()) {
            case 6:
            case 7:
                dic = (PdfDictionary) obj;
                for (PdfName key : dic.getKeys()) {
                    if (restricted && (key.equals(PdfName.PARENT) || key.equals(PdfName.KIDS))) {
                        continue;
                    }
                    PdfObject ob = dic.get(key);
                    if (ob != null && ob.isIndirect()) {
                        PRIndirectReference ind = (PRIndirectReference) ob;
                        if (!setVisited(ind) && !isPage(ind)) {
                            PdfIndirectReference ref = getNewReference(ind);
                            propagate(PdfReader.getPdfObjectRelease(ind), ref, restricted);
                        }
                        continue;
                    }
                    propagate(ob, (PdfIndirectReference) null, restricted);
                }
                break;

            case 5:
                for (it = ((PdfArray) obj).listIterator(); it.hasNext();) {
                    PdfObject ob = it.next();
                    if (ob != null && ob.isIndirect()) {
                        PRIndirectReference ind = (PRIndirectReference) ob;
                        if (!isVisited(ind) && !isPage(ind)) {
                            PdfIndirectReference ref = getNewReference(ind);
                            propagate(PdfReader.getPdfObjectRelease(ind), ref, restricted);
                        }
                        continue;
                    }
                    propagate(ob, (PdfIndirectReference) null, restricted);
                }
                break;

            case 10:
                throw new RuntimeException(MessageLocalization.getComposedMessage("reference.pointing.to.reference", new Object[0]));
        }
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

    protected PdfArray branchForm(HashMap<String, Object> level, PdfIndirectReference parent, String fname) throws IOException {
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
                addToBody(dic, ind);
                continue;
            }
            ArrayList<Object> list = (ArrayList<Object>) obj;
            dic.mergeDifferent((PdfDictionary) list.get(0));
            if (list.size() == 3) {
                dic.mergeDifferent((PdfDictionary) list.get(2));
                int page = ((Integer) list.get(1)).intValue();
                PdfDictionary pageDic = this.pageDics.get(page - 1);
                PdfArray annots = pageDic.getAsArray(PdfName.ANNOTS);
                if (annots == null) {
                    annots = new PdfArray();
                    pageDic.put(PdfName.ANNOTS, annots);
                }
                PdfNumber nn = (PdfNumber) dic.get(iTextTag);
                dic.remove(iTextTag);
                adjustTabOrder(annots, ind, nn);
            } else {

                PdfDictionary field = (PdfDictionary) list.get(0);
                PdfName v = field.getAsName(PdfName.V);
                PdfArray kids = new PdfArray();
                for (int k = 1; k < list.size(); k += 2) {
                    int page = ((Integer) list.get(k)).intValue();
                    PdfDictionary pageDic = this.pageDics.get(page - 1);
                    PdfArray annots = pageDic.getAsArray(PdfName.ANNOTS);
                    if (annots == null) {
                        annots = new PdfArray();
                        pageDic.put(PdfName.ANNOTS, annots);
                    }
                    PdfDictionary widget = new PdfDictionary();
                    widget.merge((PdfDictionary) list.get(k + 1));
                    widget.put(PdfName.PARENT, ind);
                    PdfNumber nn = (PdfNumber) widget.get(iTextTag);
                    widget.remove(iTextTag);
                    if (PdfCopy.isCheckButton(field)) {
                        PdfName as = widget.getAsName(PdfName.AS);
                        if (v != null && as != null) {
                            widget.put(PdfName.AS, v);
                        }
                    } else if (PdfCopy.isRadioButton(field)) {
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
                    PdfIndirectReference wref = addToBody(widget).getIndirectReference();
                    adjustTabOrder(annots, wref, nn);
                    kids.add(wref);
                    propagate(widget, (PdfIndirectReference) null, false);
                }
                dic.put(PdfName.KIDS, kids);
            }
            arr.add(ind);
            addToBody(dic, ind);
            propagate(dic, (PdfIndirectReference) null, false);
        }

        return arr;
    }

    protected PdfName getOffStateName(PdfDictionary widget) {
        return PdfName.Off;
    }

    protected void createAcroForms() throws IOException {
        if (this.fieldTree.isEmpty()) {
            return;
        }
        this.form = new PdfDictionary();
        this.form.put(PdfName.DR, this.resources);
        propagate(this.resources, (PdfIndirectReference) null, false);
        if (this.needAppearances) {
            this.form.put(PdfName.NEEDAPPEARANCES, PdfBoolean.PDFTRUE);
        }
        this.form.put(PdfName.DA, new PdfString("/Helv 0 Tf 0 g "));
        this.tabOrder = new HashMap<PdfArray, ArrayList<Integer>>();
        this.calculationOrderRefs = new ArrayList(this.calculationOrder);
        this.form.put(PdfName.FIELDS, branchForm(this.fieldTree, (PdfIndirectReference) null, ""));
        if (this.hasSignature) {
            this.form.put(PdfName.SIGFLAGS, new PdfNumber(3));
        }
        PdfArray co = new PdfArray();
        for (int k = 0; k < this.calculationOrderRefs.size(); k++) {
            Object obj = this.calculationOrderRefs.get(k);
            if (obj instanceof PdfIndirectReference) {
                co.add((PdfIndirectReference) obj);
            }
        }
        if (co.size() > 0) {
            this.form.put(PdfName.CO, co);
        }
    }

    public void close() {
        if (this.closing) {
            super.close();
            return;
        }
        this.closing = true;
        try {
            closeIt();
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    protected void closeIt() throws IOException {
        for (int k = 0; k < this.readers.size(); k++) {
            ((PdfReader) this.readers.get(k)).removeFields();
        }
        int r;
        for (r = 0; r < this.readers.size(); r++) {
            PdfReader reader = this.readers.get(r);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                this.pageRefs.add(getNewReference(reader.getPageOrigRef(page)));
                this.pageDics.add(reader.getPageN(page));
            }
        }
        mergeFields();
        createAcroForms();
        for (r = 0; r < this.readers.size(); r++) {
            PdfReader reader = this.readers.get(r);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                PdfDictionary dic = reader.getPageN(page);
                PdfIndirectReference pageRef = getNewReference(reader.getPageOrigRef(page));
                PdfIndirectReference parent = this.root.addPageRef(pageRef);
                dic.put(PdfName.PARENT, parent);
                propagate(dic, pageRef, false);
            }
        }
        for (Map.Entry<PdfReader, IntHashtable> entry : this.readers2intrefs.entrySet()) {
            PdfReader reader = entry.getKey();
            try {
                this.file = reader.getSafeFile();
                this.file.reOpen();
                IntHashtable t = entry.getValue();
                int[] keys = t.toOrderedKeys();
                for (int i = 0; i < keys.length; i++) {
                    PRIndirectReference ref = new PRIndirectReference(reader, keys[i]);
                    addToBody(PdfReader.getPdfObjectRelease(ref), t.get(keys[i]));
                }
            } finally {

                try {
                    this.file.close();

                } catch (Exception exception) {
                }
            }
        }

        this.pdf.close();
    }

    void addPageOffsetToField(Map<String, AcroFields.Item> fd, int pageOffset) {
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

    void createWidgets(ArrayList<Object> list, AcroFields.Item item) {
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
                if (widgetKeys.containsKey(key)) {
                    widget.put(key, merged.get(key));
                }
            }
            widget.put(iTextTag, new PdfNumber(item.getTabOrder(k).intValue() + 1));
            list.add(widget);
        }
    }

    void mergeField(String name, AcroFields.Item item) {
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
                    obj = (HashMap<Object, Object>) new HashMap<Object, Object>();
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
                if (fieldKeys.containsKey(key)) {
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

    void mergeWithMaster(Map<String, AcroFields.Item> fd) {
        for (Map.Entry<String, AcroFields.Item> entry : fd.entrySet()) {
            String name = entry.getKey();
            mergeField(name, entry.getValue());
        }
    }

    void mergeFields() {
        int pageOffset = 0;
        for (int k = 0; k < this.fields.size(); k++) {
            Map<String, AcroFields.Item> fd = ((AcroFields) this.fields.get(k)).getFields();
            addPageOffsetToField(fd, pageOffset);
            mergeWithMaster(fd);
            pageOffset += ((PdfReader) this.readers.get(k)).getNumberOfPages();
        }
    }

    public PdfIndirectReference getPageReference(int page) {
        return this.pageRefs.get(page - 1);
    }

    protected PdfDictionary getCatalog(PdfIndirectReference rootObj) {
        try {
            PdfDictionary cat = this.pdf.getCatalog(rootObj);
            if (this.form != null) {
                PdfIndirectReference ref = addToBody(this.form).getIndirectReference();
                cat.put(PdfName.ACROFORM, ref);
            }
            return cat;
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    protected PdfIndirectReference getNewReference(PRIndirectReference ref) {
        return new PdfIndirectReference(0, getNewObjectNumber(ref.getReader(), ref.getNumber(), 0));
    }

    protected int getNewObjectNumber(PdfReader reader, int number, int generation) {
        IntHashtable refs = this.readers2intrefs.get(reader);
        int n = refs.get(number);
        if (n == 0) {
            n = getIndirectReferenceNumber();
            refs.put(number, n);
        }
        return n;
    }

    protected boolean setVisited(PRIndirectReference ref) {
        IntHashtable refs = this.visited.get(ref.getReader());
        if (refs != null) {
            return (refs.put(ref.getNumber(), 1) != 0);
        }
        return false;
    }

    protected boolean isVisited(PRIndirectReference ref) {
        IntHashtable refs = this.visited.get(ref.getReader());
        if (refs != null) {
            return refs.containsKey(ref.getNumber());
        }
        return false;
    }

    protected boolean isVisited(PdfReader reader, int number, int generation) {
        IntHashtable refs = this.readers2intrefs.get(reader);
        return refs.containsKey(number);
    }

    protected boolean isPage(PRIndirectReference ref) {
        IntHashtable refs = this.pages2intrefs.get(ref.getReader());
        if (refs != null) {
            return refs.containsKey(ref.getNumber());
        }
        return false;
    }

    RandomAccessFileOrArray getReaderFile(PdfReader reader) {
        return this.file;
    }

    public void openDoc() {
        if (!this.nd.isOpen()) {
            this.nd.open();
        }
    }

    protected static final HashMap<PdfName, Integer> widgetKeys = new HashMap<PdfName, Integer>();
    protected static final HashMap<PdfName, Integer> fieldKeys = new HashMap<PdfName, Integer>();

    static {
        Integer one = Integer.valueOf(1);
        widgetKeys.put(PdfName.SUBTYPE, one);
        widgetKeys.put(PdfName.CONTENTS, one);
        widgetKeys.put(PdfName.RECT, one);
        widgetKeys.put(PdfName.NM, one);
        widgetKeys.put(PdfName.M, one);
        widgetKeys.put(PdfName.F, one);
        widgetKeys.put(PdfName.BS, one);
        widgetKeys.put(PdfName.BORDER, one);
        widgetKeys.put(PdfName.AP, one);
        widgetKeys.put(PdfName.AS, one);
        widgetKeys.put(PdfName.C, one);
        widgetKeys.put(PdfName.A, one);
        widgetKeys.put(PdfName.STRUCTPARENT, one);
        widgetKeys.put(PdfName.OC, one);
        widgetKeys.put(PdfName.H, one);
        widgetKeys.put(PdfName.MK, one);
        widgetKeys.put(PdfName.DA, one);
        widgetKeys.put(PdfName.Q, one);
        widgetKeys.put(PdfName.P, one);
        fieldKeys.put(PdfName.AA, one);
        fieldKeys.put(PdfName.FT, one);
        fieldKeys.put(PdfName.TU, one);
        fieldKeys.put(PdfName.TM, one);
        fieldKeys.put(PdfName.FF, one);
        fieldKeys.put(PdfName.V, one);
        fieldKeys.put(PdfName.DV, one);
        fieldKeys.put(PdfName.DS, one);
        fieldKeys.put(PdfName.RV, one);
        fieldKeys.put(PdfName.OPT, one);
        fieldKeys.put(PdfName.MAXLEN, one);
        fieldKeys.put(PdfName.TI, one);
        fieldKeys.put(PdfName.I, one);
        fieldKeys.put(PdfName.LOCK, one);
        fieldKeys.put(PdfName.SV, one);
    }
}
