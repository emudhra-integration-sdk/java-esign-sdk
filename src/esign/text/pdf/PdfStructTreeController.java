package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;
import java.io.IOException;
import java.util.Map;

public class PdfStructTreeController {

    private PdfDictionary structTreeRoot;
    private PdfCopy writer;
    private PdfStructureTreeRoot structureTreeRoot;
    private PdfDictionary parentTree;
    protected PdfReader reader;
    private PdfDictionary roleMap = null;
    private PdfDictionary sourceRoleMap = null;
    private PdfDictionary sourceClassMap = null;
    private PdfIndirectReference nullReference = null;

    public enum returnType {
        BELOW, FOUND, ABOVE, NOTFOUND;
    }

    protected PdfStructTreeController(PdfReader reader, PdfCopy writer) throws BadPdfFormatException {
        if (!writer.isTagged()) {
            throw new BadPdfFormatException(MessageLocalization.getComposedMessage("no.structtreeroot.found", new Object[0]));
        }
        this.writer = writer;
        this.structureTreeRoot = writer.getStructureTreeRoot();
        this.structureTreeRoot.put(PdfName.PARENTTREE, new PdfDictionary(PdfName.STRUCTELEM));
        setReader(reader);
    }

    protected void setReader(PdfReader reader) throws BadPdfFormatException {
        this.reader = reader;
        PdfObject obj = reader.getCatalog().get(PdfName.STRUCTTREEROOT);
        obj = getDirectObject(obj);
        if (obj == null || !obj.isDictionary()) {
            throw new BadPdfFormatException(MessageLocalization.getComposedMessage("no.structtreeroot.found", new Object[0]));
        }
        this.structTreeRoot = (PdfDictionary) obj;
        obj = getDirectObject(this.structTreeRoot.get(PdfName.PARENTTREE));
        if (obj == null || !obj.isDictionary()) {
            throw new BadPdfFormatException(MessageLocalization.getComposedMessage("the.document.does.not.contain.parenttree", new Object[0]));
        }
        this.parentTree = (PdfDictionary) obj;
        this.sourceRoleMap = null;
        this.sourceClassMap = null;
        this.nullReference = null;
    }

    public static boolean checkTagged(PdfReader reader) {
        PdfObject obj = reader.getCatalog().get(PdfName.STRUCTTREEROOT);
        obj = getDirectObject(obj);
        if (obj == null || !obj.isDictionary()) {
            return false;
        }
        PdfDictionary structTreeRoot = (PdfDictionary) obj;
        obj = getDirectObject(structTreeRoot.get(PdfName.PARENTTREE));
        if (obj == null || !obj.isDictionary()) {
            return false;
        }
        return true;
    }

    public static PdfObject getDirectObject(PdfObject object) {
        if (object == null) {
            return null;
        }
        while (object.isIndirect()) {
            object = PdfReader.getPdfObjectRelease(object);
        }
        return object;
    }

    public void copyStructTreeForPage(PdfNumber sourceArrayNumber, int newArrayNumber) throws BadPdfFormatException, IOException {
        if (copyPageMarks(this.parentTree, sourceArrayNumber, newArrayNumber) == returnType.NOTFOUND) {
            throw new BadPdfFormatException(MessageLocalization.getComposedMessage("invalid.structparent", new Object[0]));
        }
    }

