/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.emudhra.esign;

import org.apache.xml.security.Init;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.ElementProxy;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 *
 * @author 21729
 */
public class DigitalSignerNew {

    private final PrivateKey privateKey;
    private final X509Certificate certificate;

    static {
        try {
            System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");
            Init.init();
        } catch (Exception ex) {
        }
    }

    public DigitalSignerNew(String keyStoreFile, char[] keyStorePassword, String alias) throws Exception {
        PrivateKey key = null;
        X509Certificate cert = null;

        try ( FileInputStream keyFileStream = new FileInputStream(keyStoreFile)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(keyFileStream, keyStorePassword);
            key = (PrivateKey) ks.getKey(alias, keyStorePassword);
            cert = (X509Certificate) ks.getCertificate(alias);
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException e) {
            throw new RuntimeException("Error loading keystore", e);
        }

        if (key == null || cert == null) {
            throw new RuntimeException("Key could not be read for digital signature. Please check alias and password.");
        }

        this.privateKey = key;
        this.certificate = cert;
    }

    public String signXML(String xmlDocument, boolean includeKeyInfo) throws Exception {
        try {
            // Set the secure validation property
            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, "");

            // Parse the input XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document inputDocument = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xmlDocument)));

            // Create a new XMLSignature
            XMLSignature signature = new XMLSignature(inputDocument, "",
                    XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1);

            // Append the signature element to the root element before signing
            inputDocument.getDocumentElement().appendChild(signature.getElement());

            // Create the reference
            Transforms transforms = new Transforms(inputDocument);
            transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);

            // Add the transforms to a reference
            signature.addDocument("", transforms, Constants.ALGO_ID_DIGEST_SHA1);

            // Add key info if requested
            if (includeKeyInfo) {
                signature.addKeyInfo(certificate);
            }

            // Sign the document
            signature.sign(privateKey);

            // Convert the signedDocument to XML String
            StringWriter stringWriter = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();

            trans.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");
            trans.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");

            trans.transform(new DOMSource(inputDocument), new StreamResult(stringWriter));

            // Explicitly replace newlines to ensure Windows-style line endings
//            String signedXml = stringWriter.getBuffer().toString().replace("\n", "\r");
            String signedXml = stringWriter.getBuffer().toString();

            return signedXml;
        } catch (IOException | ParserConfigurationException | TransformerException | SAXException e) {
            throw new RuntimeException("Error while digitally signing the XML document", e);
        }
    }
}
