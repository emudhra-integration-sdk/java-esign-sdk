/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.emcastle.jce.provider.emCastleProvider;
import org.emcastle.util.encoders.Hex;

/**
 *
 * @author 20730
 * @developer
 */
public class eSign {

    private final Logger logger;
    private final String pfxpath;
    private final String password;
    private final String pfxAlias;
    private final boolean proxyreq;
    private final String proxyIp;
    private final int proxyPort;
    private final int SignatureContents;

    public enum Coordinates {
        TopLeft,
        TopMiddle,
        TopRight,
        CenterLeft,
        CenterMiddle,
        CenterRight,
        BottomLeft,
        BottomMiddle,
        BottomRight
    }

    public enum AppreanceRunDirection {
        RUN_DIRECTION_LTR,
        RUN_DIRECTION_RTL
    }

    public enum PageTobeSigned {
        All,
        Even,
        Odd,
        Last,
        First,
        PageLevel,
        Specify
    }

    public enum eSignAPIVersion {
        V2, V3
    }

    public enum AppearanceType {
        StandardSignature,
        SignatureImage,
        OneLiner,
        advanceSignature,
        ColoredGraphic,
        BackgroundImage
    }

    public enum AuthMode {
        OTP("1"), FingerPrint("2"), IRIS("3"), FaceRecognition("4");
        private String val;

        AuthMode(String val) {
            this.val = val;
        }

        public String getVal() {
            return val;
        }

    }

    public enum InputType {
        PDF,
        HASH
    }

    public eSign(String ASPID, String eSignURL, String eSignURLV2, String pfxpath, String password, String pfxAlias) throws NoSuchAlgorithmException {
        this(ASPID, eSignURL, eSignURLV2, pfxpath, password, pfxAlias, false, "", 0, 0, eSignSettings.LogType.AllLog, null, null, null, 0);
    }

    public eSign(String ASPID, String eSignURL, String eSignURLV2, String pfxpath, String password, String pfxAlias, int SignatureContents) throws NoSuchAlgorithmException {
        this(ASPID, eSignURL, eSignURLV2, pfxpath, password, pfxAlias, false, "", 0, 0, eSignSettings.LogType.AllLog, null, null, null, SignatureContents);
    }

    public eSign(String ASPID, String eSignURL, String eSignURLV2, String pfxpath, String password, String pfxAlias, boolean proxyreq, String proxyIp, int proxyPort, int SignatureContents) throws NoSuchAlgorithmException {
        this(ASPID, eSignURL, eSignURLV2, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, 0, eSignSettings.LogType.AllLog, null, null, null, SignatureContents);
    }

    public eSign(String ASPID, String eSignURL, String eSignURLV2, String pfxpath, String password, String pfxAlias, boolean proxyreq, String proxyIp, int proxyPort, int sessionTimeout, int SignatureContents) throws NoSuchAlgorithmException {
        this(ASPID, eSignURL, eSignURLV2, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, sessionTimeout, eSignSettings.LogType.AllLog, null, null, null, SignatureContents);
    }

    public eSign(String ASPID, String eSignURL, String eSignURLV2, String pfxpath, String password, String pfxAlias, boolean proxyreq,
            String proxyIp, int proxyPort, int sessionTimeout, eSignSettings.LogType logType, int SignatureContents) throws NoSuchAlgorithmException {
        this(ASPID, eSignURL, eSignURLV2, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, sessionTimeout, logType, null, null, null, SignatureContents);
    }

    public eSign(String ASPID, String eSignURL, String eSignURLV2, String pfxpath, String password, String pfxAlias, String pdfViewerLicence, int SignatureContents) throws NoSuchAlgorithmException {
        this(ASPID, eSignURL, eSignURLV2, pfxpath, password, pfxAlias, false, "", 0, 0, eSignSettings.LogType.AllLog, null, null, pdfViewerLicence, SignatureContents);
    }

    public eSign(String ASPID, String eSignURL, String eSignURLV2, String pfxpath, String password, String pfxAlias, boolean proxyreq, String proxyIp, int proxyPort, String pdfViewerLicence, int SignatureContents) throws NoSuchAlgorithmException {
        this(ASPID, eSignURL, eSignURLV2, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, 0, eSignSettings.LogType.AllLog, null, null, pdfViewerLicence, SignatureContents);
    }

