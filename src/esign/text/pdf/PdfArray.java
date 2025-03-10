package esign.text.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class PdfArray
        extends PdfObject
        implements Iterable<PdfObject> {

    protected ArrayList<PdfObject> arrayList;

    public PdfArray() {
        super(5);
        this.arrayList = new ArrayList<PdfObject>();
    }

    public PdfArray(int capacity) {
        super(5);
        this.arrayList = new ArrayList<PdfObject>(capacity);
    }

    public PdfArray(PdfObject object) {
        super(5);
        this.arrayList = new ArrayList<PdfObject>();
        this.arrayList.add(object);
    }

    public PdfArray(float[] values) {
        super(5);
        this.arrayList = new ArrayList<PdfObject>();
        add(values);
    }

    public PdfArray(int[] values) {
        super(5);
        this.arrayList = new ArrayList<PdfObject>();
        add(values);
    }

    public PdfArray(List<PdfObject> l) {
        this();
        for (PdfObject element : l) {
            add(element);
        }
    }

    public PdfArray(PdfArray array) {
        super(5);
        this.arrayList = new ArrayList<PdfObject>(array.arrayList);
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        PdfWriter.checkPdfIsoConformance(writer, 11, this);
        os.write(91);

        Iterator<PdfObject> i = this.arrayList.iterator();

        int type = 0;
        if (i.hasNext()) {
            PdfObject object = i.next();
            if (object == null) {
                object = PdfNull.PDFNULL;
            }
            object.toPdf(writer, os);
        }
        while (i.hasNext()) {
            PdfObject object = i.next();
            if (object == null) {
                object = PdfNull.PDFNULL;
            }
            type = object.type();
            if (type != 5 && type != 6 && type != 4 && type != 3) {
                os.write(32);
            }
            object.toPdf(writer, os);
        }
        os.write(93);
    }

    public String toString() {
        return this.arrayList.toString();
    }

    public PdfObject set(int idx, PdfObject obj) {
        return this.arrayList.set(idx, obj);
    }

    public PdfObject remove(int idx) {
        return this.arrayList.remove(idx);
    }

    @Deprecated
    public ArrayList<PdfObject> getArrayList() {
        return this.arrayList;
    }

    public int size() {
        return this.arrayList.size();
    }

    public boolean isEmpty() {
        return this.arrayList.isEmpty();
    }

    public boolean add(PdfObject object) {
        return this.arrayList.add(object);
    }

    public boolean add(float[] values) {
        for (int k = 0; k < values.length; k++) {
            this.arrayList.add(new PdfNumber(values[k]));
        }
        return true;
    }

    public boolean add(int[] values) {
        for (int k = 0; k < values.length; k++) {
            this.arrayList.add(new PdfNumber(values[k]));
        }
        return true;
    }

    public void add(int index, PdfObject element) {
        this.arrayList.add(index, element);
    }

    public void addFirst(PdfObject object) {
        this.arrayList.add(0, object);
    }

    public boolean contains(PdfObject object) {
        return this.arrayList.contains(object);
    }

    public ListIterator<PdfObject> listIterator() {
        return this.arrayList.listIterator();
    }

    public PdfObject getPdfObject(int idx) {
        return this.arrayList.get(idx);
    }

    public PdfObject getDirectObject(int idx) {
        return PdfReader.getPdfObject(getPdfObject(idx));
    }

    public PdfDictionary getAsDict(int idx) {
        PdfDictionary dict = null;
        PdfObject orig = getDirectObject(idx);
        if (orig != null && orig.isDictionary()) {
            dict = (PdfDictionary) orig;
        }
        return dict;
    }

    public PdfArray getAsArray(int idx) {
        PdfArray array = null;
        PdfObject orig = getDirectObject(idx);
        if (orig != null && orig.isArray()) {
            array = (PdfArray) orig;
        }
        return array;
    }

    public PdfStream getAsStream(int idx) {
        PdfStream stream = null;
        PdfObject orig = getDirectObject(idx);
        if (orig != null && orig.isStream()) {
            stream = (PdfStream) orig;
        }
        return stream;
    }

    public PdfString getAsString(int idx) {
        PdfString string = null;
        PdfObject orig = getDirectObject(idx);
        if (orig != null && orig.isString()) {
            string = (PdfString) orig;
        }
        return string;
    }

    public PdfNumber getAsNumber(int idx) {
        PdfNumber number = null;
        PdfObject orig = getDirectObject(idx);
        if (orig != null && orig.isNumber()) {
            number = (PdfNumber) orig;
        }
        return number;
    }

    public PdfName getAsName(int idx) {
        PdfName name = null;
        PdfObject orig = getDirectObject(idx);
        if (orig != null && orig.isName()) {
            name = (PdfName) orig;
        }
        return name;
    }

    public PdfBoolean getAsBoolean(int idx) {
        PdfBoolean bool = null;
        PdfObject orig = getDirectObject(idx);
        if (orig != null && orig.isBoolean()) {
            bool = (PdfBoolean) orig;
        }
        return bool;
    }

    public PdfIndirectReference getAsIndirectObject(int idx) {
        PdfIndirectReference ref = null;
        PdfObject orig = getPdfObject(idx);
        if (orig instanceof PdfIndirectReference) {
            ref = (PdfIndirectReference) orig;
        }
        return ref;
    }

    public Iterator<PdfObject> iterator() {
        return this.arrayList.iterator();
    }

    public long[] asLongArray() {
        long[] rslt = new long[size()];
        for (int k = 0; k < rslt.length; k++) {
            rslt[k] = getAsNumber(k).longValue();
        }
        return rslt;
    }

    public double[] asDoubleArray() {
        double[] rslt = new double[size()];
        for (int k = 0; k < rslt.length; k++) {
            rslt[k] = getAsNumber(k).doubleValue();
        }
        return rslt;
    }
}