    private returnType copyPageMarks(PdfDictionary parentTree, PdfNumber arrayNumber, int newArrayNumber) throws BadPdfFormatException, IOException {
        PdfArray pages = (PdfArray) getDirectObject(parentTree.get(PdfName.NUMS));
        if (pages == null) {
            PdfArray kids = (PdfArray) getDirectObject(parentTree.get(PdfName.KIDS));
            if (kids == null) {
                return returnType.NOTFOUND;
            }
            int cur = kids.size() / 2;
            int begin = 0;
            while (true) {
                PdfDictionary kidTree = (PdfDictionary) getDirectObject(kids.getPdfObject(cur + begin));
                switch (copyPageMarks(kidTree, arrayNumber, newArrayNumber)) {
                    case FOUND:
                        return returnType.FOUND;
                    case ABOVE:
                        begin += cur;
                        cur /= 2;
                        if (cur == 0) {
                            cur = 1;
                        }
                        if (cur + begin == kids.size()) {
                            return returnType.ABOVE;
                        }
                        continue;
                    case BELOW:
                        if (cur + begin == 0) {
                            return returnType.BELOW;
                        }
                        if (cur == 0) {
                            return returnType.NOTFOUND;
                        }
                        cur /= 2;
                        continue;
                }
                break;
            }
            return returnType.NOTFOUND;
        }

        if (pages.size() == 0) {
            return returnType.NOTFOUND;
        }
        return findAndCopyMarks(pages, arrayNumber.intValue(), newArrayNumber);
    }

    private returnType findAndCopyMarks(PdfArray pages, int arrayNumber, int newArrayNumber) throws BadPdfFormatException, IOException {
        if (pages.getAsNumber(0).intValue() > arrayNumber) {
            return returnType.BELOW;
        }
        if (pages.getAsNumber(pages.size() - 2).intValue() < arrayNumber) {
            return returnType.ABOVE;
        }
        int cur = pages.size() / 4;
        int begin = 0;
        int curNumber;

        while (true) {
            curNumber = pages.getAsNumber((begin + cur) * 2).intValue();
            if (curNumber == arrayNumber) {
                PdfObject obj = pages.getPdfObject((begin + cur) * 2 + 1);
                PdfObject obj1 = obj;
                while (obj.isIndirect()) {
                    obj = PdfReader.getPdfObjectRelease(obj);
                }
                if (obj.isArray()) {
                    PdfObject firstNotNullKid = null;
                    for (PdfObject numObj : (PdfArray) obj) {
                        if (numObj.isNull()) {
                            if (nullReference == null) {
                                nullReference = writer.addToBody(new PdfNull()).getIndirectReference();
                            }
                            structureTreeRoot.setPageMark(newArrayNumber, nullReference);
                        } else {
                            PdfObject res = writer.copyObject(numObj, true, false);
                            if (firstNotNullKid == null) {
                                firstNotNullKid = res;
                            }
                            structureTreeRoot.setPageMark(newArrayNumber, (PdfIndirectReference) res);
                        }
                    }
                    attachStructTreeRootKids(firstNotNullKid);
                } else if (obj.isDictionary()) {
                    PdfDictionary k = getKDict((PdfDictionary) obj);
                    if (k == null) {
                        return returnType.NOTFOUND;
                    }
                    PdfObject res = writer.copyObject(obj1, true, false);
                    structureTreeRoot.setAnnotationMark(newArrayNumber, (PdfIndirectReference) res);
                } else {
                    return returnType.NOTFOUND;
                }
                return returnType.FOUND;
            }
            if (curNumber < arrayNumber) {
                if (cur == 0) {
                    return returnType.NOTFOUND;
                }
                begin += cur;
                if (cur != 1) {
                    cur /= 2;
                }
                if (cur + begin == pages.size()) {
                    return returnType.NOTFOUND;
                }
                continue;
            }
            if (cur + begin == 0) {
                return returnType.BELOW;
            }
            if (cur == 0) {
                return returnType.NOTFOUND;
            }
            cur /= 2;
        }
    }

    protected void attachStructTreeRootKids(PdfObject firstNotNullKid) throws IOException, BadPdfFormatException {
        PdfObject structKids = structTreeRoot.get(PdfName.K);
        if (structKids == null || (!structKids.isArray() && !structKids.isIndirect())) {
            // incorrect syntax of tags
            addKid(structureTreeRoot, firstNotNullKid);
        } else {
            if (structKids.isIndirect()) {
                addKid(structKids);
            } else { //structKids.isArray()
                for (PdfObject kid : (PdfArray) structKids) {
                    addKid(kid);
                }
            }
        }
    }

