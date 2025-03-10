package esign.text.xmp.impl;

import esign.text.xmp.XMPException;
import esign.text.xmp.XMPMeta;
import esign.text.xmp.options.ParseOptions;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMPMetaParser {

    private static final Object XMP_RDF = new Object();

    private static DocumentBuilderFactory factory = createDocumentBuilderFactory();

    public static XMPMeta parse(Object input, ParseOptions options) throws XMPException {
        ParameterAsserts.assertNotNull(input);
        options = (options != null) ? options : new ParseOptions();

        Document document = parseXml(input, options);

        boolean xmpmetaRequired = options.getRequireXMPMeta();
        Object[] result = new Object[3];
        result = findRootNode(document, xmpmetaRequired, result);

        if (result != null && result[1] == XMP_RDF) {

            XMPMetaImpl xmp = ParseRDF.parse((Node) result[0]);
            xmp.setPacketHeader((String) result[2]);

            if (!options.getOmitNormalization()) {
                return XMPNormalizer.process(xmp, options);
            }

            return xmp;
        }

        return new XMPMetaImpl();
    }

    private static Document parseXml(Object input, ParseOptions options) throws XMPException {
        if (input instanceof InputStream) {
            return parseXmlFromInputStream((InputStream) input, options);
        }
        if (input instanceof byte[]) {
            return parseXmlFromBytebuffer(new ByteBuffer((byte[]) input), options);
        }

        return parseXmlFromString((String) input, options);
    }

    private static Document parseXmlFromInputStream(InputStream stream, ParseOptions options) throws XMPException {
        if (!options.getAcceptLatin1() && !options.getFixControlChars()) {
            return parseInputSource(new InputSource(stream));
        }

        try {
            ByteBuffer buffer = new ByteBuffer(stream);
            return parseXmlFromBytebuffer(buffer, options);
        } catch (IOException e) {

            throw new XMPException("Error reading the XML-file", 204, e);
        }
    }

    private static Document parseXmlFromBytebuffer(ByteBuffer buffer, ParseOptions options) throws XMPException {
        InputSource source = new InputSource(buffer.getByteStream());

        try {
            return parseInputSource(source);
        } catch (XMPException e) {

            if (e.getErrorCode() == 201 || e
                    .getErrorCode() == 204) {

                if (options.getAcceptLatin1()) {
                    buffer = Latin1Converter.convert(buffer);
                }

                if (options.getFixControlChars()) {

                    try {

                        String encoding = buffer.getEncoding();

                        Reader fixReader = new FixASCIIControlsReader(new InputStreamReader(buffer.getByteStream(), encoding));
                        return parseInputSource(new InputSource(fixReader));
                    } catch (UnsupportedEncodingException e1) {

                        throw new XMPException("Unsupported Encoding", 9, e);
                    }
                }

                source = new InputSource(buffer.getByteStream());
                return parseInputSource(source);
            }

            throw e;
        }
    }

    private static Document parseXmlFromString(String input, ParseOptions options) throws XMPException {
        InputSource source = new InputSource(new StringReader(input));

        try {
            return parseInputSource(source);
        } catch (XMPException e) {

            if (e.getErrorCode() == 201 && options.getFixControlChars()) {

                source = new InputSource(new FixASCIIControlsReader(new StringReader(input)));
                return parseInputSource(source);
            }

            throw e;
        }
    }

    private static Document parseInputSource(InputSource source) throws XMPException {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            return builder.parse(source);
        } catch (SAXException e) {

            throw new XMPException("XML parsing failure", 201, e);
        } catch (ParserConfigurationException e) {

            throw new XMPException("XML Parser not correctly configured", 0, e);

        } catch (IOException e) {

            throw new XMPException("Error reading the XML-file", 204, e);
        }
    }

    private static Object[] findRootNode(Node root, boolean xmpmetaRequired, Object[] result) {
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {

            root = children.item(i);
            if (7 == root.getNodeType() && "xpacket"
                    .equals(((ProcessingInstruction) root).getTarget())) {

                if (result != null) {
                    result[2] = ((ProcessingInstruction) root).getData();
                }
            } else if (3 != root.getNodeType() && 7 != root
                    .getNodeType()) {

                String rootNS = root.getNamespaceURI();
                String rootLocal = root.getLocalName();
                if (("xmpmeta"
                        .equals(rootLocal) || "xapmeta"
                        .equals(rootLocal)) && "adobe:ns:meta/"
                        .equals(rootNS)) {

                    return findRootNode(root, false, result);
                }
                if (!xmpmetaRequired && "RDF"
                        .equals(rootLocal) && "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                        .equals(rootNS)) {

                    if (result != null) {

                        result[0] = root;
                        result[1] = XMP_RDF;
                    }
                    return result;
                }

                Object[] newResult = findRootNode(root, xmpmetaRequired, result);
                if (newResult != null) {
                    return newResult;
                }
            }
        }

        return null;
    }

    private static DocumentBuilderFactory createDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setIgnoringComments(true);

        try {
            factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);

            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);

            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);

            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

        } catch (Exception exception) {
        }

        return factory;
    }
}
