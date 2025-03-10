package esign.text.xmp.impl;

public class QName {

    private String prefix;
    private String localName;

    public QName(String qname) {
        int colon = qname.indexOf(':');

        if (colon >= 0) {

            this.prefix = qname.substring(0, colon);
            this.localName = qname.substring(colon + 1);
        } else {

            this.prefix = "";
            this.localName = qname;
        }
    }

    public QName(String prefix, String localName) {
        this.prefix = prefix;
        this.localName = localName;
    }

    public boolean hasPrefix() {
        return (this.prefix != null && this.prefix.length() > 0);
    }

    public String getLocalName() {
        return this.localName;
    }

    public String getPrefix() {
        return this.prefix;
    }
}