    static PdfDictionary getKDict(PdfDictionary obj) {
        PdfDictionary k = obj.getAsDict(PdfName.K);
        if (k != null) {
            if (PdfName.OBJR.equals(k.getAsName(PdfName.TYPE))) {
                return k;
            }
        } else {
            PdfArray k1 = obj.getAsArray(PdfName.K);
            if (k1 == null) {
                return null;
            }
            for (int i = 0; i < k1.size(); i++) {
                k = k1.getAsDict(i);
                if (k != null
                        && PdfName.OBJR.equals(k.getAsName(PdfName.TYPE))) {
                    return k;
                }
            }
        }

        return null;
    }

    private void addKid(PdfObject obj) throws IOException, BadPdfFormatException {
        if (!obj.isIndirect()) {
            return;
        }
        PRIndirectReference currRef = (PRIndirectReference) obj;
        RefKey key = new RefKey(currRef);
        if (!this.writer.indirects.containsKey(key)) {
            this.writer.copyIndirect(currRef, true, false);
        }
        PdfIndirectReference newKid = ((PdfCopy.IndirectReferences) this.writer.indirects.get(key)).getRef();

        if (this.writer.updateRootKids) {
            addKid(this.structureTreeRoot, newKid);
            this.writer.structureTreeRootKidsForReaderImported(this.reader);
        }
    }

    private static PdfArray getDirectArray(PdfArray in) {
        PdfArray out = new PdfArray();
        for (int i = 0; i < in.size(); i++) {
            PdfObject value = getDirectObject(in.getPdfObject(i));
            if (value != null) {
                if (value.isArray()) {
                    out.add(getDirectArray((PdfArray) value));
                } else if (value.isDictionary()) {
                    out.add(getDirectDict((PdfDictionary) value));
                } else {
                    out.add(value);
                }
            }
        }
        return out;
    }

    private static PdfDictionary getDirectDict(PdfDictionary in) {
        PdfDictionary out = new PdfDictionary();
        for (Map.Entry<PdfName, PdfObject> entry : in.hashMap.entrySet()) {
            PdfObject value = getDirectObject(entry.getValue());
            if (value == null) {
                continue;
            }
            if (value.isArray()) {
                out.put(entry.getKey(), getDirectArray((PdfArray) value));
                continue;
            }
            if (value.isDictionary()) {
                out.put(entry.getKey(), getDirectDict((PdfDictionary) value));
                continue;
            }
            out.put(entry.getKey(), value);
        }

        return out;
    }

