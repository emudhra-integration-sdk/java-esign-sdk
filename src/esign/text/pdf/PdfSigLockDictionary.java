package esign.text.pdf;

public class PdfSigLockDictionary
        extends PdfDictionary {

    public enum LockAction {
        ALL(PdfName.ALL), INCLUDE(PdfName.INCLUDE), EXCLUDE(PdfName.EXCLUDE);

        private PdfName name;

        LockAction(PdfName name) {
            this.name = name;
        }

        public PdfName getValue() {
            return this.name;
        }
    }

    public enum LockPermissions {
        NO_CHANGES_ALLOWED(1), FORM_FILLING(2), FORM_FILLING_AND_ANNOTATION(3);

        private PdfNumber number;

        LockPermissions(int p) {
            this.number = new PdfNumber(p);
        }

        public PdfNumber getValue() {
            return this.number;
        }
    }

    public PdfSigLockDictionary() {
        super(PdfName.SIGFIELDLOCK);
        put(PdfName.ACTION, LockAction.ALL.getValue());
    }

    public PdfSigLockDictionary(LockPermissions p) {
        this();
        put(PdfName.P, p.getValue());
    }

    public PdfSigLockDictionary(LockAction action, String... fields) {
        this(action, (LockPermissions) null, fields);
    }

    public PdfSigLockDictionary(LockAction action, LockPermissions p, String... fields) {
        super(PdfName.SIGFIELDLOCK);
        put(PdfName.ACTION, action.getValue());
        if (p != null) {
            put(PdfName.P, p.getValue());
        }
        PdfArray fieldsArray = new PdfArray();
        for (String field : fields) {
            fieldsArray.add(new PdfString(field));
        }
        put(PdfName.FIELDS, fieldsArray);
    }
}
