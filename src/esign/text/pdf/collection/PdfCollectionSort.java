package esign.text.pdf.collection;

import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfBoolean;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;

public class PdfCollectionSort
        extends PdfDictionary {

    public PdfCollectionSort(String key) {
        super(PdfName.COLLECTIONSORT);
        put(PdfName.S, (PdfObject) new PdfName(key));
    }

    public PdfCollectionSort(String[] keys) {
        super(PdfName.COLLECTIONSORT);
        PdfArray array = new PdfArray();
        for (int i = 0; i < keys.length; i++) {
            array.add((PdfObject) new PdfName(keys[i]));
        }
        put(PdfName.S, (PdfObject) array);
    }

    public void setSortOrder(boolean ascending) {
        PdfObject o = get(PdfName.S);
        if (o instanceof PdfName) {
            put(PdfName.A, (PdfObject) new PdfBoolean(ascending));
        } else {

            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("you.have.to.define.a.boolean.array.for.this.collection.sort.dictionary", new Object[0]));
        }
    }

    public void setSortOrder(boolean[] ascending) {
        PdfObject o = get(PdfName.S);
        if (o instanceof PdfArray) {
            if (((PdfArray) o).size() != ascending.length) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.number.of.booleans.in.this.array.doesn.t.correspond.with.the.number.of.fields", new Object[0]));
            }
            PdfArray array = new PdfArray();
            for (int i = 0; i < ascending.length; i++) {
                array.add((PdfObject) new PdfBoolean(ascending[i]));
            }
            put(PdfName.A, (PdfObject) array);
        } else {

            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("you.need.a.single.boolean.for.this.collection.sort.dictionary", new Object[0]));
        }
    }
}
