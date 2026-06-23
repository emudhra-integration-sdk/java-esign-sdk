/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import static com.emudhra.esign.eSign.PageTobeSigned;
import esign.text.Font;
import esign.text.Image;
import esign.text.Rectangle;
import esign.text.awt.PdfGraphics2D;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.BaseFont;
import esign.text.pdf.ByteBuffer;
import esign.text.pdf.PdfContentByte;
import esign.text.pdf.PdfDate;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfLiteral;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfReader;
import esign.text.pdf.PdfSignature;
import esign.text.pdf.PdfSignatureAppearance;
import esign.text.pdf.PdfStamper;
import esign.text.pdf.PdfString;
import esign.text.pdf.AcroFields;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfIndirectObject;
import esign.text.pdf.PRStream;
import esign.text.pdf.PdfTemplate;
import esign.text.pdf.SignatureAppearanceCreator;
import org.emcastle.asn1.x500.RDN;
import org.emcastle.asn1.x500.X500Name;
import org.emcastle.asn1.x500.style.BCStyle;
import org.emcastle.asn1.x500.style.IETFUtils;
import org.emcastle.asn1.x509.X509CertificateStructure;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.io.IOUtils;
import org.emcastle.jce.provider.emCastleProvider;
import org.emcastle.util.encoders.Base64;
import org.emcastle.util.encoders.Hex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;

/**
 *
 * @author 21685
 */
public final class eSignImplimentation {

    private final String pfxpath;
    private final String password;
    private final String pfxAlias;
    private final boolean proxyreq;
    private final String proxyIp;
    private final int proxyPort;

    private final static Logger LOGGER = EsignLoggerFactory.getLogger(eSignImplimentation.class);

    protected eSignImplimentation(String pfxfile, String password, String pfxAlias, String proxyIp, int proxyPort, boolean proxyreq) {
        this.pfxpath = pfxfile;
        this.password = password;
        this.proxyreq = proxyreq;
        this.proxyIp = proxyIp;
        this.proxyPort = proxyPort;
        this.pfxAlias = pfxAlias;
    }

    protected eSignServiceReturn getEncryptedPath(String path) {
        eSignServiceReturn serviceReturnObj = new eSignServiceReturn();
        try {
            serviceReturnObj.setEnCryptedPath(EncryptionHelper.getEncryptedData(path, eSignSettings.getEncryptionKey()));
            serviceReturnObj.setStatus(1);
            return serviceReturnObj;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            serviceReturnObj.setErrorCode("ESS-999");
            serviceReturnObj.setStatus(0);
            serviceReturnObj.setErrorMessage(ex.getMessage());
            return serviceReturnObj;
        }
    }

    @Deprecated
    protected eSignServiceReturn getGatewayParameter(ArrayList<eSignInput> inputs, String signerID, String transactionID, String responseUrl, String redirectUrl, String tempFolder, int SignatureContents) {
        return getGatewayParameterPrivate(inputs, signerID, transactionID, responseUrl, redirectUrl, tempFolder, eSign.eSignAPIVersion.V3, eSign.AuthMode.OTP, 1440, true, SignatureContents);
    }

    protected eSignServiceReturn getGatewayParameter(ArrayList<eSignInput> inputs, String signerID, String transactionID, String responseUrl, String redirectUrl, String tempFolder, eSign.eSignAPIVersion esignType, eSign.AuthMode authMode, int maxWaitPeriodinMin, boolean isLTVRequired, int SignatureContents) {
        return getGatewayParameterPrivate(inputs, signerID, transactionID, responseUrl, redirectUrl, tempFolder, esignType, authMode, maxWaitPeriodinMin, isLTVRequired, SignatureContents);
    }

