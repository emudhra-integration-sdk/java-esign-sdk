package esign.text.pdf.security;

import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;

public class PdfSignatureAppDictionary
        extends PdfDictionary {

    public void setSignatureCreator(String name) {
        put(PdfName.NAME, (PdfObject) new PdfString(name, "UnicodeBig"));
    }
}
