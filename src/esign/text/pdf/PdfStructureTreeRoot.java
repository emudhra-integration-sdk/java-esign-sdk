package esign.text.pdf;

import esign.text.pdf.interfaces.IPdfStructureElement;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PdfStructureTreeRoot
        extends PdfDictionary
        implements IPdfStructureElement {

    private HashMap<Integer, PdfObject> parentTree = new HashMap<Integer, PdfObject>();
    private PdfIndirectReference reference;
    private PdfDictionary classMap = null;
    protected HashMap<PdfName, PdfObject> classes = null;
    private HashMap<Integer, PdfIndirectReference> numTree = null;

    private HashMap<String, PdfObject> idTreeMap;

    private PdfWriter writer;

    PdfStructureTreeRoot(PdfWriter writer) {
        super(PdfName.STRUCTTREEROOT);
        this.writer = writer;
        this.reference = writer.getPdfIndirectReference();
    }

    private void createNumTree() throws IOException {
        if (this.numTree != null) {
            return;
        }
        this.numTree = new HashMap<Integer, PdfIndirectReference>();
        for (Integer i : this.parentTree.keySet()) {
            PdfObject obj = this.parentTree.get(i);
            if (obj.isArray()) {
                PdfArray ar = (PdfArray) obj;
                this.numTree.put(i, this.writer.addToBody(ar).getIndirectReference());
                continue;
            }
            if (obj instanceof PdfIndirectReference) {
                this.numTree.put(i, (PdfIndirectReference) obj);
            }
        }
    }

    public void mapRole(PdfName used, PdfName standard) {
        PdfDictionary rm = (PdfDictionary) get(PdfName.ROLEMAP);
        if (rm == null) {
            rm = new PdfDictionary();
            put(PdfName.ROLEMAP, rm);
        }
        rm.put(used, standard);
    }

    public void mapClass(PdfName name, PdfObject object) {
        if (this.classMap == null) {
            this.classMap = new PdfDictionary();
            this.classes = new HashMap<PdfName, PdfObject>();
        }
        this.classes.put(name, object);
    }

    void putIDTree(String record, PdfObject reference) {
        if (this.idTreeMap == null) {
            this.idTreeMap = new HashMap<String, PdfObject>();
        }
        this.idTreeMap.put(record, reference);
    }

    public PdfObject getMappedClass(PdfName name) {
        if (this.classes == null) {
            return null;
        }
        return this.classes.get(name);
    }

    public PdfWriter getWriter() {
        return this.writer;
    }

    public HashMap<Integer, PdfIndirectReference> getNumTree() throws IOException {
        if (this.numTree == null) {
            createNumTree();
        }
        return this.numTree;
    }

    public PdfIndirectReference getReference() {
        return this.reference;
    }

    void setPageMark(int page, PdfIndirectReference struc) {
        Integer i = Integer.valueOf(page);
        PdfArray ar = (PdfArray) this.parentTree.get(i);
        if (ar == null) {
            ar = new PdfArray();
            this.parentTree.put(i, ar);
        }
        ar.add(struc);
    }

    void setAnnotationMark(int structParentIndex, PdfIndirectReference struc) {
        this.parentTree.put(Integer.valueOf(structParentIndex), struc);
    }

    void buildTree() throws IOException {
        createNumTree();
        PdfDictionary dicTree = PdfNumberTree.writeTree(this.numTree, this.writer);
        if (dicTree != null) {
            put(PdfName.PARENTTREE, this.writer.addToBody(dicTree).getIndirectReference());
        }
        if (this.classMap != null && !this.classes.isEmpty()) {
            for (Map.Entry<PdfName, PdfObject> entry : this.classes.entrySet()) {
                PdfObject value = entry.getValue();
                if (value.isDictionary()) {
                    this.classMap.put(entry.getKey(), this.writer.addToBody(value).getIndirectReference());
                    continue;
                }
                if (value.isArray()) {
                    PdfArray newArray = new PdfArray();
                    PdfArray array = (PdfArray) value;
                    for (int i = 0; i < array.size(); i++) {
                        if (array.getPdfObject(i).isDictionary()) {
                            newArray.add(this.writer.addToBody(array.getAsDict(i)).getIndirectReference());
                        }
                    }
                    this.classMap.put(entry.getKey(), newArray);
                }
            }
            put(PdfName.CLASSMAP, this.writer.addToBody(this.classMap).getIndirectReference());
        }
        if (this.idTreeMap != null && !this.idTreeMap.isEmpty()) {
            PdfDictionary dic = PdfNameTree.writeTree(this.idTreeMap, this.writer);
            put(PdfName.IDTREE, dic);
        }
        this.writer.addToBody(this, this.reference);
    }

    public PdfObject getAttribute(PdfName name) {
        PdfDictionary attr = getAsDict(PdfName.A);
        if (attr != null
                && attr.contains(name)) {
            return attr.get(name);
        }
        return null;
    }

    public void setAttribute(PdfName name, PdfObject obj) {
        PdfDictionary attr = getAsDict(PdfName.A);
        if (attr == null) {
            attr = new PdfDictionary();
            put(PdfName.A, attr);
        }
        attr.put(name, obj);
    }
}
