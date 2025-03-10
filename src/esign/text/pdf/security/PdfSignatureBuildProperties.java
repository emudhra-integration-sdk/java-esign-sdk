package esign.text.pdf.security;

import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;

public class PdfSignatureBuildProperties
        extends PdfDictionary {

    public void setSignatureCreator(String name) {
        getPdfSignatureAppProperty().setSignatureCreator(name);
    }

    private PdfSignatureAppDictionary getPdfSignatureAppProperty() {
        PdfSignatureAppDictionary appPropDic = (PdfSignatureAppDictionary) getAsDict(PdfName.APP);
        if (appPropDic == null) {
            appPropDic = new PdfSignatureAppDictionary();
            put(PdfName.APP, (PdfObject) appPropDic);
        }
        return appPropDic;
    }
}