    public eSign(String ASPID, String eSignURL, String eSignURLV2, String pfxpath, String password, String pfxAlias, boolean proxyreq, String proxyIp, int proxyPort, int sessionTimeout, String pdfViewerLicence, int SignatureContents) throws NoSuchAlgorithmException {
        this(ASPID, eSignURL, eSignURLV2, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, sessionTimeout, eSignSettings.LogType.AllLog, null, null, pdfViewerLicence, SignatureContents);
    }

    public eSign(String ASPID, String eSignURL, String eSignURLV2, String pfxpath, String password, String pfxAlias, boolean proxyreq, String proxyIp, int proxyPort, int sessionTimeout, eSignSettings.LogType logType, String pdfViewerLicence, int SignatureContents) throws NoSuchAlgorithmException {
        this(ASPID, eSignURL, eSignURLV2, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, sessionTimeout, logType, null, null, pdfViewerLicence, SignatureContents);
    }

    public eSign(String ASPID, String eSignURL, String eSignURLV2, String pfxpath, String password, String pfxAlias, boolean proxyreq,
            String proxyIp, int proxyPort, int sessionTimeout, eSignSettings.LogType logType, String ProxyUserID, String ProxyUserPassword, String pdfViewerLicence, int SignatureContents) throws NoSuchAlgorithmException {
        eSignSettings.setLogType(logType);   // persist so initFileHandler uses the right level
        this.logger = EsignLoggerFactory.getLogger(eSign.class, null, logType);
        Security.addProvider(new emCastleProvider());
        this.pfxpath = pfxpath;
        this.password = password;
        this.proxyreq = proxyreq;
        this.proxyIp = proxyIp;
        this.proxyPort = proxyPort;
        this.pfxAlias = pfxAlias;
        this.SignatureContents = SignatureContents;
        eSignSettings.setSessionTimeout(sessionTimeout);
        eSignSettings.setProxyUserID(ProxyUserID);
        eSignSettings.setProxyUserPassword(ProxyUserPassword);
        eSignSettings.setASPID(ASPID);
        eSignSettings.setESIGNURL(eSignURL);
        eSignSettings.setESIGNURLV2(eSignURLV2);

        // The path-encryption key derives from the ASPID alone. Set it always,
        // so getEncryptedPath() and the PDF viewer kit work without a separate
        // viewer licence file.
        eSignSettings.setEncryptionKey(getSha256("PDF_VIEWER_KIT_101" + ASPID));

        if (pdfViewerLicence != null) {
            // Backward compatibility: older emPDFViewer kits gate on
            // ValidateKitLicence.IsValidLicence(), populated only by
            // validateKitLicence(file). Keep honoring a supplied licence file.
            // Reflection, because the class ships as a compiled artifact and
            // must not be a compile-time dependency.
            try {
                java.lang.reflect.Method validate = Class
                        .forName("com.emudhra.esign.ValidateKitLicence")
                        .getDeclaredMethod("validateKitLicence", String.class);
                validate.setAccessible(true);
                validate.invoke(null, pdfViewerLicence);
            } catch (Throwable t) {
                logger.warning("PDF viewer kit licence validation skipped: " + t);
            }
        }
    }

    @Deprecated
    public eSignServiceReturn getGatewayParameter(ArrayList<eSignInput> inputs, String signerID, String transactionID, String responseUrl, String redirectUrl, String tempFolder) {
        eSignImplimentation impl = new eSignImplimentation(pfxpath, password, pfxAlias, proxyIp, proxyPort, proxyreq);
        return impl.getGatewayParameter(inputs, signerID, transactionID, responseUrl, redirectUrl, tempFolder, SignatureContents);
    }

    public eSignServiceReturn getGatewayParameter(ArrayList<eSignInput> inputs, String signerID, String transactionID, String responseUrl, String redirectUrl, String tempFolder, eSign.eSignAPIVersion eSignType, eSign.AuthMode authMode, int maxWaitPeriod) {
        eSignImplimentation impl = new eSignImplimentation(pfxpath, password, pfxAlias, proxyIp, proxyPort, proxyreq);
        return impl.getGatewayParameter(inputs, signerID, transactionID, responseUrl, redirectUrl, tempFolder, eSignType, authMode, maxWaitPeriod, true, SignatureContents);
    }

