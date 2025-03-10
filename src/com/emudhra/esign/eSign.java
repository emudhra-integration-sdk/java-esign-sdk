/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import java.security.Security;
import java.util.ArrayList;
import java.util.logging.Logger;
//import org.apache.logging.log4j.core.Logger;
import org.emcastle.jce.provider.emCastleProvider;

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

    public eSign(String licenceFilePath, String pfxpath, String password, String pfxAlias) {
        this(licenceFilePath, pfxpath, password, pfxAlias, false, "", 0, 0, eSignSettings.LogType.AllLog, null, null, null, 0);
    }

    public eSign(String licenceFilePath, String pfxpath, String password, String pfxAlias, int SignatureContents) {
        this(licenceFilePath, pfxpath, password, pfxAlias, false, "", 0, 0, eSignSettings.LogType.AllLog, null, null, null, SignatureContents);
    }

    public eSign(String licenceFilePath, String pfxpath, String password, String pfxAlias, boolean proxyreq, String proxyIp, int proxyPort, int SignatureContents) {
        this(licenceFilePath, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, 0, eSignSettings.LogType.AllLog, null, null, null, SignatureContents);
    }

    public eSign(String licenceFilePath, String pfxpath, String password, String pfxAlias, boolean proxyreq, String proxyIp, int proxyPort, int sessionTimeout, int SignatureContents) {
        this(licenceFilePath, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, sessionTimeout, eSignSettings.LogType.AllLog, null, null, null, SignatureContents);
    }

    public eSign(String licenceFilePath, String pfxpath, String password, String pfxAlias, boolean proxyreq,
            String proxyIp, int proxyPort, int sessionTimeout, eSignSettings.LogType logType, int SignatureContents) {
        this(licenceFilePath, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, sessionTimeout, logType, null, null, null, SignatureContents);
    }

    public eSign(String licenceFilePath, String pfxpath, String password, String pfxAlias, String pdfViewerLicence, int SignatureContents) {
        this(licenceFilePath, pfxpath, password, pfxAlias, false, "", 0, 0, eSignSettings.LogType.AllLog, null, null, pdfViewerLicence, SignatureContents);
    }

    public eSign(String licenceFilePath, String pfxpath, String password, String pfxAlias, boolean proxyreq, String proxyIp, int proxyPort, String pdfViewerLicence, int SignatureContents) {
        this(licenceFilePath, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, 0, eSignSettings.LogType.AllLog, null, null, pdfViewerLicence, SignatureContents);
    }

    public eSign(String licenceFilePath, String pfxpath, String password, String pfxAlias, boolean proxyreq, String proxyIp, int proxyPort, int sessionTimeout, String pdfViewerLicence, int SignatureContents) {
        this(licenceFilePath, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, sessionTimeout, eSignSettings.LogType.AllLog, null, null, pdfViewerLicence, SignatureContents);
    }

    public eSign(String licenceFilePath, String pfxpath, String password, String pfxAlias, boolean proxyreq, String proxyIp, int proxyPort, int sessionTimeout, eSignSettings.LogType logType, String pdfViewerLicence, int SignatureContents) {
        this(licenceFilePath, pfxpath, password, pfxAlias, proxyreq, proxyIp, proxyPort, sessionTimeout, logType, null, null, pdfViewerLicence, SignatureContents);
    }

    public eSign(String licenceFilePath, String pfxpath, String password, String pfxAlias, boolean proxyreq,
            String proxyIp, int proxyPort, int sessionTimeout, eSignSettings.LogType logType, String ProxyUserID, String ProxyUserPassword, String pdfViewerLicence, int SignatureContents) {
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
        eSignSettings.ValidateAndRead(licenceFilePath);
        if (pdfViewerLicence != null) {
            ValidateKitLicence.validateKitLicence(pdfViewerLicence);
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

    public eSignServiceReturn performBankKYC(String transactionID, String IFSCCode, String bankName, String accountNumber, UserInfo userInfo) {
        eSignImplimentation impl = new eSignImplimentation(pfxpath, password, pfxAlias, proxyIp, proxyPort, proxyreq);
        return impl.performBankKYC(transactionID, IFSCCode, bankName, accountNumber, userInfo);
    }

    public eSignServiceReturn getSigedDocument(String eSignResponse, String preSignedTempFile) {
        eSignImplimentation impl = new eSignImplimentation(pfxpath, password, pfxAlias, proxyIp, proxyPort, proxyreq);
        return impl.getSigedDocument(eSignResponse, preSignedTempFile, SignatureContents);
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
}
