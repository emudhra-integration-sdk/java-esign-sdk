package esign.text.pdf.security;

import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import java.util.ArrayList;
import java.util.List;

public class SignaturePermissions {

    public class FieldLock {

        PdfName action;
        PdfArray fields;

        public FieldLock(PdfName action, PdfArray fields) {
            this.action = action;
            this.fields = fields;
        }

        public PdfName getAction() {
            return this.action;
        }

        public PdfArray getFields() {
            return this.fields;
        }

        public String toString() {
            return this.action.toString() + ((this.fields == null) ? "" : this.fields.toString());
        }
    }

    boolean certification = false;

    boolean fillInAllowed = true;

    boolean annotationsAllowed = true;

    List<FieldLock> fieldLocks = new ArrayList<FieldLock>();

    public SignaturePermissions(PdfDictionary sigDict, SignaturePermissions previous) {
        if (previous != null) {
            this.annotationsAllowed &= previous.isAnnotationsAllowed();
            this.fillInAllowed &= previous.isFillInAllowed();
            this.fieldLocks.addAll(previous.getFieldLocks());
        }
        PdfArray ref = sigDict.getAsArray(PdfName.REFERENCE);
        if (ref != null) {
            for (int i = 0; i < ref.size(); i++) {
                PdfDictionary dict = ref.getAsDict(i);
                PdfDictionary params = dict.getAsDict(PdfName.TRANSFORMPARAMS);
                if (PdfName.DOCMDP.equals(dict.getAsName(PdfName.TRANSFORMMETHOD))) {
                    this.certification = true;
                }
                PdfName action = params.getAsName(PdfName.ACTION);
                if (action != null) {
                    this.fieldLocks.add(new FieldLock(action, params.getAsArray(PdfName.FIELDS)));
                }
                PdfNumber p = params.getAsNumber(PdfName.P);
                if (p != null) {
                    switch (p.intValue()) {

                        case 1:
                            this.fillInAllowed &= false;
                        case 2:
                            this.annotationsAllowed &= false;
                            break;
                    }
                }
            }
        }
    }

    public boolean isCertification() {
        return this.certification;
    }

    public boolean isFillInAllowed() {
        return this.fillInAllowed;
    }

    public boolean isAnnotationsAllowed() {
        return this.annotationsAllowed;
    }

    public List<FieldLock> getFieldLocks() {
        return this.fieldLocks;
    }
}
