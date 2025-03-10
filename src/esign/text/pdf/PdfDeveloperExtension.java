package esign.text.pdf;

public class PdfDeveloperExtension {

    public static final PdfDeveloperExtension ADOBE_1_7_EXTENSIONLEVEL3 = new PdfDeveloperExtension(PdfName.ADBE, PdfWriter.PDF_VERSION_1_7, 3);

    public static final PdfDeveloperExtension ESIC_1_7_EXTENSIONLEVEL2 = new PdfDeveloperExtension(PdfName.ESIC, PdfWriter.PDF_VERSION_1_7, 2);

    public static final PdfDeveloperExtension ESIC_1_7_EXTENSIONLEVEL5 = new PdfDeveloperExtension(PdfName.ESIC, PdfWriter.PDF_VERSION_1_7, 5);

    protected PdfName prefix;

    protected PdfName baseversion;

    protected int extensionLevel;

    public PdfDeveloperExtension(PdfName prefix, PdfName baseversion, int extensionLevel) {
        this.prefix = prefix;
        this.baseversion = baseversion;
        this.extensionLevel = extensionLevel;
    }

    public PdfName getPrefix() {
        return this.prefix;
    }

    public PdfName getBaseversion() {
        return this.baseversion;
    }

    public int getExtensionLevel() {
        return this.extensionLevel;
    }

    public PdfDictionary getDeveloperExtensions() {
        PdfDictionary developerextensions = new PdfDictionary();
        developerextensions.put(PdfName.BASEVERSION, this.baseversion);
        developerextensions.put(PdfName.EXTENSIONLEVEL, new PdfNumber(this.extensionLevel));
        return developerextensions;
    }
}
