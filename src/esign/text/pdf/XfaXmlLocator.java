package esign.text.pdf;

import esign.text.DocumentException;
import esign.text.pdf.security.XmlLocator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XfaXmlLocator
        implements XmlLocator {

    private PdfStamper stamper;
    private XfaForm xfaForm;
    private String encoding;

    public XfaXmlLocator(PdfStamper stamper) throws DocumentException, IOException {
        this.stamper = stamper;
        try {
            createXfaForm();
        } catch (ParserConfigurationException e) {
            throw new DocumentException(e);
        } catch (SAXException e) {
            throw new DocumentException(e);
        }
    }

    protected void createXfaForm() throws ParserConfigurationException, SAXException, IOException {
        this.xfaForm = new XfaForm(this.stamper.getReader());
    }

    public Document getDocument() {
        return this.xfaForm.getDomDocument();
    }

    public void setDocument(Document document) throws IOException, DocumentException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TransformerFactory tf = TransformerFactory.newInstance();

            Transformer trans = tf.newTransformer();

            trans.transform(new DOMSource(document), new StreamResult(outputStream));

            PdfIndirectReference iref = this.stamper.getWriter().addToBody(new PdfStream(outputStream.toByteArray())).getIndirectReference();
            this.stamper.getReader().getAcroForm().put(PdfName.XFA, iref);
        } catch (TransformerConfigurationException e) {
            throw new DocumentException(e);
        } catch (TransformerException e) {
            throw new DocumentException(e);
        }
    }

    public String getEncoding() {
        return this.encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
