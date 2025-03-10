package esign.text.pdf.security;

//package com.itextpdf.text.pdf.security;
//
//import com.itextpdf.text.DocumentException;
//import com.itextpdf.text.error_messages.MessageLocalization;
//import com.itextpdf.text.pdf.XmlSignatureAppearance;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.StringWriter;
//import java.security.GeneralSecurityException;
//import java.security.Key;
//import java.security.MessageDigest;
//import java.security.Provider;
//import java.security.PublicKey;
//import java.security.cert.Certificate;
//import java.security.cert.X509Certificate;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.UUID;
//import javax.xml.crypto.XMLStructure;
//import javax.xml.crypto.dom.DOMStructure;
//import javax.xml.crypto.dsig.DigestMethod;
//import javax.xml.crypto.dsig.Reference;
//import javax.xml.crypto.dsig.SignedInfo;
//import javax.xml.crypto.dsig.Transform;
//import javax.xml.crypto.dsig.XMLObject;
//import javax.xml.crypto.dsig.XMLSignatureFactory;
//import javax.xml.crypto.dsig.dom.DOMSignContext;
//import javax.xml.crypto.dsig.keyinfo.KeyInfo;
//import javax.xml.crypto.dsig.keyinfo.KeyValue;
//import javax.xml.crypto.dsig.keyinfo.X509Data;
//import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
//import javax.xml.crypto.dsig.spec.TransformParameterSpec;
//import javax.xml.crypto.dsig.spec.XPathFilter2ParameterSpec;
//import javax.xml.crypto.dsig.spec.XPathType;
//import javax.xml.transform.Transformer;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;
//import org.apache.jcp.xml.dsig.internal.dom.DOMKeyInfoFactory;
//import org.apache.jcp.xml.dsig.internal.dom.DOMReference;
//import org.apache.jcp.xml.dsig.internal.dom.DOMSignedInfo;
//import org.apache.jcp.xml.dsig.internal.dom.DOMUtils;
//import org.apache.jcp.xml.dsig.internal.dom.DOMXMLSignature;
//import org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI;
//import org.apache.xml.security.utils.Base64;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//
//public class MakeXmlSignature {
//
//    private static class EmptyKey
//            implements Key {
//
//        private static EmptyKey instance = new EmptyKey();
//
//        public static EmptyKey getInstance() {
//            return instance;
//        }
//
//        public String getAlgorithm() {
//            return null;
//        }
//
//        public String getFormat() {
//            return null;
//        }
//
//        public byte[] getEncoded() {
//            return new byte[0];
//        }
//    }
//
//    public static void signXmlDSig(XmlSignatureAppearance sap, ExternalSignature externalSignature, KeyInfo keyInfo) throws GeneralSecurityException, IOException, DocumentException {
//        verifyArguments(sap, externalSignature);
//        XMLSignatureFactory fac = createSignatureFactory();
//        Reference reference = generateContentReference(fac, sap, null);
//        String signatureMethod = null;
//        if (externalSignature.getEncryptionAlgorithm().equals("RSA")) {
//            signatureMethod = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
//        } else if (externalSignature.getEncryptionAlgorithm().equals("DSA")) {
//            signatureMethod = "http://www.w3.org/2000/09/xmldsig#dsa-sha1";
//        }
//
//        DOMSignedInfo signedInfo = (DOMSignedInfo) fac.newSignedInfo(fac
//                .newCanonicalizationMethod("http://www.w3.org/TR/2001/REC-xml-c14n-20010315", (C14NMethodParameterSpec) null), fac
//                .newSignatureMethod(signatureMethod, null), Collections.singletonList(reference));
//
//        sign(fac, externalSignature, sap.getXmlLocator(), signedInfo, null, keyInfo, null);
//
//        sap.close();
//    }
//
//    public static void signXmlDSig(XmlSignatureAppearance sap, ExternalSignature externalSignature, Certificate[] chain) throws DocumentException, GeneralSecurityException, IOException {
//        signXmlDSig(sap, externalSignature, generateKeyInfo(chain, sap));
//    }
//
//    public static void signXmlDSig(XmlSignatureAppearance sap, ExternalSignature externalSignature, PublicKey publicKey) throws GeneralSecurityException, DocumentException, IOException {
//        signXmlDSig(sap, externalSignature, generateKeyInfo(publicKey));
//    }
//
//    public static void signXades(XmlSignatureAppearance sap, ExternalSignature externalSignature, Certificate[] chain, boolean includeSignaturePolicy) throws GeneralSecurityException, DocumentException, IOException {
//        verifyArguments(sap, externalSignature);
//
//        String signatureMethod = null;
//        if (externalSignature.getEncryptionAlgorithm().equals("RSA")) {
//            signatureMethod = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
//        } else if (externalSignature.getEncryptionAlgorithm().equals("DSA")) {
//            signatureMethod = "http://www.w3.org/2000/09/xmldsig#dsa-sha1";
//        }
//        String contentReferenceId = "Reference-" + getRandomId();
//        String signedPropertiesId = "SignedProperties-" + getRandomId();
//        String signatureId = "Signature-" + getRandomId();
//
//        XMLSignatureFactory fac = createSignatureFactory();
//
//        KeyInfo keyInfo = generateKeyInfo(chain, sap);
//        String[] signaturePolicy = null;
//        if (includeSignaturePolicy) {
//            signaturePolicy = new String[2];
//            if (signatureMethod.equals("http://www.w3.org/2000/09/xmldsig#rsa-sha1")) {
//                signaturePolicy[0] = "urn:oid:1.2.840.113549.1.1.5";
//                signaturePolicy[1] = "RSA (PKCS #1 v1.5) with SHA-1 signature";
//            } else {
//                signaturePolicy[0] = "urn:oid:1.2.840.10040.4.3";
//                signaturePolicy[1] = "ANSI X9.57 DSA signature generated with SHA-1 hash (DSA x9.30)";
//            }
//        }
//        XMLObject xmlObject = generateXadesObject(fac, sap, signatureId, contentReferenceId, signedPropertiesId, signaturePolicy);
//        Reference contentReference = generateContentReference(fac, sap, contentReferenceId);
//        Reference signedPropertiesReference = generateCustomReference(fac, "#" + signedPropertiesId, "http://uri.etsi.org/01903#SignedProperties", null);
//
//        List<Reference> references = Arrays.asList(new Reference[]{signedPropertiesReference, contentReference});
//
//        DOMSignedInfo signedInfo = (DOMSignedInfo) fac.newSignedInfo(fac
//                .newCanonicalizationMethod("http://www.w3.org/TR/2001/REC-xml-c14n-20010315", (C14NMethodParameterSpec) null), fac
//                .newSignatureMethod(signatureMethod, null), references, null);
//
//        sign(fac, externalSignature, sap.getXmlLocator(), signedInfo, xmlObject, keyInfo, signatureId);
//
//        sap.close();
//    }
//
//    public static void signXadesBes(XmlSignatureAppearance sap, ExternalSignature externalSignature, Certificate[] chain) throws GeneralSecurityException, DocumentException, IOException {
//        signXades(sap, externalSignature, chain, false);
//    }
//
//    public static void signXadesEpes(XmlSignatureAppearance sap, ExternalSignature externalSignature, Certificate[] chain) throws GeneralSecurityException, DocumentException, IOException {
//        signXades(sap, externalSignature, chain, true);
//    }
//
//    private static void verifyArguments(XmlSignatureAppearance sap, ExternalSignature externalSignature) throws DocumentException {
//        if (sap.getXmlLocator() == null) {
//            throw new DocumentException(MessageLocalization.getComposedMessage("xmllocator.cannot.be.null", new Object[0]));
//        }
//        if (!externalSignature.getHashAlgorithm().equals("SHA1")) {
//            throw new UnsupportedOperationException(MessageLocalization.getComposedMessage("support.only.sha1.hash.algorithm", new Object[0]));
//        }
//        if (!externalSignature.getEncryptionAlgorithm().equals("RSA") && !externalSignature.getEncryptionAlgorithm().equals("DSA")) {
//            throw new UnsupportedOperationException(MessageLocalization.getComposedMessage("support.only.rsa.and.dsa.algorithms", new Object[0]));
//        }
//    }
//
//    private static Element findElement(NodeList nodes, String localName) {
//        for (int i = nodes.getLength() - 1; i >= 0; i--) {
//            Node currNode = nodes.item(i);
//            if (currNode.getNodeType() == 1 && currNode.getLocalName().equals(localName)) {
//                return (Element) currNode;
//            }
//        }
//        return null;
//    }
//
//    private static KeyInfo generateKeyInfo(Certificate[] chain, XmlSignatureAppearance sap) {
//        Certificate certificate = chain[0];
//        sap.setCertificate(certificate);
//        DOMKeyInfoFactory dOMKeyInfoFactory = new DOMKeyInfoFactory();
//
//        X509Data x509d = dOMKeyInfoFactory.newX509Data(Collections.singletonList(certificate));
//
//        return dOMKeyInfoFactory.newKeyInfo(Collections.singletonList(x509d));
//    }
//
//    private static KeyInfo generateKeyInfo(PublicKey publicKey) throws GeneralSecurityException {
//        DOMKeyInfoFactory dOMKeyInfoFactory = new DOMKeyInfoFactory();
//        KeyValue kv = dOMKeyInfoFactory.newKeyValue(publicKey);
//        return dOMKeyInfoFactory.newKeyInfo(Collections.singletonList(kv));
//    }
//
//    private static String getRandomId() {
//        return UUID.randomUUID().toString().substring(24);
//    }
//
//    private static XMLSignatureFactory createSignatureFactory() {
//        return XMLSignatureFactory.getInstance("DOM", (Provider) new XMLDSigRI());
//    }
//
//    private static XMLObject generateXadesObject(XMLSignatureFactory fac, XmlSignatureAppearance sap, String signatureId, String contentReferenceId, String signedPropertiesId, String[] signaturePolicy) throws GeneralSecurityException {
//        MessageDigest md = MessageDigest.getInstance("SHA1");
//        Certificate cert = sap.getCertificate();
//
//        Document doc = sap.getXmlLocator().getDocument();
//
//        Element QualifyingProperties = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:QualifyingProperties");
//        QualifyingProperties.setAttribute("Target", "#" + signatureId);
//        Element SignedProperties = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:SignedProperties");
//        SignedProperties.setAttribute("Id", signedPropertiesId);
//        SignedProperties.setIdAttribute("Id", true);
//        Element SignedSignatureProperties = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:SignedSignatureProperties");
//        Element SigningTime = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:SigningTime");
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
//        String result = sdf.format(sap.getSignDate().getTime());
//        result = result.substring(0, result.length() - 2).concat(":").concat(result.substring(result.length() - 2));
//        SigningTime.appendChild(doc.createTextNode(result));
//        SignedSignatureProperties.appendChild(SigningTime);
//        Element SigningCertificate = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:SigningCertificate");
//        Element Cert = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:Cert");
//        Element CertDigest = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:CertDigest");
//        Element DigestMethod = doc.createElementNS("http://www.w3.org/2000/09/xmldsig#", "DigestMethod");
//        DigestMethod.setAttribute("Algorithm", "http://www.w3.org/2000/09/xmldsig#sha1");
//        CertDigest.appendChild(DigestMethod);
//        Element DigestValue = doc.createElementNS("http://www.w3.org/2000/09/xmldsig#", "DigestValue");
//        DigestValue.appendChild(doc.createTextNode(Base64.encode(md.digest(cert.getEncoded()))));
//        CertDigest.appendChild(DigestValue);
//        Cert.appendChild(CertDigest);
//        if (cert instanceof X509Certificate) {
//            Element IssueSerial = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:IssuerSerial");
//            Element X509IssuerName = doc.createElementNS("http://www.w3.org/2000/09/xmldsig#", "X509IssuerName");
//            X509IssuerName.appendChild(doc.createTextNode(getX509IssuerName((X509Certificate) cert)));
//            IssueSerial.appendChild(X509IssuerName);
//            Element X509SerialNumber = doc.createElementNS("http://www.w3.org/2000/09/xmldsig#", "X509SerialNumber");
//            X509SerialNumber.appendChild(doc.createTextNode(getX509SerialNumber((X509Certificate) cert)));
//            IssueSerial.appendChild(X509SerialNumber);
//            Cert.appendChild(IssueSerial);
//        }
//        SigningCertificate.appendChild(Cert);
//        SignedSignatureProperties.appendChild(SigningCertificate);
//        if (signaturePolicy != null) {
//            Element SignaturePolicyIdentifier = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:SignaturePolicyIdentifier");
//            Element SignaturePolicyId = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:SignaturePolicyId");
//            Element SigPolicyId = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:SigPolicyId");
//            Element Identifier = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:Identifier");
//            Identifier.appendChild(doc.createTextNode(signaturePolicy[0]));
//            Identifier.setAttribute("Qualifier", "OIDAsURN");
//            SigPolicyId.appendChild(Identifier);
//
//            Element Description = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:Description");
//            Description.appendChild(doc.createTextNode(signaturePolicy[1]));
//            SigPolicyId.appendChild(Description);
//            SignaturePolicyId.appendChild(SigPolicyId);
//            Element SigPolicyHash = doc.createElementNS("http://uri.etsi.org/01903/v1.3.2#", "xades:SigPolicyHash");
//            DigestMethod = doc.createElementNS("http://www.w3.org/2000/09/xmldsig#", "DigestMethod");
//            DigestMethod.setAttribute("Algorithm", "http://www.w3.org/2000/09/xmldsig#sha1");
//            SigPolicyHash.appendChild(DigestMethod);
//            DigestValue = doc.createElementNS("http://www.w3.org/2000/09/xmldsig#", "DigestValue");
//            byte[] policyIdContent = getByteArrayOfNode(SigPolicyId);
//            DigestValue.appendChild(doc.createTextNode(Base64.encode(md.digest(policyIdContent))));
//            SigPolicyHash.appendChild(DigestValue);
//            SignaturePolicyId.appendChild(SigPolicyHash);
//            SignaturePolicyIdentifier.appendChild(SignaturePolicyId);
//            SignedSignatureProperties.appendChild(SignaturePolicyIdentifier);
//        }
//        SignedProperties.appendChild(SignedSignatureProperties);
//        Element SignedDataObjectProperties = doc.createElement("xades:SignedDataObjectProperties");
//        Element DataObjectFormat = doc.createElement("xades:DataObjectFormat");
//        DataObjectFormat.setAttribute("ObjectReference", "#" + contentReferenceId);
//        String descr = sap.getDescription();
//        if (descr != null) {
//            Element Description = doc.createElement("xades:Description");
//            Description.appendChild(doc.createTextNode(descr));
//            DataObjectFormat.appendChild(Description);
//        }
//        Element MimeType = doc.createElement("xades:MimeType");
//        MimeType.appendChild(doc.createTextNode(sap.getMimeType()));
//        DataObjectFormat.appendChild(MimeType);
//        String enc = sap.getXmlLocator().getEncoding();
//        if (enc != null) {
//            Element Encoding = doc.createElement("xades:Encoding");
//            Encoding.appendChild(doc.createTextNode(enc));
//            DataObjectFormat.appendChild(Encoding);
//        }
//        SignedDataObjectProperties.appendChild(DataObjectFormat);
//        SignedProperties.appendChild(SignedDataObjectProperties);
//        QualifyingProperties.appendChild(SignedProperties);
//
//        XMLStructure content = new DOMStructure(QualifyingProperties);
//        return fac.newXMLObject(Collections.singletonList(content), null, null, null);
//    }
//
//    private static String getX509IssuerName(X509Certificate cert) {
//        return cert.getIssuerX500Principal().toString();
//    }
//
//    private static String getX509SerialNumber(X509Certificate cert) {
//        return cert.getSerialNumber().toString();
//    }
//
//    private static Reference generateContentReference(XMLSignatureFactory fac, XmlSignatureAppearance sap, String referenceId) throws GeneralSecurityException {
//        DigestMethod digestMethodSHA1 = fac.newDigestMethod("http://www.w3.org/2000/09/xmldsig#sha1", null);
//
//        List<Transform> transforms = new ArrayList<Transform>();
//        transforms.add(fac.newTransform("http://www.w3.org/2000/09/xmldsig#enveloped-signature", (TransformParameterSpec) null));
//
//        XpathConstructor xpathConstructor = sap.getXpathConstructor();
//        if (xpathConstructor != null && xpathConstructor.getXpathExpression().length() > 0) {
//            XPathFilter2ParameterSpec xpath2Spec = new XPathFilter2ParameterSpec(Collections.singletonList(new XPathType(xpathConstructor.getXpathExpression(), XPathType.Filter.INTERSECT)));
//            transforms.add(fac.newTransform("http://www.w3.org/2002/06/xmldsig-filter2", xpath2Spec));
//        }
//        return fac.newReference("", digestMethodSHA1, transforms, null, referenceId);
//    }
//
//    private static Reference generateCustomReference(XMLSignatureFactory fac, String uri, String type, String id) throws GeneralSecurityException {
//        DigestMethod dsDigestMethod = fac.newDigestMethod("http://www.w3.org/2000/09/xmldsig#sha1", null);
//        return fac.newReference(uri, dsDigestMethod, null, type, id);
//    }
//
//    private static void sign(XMLSignatureFactory fac, ExternalSignature externalSignature, XmlLocator locator, DOMSignedInfo si, XMLObject xo, KeyInfo ki, String signatureId) throws DocumentException {
//        Document doc = locator.getDocument();
//
//        DOMSignContext domSignContext = new DOMSignContext(EmptyKey.getInstance(), doc.getDocumentElement());
//
//        List<XMLObject> objects = null;
//        if (xo != null) {
//            objects = Collections.singletonList(xo);
//        }
//        DOMXMLSignature signature = (DOMXMLSignature) fac.newXMLSignature((SignedInfo) si, ki, objects, signatureId, null);
//
//        ByteArrayOutputStream byteRange = new ByteArrayOutputStream();
//        try {
//            signature.marshal(domSignContext.getParent(), domSignContext.getNextSibling(),
//                    DOMUtils.getSignaturePrefix(domSignContext), domSignContext);
//            Element signElement = findElement(doc.getDocumentElement().getChildNodes(), "Signature");
//            if (signatureId != null) {
//                signElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xades", "http://uri.etsi.org/01903/v1.3.2#");
//            }
//            List<DOMReference> references = si.getReferences();
//            for (int i = 0; i < references.size(); i++) {
//                ((DOMReference) references.get(i)).digest(domSignContext);
//            }
//            si.canonicalize(domSignContext, byteRange);
//
//            Element signValue = findElement(signElement.getChildNodes(), "SignatureValue");
//
//            String valueBase64 = Base64.encode(externalSignature.sign(byteRange.toByteArray()));
//
//            signValue.appendChild(doc.createTextNode(valueBase64));
//            locator.setDocument(doc);
//        } catch (Exception e) {
//            throw new DocumentException(e);
//        }
//    }
//
//    private static byte[] getByteArrayOfNode(Node node) {
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        try {
//            StreamResult xmlOutput = new StreamResult(new StringWriter());
//            Transformer transformer = TransformerFactory.newInstance().newTransformer();
//            transformer.setOutputProperty("omit-xml-declaration", "yes");
//            transformer.transform(new DOMSource(node), xmlOutput);
//            return xmlOutput.getWriter().toString().getBytes();
//        } catch (Exception exception) {
//
//            return stream.toByteArray();
//        }
//    }
//}
//
