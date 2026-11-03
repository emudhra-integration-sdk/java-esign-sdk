package com.emudhra.esign;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.xml.security.c14n.Canonicalizer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * NPCI eMandate (pain.009.001.04) handling for {@link eSign.InputType#EMANDATE}.
 * <p>
 * Same output as the .NET SDK's eMandate signing: the gateway signs SHA-256 of the
 * canonical mandate (C14N 1.0 without comments, whitespace-only text dropped, so no XML
 * declaration and no indentation), and the signed mandate is that canonical XML with
 * Mndt/Dbtr/CtctDtls/Nm = "00000000" + the certificate title (Aadhaar last 4 digits)
 * inserted and Mndt/SplmtryData/Envlp/Cnts/esign{SignedContent, X509Certificate, Signature}
 * appended. CtctDtls/Nm is added after signing, so it is not covered by the signature.
 * <p>
 * Unlike the .NET SDK, unusable mandates are rejected in phase 1 (before the user's OTP is
 * spent) rather than failing in phase 2, and the Nm goes into Mndt/Dbtr/CtctDtls rather
 * than the first CtctDtls in the document.
 */
final class EMandateXml {

    static final String PAIN_009 = "urn:iso:std:iso:20022:tech:xsd:pain.009.001.04";

    /** A mandate or response the SDK cannot sign; the message is meant for the caller. */
    static final class EMandateException extends Exception {

        private static final long serialVersionUID = 1L;

        EMandateException(String message) {
            super(message);
        }
    }

    private EMandateXml() {
    }

    /**
     * Phase 1: checks the mandate can be completed after signing and returns its
     * canonical bytes. The gateway signs SHA-256 of exactly these bytes, and they
     * become SignedContent.
     */
    static byte[] canonicalize(String xml) throws Exception {
        if (xml == null || xml.trim().isEmpty()) {
            throw new EMandateException("eMandate XML is empty (set it with eSignInputBuilder.setDocXml).");
        }
        Document doc;
        try {
            doc = parse(new InputSource(new StringReader(xml.trim())));
        } catch (SAXException e) {
            throw new EMandateException("eMandate XML is not well-formed: " + e.getMessage());
        }
        Element root = doc.getDocumentElement();
        if (!PAIN_009.equals(root.getNamespaceURI())) {
            throw new EMandateException("Root namespace is '" + root.getNamespaceURI() + "'; only " + PAIN_009 + " is supported.");
        }
        Element mndt = (Element) doc.getElementsByTagNameNS(PAIN_009, "Mndt").item(0);
        if (mndt == null) {
            throw new EMandateException("No Mndt element: the signature is appended to it as SplmtryData.");
        }
        if (doc.getElementsByTagNameNS(PAIN_009, "SplmtryData").getLength() > 0) {
            throw new EMandateException("The mandate already contains SplmtryData (is it already signed?).");
        }
        Element ctctDtls = child(child(mndt, "Dbtr"), "CtctDtls");
        if (ctctDtls == null) {
            throw new EMandateException("No Mndt/Dbtr/CtctDtls element: the Aadhaar Nm is inserted there after signing.");
        }
        if (child(ctctDtls, "Nm") != null) {
            throw new EMandateException("Mndt/Dbtr/CtctDtls already has an Nm element. The SDK inserts CtctDtls/Nm "
                    + "(00000000 + Aadhaar last 4 digits) itself after signing; remove it from the input.");
        }
        stripWhitespace(root, false);
        return c14n(doc);
    }

    /**
     * Phase 2: the signed mandate, built from the canonical bytes kept in the temp file,
     * the signer certificate and the gateway's PKCS#7.
     */
    static String buildSignedXml(byte[] canonical, String userCertBase64, String pkcs7Base64) throws Exception {
        SignerCertificateInfo cert = SignerCertificateInfo.fromBase64(userCertBase64);
        if (cert == null) {
            throw new EMandateException("UserX509Certificate missing or unreadable in the response.");
        }
        String aadhaarLast4 = cert.getAadhaarNumber();
        if (eSignUtility.isNullOrWhitespace(aadhaarLast4)) {
            throw new EMandateException("Signer certificate has no title (OID 2.5.4.12, the Aadhaar last 4 digits), "
                    + "so Dbtr/CtctDtls/Nm cannot be filled. PAN-based certificates do not carry it.");
        }

        Document doc = parse(new InputSource(new ByteArrayInputStream(canonical)));
        Element mndt = (Element) doc.getElementsByTagNameNS(PAIN_009, "Mndt").item(0);
        Element ctctDtls = child(child(mndt, "Dbtr"), "CtctDtls");

        Element nm = doc.createElementNS(PAIN_009, "Nm");
        nm.setTextContent("00000000" + aadhaarLast4.trim());
        ctctDtls.insertBefore(nm, ctctDtls.getFirstChild());

        Element esign = append(append(append(append(mndt, "SplmtryData"), "Envlp"), "Cnts"), "esign");
        append(esign, "SignedContent").setTextContent(java.util.Base64.getEncoder().encodeToString(canonical));
        append(esign, "X509Certificate").setTextContent(userCertBase64.replaceAll("\\s+", ""));
        append(esign, "Signature").setTextContent(pkcs7Base64.replaceAll("\\s+", ""));

        return eSignUtility.convertDocumentToString(doc, true);
    }

    private static Document parse(InputSource source) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        return f.newDocumentBuilder().parse(source);
    }

    private static byte[] c14n(Document doc) throws Exception {
        if (!org.apache.xml.security.Init.isInitialized()) {
            org.apache.xml.security.Init.init();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS).canonicalizeSubtree(doc, out);
        return out.toByteArray();
    }

    /** Drops whitespace-only text nodes outside xml:space="preserve", as .NET's XmlDocument does when loading. */
    private static void stripWhitespace(Element e, boolean preserve) {
        String space = e.getAttributeNS(XMLConstants.XML_NS_URI, "space");
        if (!space.isEmpty()) {
            preserve = "preserve".equals(space);
        }
        Node n = e.getFirstChild();
        while (n != null) {
            Node next = n.getNextSibling();
            if (n.getNodeType() == Node.TEXT_NODE && !preserve && n.getNodeValue().trim().isEmpty()) {
                e.removeChild(n);
            } else if (n.getNodeType() == Node.ELEMENT_NODE) {
                stripWhitespace((Element) n, preserve);
            }
            n = next;
        }
    }

    private static Element child(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE && PAIN_009.equals(n.getNamespaceURI()) && localName.equals(n.getLocalName())) {
                return (Element) n;
            }
        }
        return null;
    }

    private static Element append(Element parent, String localName) {
        Element e = parent.getOwnerDocument().createElementNS(PAIN_009, localName);
        parent.appendChild(e);
        return e;
    }
}
