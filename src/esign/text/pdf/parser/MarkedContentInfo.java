package esign.text.pdf.parser;

import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;

public class MarkedContentInfo {

    private final PdfName tag;
    private final PdfDictionary dictionary;

    public MarkedContentInfo(PdfName tag, PdfDictionary dictionary) {
        this.tag = tag;
        this.dictionary = (dictionary != null) ? dictionary : new PdfDictionary();
    }

    public PdfName getTag() {
        return this.tag;
    }

    public boolean hasMcid() {
        return this.dictionary.contains(PdfName.MCID);
    }

    public int getMcid() {
        PdfNumber id = this.dictionary.getAsNumber(PdfName.MCID);
        if (id == null) {
            throw new IllegalStateException("MarkedContentInfo does not contain MCID");
        }
        return id.intValue();
    }
}