    private eSignServiceReturn getGatewayParameterPrivate(ArrayList<eSignInput> inputs, String signerID, String transactionID, String responseUrl, String redirectUrl, String tempFolder, eSign.eSignAPIVersion esignType, eSign.AuthMode authMode, int maxWaitPeriodinMin, boolean isLTVRequired, int SignatureContents) {
        eSignServiceReturn serviceReturnObj = new eSignServiceReturn();
        int contentEstimated = 21000;
        if (SignatureContents != 0) {
            contentEstimated = SignatureContents;
        }
//        int contentEstimated = 8192 * 2;
        String maxWaitPeriod = "";
        try {
            if (inputs.size() > 5 || inputs.isEmpty()) {
                serviceReturnObj.setResponseXML("");
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setErrorCode("ESS-100");
                serviceReturnObj.setStatus(0);
                serviceReturnObj.setErrorMessage("Minimum of 1 and Maximum of 5 Documents can be signed in a single request.");
                return serviceReturnObj;
            }
            if (maxWaitPeriodinMin < 1) {
                serviceReturnObj.setResponseXML("");
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setErrorCode("ESS-109");
                serviceReturnObj.setStatus(0);
                serviceReturnObj.setErrorMessage("Invalid value for max wait time period.");
                return serviceReturnObj;
            }
            maxWaitPeriod = Integer.toString(maxWaitPeriodinMin);

            if (eSignUtility.isNullOrWhitespace(signerID)) {
                signerID = "";
            }
            if (eSignUtility.isNullOrWhitespace(tempFolder)) {
                serviceReturnObj.setResponseXML("");
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setErrorCode("ESS-103");
                serviceReturnObj.setStatus(0);
                serviceReturnObj.setErrorMessage("temp folder path be empty");
                return serviceReturnObj;
            }
            if (transactionID.length() >= 50) {
                serviceReturnObj.setResponseXML("");
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setErrorCode("ESS-114");
                serviceReturnObj.setStatus(0);
                serviceReturnObj.setErrorMessage("transactionID should be less then 50 character.");
                return serviceReturnObj;
            }

            File dir = new File(tempFolder);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // Attach file logger to tempFolder/logs — writable on every platform
            // (Android, server, desktop). No-op if already attached or logging is off.
            EsignLoggerFactory.initFileHandler(tempFolder + File.separator + "logs");

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, 0);
            SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            TimeZone timeZone = TimeZone.getTimeZone("IST");
            tsFormat.setTimeZone(timeZone);
            Date now = new Date(System.currentTimeMillis());
            String timeStamp = tsFormat.format(now);

            if (eSignUtility.isNullOrWhitespace(transactionID)) {
                transactionID = UUID.randomUUID().toString().replace("-", "");
            }
            serviceReturnObj.setTransactionID(transactionID);
            String tempFilePath = tempFolder + File.separator + transactionID + ".sig";
            int count = 1;
            ArrayList<ReturnDocument> returnDocuments = new ArrayList<>();
            for (eSignInput input : inputs) {
                if (input.getInputType() == eSign.InputType.PDF) {
                    try {

                        String hexHashDocument = "";
                        String preSignedPdf = "";
                        String cordinate = "";
                        boolean isPDF = true;
                        try ( ByteArrayOutputStream fos = new ByteArrayOutputStream()) {
                            if (!input.getDocHash().isEmpty()) {
                                hexHashDocument = input.getDocHash();
                                isPDF = false;
                            } else {
                                PageTobeSigned Page = input.getPage();
                                String pagenumber = input.getPageNumbers();
                                byte[] decodePDF = Base64.decode(input.getDocBase64());
                                PdfReader reader;
                                if (input.getPdfPassword() == null || input.getPdfPassword().isEmpty()) {
                                    reader = new PdfReader(decodePDF);
                                } else {
                                    reader = new PdfReader(decodePDF, input.getPdfPassword().getBytes());
                                }

                                if (input.getContentSearch() != null) {
                                    //validation for content search
                                    if (input.getContentSearch().getHeight() <= 0) {
                                        serviceReturnObj.setErrorCode("ESS-121");
                                        serviceReturnObj.setErrorMessage("Invalid height");
                                        return serviceReturnObj;
                                    }
                                    if (input.getContentSearch().getWidth() <= 0) {
                                        serviceReturnObj.setErrorCode("ESS-121");
                                        serviceReturnObj.setErrorMessage("Invalid Width");
                                        return serviceReturnObj;
                                    }
                                    if (eSignUtility.isNullOrEmpty(input.getContentSearch().getOffset())) {
                                        serviceReturnObj.setErrorCode("ESS-121");
                                        serviceReturnObj.setErrorMessage("Offset cannot be empty");
                                        return serviceReturnObj;
                                    }
                                    if (eSignUtility.isNullOrEmpty(input.getContentSearch().getSearchText())) {
                                        serviceReturnObj.setErrorCode("ESS-121");
                                        serviceReturnObj.setErrorMessage("Search text cannot be empty");
                                        return serviceReturnObj;
                                    }
                                    if (input.getContentSearch().getPosition() == null) {
                                        serviceReturnObj.setErrorCode("ESS-121");
                                        serviceReturnObj.setErrorMessage("Invalid Postion");
                                        return serviceReturnObj;
                                    }
                                    TextCoordinates objtxt = new TextCoordinates();
                                    cordinate = objtxt.getCoordinates(reader, input.getContentSearch().getSearchText(), input.getContentSearch().getOffset(), input.getContentSearch().getHeight(), input.getContentSearch().getWidth(), input.getContentSearch().getPosition());
                                    input.pageLevelCoordinates(cordinate);
                                    if (eSignUtility.isNullOrEmpty(cordinate)) {
                                        serviceReturnObj.setErrorCode("ESS-120");
                                        serviceReturnObj.setErrorMessage("Unable to find content");
                                        return serviceReturnObj;
                                    }
                                }

                                if (input.getContentSearch() != null) {
                                    try {
                                        input.pageLevelCoordinates(eSignUtility.validatePageLevelCordinate(input.getPageLevelCoordinates(), true, reader));
                                    } catch (Exception ex) {
                                        serviceReturnObj.setErrorCode("ESS-120");
                                        serviceReturnObj.setErrorMessage("Unable to find content");
                                        return serviceReturnObj;
                                    }
                                } else {
                                    try {
                                        String pageLevelCoordinates = input.getPageLevelCoordinates();
                                        if (Page.toString().equalsIgnoreCase("pagelevel")) {
                                            pageLevelCoordinates = reformatPagelevelCoordinates(pageLevelCoordinates, reader.getNumberOfPages());     
                                            input.pageLevelCoordinates(eSignUtility.validatePageLevelCordinate(pageLevelCoordinates, false, reader));
                                        }
                                    } catch (Exception ex) {
                                        serviceReturnObj.setErrorCode("RDSA-120");
                                        serviceReturnObj.setErrorMessage("Invalid Coordinate.");
                                        return serviceReturnObj;
                                    }
                                }

                                if (eSignUtility.isNullOrEmpty(input.getPageLevelCoordinates())) {
                                    serviceReturnObj.setErrorCode("ESS-120");
                                    serviceReturnObj.setErrorMessage("Unable to find content");
                                    return serviceReturnObj;
                                }

                                if (reader.isRebuilt()) {
                                    reader.enableRebuild();
                                    ByteArrayOutputStream fos1 = new ByteArrayOutputStream();
                                    PdfStamper stamper = new PdfStamper(reader, fos1);
                                    stamper.close();
                                    reader = new PdfReader(fos1.toByteArray());
                                }

                                PdfStamper stamper = PdfStamper.createSignature(reader, fos, '\0', null, input.isCoSign());
                                PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
                                StringBuilder layer2text = new StringBuilder();
                                Font font = new Font(Font.FontFamily.HELVETICA, input.getSignatureFontSize(), Font.NORMAL);

                                if (input.getCustomStyle() != null) {

                                    font.setColor(input.getCustomStyle().getFontColor());
                                }

                                if (null != input.getAppearanceType()) {

                                    switch (input.getAppearanceType()) {

                                        case StandardSignature:

                                            String signedByName = input.getSignedBy();
                                            appearance.setSignDate(cal);
                                            appearance.setSignerName(input.getSignedBy());

                                            appearance.setReason(input.getReason());
                                            appearance.setLocation(input.getLocation());

                                            StringBuilder sb = new StringBuilder();
                                            if (!eSignUtility.isNullOrEmpty(input.getAppearanceText())) {
                                                sb.append(input.getAppearanceText());
                                                sb.append("\n");
                                                sb.toString();
                                            } else {
                                                if (!eSignUtility.isNullOrEmpty(signedByName)) {
                                                    sb.append("Signed by: ");
                                                    sb.append(signedByName);
                                                    sb.append("\n");
                                                }
                                                if (!eSignUtility.isNullOrEmpty(appearance.getReason())) {
                                                    sb.append("Reason: ");
                                                    sb.append(appearance.getReason());
                                                    sb.append("\n");
                                                }
                                                if (!eSignUtility.isNullOrEmpty(appearance.getLocation())) {
                                                    sb.append("Location: ");
                                                    sb.append(appearance.getLocation());
                                                    sb.append("\n");
                                                }
                                                sb.append("Date: ");
                                                sb.append(timeStamp.replace('T', ' '));
                                                sb.append("\n");
                                            }
                                            appearance.setLayer2Text(sb.toString());

                                            if (input.getSignatureFontSize() < -1) {
                                                serviceReturnObj.setErrorCode("ESS-122");
                                                serviceReturnObj.setErrorMessage("Invalid font size");
                                                return serviceReturnObj;
                                            }

                                            if (input.getSignatureFontSize() != -1) {
                                                Font font1 = new Font(Font.FontFamily.HELVETICA, input.getSignatureFontSize(), Font.NORMAL);
                                                appearance.setLayer2Font(font1);
                                            }
//                                            appearance.setAcro6Layers(false);
                                            appearance.setAcro6Layers(!(input.isTickRequired()));
                                            appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
                                            break;

                                        case OneLiner:
                                            appearance.setLayer2Font(font);
                                            appearance.setLayer2Text(input.getOneLiner());
                                            appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION);
                                            appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
                                            break;

                                        case SignatureImage:
                                            if (input.getSignatureImage() == null) {
                                                serviceReturnObj.setErrorCode("ESS-126");
                                                serviceReturnObj.setErrorMessage("SignatureImage cannot be empty");
                                                return serviceReturnObj;
                                            }

                                            Image img = Image.getInstance(Base64.decode(input.getSignatureImage()));
                                            Image imgFinal = img;

                                            appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC_AND_DESCRIPTION);
                                            img.setAbsolutePosition(0, 0);
                                            if (img.getHeight() != img.getWidth()) {
                                                float appearanceHeight = 280 * 0.57f;
                                                float appearanceWidth = 280 * 0.57f;
                                                img.scaleAbsolute(appearanceWidth, appearanceHeight);
                                                PdfTemplate templateTemp = PdfTemplate.createTemplate(stamper.getWriter(), appearanceWidth, appearanceHeight);
                                                templateTemp.addImage(img);
                                                imgFinal = Image.getInstance(templateTemp);
                                                imgFinal.setAbsolutePosition(2, 0);
                                                imgFinal.scaleAbsolute(appearanceWidth, appearanceHeight);
                                            } else {
                                                img.scaleAbsolute(img.getWidth(), img.getHeight());
                                                PdfTemplate templateTemp = PdfTemplate.createTemplate(stamper.getWriter(), img.getWidth(), img.getHeight());
                                                templateTemp.addImage(img);
                                                imgFinal.setAbsolutePosition(2, 0);
                                                imgFinal.scaleAbsolute(img.getWidth(), img.getHeight());
                                            }

                                            if (eSignUtility.isNullOrWhitespace(input.getAppearanceText())) {

                                                layer2text = new StringBuilder();
                                                layer2text.append("Digitally Signed.\n");
                                                if (!eSignUtility.isNullOrWhitespace(input.getSignedBy())) {
                                                    layer2text.append("Name: ").append(input.getSignedBy()).append("\n");
                                                    appearance.setContact(input.getSignedBy());
                                                }
                                                if (!eSignUtility.isNullOrWhitespace(input.getReason())) {
                                                    layer2text.append("Reason: ").append(input.getReason()).append("\n");
                                                    appearance.setReason(input.getReason());
                                                }
                                                if (!eSignUtility.isNullOrWhitespace(input.getLocation())) {
                                                    layer2text.append("Location: ").append(input.getLocation()).append("\n");
                                                    appearance.setLocation(input.getLocation());
                                                }
                                                cal = Calendar.getInstance();
                                                cal.add(Calendar.MINUTE, 1);
                                                layer2text.append("Date: ").append(tsFormat.format(cal.getTime())).append("\n");
                                                appearance.setSignDate(cal);
                                                appearance.setLayer2Font(font);
                                                appearance.setLayer2Text(layer2text.toString());

                                            } else {
                                                appearance.setLayer2Font(font);
                                                appearance.setLayer2Text(input.getAppearanceText());
                                            }

                                            appearance.setSignatureGraphic(imgFinal);

                                            break;

                                        case advanceSignature:
                                            ImgTemplate imgTemplate;
                                            if (input.getAdvanceSignature().getImageType() == Enums.ImageType.SVG) {
                                                String str;
                                                float f1;
                                                SVGDocument sVGDocument1;
                                                GraphicsNode graphicsNode;
                                                SAXSVGDocumentFactory sAXSVGDocumentFactory;
                                                SVGDocument sVGDocument2;
                                                UserAgentAdapter userAgentAdapter;
                                                BridgeContext bridgeContext;

                                                float f2;
                                                DocumentLoader documentLoader;
                                                GVTBuilder gVTBuilder;
                                                PdfGraphics2D pdfGraphics2D;
                                                str = new String(Base64.decode(input.getAdvanceSignature().getImagebase64()));
                                                str = str.replaceAll("(\\s+)font=\"(.*?)\"", "");
                                                str = str.replaceAll("fill='transparent'", "fill='none'");
                                                sAXSVGDocumentFactory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
                                                sVGDocument2 = sAXSVGDocumentFactory.createSVGDocument("", new ByteArrayInputStream(str.getBytes()));
                                                f1 = Float.parseFloat("100px".replaceAll("[^0-9.,]", ""));
                                                f2 = Float.parseFloat("100px".replaceAll("[^0-9.,]", ""));
                                                sVGDocument1 = sVGDocument2;

                                                PdfTemplate pdfTemplate = PdfTemplate.createTemplate(stamper.getWriter(), f2, f1);
                                                userAgentAdapter = new UserAgentAdapter();
                                                documentLoader = new DocumentLoader((UserAgent) userAgentAdapter);
                                                bridgeContext = new BridgeContext((UserAgent) userAgentAdapter, documentLoader);
                                                bridgeContext.setDynamicState(2);
                                                gVTBuilder = new GVTBuilder();
                                                pdfGraphics2D = new PdfGraphics2D((PdfContentByte) pdfTemplate, pdfTemplate.getWidth(),
                                                        pdfTemplate.getHeight());
                                                pdfGraphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                                graphicsNode = gVTBuilder.build(bridgeContext, (Document) sVGDocument1);

                                                graphicsNode.setTransform(AffineTransform.getScaleInstance(100, 100));
                                                graphicsNode.setPointerEventType(8);
                                                graphicsNode.paint((PdfGraphics2D) pdfGraphics2D);
                                                pdfGraphics2D.dispose();
                                                imgTemplate = new ImgTemplate(pdfTemplate);
                                            } else {
                                                img = Image.getInstance(Base64.decode(input.getAdvanceSignature().getImagebase64()));
                                                imgFinal = img;
                                                img.setAbsolutePosition(0, 0);
                                                PdfTemplate templateTemp = PdfTemplate.createTemplate(stamper.getWriter(), img.getWidth(), img.getHeight());
                                                templateTemp.addImage(img);
                                                imgFinal = Image.getInstance(templateTemp);
                                                imgTemplate = new ImgTemplate(templateTemp);

                                                appearance.setSignerName(input.getAdvanceSignature().getLeftSideText());
                                                appearance.setLayer2Font(font);
                                                appearance.setLayer2Text(input.getAdvanceSignature().getRightSideText());
                                                appearance.setImageScale((float) 1);
                                                appearance.setSignatureGraphic((Image) imgTemplate);
                                                appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.NAME_GRAPHIC_AND_DESCRIPTION);
                                            }
                                            break;

                                        case ColoredGraphic:
                                            PdfTemplate pdfTemplate1 = SignatureAppearanceCreator.createSignatureAppearance(stamper, input.getSignedBy(), input.getReason(), input.getLocation());
                                            ImgTemplate imgTemplate1 = new ImgTemplate(pdfTemplate1);
                                            appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
                                            appearance.setSignatureGraphic((Image) imgTemplate1);
                                            break;
                                        case BackgroundImage:
                                            Font fontbg = new Font(BaseFont.createFont(BaseFont.TIMES_ITALIC, BaseFont.CP1252, BaseFont.NOT_EMBEDDED), input.getSignatureFontSize());
                                            Image image = Image.getInstance(Base64.decode(input.getSignatureImage()));
                                            appearance.setImage(image);

                                            StringBuilder sb1 = new StringBuilder();
                                            if (!eSignUtility.isNullOrEmpty(input.getAppearanceText())) {
                                                sb1.append(input.getAppearanceText());
                                                sb1.append("\n");
                                                sb1.toString();
                                            } else {
                                                if (!eSignUtility.isNullOrEmpty(input.getSignedBy())) {
                                                    sb1.append("Digitally Signed by:\n");
                                                    sb1.append("Name: ").append(input.getSignedBy());
                                                    sb1.append("\n");
                                                }
                                                if (!eSignUtility.isNullOrEmpty(input.getLocation())) {
                                                    sb1.append("Location: ");
                                                    sb1.append(input.getLocation());
                                                    sb1.append("\n");
                                                }
                                                if (!eSignUtility.isNullOrEmpty(input.getReason())) {
                                                    sb1.append("Reason: ");
                                                    sb1.append(input.getReason());
                                                    sb1.append("\n");
                                                }

                                                sb1.append("Date: ");
                                                SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
                                                format.setTimeZone(timeZone);
                                                String formattedDateTime = format.format(now);
                                                sb1.append(formattedDateTime);
                                                sb1.append("\n");
                                            }
                                            appearance.setLayer2Text(sb1.toString());

                                            appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION);
                                            appearance.setLayer2Font(fontbg);
                                            break;
                                        default:
                                            break;
                                    }
                                }

                                int[] pages = null;
                                ArrayList<Integer> ar;
                                String coord = null;
                                if (Page == null) {
                                    Page = eSign.PageTobeSigned.PageLevel;
                                }
                                switch (Page) {
                                    case First: {
                                        pages = new int[1];
                                        pages[0] = 1;
                                    }
                                    break;

                                    case Last: {
                                        pages = new int[1];
                                        pages[0] = reader.getNumberOfPages();
                                    }
                                    break;

                                    case Even: {
                                        ar = new ArrayList<>();
                                        for (int i = 2; i <= reader.getNumberOfPages(); i = i + 2) {
                                            ar.add(i);
                                        }
                                        pages = new int[ar.size()];
                                        for (int j = 0; j < ar.size(); j++) {
                                            pages[j] = (int) ar.get(j);
                                        }
                                    }
                                    break;

                                    case Odd: {
                                        ar = new ArrayList<>();
                                        for (int i = 1; i <= reader.getNumberOfPages(); i = i + 2) {
                                            ar.add(i);

                                        }
                                        pages = new int[ar.size()];
                                        for (int j = 0; j < ar.size(); j++) {
                                            pages[j] = (int) ar.get(j);
                                        }
                                    }
                                    break;

                                    case All: {
                                        ar = new ArrayList<>();
                                        pages = new int[reader.getNumberOfPages()];
                                        for (int i = 0; i < reader.getNumberOfPages(); i++) {
                                            ar.add(i + 1);
                                        }
                                        for (int j = 0; j < pages.length; j++) {
                                            pages[j] = (int) ar.get(j);
                                        }

                                    }
                                    break;

                                    case Specify:
                                        String[] Pagelevel;
                                        Pagelevel = pagenumber.split(",");
                                        pages = new int[Pagelevel.length];
                                        for (int j = 0; j < Pagelevel.length; j++) {
                                            pages[j] = Integer.parseInt(Pagelevel[j]);
                                        }
                                        break;
                                    case PageLevel:
                                        // pages[] is allocated below after parsing pageLevelCoordinates
                                        break;
                                    default:
                                        break;
                                }
                                if (!Page.toString().equalsIgnoreCase("pagelevel")) {
                                    switch (input.getCoordinates()) {
                                        case TopLeft:
                                            coord = "25,725,145,785";
                                            break;
                                        case TopMiddle:
                                            coord = "225,725,345,785";
                                            break;
                                        case TopRight:
                                            coord = "425,725,545,785";
                                            break;
                                        case CenterLeft:
                                            coord = "25,425,145,485";
                                            break;
                                        case CenterMiddle:
                                            coord = "225,425,345,485";
                                            break;
                                        case CenterRight:
                                            coord = "425,425,545,485";
                                            break;
                                        case BottomLeft:
                                            coord = "25,100,145,160";
                                            break;
                                        case BottomMiddle:
                                            coord = "225,100,345,160";
                                            break;
                                        case BottomRight:
                                            coord = "425,100,545,160";
                                            break;
                                        default:
                                            coord = "exception in case";
                                            break;
                                    }
                                }
                                Rectangle rect;
                                List<Rectangle> rList = new ArrayList<>();
                                if (Page.toString().equalsIgnoreCase("pagelevel")) {

                                    String pageLevelCoordinates = input.getPageLevelCoordinates();
                                    pageLevelCoordinates = reformatPagelevelCoordinates(pageLevelCoordinates, reader.getNumberOfPages());
                                    String[] pl = pageLevelCoordinates.split(";");

                                    // Count valid (non-empty) entries so pages[] has no zero-filled
                                    // tail slots — page 0 is invalid in iText and causes "page empty".
                                    int validCount = 0;
                                    for (String s : pl) {
                                        if (!s.trim().isEmpty()) validCount++;
                                    }
                                    pages = new int[validCount];
                                    int y = 0;

                                    for (String pl1 : pl) {
                                        if ("".equals(pl1.trim())) {
                                            continue;
                                        }

                                        if (!pl1.contains("-")) {
                                            pl1 = y + "-" + pl1;
                                        }
                                        String[] newpages = pl1.split("-");
                                        String[] numbers = newpages[1].split(",");
                                        float x11;
                                        float y1;
                                        float x2;
                                        float y2;
                                        try {

                                            x11 = Float.valueOf(numbers[0]);
                                            y1 = Float.valueOf(numbers[1]);
                                            x2 = Float.valueOf(numbers[2]);
                                            y2 = Float.valueOf(numbers[3]);
                                            if (input.isRightOrigin()) {

                                                x11 = reader.getPageSizeWithRotation(Integer.parseInt(newpages[0])).getWidth() - Float.valueOf(numbers[2]);
                                                x2 = reader.getPageSizeWithRotation(Integer.parseInt(newpages[0])).getWidth() - Float.valueOf(numbers[0]);
                                            }
                                        } catch (NumberFormatException ex) {
                                            LOGGER.warning(ex.getLocalizedMessage());
                                            LOGGER.info("Entered into default coordinates - bottom,right");
                                            x11 = 425;
                                            y1 = 100;
                                            x2 = 555;
                                            y2 = 160;
                                        }
                                        pages[y] = Integer.parseInt(newpages[0]);
                                        rect = new Rectangle(x11, y1, x2, y2);
                                        rList.add(rect);
                                        y++;
                                    }
                                    // Call once after pages[] and rList are fully populated.
                                    // Calling inside the loop passes a partially-filled pages[] whose
                                    // unfilled slots default to 0 — an invalid iText page number.
                                    if (!rList.isEmpty()) {
                                        appearance.setVisibleSignature(rList.get(0), pages, null, rList);
                                    }
                                } else {
                                    String[] numbers1;
                                    if (coord != null) {
                                        numbers1 = coord.split(",");
                                    } else {
                                        numbers1 = new String[]{"1"};
                                    }
                                    float x11;
                                    float y11;
                                    float x21;
                                    float y21;
                                    try {
                                        x11 = Float.valueOf(numbers1[0]);
                                        y11 = Float.valueOf(numbers1[1]);
                                        x21 = Float.valueOf(numbers1[2]);
                                        y21 = Float.valueOf(numbers1[3]);
                                    } catch (NumberFormatException ex) {
                                        LOGGER.warning(ex.getLocalizedMessage());
                                        LOGGER.info("Entered into default coordinates - bottom,right");
                                        x11 = 425;
                                        y11 = 100;
                                        x21 = 555;
                                        y21 = 160;
                                    }
                                    rect = new Rectangle(x11, y11, x21, y21);
                                    for (int i = 0; i < pages.length; i++) {
                                        rList.add(rect);
                                    }
                                    appearance.setVisibleSignature(rect, pages, null, rList);
                                    // appearance.setVisibleSignature(rect, 1, null);
                                }
                                PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, PdfName.ADBE_PKCS7_DETACHED);
                                dic.setDate(new PdfDate(appearance.getSignDate()));
                                dic.setSignatureCreator("eMudhra");
                                if (appearance.getReason() != null) {
                                    dic.setReason(appearance.getReason());
                                }
                                if (appearance.getLocation() != null) {
                                    dic.setLocation(appearance.getLocation());
                                }
                                appearance.setCryptoDictionary(dic);

                                //Signature Border
                                if (input.getAppearanceType() == eSign.AppearanceType.ColoredGraphic) {
                                    appearance.getAppearance(1);
                                    PdfTemplate sigAppLayer2 = appearance.getLayer(2);
                                    Rectangle BorderRect = sigAppLayer2.getBoundingBox();
                                    sigAppLayer2.setRGBColorStroke(0, 0, 0); // Black color (RGB: 0, 0, 0)
                                    sigAppLayer2.setLineWidth(1.0f); // Set the line width for the top border
                                    sigAppLayer2.moveTo(BorderRect.getLeft(), BorderRect.getTop());
                                    sigAppLayer2.lineTo(BorderRect.getRight(), BorderRect.getTop());
                                    sigAppLayer2.stroke();

                                    sigAppLayer2.setRGBColorStroke(0, 0, 0); // Black color (RGB: 0, 0, 0)
                                    sigAppLayer2.setLineWidth(1.0f); // Set the line width for the bottom border
                                    sigAppLayer2.moveTo(BorderRect.getLeft(), BorderRect.getBottom());
                                    sigAppLayer2.lineTo(BorderRect.getRight(), BorderRect.getBottom());
                                    sigAppLayer2.stroke();

                                    if (input.getColoredGraphicInputs() != null) {
                                        sigAppLayer2.setRGBColorStroke(input.getColoredGraphicInputs().getRightBorder()[0], input.getColoredGraphicInputs().getRightBorder()[1], input.getColoredGraphicInputs().getRightBorder()[2]); // Violet color (RGB: 148, 0, 211)
                                    } else {
                                        sigAppLayer2.setRGBColorStroke(148, 0, 211); // Violet color (RGB: 148, 0, 211)
                                    }
                                    sigAppLayer2.setLineWidth(5.0f); // Increase the line width for the right border
                                    sigAppLayer2.moveTo(BorderRect.getRight(), BorderRect.getBottom());
                                    sigAppLayer2.lineTo(BorderRect.getRight(), BorderRect.getTop());
                                    sigAppLayer2.stroke();

                                    if (input.getColoredGraphicInputs() != null) {
                                        sigAppLayer2.setRGBColorStroke(input.getColoredGraphicInputs().getLeftBorder()[0], input.getColoredGraphicInputs().getLeftBorder()[1], input.getColoredGraphicInputs().getLeftBorder()[2]); // Orange color (RGB: 255, 165, 0)
                                    } else {
                                        sigAppLayer2.setRGBColorStroke(255, 165, 0);
                                    }
                                    sigAppLayer2.setLineWidth(5.0f); // Increase the line width for the left border
                                    sigAppLayer2.moveTo(BorderRect.getLeft(), BorderRect.getBottom());
                                    sigAppLayer2.lineTo(BorderRect.getLeft(), BorderRect.getTop());
                                    sigAppLayer2.stroke();
                                } else if (input.isBorderRequired() == true) {
                                    appearance.getAppearance(1);
                                    PdfTemplate sigAppLayer2 = appearance.getLayer(2);
                                    Rectangle BorderRect = sigAppLayer2.getBoundingBox();
                                    sigAppLayer2.setRGBColorFill(255, 0, 0);
                                    sigAppLayer2.setLineWidth(0.5f);
                                    sigAppLayer2.rectangle(BorderRect.LEFT - 3f, BorderRect.BOTTOM - 1f, BorderRect.getWidth() - 2f, BorderRect.getHeight() - 2.5f);
                                    sigAppLayer2.stroke();
                                }

                                HashMap<PdfName, Integer> exc = new HashMap<>();
                                exc.put(PdfName.CONTENTS, contentEstimated * 2 + 2);
                                appearance.preClose(exc);
                                int position = (int) appearance.exclusionLocations.get(PdfName.CONTENTS).getPosition();
                                int outBufferSIZE = appearance.getSigout().size();
                                String preSignedBytes = new String(Base64.encode(appearance.getSigout().toByteArray()), "UTF-8");
                                preSignedPdf = position + "|" + outBufferSIZE + "|" + preSignedBytes;
                                preSignedPdf = org.emcastle.util.encoders.Base64.toBase64String(preSignedPdf.getBytes("utf-8"));
                                InputStream is1 = appearance.getRangeStream();
                                byte[] data = IOUtils.toByteArray(is1);
                                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                                digest.update(data);
                                byte[] hash = digest.digest();
                                String hashData = new String(Base64.encode(hash));
                                byte[] hashdata = Base64.decode(hashData);
                                hexHashDocument = Hex.toHexString(hashdata);
                            }
                            ReturnDocument returnDocument = new ReturnDocument("", count, input.getDocInfo(), input.getDocURL(), hexHashDocument, preSignedPdf, eSign.InputType.PDF, input.isShowAadhaarOnSignature());
                            returnDocuments.add(returnDocument);
                            count++;
                        } catch (Exception e) {
                            LOGGER.log(java.util.logging.Level.WARNING,
                                "Unable to generate appearance for doc[" + count + "] docInfo=" + input.getDocInfo()
                                + " appearanceType=" + input.getAppearanceType()
                                + " txn=" + transactionID, e);
                            ReturnDocument returnDocument = new ReturnDocument(0, "Unable to generate appreance - " + e, "ESS-108", 0);
                            returnDocuments.add(returnDocument);
                        }
                    } catch (Exception e) {
                        LOGGER.log(java.util.logging.Level.WARNING,
                            "Unable to process document doc[" + count + "] docInfo=" + input.getDocInfo()
                            + " txn=" + transactionID, e);
                        ReturnDocument returnDocument = new ReturnDocument(0, "Unable to generate appreance - " + e, "ESS-108", 0);
                        returnDocuments.add(returnDocument);
                    }
                } else if (input.getInputType() == eSign.InputType.HASH) {
                    if (input.getDocHash().matches("^[a-fA-F0-9]{64}$")) {
                        ReturnDocument returnDocument = new ReturnDocument("", count, input.getDocInfo(), input.getDocURL(), input.getDocHash(), "", eSign.InputType.HASH);
                        returnDocuments.add(returnDocument);
                        count++;
                    } else {
                        ReturnDocument returnDocument = new ReturnDocument(0, "Only SHA-256 hash is allowed ", "ESS-108", 0);
                        returnDocuments.add(returnDocument);
                        count++;
                    }
                }
            }
            if (!eSignUtility.allDocumentHaveError(returnDocuments)) {
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setErrorCode("ESS-108");
                serviceReturnObj.setStatus(0);
                serviceReturnObj.setReturnValues(returnDocuments);
                serviceReturnObj.setErrorMessage("Unable to generate appreance");
                return serviceReturnObj;
            }
            String tempData = eSignUtility.generateTempTransactionData(returnDocuments);
            try ( PrintWriter writer = new PrintWriter(new File(tempFilePath))) {
                writer.print(tempData);
            }
            serviceReturnObj.setPreSignedTempFile(tempFilePath);

            String requestXML = "";
            if (esignType == eSign.eSignAPIVersion.V2) {
                requestXML = eSignUtility.generateRequestXMLV2(returnDocuments, eSignSettings.getASPID(), responseUrl, redirectUrl, transactionID, timeStamp, authMode, isLTVRequired);
            } else {
                requestXML = eSignUtility.generateRequestXML(returnDocuments, signerID, eSignSettings.getASPID(), responseUrl, redirectUrl, transactionID, timeStamp, maxWaitPeriod, isLTVRequired);
            }
            String signedRequestXML = eSignUtility.signXMLAndroid(requestXML, pfxpath, password, pfxAlias);

            // Encrypted Aadhaar flow: skip gateway API call, build wrapper XML instead.
            EncryptedAadhaarConfig aadhaarConfig = null;
            for (eSignInput inp : inputs) {
                if (inp.isEncryptedAadhaarFlowEnabled() && inp.getEncryptedAadhaarConfig() != null) {
                    aadhaarConfig = inp.getEncryptedAadhaarConfig();
                    break;
                }
            }
            if (aadhaarConfig != null) {
                try {
                    String aadhaar = aadhaarConfig.getAadhaarNumber();
                    if (aadhaar == null || !aadhaar.matches("\\d{12}")) {
                        serviceReturnObj.setTransactionID(transactionID);
                        serviceReturnObj.setErrorCode("ESS-130");
                        serviceReturnObj.setStatus(0);
                        serviceReturnObj.setErrorMessage("Invalid Aadhaar number: must be exactly 12 digits with no spaces or separators.");
                        return serviceReturnObj;
                    }
                    PublicKey publicKey = loadPublicKeyFromConfig(aadhaarConfig);
                    String encryptedAadhaar = encryptAadhaar(aadhaar, publicKey);
                    String base64SignedXML = new String(Base64.encode(signedRequestXML.getBytes("UTF-8")), "UTF-8");
                    String wrapperXML = "<eSignXML>"
                        + "<EncryptedAadhaar txn=\"" + transactionID + "\">" + encryptedAadhaar + "</EncryptedAadhaar>"
                        + "<Base64eSignXML>" + base64SignedXML + "</Base64eSignXML>"
                        + "</eSignXML>";
                    String gatewayParam = URLEncoder.encode(wrapperXML, "UTF-8");
                    serviceReturnObj.setRequestXML(signedRequestXML);
                    serviceReturnObj.setPreSignedTempFile(tempFilePath);
                    serviceReturnObj.setTransactionID(transactionID);
                    serviceReturnObj.setReturnValues(returnDocuments);
                    serviceReturnObj.setGatewayParameter(gatewayParam);
                    serviceReturnObj.setStatus(1);
                    return serviceReturnObj;
                } catch (IllegalArgumentException e) {
                    serviceReturnObj.setTransactionID(transactionID);
                    serviceReturnObj.setErrorCode("ESS-131");
                    serviceReturnObj.setStatus(0);
                    serviceReturnObj.setErrorMessage(e.getMessage());
                    return serviceReturnObj;
                } catch (Exception e) {
                    LOGGER.log(java.util.logging.Level.WARNING, "Aadhaar encryption failed for txn: " + transactionID, e);
                    serviceReturnObj.setTransactionID(transactionID);
                    serviceReturnObj.setErrorCode("ESS-132");
                    serviceReturnObj.setStatus(0);
                    serviceReturnObj.setErrorMessage("Aadhaar encryption failed: " + e.getMessage());
                    return serviceReturnObj;
                }
            }

            String URLEncodedsignedRequestXML = URLEncoder.encode(signedRequestXML, "UTF-8");
            serviceReturnObj.setRequestXML(signedRequestXML);
            String responseXML = "";
            try {
                String url = (esignType == eSign.eSignAPIVersion.V2) ? eSignSettings.getESIGNURLV2() : eSignSettings.getESIGNURL();
                responseXML = HttpsConnection.excutePostHttpsXml(url, URLEncodedsignedRequestXML, proxyIp, proxyPort, proxyreq, transactionID);
            } catch (Exception e) {
                LOGGER.log(java.util.logging.Level.WARNING, "Unable to call eSign URL for txn: " + transactionID, e);
                serviceReturnObj.setPreSignedTempFile(tempFilePath);
                serviceReturnObj.setRequestXML(signedRequestXML);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setErrorCode("ESS-103");
                serviceReturnObj.setStatus(0);
                serviceReturnObj.setErrorMessage("Unable to call eSign Url: " + e);
                return serviceReturnObj;
            }
            if (responseXML.isEmpty()) {
                serviceReturnObj.setPreSignedTempFile(tempFilePath);
                serviceReturnObj.setRequestXML(signedRequestXML);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setErrorCode("ESS-104");
                serviceReturnObj.setStatus(0);
                serviceReturnObj.setErrorMessage("empty response from eSign Url");
                return serviceReturnObj;
            }
            Document doc = eSignUtility.convertStringToDocument(responseXML);
            if (doc == null) {
                serviceReturnObj.setPreSignedTempFile(tempFilePath);
                serviceReturnObj.setRequestXML(signedRequestXML);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setErrorCode("ESS-104");
                serviceReturnObj.setErrorMessage("Unable to Parse response XMl document");
                serviceReturnObj.setStatus(0);
                return serviceReturnObj;
            }
            XPath xPath = XPathFactory.newInstance().newXPath();
            String status = eSignUtility.GetXpathValue(xPath, "/EsignResp/@status", doc);
            ArrayList<ReturnDocument> docsToReturn = new ArrayList<>();
            if (status.equals("0")) {
                String errormessage = eSignUtility.GetXpathValue(xPath, (esignType == eSign.eSignAPIVersion.V2) ? "/EsignResp/@errMsg" : "/EsignResp/@errorMessage", doc);
                String errorCode = eSignUtility.GetXpathValue(xPath, "/EsignResp/@errorCode", doc);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setPreSignedTempFile(tempFilePath);
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setErrorCode(errorCode);
                serviceReturnObj.setErrorMessage(errormessage);
                serviceReturnObj.setStatus(0);
                return serviceReturnObj;
            } else if (status.equals("2")) {
                String responseCode = eSignUtility.GetXpathValue(xPath, "/EsignResp/@resCode", doc);
                String gateWayParamter = transactionID + "|" + responseCode;
                gateWayParamter = org.emcastle.util.encoders.Base64.toBase64String(gateWayParamter.getBytes("utf-8"));
                serviceReturnObj.setRequestXML(signedRequestXML);
                serviceReturnObj.setPreSignedTempFile(tempFilePath);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setResponseCode(responseCode);
                serviceReturnObj.setReturnValues(returnDocuments);
                serviceReturnObj.setGatewayParameter(gateWayParamter);
                serviceReturnObj.setStatus(1);
                return serviceReturnObj;
            }
            serviceReturnObj.setRequestXML(signedRequestXML);
            serviceReturnObj.setResponseXML(responseXML);
            serviceReturnObj.setTransactionID(transactionID);
            serviceReturnObj.setStatus(0);
            serviceReturnObj.setReturnValues(docsToReturn);
            return serviceReturnObj;
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "Unexpected exception in getGatewayParameter for txn: " + transactionID, e);
            serviceReturnObj.setTransactionID(transactionID);
            serviceReturnObj.setErrorCode("ESS-999");
            serviceReturnObj.setStatus(0);
            serviceReturnObj.setErrorMessage(e.toString());
            return serviceReturnObj;
        }
    }

    private PublicKey loadPublicKeyFromConfig(EncryptedAadhaarConfig config) throws Exception {
        byte[] cerBytes;
        if (config.getCerFilePath() != null) {
            cerBytes = Files.readAllBytes(new File(config.getCerFilePath()).toPath());
        } else if (config.getCerBase64() != null) {
            cerBytes = Base64.decode(config.getCerBase64());
        } else {
            throw new IllegalArgumentException("EncryptedAadhaarConfig must specify either a CER file path or base64 certificate data.");
        }
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        java.security.cert.Certificate cert = cf.generateCertificate(new ByteArrayInputStream(cerBytes));
        PublicKey key = cert.getPublicKey();
        if (!"RSA".equals(key.getAlgorithm())) {
            throw new IllegalArgumentException("Aadhaar public key must be RSA; found: " + key.getAlgorithm());
        }
        return key;
    }

    private String encryptAadhaar(String aadhaarNumber, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(aadhaarNumber.getBytes("UTF-8"));
        return new String(Base64.encode(encryptedBytes), "UTF-8");
    }

    protected eSignServiceReturn getSigedDocument(String responseXML, String tempFilePath, int SignatureContents) {
        eSignServiceReturn serviceReturnObj = new eSignServiceReturn();
        int contentEstimated = 8192 * 2;
        try {
            if (responseXML.isEmpty()) {
                serviceReturnObj.setPreSignedTempFile(tempFilePath);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setErrorCode("ESS-104");
                serviceReturnObj.setStatus(0);
                serviceReturnObj.setErrorMessage("empty response xml");
                return serviceReturnObj;
            }
            Document doc = eSignUtility.convertStringToDocument(responseXML);
            if (doc == null) {
                serviceReturnObj.setPreSignedTempFile(tempFilePath);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setErrorCode("ESS-104");
                serviceReturnObj.setErrorMessage("Unable to Parse response XMl document");
                serviceReturnObj.setStatus(0);
                return serviceReturnObj;
            }

            File tempfile = new File(tempFilePath);
            if (!tempfile.exists()) {
                serviceReturnObj.setPreSignedTempFile(tempFilePath);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setErrorCode("ESS-108");
                serviceReturnObj.setErrorMessage("TempFile does not exist in path");
                serviceReturnObj.setStatus(0);
                return serviceReturnObj;
            }
            byte[] preSignedBytes = null;
            try {
                preSignedBytes = Files.readAllBytes(tempfile.toPath());
            } catch (Exception e) {
                LOGGER.log(java.util.logging.Level.WARNING, "Unable to read temp file: " + tempFilePath, e);
                serviceReturnObj.setPreSignedTempFile(tempFilePath);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setErrorCode("ESS-108");
                serviceReturnObj.setErrorMessage("Unable to read temp File");
                serviceReturnObj.setStatus(0);
                return serviceReturnObj;
            }
            XPath xPath = XPathFactory.newInstance().newXPath();
            String status = eSignUtility.GetXpathValue(xPath, "/EsignResp/@status", doc);
            String transactionID = eSignUtility.GetXpathValue(xPath, "/EsignResp/@txn", doc);
            serviceReturnObj.setTransactionID(transactionID);
            ArrayList<ReturnDocument> docsToReturn = new ArrayList<>();
            if (status.equals("0")) {
                String errormessage = eSignUtility.GetXpathValue(xPath, "/EsignResp/@errorMessage", doc);
                String errorCode = eSignUtility.GetXpathValue(xPath, "/EsignResp/@errorCode", doc);
                transactionID = eSignUtility.GetXpathValue(xPath, "/EsignResp/@txn", doc);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setPreSignedTempFile(tempFilePath);
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setErrorCode(errorCode);
                serviceReturnObj.setErrorMessage(errormessage);
                serviceReturnObj.setStatus(0);
                return serviceReturnObj;
            } else if (status.equals("1")) {
                String responseCode = eSignUtility.GetXpathValue(xPath, "/EsignResp/@resCode", doc);
                serviceReturnObj.setResponseCode(responseCode);
                ArrayList<ReturnDocument> returnDocuments = eSignUtility.getReturnDocumentsFromPreSignedPDFFile(preSignedBytes);
                NodeList signatureNodes = doc.getElementsByTagName("DocSignature");
                if (signatureNodes.getLength() <= 0) {
                    serviceReturnObj.setResponseXML(responseXML);
                    serviceReturnObj.setTransactionID(transactionID);
                    serviceReturnObj.setErrorCode("ESS-105");
                    serviceReturnObj.setErrorMessage("Signature element not found");
                    serviceReturnObj.setStatus(0);
                    return serviceReturnObj;
                }
                NodeList tempNodeList = doc.getElementsByTagName("Signatures");
                if (tempNodeList.getLength() <= 0) {
                    throw new IllegalArgumentException("No document signatures found in response xml");
                }
                if (tempNodeList.item(0) == null) {
                    throw new IllegalArgumentException("No document signatures found in response xml");
                }
                // Extract user X509 certificate from response for appearance patching
                String userX509CertBase64 = "";
                NodeList certNodes = doc.getElementsByTagName("UserX509Certificate");
                if (certNodes.getLength() > 0 && certNodes.item(0) != null) {
                    userX509CertBase64 = eSignUtility.getCharacterDataFromElement((Element) certNodes.item(0));
                }

                NodeList docSignatureNodes = tempNodeList.item(0).getChildNodes();
                for (int itrCount = 0; itrCount < docSignatureNodes.getLength(); itrCount++) {
                    Node signatureNode = signatureNodes.item(itrCount);
                    if (signatureNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    Element sigElement = (Element) signatureNode;
                    String docID = sigElement.getAttribute("id");
                    String errorCode = sigElement.getAttribute("errorCode");
                    String errorMessage = sigElement.getAttribute("errorMessage");
                    int docId = 0;
                    if (eSignUtility.tryParseInt(docID)) {
                        docId = Integer.parseInt(docID);
                    }
                    ReturnDocument returnDocument = eSignUtility.getReturnDocumentById(docId, returnDocuments);
                    if (returnDocument == null) {
                        docsToReturn.add(new ReturnDocument(0, "ESS-113", "Unable to get document from temp path", docId));
                        continue;
                    }
                    if (!eSignUtility.isNullOrWhitespace(returnDocument.getErrorMessage())) {
                        docsToReturn.add(returnDocument);
                        continue;
                    }
                    if (!(errorCode.isEmpty() && errorMessage.isEmpty())) {
                        returnDocument.setErrorMessage(errorMessage);
                        returnDocument.setErrorCode(errorCode);
                        returnDocument.setStatus(0);
                        docsToReturn.add(returnDocument);
                    } else {
                        String PKCS7ResponseBase64 = eSignUtility.getCharacterDataFromElement(sigElement);
                        try {
                            if (eSignUtility.isNullOrWhitespace(returnDocument.getPreSignedDocument())) {
                                returnDocument.setSignedData(PKCS7ResponseBase64);
                                returnDocument.setStatus(1);
                            } else {
                                byte[] array = signClose(PKCS7ResponseBase64, returnDocument.getPreSignedDocument(), SignatureContents);
                                if (returnDocument.isShowAadhaarOnSignature()) {
                                    array = patchSignatureAppearance(array, userX509CertBase64);
                                }
                                String pdfBase64 = java.util.Base64.getEncoder().encodeToString(array);
                                returnDocument.setSignedDocument(pdfBase64);
                                returnDocument.setStatus(1);
                            }
                            docsToReturn.add(returnDocument);
                        } catch (Exception e) {
                            LOGGER.log(java.util.logging.Level.WARNING, "Unable to append signature to document id: " + docId, e);
                            docsToReturn.add(new ReturnDocument(0, "ESS-112", "Unable to get Append signature to document", docId));
                            continue;
                        }
                    }
                }
                serviceReturnObj.setPreSignedTempFile(tempFilePath);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setTransactionID(transactionID);
                serviceReturnObj.setReturnValues(docsToReturn);
                serviceReturnObj.setResponseCode(responseCode);
                serviceReturnObj.setStatus(1);
                return serviceReturnObj;
            }
            serviceReturnObj.setErrorCode("ESS-999");
            serviceReturnObj.setStatus(0);
            serviceReturnObj.setErrorMessage("Unknown error invalid status in xml");
            return serviceReturnObj;
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "Unexpected exception in getSigedDocument", e);
            serviceReturnObj.setErrorCode("ESS-999");
            serviceReturnObj.setStatus(0);
            serviceReturnObj.setErrorMessage(e.toString());
            return serviceReturnObj;
        }
    }

    private byte[] signClose(String pkcs7, String preSignedValue, int SignatureContents) throws Exception {
        try {
            byte[] preSignedBytes = org.emcastle.util.encoders.Base64.decode(preSignedValue);
            String preSignedDoc = new String(preSignedBytes, StandardCharsets.UTF_8);
            ByteArrayOutputStream originalout = new ByteArrayOutputStream();
            String[] Doc = preSignedDoc.split("\\|");
            byte[] sigbytes = org.emcastle.util.encoders.Base64.decode(pkcs7);
            byte[] paddedSig;
            if (SignatureContents != 0) {
                paddedSig = new byte[SignatureContents];
            } else {
                paddedSig = new byte[21000];
            }

            System.arraycopy(sigbytes, 0, paddedSig, 0, sigbytes.length);
            PdfDictionary dic2 = new PdfDictionary();
            dic2.put(PdfName.CONTENTS, new PdfString(paddedSig).setHexWriting(true));

            byte[] bout = org.emcastle.util.encoders.Base64.decode(Doc[2]);
            int boutLen = Integer.parseInt(Doc[1]);
            int position = Integer.parseInt(Doc[0]);
            //Calculate exclusionLocations
            HashMap<PdfName, Integer> exclusionSizes = new HashMap<PdfName, Integer>();
            if (SignatureContents != 0) {
                exclusionSizes.put(PdfName.CONTENTS, SignatureContents * 2 + 2);
            } else {
                exclusionSizes.put(PdfName.CONTENTS, 21000 * 2 + 2);
            }

            HashMap<PdfName, PdfLiteral> exclusionLocations = new HashMap<PdfName, PdfLiteral>();
            PdfLiteral litEL = new PdfLiteral(80);
            exclusionLocations.put(PdfName.BYTERANGE, litEL);
            for (Map.Entry<PdfName, Integer> entry : exclusionSizes.entrySet()) {
                PdfName key = entry.getKey();
                int v = entry.getValue();
                litEL = new PdfLiteral(v);
                exclusionLocations.put(key, litEL);
            }
            ((PdfLiteral) exclusionLocations.get(PdfName.CONTENTS)).setPosition(position);
            exclusionLocations.remove(PdfName.BYTERANGE);
            ByteBuffer bf = new ByteBuffer();
            for (PdfName key : dic2.getKeys()) {
                PdfObject obj = dic2.get(key);
                PdfLiteral lit = exclusionLocations.get(key);
                if (lit == null) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.key.1.is.too.big.is.2.reserved.3", key.toString(), String.valueOf(bf.size()), String.valueOf(lit.getPosLength())));
                }
                bf.reset();
                obj.toPdf(null, bf);
                if (bf.size() > lit.getPosLength()) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.key.1.is.too.big.is.2.reserved.3", key.toString(), String.valueOf(bf.size()), String.valueOf(lit.getPosLength())));
                }
                System.arraycopy(bf.getBuffer(), 0, bout, (int) lit.getPosition(), bf.size());
            }
            if (dic2.size() != exclusionLocations.size()) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.update.dictionary.has.less.keys.than.required"));
            }
            originalout.write(bout, 0, boutLen);

            return originalout.toByteArray();
        } catch (IOException | IllegalArgumentException e) {
            throw e;
        }
    }

    /**
     * Patches the visual appearance of every signature field in a signed PDF
     * so that it displays signer name and masked Aadhaar number, equivalent to
     * the C# PatchSignatureAppearance method.
     *
     * @param signedPdfBytes     bytes of the fully-signed PDF
     * @param userX509CertBase64 Base64-encoded DER X.509 certificate returned
     *                           by the eSign gateway in UserX509Certificate
     * @return patched PDF bytes (or the original bytes if anything fails)
     */
    private static byte[] patchSignatureAppearance(byte[] signedPdfBytes, String userX509CertBase64) {
        try {
            if (userX509CertBase64 == null || userX509CertBase64.trim().isEmpty())
                return signedPdfBytes;

            // Parse the signer certificate to extract CN and masked Aadhaar (Title OID)
            byte[] certBytes = org.emcastle.util.encoders.Base64.decode(userX509CertBase64.trim());
            X509CertificateStructure cert = X509CertificateStructure.getInstance(
                    org.emcastle.asn1.ASN1Primitive.fromByteArray(certBytes));
            X500Name subject = cert.getSubject();

            String certName = "Unknown";
            RDN[] cnRDNs = subject.getRDNs(BCStyle.CN);
            if (cnRDNs != null && cnRDNs.length > 0) {
                certName = IETFUtils.valueToString(cnRDNs[0].getFirst().getValue());
            }

            // OID 2.5.4.12 (Title / T) — eMudhra stores the masked Aadhaar here
            String aadhaarLast4 = "XXXX";
            try {
                RDN[] titleRDNs = subject.getRDNs(BCStyle.T);
                if (titleRDNs != null && titleRDNs.length > 0) {
                    String val = IETFUtils.valueToString(titleRDNs[0].getFirst().getValue());
                    if (val != null && val.length() >= 4) {
                        aadhaarLast4 = val.substring(val.length() - 4);
                    }
                }
            } catch (Exception ignored) { }

            ByteArrayInputStream inputMs = new ByteArrayInputStream(signedPdfBytes);
            ByteArrayOutputStream outputMs = new ByteArrayOutputStream();

            PdfReader reader = new PdfReader(inputMs);
            PdfStamper stamper = new PdfStamper(reader, outputMs, '\0', true); // append mode

            AcroFields acroFields = reader.getAcroFields();
            ArrayList<String> sigNames = acroFields.getSignatureNames();
            if (sigNames.isEmpty()) {
                stamper.close();
                reader.close();
                return signedPdfBytes;
            }

            // Build a shared font resource dictionary (Helvetica Type1, /F1)
            PdfDictionary fontObj = new PdfDictionary();
            fontObj.put(PdfName.TYPE, PdfName.FONT);
            fontObj.put(PdfName.SUBTYPE, new PdfName("Type1"));
            fontObj.put(PdfName.BASEFONT, new PdfName("Helvetica"));
            fontObj.put(new PdfName("Encoding"), new PdfName("WinAnsiEncoding"));
            PdfIndirectObject fontRef = stamper.getWriter().addToBody(fontObj);

            PdfDictionary fontResources = new PdfDictionary();
            fontResources.put(new PdfName("F1"), fontRef.getIndirectReference());
            PdfDictionary resDict = new PdfDictionary();
            resDict.put(PdfName.FONT, fontResources);

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

            for (String sigFieldName : sigNames) {
                AcroFields.Item item = acroFields.getFieldItem(sigFieldName);
                if (item == null) continue;
                PdfDictionary widget = item.getWidget(0);
                if (widget == null) continue;
                Rectangle rect = PdfReader.getNormalizedRectangle(widget.getAsArray(PdfName.RECT));

                // Read date / reason / location from the embedded signature dictionary
                String signDate = sdf.format(new Date());
                String reason = "";
                String location = "";
                try {
                    PdfDictionary sigDict = (PdfDictionary) PdfReader.getPdfObject(widget.get(PdfName.V));
                    if (sigDict != null) {
                        PdfString dateStr = sigDict.getAsString(PdfName.M);
                        if (dateStr != null) {
                            try {
                                Calendar cal = PdfDate.decode(dateStr.toString());
                                if (cal != null) signDate = sdf.format(cal.getTime());
                            } catch (Exception ignored) { }
                        }
                        PdfString rs = sigDict.getAsString(PdfName.REASON);
                        PdfString ls = sigDict.getAsString(PdfName.LOCATION);
                        if (rs != null) reason = rs.toUnicodeString();
                        if (ls != null) location = ls.toUnicodeString();
                    }
                } catch (Exception ignored) { }

                // Build text lines for the appearance
                List<String> lines = new ArrayList<>();
                lines.add("Digitally Signed by");
                lines.add("Name : " + certName);
                lines.add("Aadhaar No : **** **** " + aadhaarLast4);
                if (reason != null && !reason.trim().isEmpty())
                    lines.add("Reason: " + reason);
                lines.add("Date : " + signDate);

                // Auto-fit font size so all lines stay within the signature box
                final float leftMargin = 4f;
                final float rightMargin = 4f;
                final float topMargin = 3f;
                final float bottomMargin = 3f;
                float availableWidth = rect.getWidth() - leftMargin - rightMargin;
                float availableHeight = rect.getHeight() - topMargin - bottomMargin;

                // Start from height-based maximum
                float fontSize = (lines.isEmpty()) ? 8f : availableHeight / lines.size();

                // Shrink further if any line is wider than the box using BaseFont metrics
                try {
                    BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                    for (String line : lines) {
                        float lineWidth = bf.getWidthPoint(line, fontSize);
                        if (lineWidth > availableWidth && lineWidth > 0) {
                            float scaled = fontSize * availableWidth / lineWidth;
                            if (scaled < fontSize) fontSize = scaled;
                        }
                    }
                } catch (Exception ignored) { }

                // Clamp to a readable range
                fontSize = Math.max(4f, Math.min(fontSize, 10f));
                float leading = fontSize;
                float startY = rect.getHeight() - topMargin - fontSize;

                StringBuilder cs = new StringBuilder();
                cs.append("BT\n");
                cs.append(String.format(java.util.Locale.US, "/F1 %.2f Tf\n", fontSize));
                cs.append("/DeviceRGB cs\n0 0 0 sc\n");
                cs.append(String.format(java.util.Locale.US, "%.2f %.2f Td\n", leftMargin, startY));
                cs.append(String.format(java.util.Locale.US, "%.2f TL\n", leading));
                for (int li = 0; li < lines.size(); li++) {
                    String escaped = lines.get(li)
                            .replace("\\", "\\\\")
                            .replace("(", "\\(")
                            .replace(")", "\\)");
                    if (li < lines.size() - 1) {
                        cs.append("(").append(escaped).append(") Tj T*\n");
                    } else {
                        cs.append("(").append(escaped).append(") Tj\n");
                    }
                }
                cs.append("ET");

                byte[] streamBytes = cs.toString().getBytes(java.nio.charset.Charset.forName("windows-1252"));

                // Create a Form XObject containing the text content stream
                PRStream apStream = new PRStream(reader, streamBytes);
                apStream.put(PdfName.TYPE, PdfName.XOBJECT);
                apStream.put(PdfName.SUBTYPE, PdfName.FORM);
                apStream.put(PdfName.BBOX, new PdfArray(new float[]{0f, 0f, rect.getWidth(), rect.getHeight()}));
                apStream.put(PdfName.RESOURCES, resDict);
                PdfIndirectObject apRef = stamper.getWriter().addToBody(apStream);

                // Point the widget's appearance /AP /N at the new Form XObject
                PdfDictionary newAp = new PdfDictionary();
                newAp.put(PdfName.N, apRef.getIndirectReference());
                widget.put(PdfName.AP, newAp);

                // Mark widget as modified so it is written in the incremental revision
                stamper.markUsed(widget);
            }

            stamper.close();
            reader.close();

            return outputMs.toByteArray();
        } catch (Exception e) {
            return signedPdfBytes;
        }
    }

    protected eSignServiceReturn getStatus(String transactionId) {
        eSignServiceReturn serviceReturnObj = new eSignServiceReturn();
        try {
            if (eSignUtility.isNullOrWhitespace(transactionId)) {
                serviceReturnObj.setErrorCode("ESS-105");
                serviceReturnObj.setStatus(0);
                serviceReturnObj.setErrorMessage("transaction ID is required");
                return serviceReturnObj;
            }
            SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            TimeZone timeZone = TimeZone.getTimeZone("IST");
            tsFormat.setTimeZone(timeZone);
            Date now = new Date(System.currentTimeMillis());
            String timeStamp = tsFormat.format(now);
            String requestXML = eSignUtility.checkeSignStatus(timeStamp, transactionId, eSignSettings.getASPID());
            String signedRequestXML = eSignUtility.signXML(requestXML, pfxpath, password, pfxAlias);
            String URLEncodedsignedRequestXML = URLEncoder.encode(signedRequestXML, "UTF-8");
            serviceReturnObj.setRequestXML(signedRequestXML);
            String responseXML = "";
            try {
                responseXML = HttpsConnection.excutePostHttpsXml(eSignSettings.getESIGNStatusURL(), URLEncodedsignedRequestXML, proxyIp, proxyPort, proxyreq, transactionId);
            } catch (Exception e) {
                LOGGER.log(java.util.logging.Level.WARNING, "Unable to call eSign status URL for txn: " + transactionId, e);
                serviceReturnObj.setRequestXML(signedRequestXML);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setErrorCode("ESS-103");
                serviceReturnObj.setStatus(0);
                serviceReturnObj.setErrorMessage("Unable to call eSign Url" + e.getMessage());
                return serviceReturnObj;
            }
            if (responseXML.isEmpty()) {
                serviceReturnObj.setRequestXML(signedRequestXML);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setTransactionID(transactionId);
                serviceReturnObj.setErrorCode("ESS-104");
                serviceReturnObj.setStatus(0);
                serviceReturnObj.setErrorMessage("empty response from eSign Url");
                return serviceReturnObj;
            }
            Document doc = eSignUtility.convertStringToDocument(responseXML);
            if (doc == null) {
                serviceReturnObj.setRequestXML(signedRequestXML);
                serviceReturnObj.setResponseXML(responseXML);
                serviceReturnObj.setTransactionID(transactionId);
                serviceReturnObj.setErrorCode("ESS-104");
                serviceReturnObj.setErrorMessage("Unable to Parse response XMl document");
                serviceReturnObj.setStatus(0);
                return serviceReturnObj;
            }
            XPath xPath = XPathFactory.newInstance().newXPath();
            String status = eSignUtility.GetXpathValue(xPath, "/EsignResp/@status", doc);
            switch (status) {
                case "0":
                    String errormessage = eSignUtility.GetXpathValue(xPath, "/EsignResp/@errorMessage", doc);
                    String errorCode = eSignUtility.GetXpathValue(xPath, "/EsignResp/@errorCode", doc);
                    serviceReturnObj.setResponseXML(responseXML);
                    serviceReturnObj.setTransactionID(transactionId);
                    serviceReturnObj.setErrorCode(errorCode);
                    serviceReturnObj.setErrorMessage(errormessage);
                    serviceReturnObj.setStatus(0);
                    return serviceReturnObj;
                case "2":
                    serviceReturnObj.setRequestXML(signedRequestXML);
                    serviceReturnObj.setResponseXML(responseXML);
                    serviceReturnObj.setStatus(2);
                    return serviceReturnObj;
                default:
                    serviceReturnObj.setRequestXML(signedRequestXML);
                    serviceReturnObj.setResponseXML(responseXML);
                    serviceReturnObj.setStatus(1);
                    return serviceReturnObj;
            }
        } catch (Exception e) {
            serviceReturnObj.setErrorCode("ESS-999");
            serviceReturnObj.setStatus(0);
            serviceReturnObj.setErrorMessage(e.getMessage());
            return serviceReturnObj;
        }
    }

    private static String reformatPagelevelCoordinates(String pageLevelCoordinates, int totalPages) {
        String[] plArray = pageLevelCoordinates.split(";");
        ArrayList<String> newPageLevel = new ArrayList<>();
        for (String pl : plArray) {
            if (pl.isEmpty()) {
                continue;
            }
            String pageNumber = pl.split("-")[0];
            String coordinates = pl.split("-")[1];
            switch (pageNumber.trim().toLowerCase()) {
                case "l": {
                    newPageLevel.add(totalPages + "-" + coordinates);
                    break;
                }
                case "all": {
                    for (int i = 1; i <= totalPages; i++) {
                        newPageLevel.add(i + "-" + coordinates);
                    }
                    break;
                }
                case "sl": {
                    newPageLevel.add((totalPages - 1) + "-" + coordinates);
                    break;
                }
                case "f": {
                    newPageLevel.add("1-" + coordinates);
                    break;
                }
                case "s": {
                    newPageLevel.add("2-" + coordinates);
                    break;
                }
                default:
                    newPageLevel.add(pl);
                    break;
            }
        }
        return String.join(";", newPageLevel);
    }

    protected eSignServiceReturn isValidPdf(String docBase64) {
        eSignServiceReturn resp = new eSignServiceReturn();
        resp.setStatus(0);
        try ( ByteArrayOutputStream fos = new ByteArrayOutputStream()) {
            byte[] decodePDF = esign.text.pdf.codec.Base64.decode(docBase64);
            PdfReader reader = new PdfReader(decodePDF);
            PdfStamper stamper = null;

            if (reader.isRebuilt()) {
                reader.enableRebuild();
                ByteArrayOutputStream fos1 = new ByteArrayOutputStream();
                stamper = new PdfStamper(reader, fos1);
                stamper.close();
                reader = new PdfReader(fos1.toByteArray());
            }
            stamper = PdfStamper.createSignature(reader, fos, '\0', null, true);
            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
            appearance.setReason("test");
            appearance.setLocation("test");
            StringBuilder sb = new StringBuilder();
            sb.append("test");
            sb.append("\n");
            appearance.setLayer2Text(sb.toString());
            appearance.setAcro6Layers(false);
            appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
            int[] pages = null;
            ArrayList<Integer> ar;
            String coord = null;
            Rectangle rect;
            List<Rectangle> rList = new ArrayList<>();

            String pageLevelCoordinates = "1-425,100,545,160";
            pageLevelCoordinates = reformatPagelevelCoordinates(pageLevelCoordinates, reader.getNumberOfPages());
            String[] pl = pageLevelCoordinates.split(";");
            pages = new int[pl.length];
            int y = 0;
            for (String pl1 : pl) {
                if ("".equals(pl1.trim())) {
                    continue;
                }

                if (!pl1.contains("-")) {
                    pl1 = y + "-" + pl1;
                }
                String[] newpages = pl1.split("-");
                String[] numbers = newpages[1].split(",");
                float x11;
                float y1;
                float x2;
                float y2;
                try {
                    x11 = Float.valueOf(numbers[0]);
                    y1 = Float.valueOf(numbers[1]);
                    x2 = Float.valueOf(numbers[2]);
                    y2 = Float.valueOf(numbers[3]);
                } catch (NumberFormatException ex) {
                    LOGGER.warning(ex.getLocalizedMessage());
                    LOGGER.info("Entered into default coordinates - bottom,right");
                    x11 = 425;
                    y1 = 100;
                    x2 = 555;
                    y2 = 160;
                }
                pages[y] = Integer.parseInt(newpages[0]);
                rect = new Rectangle(x11, y1, x2, y2);
                rList.add(rect);
                y++;
                appearance.setVisibleSignature(rect, pages, null, rList);
            }
            PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, PdfName.ADBE_PKCS7_DETACHED);
            dic.setDate(new PdfDate(appearance.getSignDate()));
            dic.setSignatureCreator("eMudhra");
            if (appearance.getReason() != null) {
                dic.setReason(appearance.getReason());
            }
            if (appearance.getLocation() != null) {
                dic.setLocation(appearance.getLocation());
            }
            appearance.setCryptoDictionary(dic);
            HashMap<PdfName, Integer> exc = new HashMap<>();
            exc.put(PdfName.CONTENTS, 4096 * 2 + 2);
            appearance.preClose(exc);

            InputStream is1 = appearance.getRangeStream();
            byte[] data = IOUtils.toByteArray(is1);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(data);
            byte[] hash = digest.digest();

            resp.setStatus(1);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "Exception in isValidPdf", e);
            resp.setErrorMessage("Something went wrong : " + e.getMessage());
        }
        return resp;
    }

    protected eSignServiceReturn performBankKYC(String transactionID, String IFSCCode, String bankName, String accountNumber, UserInfo userInfo, String BankKYCURL) {
        eSignServiceReturn serviceReturnObj = new eSignServiceReturn();
        try {
            if (eSignUtility.isNullOrWhitespace(transactionID)) {
                transactionID = UUID.randomUUID().toString().replace("-", "");
            }
            SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            TimeZone timeZone = TimeZone.getTimeZone("IST");
            tsFormat.setTimeZone(timeZone);
            Date now = new Date(System.currentTimeMillis() + 3 * 60 * 1000);
            String timeStamp = tsFormat.format(now);
            serviceReturnObj.setTransactionID(transactionID);
            String requestXML = eSignUtility.generateBankKYCXML(timeStamp, transactionID, IFSCCode, bankName, accountNumber, userInfo);
            String signedRequestXML = eSignUtility.signXML(requestXML, pfxpath, password, pfxAlias);
            String URLEncodedsignedRequestXML = URLEncoder.encode(signedRequestXML, "UTF-8");
            serviceReturnObj.setRequestXML(signedRequestXML);
            String responseXML = "";
            try {
                responseXML = HttpsConnection.excutePostHttpsXml(BankKYCURL, URLEncodedsignedRequestXML, proxyIp, proxyPort, proxyreq, transactionID);
            } catch (Exception e) {
                LOGGER.log(java.util.logging.Level.WARNING, "Unable to call BankKYC URL for txn: " + transactionID, e);
                serviceReturnObj.setErrorCode("ESS-103");
                serviceReturnObj.setErrorMessage("Unable to call eSign Url");
                return serviceReturnObj;
            }
            if (responseXML.isEmpty()) {
                serviceReturnObj.setErrorCode("ESS-104");
                serviceReturnObj.setErrorMessage("empty response from eSign Url");
                return serviceReturnObj;
            }
            serviceReturnObj.setResponseXML(responseXML);

            Document doc = eSignUtility.convertStringToDocument(responseXML);
            if (doc == null) {
                serviceReturnObj.setErrorCode("ESS-104");
                serviceReturnObj.setErrorMessage("Unable to Parse response XMl document");
                return serviceReturnObj;
            }
            XPath xPath = XPathFactory.newInstance().newXPath();
            String status = eSignUtility.GetXpathValue(xPath, "/BankKYCResp/@status", doc);
            if (status.equals("0")) {
                String errormessage = eSignUtility.GetXpathValue(xPath, "/BankKYCResp/@error", doc);
                String errorCode = eSignUtility.GetXpathValue(xPath, "/BankKYCResp/@resCode", doc);
                serviceReturnObj.setErrorCode(errorCode);
                serviceReturnObj.setErrorMessage(errormessage);
                serviceReturnObj.setStatus(0);
                return serviceReturnObj;
            } else if (status.equals("1")) {
                String responseCode = eSignUtility.GetXpathValue(xPath, "/BankKYCResp/@resCode", doc);
                serviceReturnObj.setResponseCode(responseCode);
                serviceReturnObj.setStatus(1);
            }
            return serviceReturnObj;
        } catch (Exception e) {
            serviceReturnObj.setErrorCode("ESS-999");
            serviceReturnObj.setErrorMessage(e.getMessage());
            return serviceReturnObj;
        }
    }

}