    public static boolean compareObjects(PdfObject value1, PdfObject value2) {
        value2 = getDirectObject(value2);
        if (value2 == null) {
            return false;
        }
        if (value1.type() != value2.type()) {
            return false;
        }
        if (value1.isBoolean()) {
            if (value1 == value2) {
                return true;
            }
            if (value2 instanceof PdfBoolean) {
                return (((PdfBoolean) value1).booleanValue() == ((PdfBoolean) value2).booleanValue());
            }
            return false;
        }
        if (value1.isName()) {
            return value1.equals(value2);
        }
        if (value1.isNumber()) {
            if (value1 == value2) {
                return true;
            }
            if (value2 instanceof PdfNumber) {
                return (((PdfNumber) value1).doubleValue() == ((PdfNumber) value2).doubleValue());
            }
            return false;
        }
        if (value1.isNull()) {
            if (value1 == value2) {
                return true;
            }
            if (value2 instanceof PdfNull) {
                return true;
            }
            return false;
        }
        if (value1.isString()) {
            if (value1 == value2) {
                return true;
            }
            if (value2 instanceof PdfString) {
                return ((((PdfString) value2).value == null && ((PdfString) value1).value == null) || (((PdfString) value1).value != null && ((PdfString) value1).value
                        .equals(((PdfString) value2).value)));
            }
            return false;
        }
        if (value1.isArray()) {
            PdfArray array1 = (PdfArray) value1;
            PdfArray array2 = (PdfArray) value2;
            if (array1.size() != array2.size()) {
                return false;
            }
            for (int i = 0; i < array1.size(); i++) {
                if (!compareObjects(array1.getPdfObject(i), array2.getPdfObject(i))) {
                    return false;
                }
            }
            return true;
        }
        if (value1.isDictionary()) {
            PdfDictionary first = (PdfDictionary) value1;
            PdfDictionary second = (PdfDictionary) value2;
            if (first.size() != second.size()) {
                return false;
            }
            for (PdfName name : first.hashMap.keySet()) {
                if (!compareObjects(first.get(name), second.get(name))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    protected void addClass(PdfObject object) throws BadPdfFormatException {
        object = getDirectObject(object);
        if (object.isDictionary()) {
            PdfObject curClass = ((PdfDictionary) object).get(PdfName.C);
            if (curClass == null) {
                return;
            }
            if (curClass.isArray()) {
                PdfArray array = (PdfArray) curClass;
                for (int i = 0; i < array.size(); i++) {
                    addClass(array.getPdfObject(i));
                }
            } else if (curClass.isName()) {
                addClass(curClass);
            }
        } else if (object.isName()) {
            PdfName name = (PdfName) object;
            if (this.sourceClassMap == null) {
                object = getDirectObject(this.structTreeRoot.get(PdfName.CLASSMAP));
                if (object == null || !object.isDictionary()) {
                    return;
                }
                this.sourceClassMap = (PdfDictionary) object;
            }
            object = getDirectObject(this.sourceClassMap.get(name));
            if (object == null) {
                return;
            }
            PdfObject put = this.structureTreeRoot.getMappedClass(name);
            if (put != null) {
                if (!compareObjects(put, object)) {
                    throw new BadPdfFormatException(MessageLocalization.getComposedMessage("conflict.in.classmap", new Object[]{name}));
                }
            } else if (object.isDictionary()) {
                this.structureTreeRoot.mapClass(name, getDirectDict((PdfDictionary) object));
            } else if (object.isArray()) {
                this.structureTreeRoot.mapClass(name, getDirectArray((PdfArray) object));
            }
        }
    }

    protected void addRole(PdfName structType) throws BadPdfFormatException {
        if (structType == null) {
            return;
        }
        for (PdfName name : this.writer.getStandardStructElems()) {
            if (name.equals(structType)) {
                return;
            }
        }
        if (this.sourceRoleMap == null) {
            PdfObject pdfObject = getDirectObject(this.structTreeRoot.get(PdfName.ROLEMAP));
            if (pdfObject == null || !pdfObject.isDictionary()) {
                return;
            }
            this.sourceRoleMap = (PdfDictionary) pdfObject;
        }
        PdfObject object = this.sourceRoleMap.get(structType);
        if (object == null || !object.isName()) {
            return;
        }

        if (this.roleMap == null) {
            this.roleMap = new PdfDictionary();
            this.structureTreeRoot.put(PdfName.ROLEMAP, this.roleMap);
            this.roleMap.put(structType, object);
        } else {
            PdfObject currentRole;
            if ((currentRole = this.roleMap.get(structType)) != null) {
                if (!currentRole.equals(object)) {
                    throw new BadPdfFormatException(MessageLocalization.getComposedMessage("conflict.in.rolemap", new Object[]{structType}));
                }
            } else {
                this.roleMap.put(structType, object);
            }
        }

    }

    protected void addKid(PdfDictionary parent, PdfObject kid) {
        PdfArray kids;
        PdfObject kidObj = parent.get(PdfName.K);

        if (kidObj instanceof PdfArray) {
            kids = (PdfArray) kidObj;
        } else {
            kids = new PdfArray();
            if (kidObj != null) {
                kids.add(kidObj);
            }
        }
        kids.add(kid);
        parent.put(PdfName.K, kids);
    }
}
