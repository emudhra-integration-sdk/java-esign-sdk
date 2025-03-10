package esign.text.xml.xmp;

import esign.text.Version;

@Deprecated
public class PdfSchema
        extends XmpSchema {

    private static final long serialVersionUID = -1541148669123992185L;
    public static final String DEFAULT_XPATH_ID = "pdf";
    public static final String DEFAULT_XPATH_URI = "http://ns.adobe.com/pdf/1.3/";
    public static final String KEYWORDS = "pdf:Keywords";
    public static final String VERSION = "pdf:PDFVersion";
    public static final String PRODUCER = "pdf:Producer";

    public PdfSchema() {
        super("xmlns:pdf=\"http://ns.adobe.com/pdf/1.3/\"");
        addProducer(Version.getInstance().getVersion());
    }

    public void addKeywords(String keywords) {
        setProperty("pdf:Keywords", keywords);
    }

    public void addProducer(String producer) {
        setProperty("pdf:Producer", producer);
    }

    public void addVersion(String version) {
        setProperty("pdf:PDFVersion", version);
    }
}


