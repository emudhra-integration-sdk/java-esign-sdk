package esign.text.pdf.collection;

import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;

public class PdfCollectionSchema
        extends PdfDictionary {

    public PdfCollectionSchema() {
        super(PdfName.COLLECTIONSCHEMA);
    }

    public void addField(String name, PdfCollectionField field) {
        put(new PdfName(name), (PdfObject) field);
    }
}
