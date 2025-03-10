package esign.text.xml.xmp;

@Deprecated
public class DublinCoreSchema
        extends XmpSchema {

    private static final long serialVersionUID = -4551741356374797330L;
    public static final String DEFAULT_XPATH_ID = "dc";
    public static final String DEFAULT_XPATH_URI = "http://purl.org/dc/elements/1.1/";
    public static final String CONTRIBUTOR = "dc:contributor";
    public static final String COVERAGE = "dc:coverage";
    public static final String CREATOR = "dc:creator";
    public static final String DATE = "dc:date";
    public static final String DESCRIPTION = "dc:description";
    public static final String FORMAT = "dc:format";
    public static final String IDENTIFIER = "dc:identifier";
    public static final String LANGUAGE = "dc:language";
    public static final String PUBLISHER = "dc:publisher";
    public static final String RELATION = "dc:relation";
    public static final String RIGHTS = "dc:rights";
    public static final String SOURCE = "dc:source";
    public static final String SUBJECT = "dc:subject";
    public static final String TITLE = "dc:title";
    public static final String TYPE = "dc:type";

    public DublinCoreSchema() {
        super("xmlns:dc=\"http://purl.org/dc/elements/1.1/\"");
        setProperty("dc:format", "application/pdf");
    }

    public void addTitle(String title) {
        XmpArray array = new XmpArray("rdf:Alt");
        array.add(title);
        setProperty("dc:title", array);
    }

    public void addTitle(LangAlt title) {
        setProperty("dc:title", title);
    }

    public void addDescription(String desc) {
        XmpArray array = new XmpArray("rdf:Alt");
        array.add(desc);
        setProperty("dc:description", array);
    }

    public void addDescription(LangAlt desc) {
        setProperty("dc:description", desc);
    }

    public void addSubject(String subject) {
        XmpArray array = new XmpArray("rdf:Bag");
        array.add(subject);
        setProperty("dc:subject", array);
    }

    public void addSubject(String[] subject) {
        XmpArray array = new XmpArray("rdf:Bag");
        for (int i = 0; i < subject.length; i++) {
            array.add(subject[i]);
        }
        setProperty("dc:subject", array);
    }

    public void addAuthor(String author) {
        XmpArray array = new XmpArray("rdf:Seq");
        array.add(author);
        setProperty("dc:creator", array);
    }

    public void addAuthor(String[] author) {
        XmpArray array = new XmpArray("rdf:Seq");
        for (int i = 0; i < author.length; i++) {
            array.add(author[i]);
        }
        setProperty("dc:creator", array);
    }

    public void addPublisher(String publisher) {
        XmpArray array = new XmpArray("rdf:Seq");
        array.add(publisher);
        setProperty("dc:publisher", array);
    }

    public void addPublisher(String[] publisher) {
        XmpArray array = new XmpArray("rdf:Seq");
        for (int i = 0; i < publisher.length; i++) {
            array.add(publisher[i]);
        }
        setProperty("dc:publisher", array);
    }
}
