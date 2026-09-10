package com.emudhra.esign;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Parsed view of an eSign gateway {@code <EsignResp>} (V2 or V3): status and error attributes,
 * transaction id and timestamp, the {@code UserX509Certificate}, every {@code DocSignature}
 * (id to Base64 PKCS#7) and the outcome of the gateway's enveloped XML signature.
 * Package-private helper for {@link eSignVerifier}.
 */
final class eSignResponseInfo {

    String status;
    String errorCode;
    String errorMessage;
    String transactionId;
    String timestamp;
    String responseCode;
    String userCertificateBase64;

    /** DocSignature id to its text content (Base64 PKCS#7, trimmed; may be empty). Insertion order = document order. */
    final Map<String, String> docSignatures = new LinkedHashMap<String, String>();
    final Map<String, String> docSigHashAlgorithms = new LinkedHashMap<String, String>();
    final Map<String, String> docErrors = new LinkedHashMap<String, String>();

    /** null when the response has no XML signature or it could not be evaluated - see {@link #xmlSignatureNote}. */
    Boolean xmlSignatureValid;
    String xmlSignatureNote;

    private eSignResponseInfo() {
    }

    static eSignResponseInfo parse(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        try {
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignore) {
        }
        try {
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Exception ignore) {
        }
        javax.xml.parsers.DocumentBuilder builder = f.newDocumentBuilder();
        builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());   // no "[Fatal Error]" on stderr; fatal errors still throw
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        Element root = doc.getDocumentElement();
        String rootName = root == null ? null : (root.getLocalName() != null ? root.getLocalName() : root.getTagName());
        if (!"EsignResp".equals(rootName)) {
            throw new Exception("root element is <" + rootName + ">, expected <EsignResp>");
        }

        eSignResponseInfo r = new eSignResponseInfo();
        r.status = attr(root, "status");
        r.errorCode = attr(root, "errorCode");
        r.errorMessage = firstNonEmpty(attr(root, "errorMessage"), attr(root, "errMsg"), attr(root, "error"));
        r.transactionId = attr(root, "txn");
        r.timestamp = attr(root, "ts");
        r.responseCode = attr(root, "resCode");

        NodeList certs = root.getElementsByTagName("UserX509Certificate");
        if (certs.getLength() > 0) {
            r.userCertificateBase64 = certs.item(0).getTextContent().trim();
        }
        NodeList sigs = root.getElementsByTagName("DocSignature");
        for (int i = 0; i < sigs.getLength(); i++) {
            Element e = (Element) sigs.item(i);
            String id = attr(e, "id");
            if (id.isEmpty()) {
                id = String.valueOf(i + 1);
            }
            r.docSignatures.put(id, e.getTextContent().trim());
            r.docSigHashAlgorithms.put(id, attr(e, "sigHashAlgorithm"));
            r.docErrors.put(id, attr(e, "error"));
        }
        r.checkXmlSignature(doc);
        return r;
    }

    /** The certificate the gateway reports in UserX509Certificate, or null when absent. */
    X509Certificate userCertificate() throws Exception {
        if (userCertificateBase64 == null || userCertificateBase64.isEmpty()) {
            return null;
        }
        byte[] der = java.util.Base64.getMimeDecoder().decode(userCertificateBase64);
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der));
    }

    /**
     * Core validation of the gateway's enveloped XML-DSig with the certificate it carries in
     * KeyInfo. Proves the XML is byte-for-byte what that key signed; trust in the key itself
     * is a separate question. Never throws.
     */
    private void checkXmlSignature(Document doc) {
        try {
            NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (nl.getLength() == 0) {
                xmlSignatureNote = "response carries no XML signature";
                return;
            }
            Element sigEl = (Element) nl.item(0);
            NodeList certNodes = sigEl.getElementsByTagNameNS(XMLSignature.XMLNS, "X509Certificate");
            if (certNodes.getLength() == 0) {
                xmlSignatureNote = "XML signature carries no KeyInfo certificate";
                return;
            }
            byte[] der = java.util.Base64.getMimeDecoder().decode(certNodes.item(0).getTextContent().trim());
            X509Certificate signer = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));

            DOMValidateContext ctx = new DOMValidateContext(signer.getPublicKey(), sigEl);
            // The gateway signs with RSA-SHA1 / SHA-1; newer JDKs reject those under secure validation.
            ctx.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
            XMLSignature sig = fac.unmarshalXMLSignature(ctx);
            xmlSignatureValid = sig.validate(ctx);
            xmlSignatureNote = (xmlSignatureValid
                    ? "XML signature verified; signed by "
                    : "XML signature does NOT verify (altered or re-serialised XML?); claimed signer ")
                    + signer.getSubjectX500Principal().getName();
        } catch (Exception e) {
            xmlSignatureValid = null;
            xmlSignatureNote = "XML signature could not be evaluated: " + e;
        }
    }

    private static String attr(Element e, String name) {
        String v = e.getAttribute(name);
        return v == null ? "" : v.trim();
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return "";
    }
}
