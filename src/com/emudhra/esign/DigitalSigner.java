package com.emudhra.esign;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
//import org.apache.logging.log4j.core.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Vijay
 */
public final class DigitalSigner {

    private static final Logger LOGGER = EsignLoggerFactory.getLogger(DigitalSigner.class);
    private static final String MEC_TYPE = "DOM";
    private static final String KEY_STORE_TYPE = "PKCS12";
    private KeyStore.PrivateKeyEntry keyEntry = null;

    protected DigitalSigner(String keyStoreFile, char[] keyStorePassword, String alias) {
        this.keyEntry = getKeyFromKeyStore(keyStoreFile, keyStorePassword, alias);
        if (keyEntry == null) {
            throw new RuntimeException("Key could not be read for digital signature. Please check value of signature alias and signature password, and restart the Auth Client");
        }
    }

    protected String signXML(String xmlDocument, boolean includeKeyInfo) {
        try {
            // Parse the input XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document inputDocument = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xmlDocument)));
            // Sign the input XML's DOM document
            Document signedDocument = sign(inputDocument, includeKeyInfo);
            // Convert the signedDocument to XML String
            StringWriter stringWriter = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            trans.transform(new DOMSource(signedDocument), new StreamResult(stringWriter));
            return stringWriter.getBuffer().toString();
        } catch (Exception e) {
            LOGGER.warning("XML Sign"+ e);
            throw new RuntimeException("Error while digitally signing the XML document", e);
        }
    }

    private Document sign(Document xmlDoc, boolean includeKeyInfo) throws Exception {
        // String prov = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance(MEC_TYPE);
        DigestMethod digestMethod = factory.newDigestMethod(DigestMethod.SHA1, null);
        Transform transform = factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
        Reference reference = factory.newReference("", digestMethod, Collections.singletonList(transform), null, null);
        CanonicalizationMethod canonicalizationMethod = factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null);
        SignatureMethod signatureMethod = factory.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
        SignedInfo sInfo = factory.newSignedInfo(canonicalizationMethod, signatureMethod, Collections.singletonList(reference));

        if (keyEntry == null) {
            throw new RuntimeException("Key could not be read for digital signature. Please check value of signature alias and signature password, and restart the Auth Client");
        }
        X509Certificate x509Cert = (X509Certificate) keyEntry.getCertificate();
        KeyInfo kInfo = getKeyInfo(x509Cert, factory);
        DOMSignContext dsc = new DOMSignContext(this.keyEntry.getPrivateKey(), xmlDoc.getDocumentElement());
        XMLSignature signature = factory.newXMLSignature(sInfo, includeKeyInfo ? kInfo : null);
        signature.sign(dsc);
        Node node = dsc.getParent();
        return node.getOwnerDocument();
    }

    @SuppressWarnings("unchecked")
    private KeyInfo getKeyInfo(X509Certificate cert, XMLSignatureFactory fac) {
        // Create the KeyInfo containing the X509Data.
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        List x509Content = new ArrayList();
        x509Content.add(cert.getSubjectX500Principal().getName());
        x509Content.add(cert);
        X509Data xd = kif.newX509Data(x509Content);
        return kif.newKeyInfo(Collections.singletonList(xd));
    }

    private KeyStore.PrivateKeyEntry getKeyFromKeyStore(String keyStoreFile, char[] keyStorePassword, String alias) {
        // Load the KeyStore and get the signing key and certificate.
        FileInputStream keyFileStream = null;
        try {
            KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE);
            keyFileStream = new FileInputStream(keyStoreFile);
            ks.load(keyFileStream, keyStorePassword);
//   
            return (KeyStore.PrivateKeyEntry) ks.getEntry(alias, new KeyStore.PasswordProtection(keyStorePassword));
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException | CertificateException e) {
            return null;
        } finally {
            if (keyFileStream != null) {
                try {
                    keyFileStream.close();
                } catch (IOException e) {
                    LOGGER.warning("getKeyFromKeyStore"+ e);
                }
            }
        }
    }
}