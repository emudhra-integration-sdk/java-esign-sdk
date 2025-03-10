package esign.text.pdf.security;

import esign.text.DocumentException;
import java.io.IOException;
import org.w3c.dom.Document;

public interface XmlLocator {

    Document getDocument();

    void setDocument(Document paramDocument) throws IOException, DocumentException;

    String getEncoding();
}
