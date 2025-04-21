/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import esign.text.Rectangle;
import esign.text.pdf.PdfReader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 *
 * @author 20476
 */
public final class eSignUtility {

    public static boolean isNullOrEmpty(String str) {
        return !(str != null && !str.isEmpty());
    }

    protected static boolean isNullOrWhitespace(String s) {
        return isNullOrEmpty(s) ? true : isNullOrEmpty(s.trim());
    }

    protected static String GetXpathValue(XPath xPath, String RequestPath, Document doc) throws XPathExpressionException {
        String XpathValue = xPath.compile(RequestPath).evaluate(doc);
        xPath.reset();
        return XpathValue;
    }

    protected static String getCharacterDataFromElement(Element node) {
        Node child = node.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
        }
        return "";
    }

    protected static Document convertStringToDocument(String xmlStr) throws Exception {
        Document doc = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            builder = factory.newDocumentBuilder();
            doc = builder.parse(new InputSource(new StringReader(xmlStr)));
        } catch (Exception e) {
            throw e;
        }
        return doc;
    }

    protected static String convertDocumentToString(Document doc, boolean omitXMLDeclaration) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();
            if (omitXMLDeclaration) {
                transformer.setOutputProperty("omit-xml-declaration", "yes");
            }
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.getBuffer().toString();
            return output;
        } catch (TransformerException e) {
            throw e;
        }
    }

    protected static String generateBankKYCXML(String ts, String transactionID, String bankIfscCode, String bankName, String accountNumber, UserInfo kycInfo) throws ParserConfigurationException, TransformerException {
        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element bankKYCTag = document.createElement("BankKYC");

            Attr verAttr = document.createAttribute("ver");
            verAttr.setValue("1.0");
            bankKYCTag.setAttributeNode(verAttr);

            Attr tsAttr = document.createAttribute("ts");
            tsAttr.setValue(ts);
            bankKYCTag.setAttributeNode(tsAttr);

            Attr txnAttr = document.createAttribute("txn");
            txnAttr.setValue(transactionID);
            bankKYCTag.setAttributeNode(txnAttr);

            Attr bankIfscCodeAttr = document.createAttribute("bankIfscCode");
            bankIfscCodeAttr.setValue(bankIfscCode);
            bankKYCTag.setAttributeNode(bankIfscCodeAttr);

            Attr bankNameAttr = document.createAttribute("bankName");
            bankNameAttr.setValue(bankName);
            bankKYCTag.setAttributeNode(bankNameAttr);

            Attr accountNumberAttr = document.createAttribute("accountNumber");
            accountNumberAttr.setValue(accountNumber);
            bankKYCTag.setAttributeNode(accountNumberAttr);

            Element kYCInfoTag = document.createElement("KYCInfo");

            Attr nameAttr = document.createAttribute("name");
            nameAttr.setValue(kycInfo.getName());
            kYCInfoTag.setAttributeNode(nameAttr);

            Attr mobileAttr = document.createAttribute("mobile");
            mobileAttr.setValue(kycInfo.getMobile());
            kYCInfoTag.setAttributeNode(mobileAttr);

            Attr emailAttr = document.createAttribute("email");
            emailAttr.setValue(kycInfo.getEmail());
            kYCInfoTag.setAttributeNode(emailAttr);

            Attr addressAttr = document.createAttribute("address");
            addressAttr.setValue(kycInfo.getAddress());
            kYCInfoTag.setAttributeNode(addressAttr);

            Attr stateProvinceAttr = document.createAttribute("stateProvince");
            stateProvinceAttr.setValue(kycInfo.getStateProvince());
            kYCInfoTag.setAttributeNode(stateProvinceAttr);

            Attr countryAttr = document.createAttribute("country");
            countryAttr.setValue(kycInfo.getCountry());
            kYCInfoTag.setAttributeNode(countryAttr);

            Attr postalCodeAttr = document.createAttribute("postalCode");
            postalCodeAttr.setValue(kycInfo.getPostalCode());
            kYCInfoTag.setAttributeNode(postalCodeAttr);

            Attr dateOfBirthAttr = document.createAttribute("dateOfBirth");
            dateOfBirthAttr.setValue(kycInfo.getDateOfBirth());
            kYCInfoTag.setAttributeNode(dateOfBirthAttr);

            Attr gender = document.createAttribute("gender");
            gender.setValue(kycInfo.getGender());
            kYCInfoTag.setAttributeNode(gender);

            Attr panAttr = document.createAttribute("pan");
            panAttr.setValue(kycInfo.getPan());
            kYCInfoTag.setAttributeNode(panAttr);

            Attr aadhaarAttr = document.createAttribute("Aadhaar");
            aadhaarAttr.setValue(kycInfo.getAadhaar());
            kYCInfoTag.setAttributeNode(aadhaarAttr);

            Element photoTag = document.createElement("Photo");

            Attr formatAttr = document.createAttribute("format");
            formatAttr.setValue(kycInfo.getPhotoFormat());
            photoTag.setAttributeNode(formatAttr);
            photoTag.appendChild(document.createTextNode(kycInfo.getPhotoBase64()));

            bankKYCTag.appendChild(kYCInfoTag);
            bankKYCTag.appendChild(photoTag);
            document.appendChild(bankKYCTag);
            return convertDocumentToString(document, false);

        } catch (ParserConfigurationException | TransformerException | DOMException e) {
            throw e;
        }
    }

    protected static String signXML(String xml, String filepath, String password, String pfxAlias) throws Exception {
        try {

            DigitalSigner ds = new DigitalSigner(filepath, password.toCharArray(), pfxAlias);
            String XmlSigned = ds.signXML(xml, true);
            return XmlSigned;

        } catch (Exception ex) {
            throw ex;
        }
    }

    protected static String signXMLAndroid(String xml, String filepath, String password, String pfxAlias) throws Exception {
        try {

            DigitalSignerNew ds = new DigitalSignerNew(filepath, password.toCharArray(), pfxAlias);
            String XmlSigned = ds.signXML(xml, true);
            return XmlSigned;

        } catch (Exception ex) {
            throw ex;
        }
    }

    protected static boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected static ReturnDocument getReturnDocumentById(int id, ArrayList<ReturnDocument> returnDocuments) {
        try {
            for (ReturnDocument r : returnDocuments) {
                if (r.getDocId() == id) {
                    return r;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    protected static boolean allDocumentHaveError(ArrayList<ReturnDocument> returnDocuments) {
        int docsWithError = 0;
        try {
            for (ReturnDocument r : returnDocuments) {
                if (!isNullOrWhitespace(r.getErrorMessage())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            throw e;
        }
    }

    protected static String getRequestXML(ArrayList<ReturnDocument> returnDocuments, String SipID, String ts, String txn, String kycID, boolean sipIsRka, boolean userConsentObtained, String base64SignatoryInfo) throws Exception {
        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element SignReqTag = document.createElement("SignReq");

            Attr verAttr = document.createAttribute("version");
            verAttr.setValue("1.00");
            SignReqTag.setAttributeNode(verAttr);

            Attr sipIdAttr = document.createAttribute("sipId");
            sipIdAttr.setValue(SipID);
            SignReqTag.setAttributeNode(sipIdAttr);

            Attr tsAttr = document.createAttribute("ts");
            tsAttr.setValue(ts);
            SignReqTag.setAttributeNode(tsAttr);

            Attr txnAttr = document.createAttribute("txn");
            txnAttr.setValue(txn);
            SignReqTag.setAttributeNode(txnAttr);

            Attr kycIdAttr = document.createAttribute("kycId");
            kycIdAttr.setValue(kycID);
            SignReqTag.setAttributeNode(kycIdAttr);

            Attr signAlgorithmAttr = document.createAttribute("signAlgorithm");
            signAlgorithmAttr.setValue("ECC");
            SignReqTag.setAttributeNode(signAlgorithmAttr);

            Attr sipIsRkaAttr = document.createAttribute("sipIsRka");
            String isRkaStr = sipIsRka ? "1" : "0";
            sipIsRkaAttr.setValue(isRkaStr);
            SignReqTag.setAttributeNode(sipIsRkaAttr);

            Attr userConsentObtainedAttr = document.createAttribute("userConsentObtained");
            String userConsentObtainedStr = userConsentObtained ? "1" : "0";
            userConsentObtainedAttr.setValue(userConsentObtainedStr);
            SignReqTag.setAttributeNode(userConsentObtainedAttr);

            Element DocsTag = document.createElement("Docs");

            for (ReturnDocument returnDocument : returnDocuments) {
                int docId = returnDocument.getDocId();
                if (docId == 0) {
                    continue;
                }
                Element InputHashTag = document.createElement("InputHash");

                Attr idAttr = document.createAttribute("id");
                idAttr.setValue(Integer.toString(docId));
                InputHashTag.setAttributeNode(idAttr);

                Attr hashAlgorithmAttr = document.createAttribute("hashAlgorithm");
                hashAlgorithmAttr.setValue("SHA256");
                InputHashTag.setAttributeNode(hashAlgorithmAttr);

                Attr responseSigTypeAttr = document.createAttribute("responseSigType");
                responseSigTypeAttr.setValue("pkcs7");
                InputHashTag.setAttributeNode(responseSigTypeAttr);

                InputHashTag.appendChild(document.createTextNode(returnDocument.getDocumentHash()));

                DocsTag.appendChild(InputHashTag);
            }
            SignReqTag.appendChild(DocsTag);

            Element SignatoryInfoTag = document.createElement("SignatoryInfo");
            SignatoryInfoTag.appendChild(document.createTextNode(base64SignatoryInfo));

            SignReqTag.appendChild(SignatoryInfoTag);

            document.appendChild(SignReqTag);
            String outXML = convertDocumentToString(document, false);
            return outXML;
        } catch (ParserConfigurationException | TransformerException | DOMException e) {
            throw e;
        }
    }

    protected static String excutePostHttp(String targetURL, String urlParameters) throws Exception {
        URL url;
        HttpURLConnection connection = null;
        try {

            //Create posttohttpurlconnection
            url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            //Get Response	
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();

        } catch (Exception e) {
            return "";

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    protected static String checkeSignStatus(String ts, String txn, String ASPID) throws Exception {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        Element eSignTag = document.createElement("EsignStatus");

        Attr verAttr = document.createAttribute("ver");
        verAttr.setValue("3.0");
        eSignTag.setAttributeNode(verAttr);

        Attr tsAttr = document.createAttribute("ts");
        tsAttr.setValue(ts);
        eSignTag.setAttributeNode(tsAttr);

        Attr txnAttr = document.createAttribute("txn");
        txnAttr.setValue(txn);
        eSignTag.setAttributeNode(txnAttr);

        Attr aspIdAttr = document.createAttribute("aspId");
        aspIdAttr.setValue(ASPID);
        eSignTag.setAttributeNode(aspIdAttr);

        document.appendChild(eSignTag);
        String outXML = convertDocumentToString(document, false);
        return outXML;
    }

    protected static String validatePageLevelCordinate(String pagelevelCoordinate, boolean isContentSearch, PdfReader reader) throws Exception {
        String[] cords = pagelevelCoordinate.split(";");
        for (String cord : cords) {
            String[] signature = cord.split("-");
            int pageNumber = Integer.parseInt(signature[0]);
            String position = signature[1];
            String[] rect = position.split(",");
            int x1 = Integer.parseInt(rect[0]);
            int y1 = Integer.parseInt(rect[1]);
            int x2 = Integer.parseInt(rect[2]);
            int y2 = Integer.parseInt(rect[3]);
            Rectangle r = reader.getPageSizeWithRotation(pageNumber);
            if (isContentSearch) {
                if (!(r.getWidth() >= x1 && r.getWidth() >= x2 && r.getHeight() >= y1 && r.getHeight() >= y2)) {
                    int x = pagelevelCoordinate.indexOf(cord) + cord.length() + 1;
                    if (x >= pagelevelCoordinate.length()) {
                        pagelevelCoordinate = pagelevelCoordinate.replace(cord, "");
                    } else {
                        pagelevelCoordinate = pagelevelCoordinate.replace(cord + ";", "");
                    }
                }
            } else {
                if (!(r.getWidth() >= x1 && r.getWidth() >= x2 && r.getHeight() >= y1 && r.getHeight() >= y2)) {
                    throw new Exception("Invalid coordinates");
                }

            }

        }
        return pagelevelCoordinate;
    }

    protected static String generateRequestXML(ArrayList<ReturnDocument> returnDocuments, String signerId, String ASPID, String responseUrl, String redirectUrl, String transactionID, String ts, String maxWaitPeriod, boolean isLTVRequired) throws Exception {
        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element eSignTag = document.createElement("Esign");

            Attr verAttr = document.createAttribute("ver");
            verAttr.setValue("3.0");
            eSignTag.setAttributeNode(verAttr);

            Attr signeridAttr = document.createAttribute("signerid");
            signeridAttr.setValue(signerId);
            eSignTag.setAttributeNode(signeridAttr);

            Attr tsAttr = document.createAttribute("ts");
            tsAttr.setValue(ts);
            eSignTag.setAttributeNode(tsAttr);

            Attr txnAttr = document.createAttribute("txn");
            txnAttr.setValue(transactionID);
            eSignTag.setAttributeNode(txnAttr);

            Attr aspIdAttr = document.createAttribute("aspId");
            aspIdAttr.setValue(ASPID);
            eSignTag.setAttributeNode(aspIdAttr);

            Attr responseUrlAttr = document.createAttribute("responseUrl");
            responseUrlAttr.setValue(responseUrl);
            eSignTag.setAttributeNode(responseUrlAttr);

            Attr redirectUrlAttr = document.createAttribute("redirectUrl");
            redirectUrlAttr.setValue(redirectUrl);
            eSignTag.setAttributeNode(redirectUrlAttr);

            Attr signingAlgorithmAttr = document.createAttribute("signingAlgorithm");
            signingAlgorithmAttr.setValue("ECDSA");
//            signingAlgorithmAttr.setValue("RSA");
            eSignTag.setAttributeNode(signingAlgorithmAttr);

            Attr maxWaitPeriodAttr = document.createAttribute("maxWaitPeriod");
            maxWaitPeriodAttr.setValue(maxWaitPeriod);
            eSignTag.setAttributeNode(maxWaitPeriodAttr);

            Element docsTag = document.createElement("Docs");

            for (ReturnDocument returnDocument : returnDocuments) {
                if (returnDocument.getDocId() == 0) {
                    continue;
                }
                Element inputHashTag = document.createElement("InputHash");

                Attr idAttr = document.createAttribute("id");
                idAttr.setValue(Integer.toString(returnDocument.getDocId()));
                inputHashTag.setAttributeNode(idAttr);

                Attr hashAlgorithmAttr = document.createAttribute("hashAlgorithm");
                hashAlgorithmAttr.setValue("SHA256");
                inputHashTag.setAttributeNode(hashAlgorithmAttr);

                Attr docInfoAttr = document.createAttribute("docInfo");
                docInfoAttr.setValue(returnDocument.getDocInfo());
                inputHashTag.setAttributeNode(docInfoAttr);

                Attr docUrlAttr = document.createAttribute("docUrl");
                docUrlAttr.setValue(returnDocument.getDocURL());
                inputHashTag.setAttributeNode(docUrlAttr);

                Attr responseSigTypeAttr = document.createAttribute("responseSigType");
                responseSigTypeAttr.setValue(isLTVRequired ? "PKCS7pdf" : "PKCS7");
                inputHashTag.setAttributeNode(responseSigTypeAttr);

                inputHashTag.appendChild(document.createTextNode(returnDocument.getDocumentHash()));

                docsTag.appendChild(inputHashTag);
            }
            eSignTag.appendChild(docsTag);
            document.appendChild(eSignTag);
            String outXML = convertDocumentToString(document, false);
            return outXML;
        } catch (Exception e) {
            throw e;
        }
    }

    protected static String generateRequestXMLV2(ArrayList<ReturnDocument> returnDocuments, String ASPID, String responseUrl, String redirectUrl, String transactionID, String ts, eSign.AuthMode authMode, boolean isLTVRequired) throws Exception {
        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element eSignTag = document.createElement("Esign");

            Attr verAttr = document.createAttribute("ver");
            verAttr.setValue("2.1");
            eSignTag.setAttributeNode(verAttr);

//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
//            ts = sdf.format(new Date());
            Attr tsAttr = document.createAttribute("ts");
            tsAttr.setValue(ts);
            eSignTag.setAttributeNode(tsAttr);

            Attr txnAttr = document.createAttribute("txn");
            txnAttr.setValue(transactionID);
            eSignTag.setAttributeNode(txnAttr);

            Attr aspIdAttr = document.createAttribute("aspId");
            aspIdAttr.setValue(ASPID);
            eSignTag.setAttributeNode(aspIdAttr);

            Attr responseUrlAttr = document.createAttribute("responseUrl");
            responseUrlAttr.setValue(responseUrl);
            eSignTag.setAttributeNode(responseUrlAttr);

            Attr signingAlgorithmAttr = document.createAttribute("responseSigType");
            signingAlgorithmAttr.setValue(isLTVRequired ? "PKCS7pdf" : "PKCS7");
            eSignTag.setAttributeNode(signingAlgorithmAttr);

            Attr sc = document.createAttribute("sc");
            sc.setValue("y");
            eSignTag.setAttributeNode(sc);

            Attr ekycIdType = document.createAttribute("ekycIdType");
            ekycIdType.setValue("A");
            eSignTag.setAttributeNode(ekycIdType);

            Attr AuthMode = document.createAttribute("AuthMode");
            AuthMode.setValue(authMode.getVal());
            eSignTag.setAttributeNode(AuthMode);

            Attr ekycIdAttr = document.createAttribute("ekycId");
            ekycIdAttr.setValue("");
            eSignTag.setAttributeNode(ekycIdAttr);

            Element docsTag = document.createElement("Docs");

            for (ReturnDocument returnDocument : returnDocuments) {
                if (returnDocument.getDocId() == 0) {
                    continue;
                }
                Element inputHashTag = document.createElement("InputHash");

                Attr idAttr = document.createAttribute("id");
                idAttr.setValue(Integer.toString(returnDocument.getDocId()));
                inputHashTag.setAttributeNode(idAttr);

                Attr hashAlgorithmAttr = document.createAttribute("hashAlgorithm");
                hashAlgorithmAttr.setValue("SHA256");
                inputHashTag.setAttributeNode(hashAlgorithmAttr);

                Attr docInfoAttr = document.createAttribute("docInfo");
                docInfoAttr.setValue(returnDocument.getDocInfo());
                inputHashTag.setAttributeNode(docInfoAttr);

                inputHashTag.appendChild(document.createTextNode(returnDocument.getDocumentHash()));

                docsTag.appendChild(inputHashTag);
            }
            eSignTag.appendChild(docsTag);
            document.appendChild(eSignTag);
            String outXML = convertDocumentToString(document, false);
            return outXML;
        } catch (Exception e) {
            throw e;
        }
    }

    protected static String generateTempTransactionData(ArrayList<ReturnDocument> returnDocuments) throws Exception {
        try {
            String tempData = "";
            for (ReturnDocument returnDocument : returnDocuments) {
                if (returnDocument.getDocId() == 0) {
                    continue;
                }
                tempData = tempData + returnDocument.getReturnDocumentObjBase64() + "|";

            }
            return org.emcastle.util.encoders.Base64.toBase64String(tempData.getBytes("utf-8"));
        } catch (Exception e) {
            throw e;
        }
    }

    protected static ArrayList<ReturnDocument> getReturnDocumentsFromPreSignedPDFFile(byte[] preSignedBytes) throws Exception {
        try {
            ArrayList<ReturnDocument> returnDocuments = new ArrayList<>();
            byte[] decodedBytes = org.emcastle.util.encoders.Base64.decode(preSignedBytes);
            String tempFileStr = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] returnDocumentsStrs = tempFileStr.split("\\|");
            for (String returnDocumentStr : returnDocumentsStrs) {
                if (isNullOrWhitespace(returnDocumentStr)) {
                    continue;
                }
                returnDocuments.add(new ReturnDocument(returnDocumentStr));
            }
            return returnDocuments;
        } catch (Exception e) {
            throw new Exception("Unable to Parse temp file", e);
        }
    }
}
