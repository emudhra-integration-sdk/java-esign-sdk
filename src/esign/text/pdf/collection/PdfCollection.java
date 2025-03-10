package esign.text.pdf.collection;

import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;

public class PdfCollection
        extends PdfDictionary {

    public static final int DETAILS = 0;
    public static final int TILE = 1;
    public static final int HIDDEN = 2;
    public static final int CUSTOM = 3;

    public PdfCollection(int type) {
        super(PdfName.COLLECTION);
        switch (type) {
            case 1:
                put(PdfName.VIEW, (PdfObject) PdfName.T);
                return;
            case 2:
                put(PdfName.VIEW, (PdfObject) PdfName.H);
                return;
            case 3:
                put(PdfName.VIEW, (PdfObject) PdfName.C);
                return;
        }
        put(PdfName.VIEW, (PdfObject) PdfName.D);
    }

    public void setInitialDocument(String description) {
        put(PdfName.D, (PdfObject) new PdfString(description, null));
    }

    public void setSchema(PdfCollectionSchema schema) {
        put(PdfName.SCHEMA, (PdfObject) schema);
    }

    public PdfCollectionSchema getSchema() {
        return (PdfCollectionSchema) get(PdfName.SCHEMA);
    }

    public void setSort(PdfCollectionSort sort) {
        put(PdfName.SORT, (PdfObject) sort);
    }
}
