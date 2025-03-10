package esign.text.xml;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class XmlDomWriter {

    protected PrintWriter fOut;
    protected boolean fCanonical;
    protected boolean fXML11;

    public XmlDomWriter() {
    }

    public XmlDomWriter(boolean canonical) {
        this.fCanonical = canonical;
    }

    public void setCanonical(boolean canonical) {
        this.fCanonical = canonical;
    }

    public void setOutput(OutputStream stream, String encoding) throws UnsupportedEncodingException {
        if (encoding == null) {
            encoding = "UTF8";
        }

        Writer writer = new OutputStreamWriter(stream, encoding);
        this.fOut = new PrintWriter(writer);
    }

    public void setOutput(Writer writer) {
        this.fOut = (writer instanceof PrintWriter) ? (PrintWriter) writer : new PrintWriter(writer);
    }

    public void write(Node node) {
        Document document;
        DocumentType doctype;
        Attr[] attrs;
        String data, publicId;
        int i;
        Node child;
        String systemId, internalSubset;
        if (node == null) {
            return;
        }

        short type = node.getNodeType();
        switch (type) {
            case 9:
                document = (Document) node;
                this.fXML11 = false;
                if (!this.fCanonical) {
                    if (this.fXML11) {
                        this.fOut.println("<?xml version=\"1.1\" encoding=\"UTF-8\"?>");
                    } else {
                        this.fOut.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    }
                    this.fOut.flush();
                    write(document.getDoctype());
                }
                write(document.getDocumentElement());
                break;

            case 10:
                doctype = (DocumentType) node;
                this.fOut.print("<!DOCTYPE ");
                this.fOut.print(doctype.getName());
                publicId = doctype.getPublicId();
                systemId = doctype.getSystemId();
                if (publicId != null) {
                    this.fOut.print(" PUBLIC '");
                    this.fOut.print(publicId);
                    this.fOut.print("' '");
                    this.fOut.print(systemId);
                    this.fOut.print('\'');
                } else if (systemId != null) {
                    this.fOut.print(" SYSTEM '");
                    this.fOut.print(systemId);
                    this.fOut.print('\'');
                }
                internalSubset = doctype.getInternalSubset();
                if (internalSubset != null) {
                    this.fOut.println(" [");
                    this.fOut.print(internalSubset);
                    this.fOut.print(']');
                }
                this.fOut.println('>');
                break;

            case 1:
                this.fOut.print('<');
                this.fOut.print(node.getNodeName());
                attrs = sortAttributes(node.getAttributes());
                for (i = 0; i < attrs.length; i++) {
                    Attr attr = attrs[i];
                    this.fOut.print(' ');
                    this.fOut.print(attr.getNodeName());
                    this.fOut.print("=\"");
                    normalizeAndPrint(attr.getNodeValue(), true);
                    this.fOut.print('"');
                }
                this.fOut.print('>');
                this.fOut.flush();

                child = node.getFirstChild();
                while (child != null) {
                    write(child);
                    child = child.getNextSibling();
                }
                break;

            case 5:
                if (this.fCanonical) {
                    Node node1 = node.getFirstChild();
                    while (node1 != null) {
                        write(node1);
                        node1 = node1.getNextSibling();
                    }
                    break;
                }
                this.fOut.print('&');
                this.fOut.print(node.getNodeName());
                this.fOut.print(';');
                this.fOut.flush();
                break;

            case 4:
                if (this.fCanonical) {
                    normalizeAndPrint(node.getNodeValue(), false);
                } else {
                    this.fOut.print("<![CDATA[");
                    this.fOut.print(node.getNodeValue());
                    this.fOut.print("]]>");
                }
                this.fOut.flush();
                break;

            case 3:
                normalizeAndPrint(node.getNodeValue(), false);
                this.fOut.flush();
                break;

            case 7:
                this.fOut.print("<?");
                this.fOut.print(node.getNodeName());
                data = node.getNodeValue();
                if (data != null && data.length() > 0) {
                    this.fOut.print(' ');
                    this.fOut.print(data);
                }
                this.fOut.print("?>");
                this.fOut.flush();
                break;

            case 8:
                if (!this.fCanonical) {
                    this.fOut.print("<!--");
                    String comment = node.getNodeValue();
                    if (comment != null && comment.length() > 0) {
                        this.fOut.print(comment);
                    }
                    this.fOut.print("-->");
                    this.fOut.flush();
                }
                break;
        }

        if (type == 1) {
            this.fOut.print("</");
            this.fOut.print(node.getNodeName());
            this.fOut.print('>');
            this.fOut.flush();
        }
    }

    protected Attr[] sortAttributes(NamedNodeMap attrs) {
        int len = (attrs != null) ? attrs.getLength() : 0;
        Attr[] array = new Attr[len];
        int i;
        for (i = 0; i < len; i++) {
            array[i] = (Attr) attrs.item(i);
        }
        for (i = 0; i < len - 1; i++) {
            String name = array[i].getNodeName();
            int index = i;
            for (int j = i + 1; j < len; j++) {
                String curName = array[j].getNodeName();
                if (curName.compareTo(name) < 0) {
                    name = curName;
                    index = j;
                }
            }
            if (index != i) {
                Attr temp = array[i];
                array[i] = array[index];
                array[index] = temp;
            }
        }

        return array;
    }

    protected void normalizeAndPrint(String s, boolean isAttValue) {
        int len = (s != null) ? s.length() : 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            normalizeAndPrint(c, isAttValue);
        }
    }

    protected void normalizeAndPrint(char c, boolean isAttValue) {
        switch (c) {
            case '<':
                this.fOut.print("&lt;");
                return;

            case '>':
                this.fOut.print("&gt;");
                return;

            case '&':
                this.fOut.print("&amp;");
                return;

            case '"':
                if (isAttValue) {
                    this.fOut.print("&quot;");
                } else {
                    this.fOut.print("\"");
                }
                return;

            case '\r':
                this.fOut.print("&#xD;");
                return;

            case '\n':
                if (this.fCanonical) {
                    this.fOut.print("&#xA;");
                    return;
                }
                break;
        }

        if ((this.fXML11 && ((c >= '\001' && c <= '\037' && c != '\t' && c != '\n') || (c >= '' && c <= '') || c == ' ')) || (isAttValue && (c == '\t' || c == '\n'))) {

            this.fOut.print("&#x");
            this.fOut.print(Integer.toHexString(c).toUpperCase());
            this.fOut.print(";");
        } else {
            this.fOut.print(c);
        }
    }
}

