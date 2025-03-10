package esign.text.pdf.collection;

import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;

public class PdfTargetDictionary
        extends PdfDictionary {

    public PdfTargetDictionary(PdfTargetDictionary nested) {
        put(PdfName.R, (PdfObject) PdfName.P);
        if (nested != null) {
            setAdditionalPath(nested);
        }
    }

    public PdfTargetDictionary(boolean child) {
        if (child) {
            put(PdfName.R, (PdfObject) PdfName.C);
        } else {

            put(PdfName.R, (PdfObject) PdfName.P);
        }
    }

    public void setEmbeddedFileName(String target) {
        put(PdfName.N, (PdfObject) new PdfString(target, null));
    }

    public void setFileAttachmentPagename(String name) {
        put(PdfName.P, (PdfObject) new PdfString(name, null));
    }

    public void setFileAttachmentPage(int page) {
        put(PdfName.P, (PdfObject) new PdfNumber(page));
    }

    public void setFileAttachmentName(String name) {
        put(PdfName.A, (PdfObject) new PdfString(name, "UnicodeBig"));
    }

    public void setFileAttachmentIndex(int annotation) {
        put(PdfName.A, (PdfObject) new PdfNumber(annotation));
    }

    public void setAdditionalPath(PdfTargetDictionary nested) {
        put(PdfName.T, (PdfObject) nested);
    }
}
