package esign.text.pdf.security;

import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import java.io.InputStream;
import java.security.GeneralSecurityException;

public class ExternalBlankSignatureContainer
        implements ExternalSignatureContainer {

    private PdfDictionary sigDic;

    public ExternalBlankSignatureContainer(PdfDictionary sigDic) {
        this.sigDic = sigDic;
    }

    public ExternalBlankSignatureContainer(PdfName filter, PdfName subFilter) {
        this.sigDic = new PdfDictionary();
        this.sigDic.put(PdfName.FILTER, (PdfObject) filter);
        this.sigDic.put(PdfName.SUBFILTER, (PdfObject) subFilter);
    }

    public byte[] sign(InputStream data) throws GeneralSecurityException {
        return new byte[0];
    }

    public void modifySigningDictionary(PdfDictionary signDic) {
        signDic.putAll(this.sigDic);
    }
}