    public eSignServiceReturn getGatewayParameter(ArrayList<eSignInput> inputs, String signerID, String transactionID, String responseUrl, String redirectUrl, String tempFolder, eSign.eSignAPIVersion eSignType, eSign.AuthMode authMode) {
        eSignImplimentation impl = new eSignImplimentation(pfxpath, password, pfxAlias, proxyIp, proxyPort, proxyreq);
        return impl.getGatewayParameter(inputs, signerID, transactionID, responseUrl, redirectUrl, tempFolder, eSignType, authMode, 1440, true, SignatureContents);
    }

    public eSignServiceReturn performBankKYC(String transactionID, String IFSCCode, String bankName, String accountNumber, UserInfo userInfo, String BankKYCURL) {
        eSignImplimentation impl = new eSignImplimentation(pfxpath, password, pfxAlias, proxyIp, proxyPort, proxyreq);
        return impl.performBankKYC(transactionID, IFSCCode, bankName, accountNumber, userInfo, BankKYCURL);
    }

    public eSignServiceReturn getSigedDocument(String eSignResponse, String preSignedTempFile) {
        return getSigedDocument(eSignResponse, preSignedTempFile, null);
    }

    /**
     * Phase 2 with a caller-supplied visible signature appearance.
     *
     * <p>The appearance controls the header line, which of name / Aadhaar /
     * reason / location / date are drawn, their labels, and the block's layout.
     * Pass {@code null} for the default appearance.
     *
     * <p>Supplying an appearance is enough to trigger appearance patching —
     * {@code showAadhaarOnSignature} need not be set on the input.
     *
     * <p>On success {@link eSignServiceReturn#getSignerCertificateInfo()} carries
     * the details of the signer certificate returned by the gateway.
     */
    public eSignServiceReturn getSigedDocument(String eSignResponse, String preSignedTempFile, AadhaarSignatureAppearance appearance) {
        eSignImplimentation impl = new eSignImplimentation(pfxpath, password, pfxAlias, proxyIp, proxyPort, proxyreq);
        return impl.getSigedDocument(eSignResponse, preSignedTempFile, SignatureContents, appearance);
    }

    public eSignServiceReturn getEncryptedPath(String path) {
        eSignImplimentation impl = new eSignImplimentation(pfxpath, password, pfxAlias, proxyIp, proxyPort, proxyreq);
        return impl.getEncryptedPath(path);
    }

    public eSignServiceReturn getStatus(String transactionId) {
        eSignImplimentation impl = new eSignImplimentation(pfxpath, password, pfxAlias, proxyIp, proxyPort, proxyreq);
        return impl.getStatus(transactionId);
    }

    public eSignServiceReturn isValidPdf(String docBase64) {
        eSignImplimentation impl = new eSignImplimentation(pfxpath, password, pfxAlias, proxyIp, proxyPort, proxyreq);
        return impl.isValidPdf(docBase64);
    }

    private static String getSha256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return Hex.toHexString(hash);
    }

    /**
     * Verifies the eSign signature over data you hashed yourself, and checks the signer
     * certificate for revocation via OCSP, falling back to CRL.
     *
     * @param sha256Hex      the SHA-256 hex that was sent for signing: your own digest in hash
     *                       mode, or ReturnDocument.getDocumentHash() for a PDF signing.
     * @param esignRespXml   the gateway's EsignResp XML (the same string passed to
     *                       getSigedDocument), or just the Base64 PKCS#7 from
     *                       ReturnDocument.getSignedData() / a DocSignature element.
     * @param checkRevocation whether to query OCSP (then CRL). Adds a network round trip;
     *                       the eMudhra UAT responder takes roughly 20 seconds.
     * @return the outcome; never throws. isValid() is the single success flag, and
     *         getErrorMessage() says why when it is false. When the response XML carries
     *         several documents, the DocSignature matching sha256Hex is the one verified.
     */
    public eSignVerificationResult verifyEsignResponseHash(String sha256Hex, String esignRespXml, boolean checkRevocation) {
        return eSignVerifier.verifyResponseHash(sha256Hex, esignRespXml, checkRevocation);
    }
}
