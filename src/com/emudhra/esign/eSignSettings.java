/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
//import org.apache.logging.log4j.core.Logger;
import org.emcastle.cms.CMSProcessableByteArray;
import org.emcastle.cms.CMSSignedData;
import org.emcastle.cms.SignerInformation;
import org.emcastle.cms.SignerInformationStore;
import org.emcastle.jce.provider.emCastleProvider;
import org.emcastle.util.encoders.Base64;

/**
 *
 * @author 20476
 */
public final class eSignSettings {

    /**
     * @return the IsValidLicence
     */
    protected static boolean IsValidLicence() {
        return isValidLicence;
    }

    /**
     * @param aIsValidLicence the IsValidLicence to set
     */
    private static void setIsValidLicence(boolean aIsValidLicence) {
        isValidLicence = aIsValidLicence;
    }

    public enum LogType {
        NoLog,
        NoDebugLog,
        AllLog
    }

    private static final Logger LOGGER = EsignLoggerFactory.getLogger(eSignSettings.class);
    private static final String PRODUCTCODE = "101";
    private static String ASPID;
    private static String OTPURL;
    private static String ESIGNURL;
    private static String ESIGNURLV2;
    private static String SignedData;
    private static String Toverify;
    private static boolean isValidLicence;
    private static String ValidationError;
    private static int SessionTimeout;
    private static String ProxyUserID;
    private static String ProxyUserPassword;
    private static String BankKYCURL;
    private static boolean isBankLicense;

    /**
     * @return the ASPID
     */
    protected static String getASPID() {
        return ASPID;
    }

    public static String getBankKYCURL() {
        return BankKYCURL;
    }

    public static void setBankKYCURL(String BankKYCURL) {
        eSignSettings.BankKYCURL = BankKYCURL;
    }

    public static boolean isIsBankLicense() {
        return isBankLicense;
    }

    public static void setIsBankLicense(boolean isBankLicense) {
        eSignSettings.isBankLicense = isBankLicense;
    }

    protected static String getESIGNURLV2() {
        return ESIGNURLV2;
    }

    /**
     * @return the OTPURL
     */
    protected static String getOTPURL() {
        return OTPURL;
    }

    /**
     * @return the ESIGNURL
     */
    protected static String getESIGNURL() {
        return ESIGNURL;
    }

    /**
     * @return the LicenceKey
     */
    protected static String getSignedData() {
        return SignedData;
    }

    /**
     * @return the Toverify
     */
    protected static String getToverify() {
        return Toverify;
    }

    /**
     * @return the ValidationError
     */
    protected static String getValidationError() {
        return ValidationError;
    }

    /**
     * @return the SessionTimeout
     */
    public static int getSessionTimeout() {
        return SessionTimeout;
    }

    /**
     * @param aSessionTimeout the SessionTimeout to set
     */
    public static void setSessionTimeout(int aSessionTimeout) {
        SessionTimeout = aSessionTimeout;
    }

    /**
     * @return the ProxyUserID
     */
    public static String getProxyUserID() {
        return ProxyUserID;
    }

    /**
     * @param aProxyUserID the ProxyUserID to set
     */
    public static void setProxyUserID(String aProxyUserID) {
        ProxyUserID = aProxyUserID;
    }

    /**
     * @return the ProxyUserPassword
     */
    public static String getProxyUserPassword() {
        return ProxyUserPassword;
    }

    protected static String getESIGNStatusURL() {
        return ESIGNURL.replace("eSignRequest", "checkSignStatusAPI");
    }

    /**
     * @param aProxyUserPassword the ProxyUserPassword to set
     */
    public static void setProxyUserPassword(String aProxyUserPassword) {
        ProxyUserPassword = aProxyUserPassword;
    }

    protected static void ValidateAndRead(String LicFilePath) {
        try {
            LOGGER.info("Reading licence file :" + LicFilePath);
            File file = new File(LicFilePath);
            if (file.exists() && !file.isDirectory()) {
                int Length = LicFilePath.length();
                String Extension = LicFilePath.substring(Length - 4, Length);
                if (Extension.toLowerCase().equals(".lic")) {
                    FileInputStream fisTargetFile = new FileInputStream(new File(LicFilePath));
                    String targetFileStr = IOUtils.toString(fisTargetFile, "UTF-8");
                    String sArry[] = targetFileStr.split("\\|");
                    if (sArry.length > 1) {
                        String decodedString;
                        try {
                            Toverify = sArry[0];
                            decodedString = new String(Base64.decode(sArry[0]));
                            SignedData = sArry[1];
                            try {
                                setIsValidLicence(verifySignature(decodedString, SignedData));
                            } catch (Exception ex) {
                                ValidationError = ex.toString();
                                setIsValidLicence(false);
                            }
                            if (IsValidLicence() && !(decodedString.equals(""))) {
                                String[] Arry = decodedString.split("\\|");
                                if (Arry.length > 1) {
                                    ASPID = Arry[0];
                                    ESIGNURL = Arry[1];
                                    long unixTime = System.currentTimeMillis() / 1000L;
                                    if (PRODUCTCODE.equals(Arry[2])) {
                                        if (unixTime < Long.parseLong(Arry[3])) {
                                            setIsValidLicence(true);
                                        } else {
                                            setIsValidLicence(false);
                                            ValidationError = "Licence period completed";
                                        }
                                        if (Arry.length == 6) {
                                            ESIGNURLV2 = Arry[5];
                                        }
                                        if (Arry.length > 4) {
                                            BankKYCURL = Arry[4];
                                            isBankLicense = true;
                                        }
                                    } else {
                                        setIsValidLicence(false);
                                        ValidationError = "Invalid Licence for this product";
                                    }
                                }
                            } else {
                                ValidationError = "Invalid Data";
                                setIsValidLicence(false);
                            }
                        } catch (NumberFormatException e) {
                            ValidationError = e.toString();
                            setIsValidLicence(false);
                        }
                    } else {
                        ValidationError = "Tamperd Lic file";
                        setIsValidLicence(false);
                    }
                } else {
                    ValidationError = "Invalid File Extension";
                    setIsValidLicence(false);
                }
            } else {
                ValidationError = "Invalid File Path";
                setIsValidLicence(false);
            }
        } catch (Exception ex) {
            LOGGER.warning(ex.getLocalizedMessage());
            ValidationError = ex.toString();
        }
        LOGGER.info("ValidationError :" + ValidationError + " IsValidLicence:" + IsValidLicence());
    }

    private static boolean verifySignature(String toVerify, String signeddata) {
        boolean result = false;
        try {
            String Certificate = "-----BEGIN CERTIFICATE-----\n"
                    + "MIIB7zCCAZWgAwIBAgIEV+y33zAKBggqhkjOPQQDAjB3MQswCQYDVQQGEwJJTjES\n"
                    + "MBAGA1UECAwJS2FybmF0YWthMRgwFgYDVQQKDA9lTXVkaHJhIExpbWl0ZWQxEzAR\n"
                    + "BgNVBAsMClRlY2hub2xvZ3kxJTAjBgNVBAMMHGVNdWRocmEgTGljZW5zZSBQcm90\n"
                    + "ZWN0aW9uIDEwHhcNMTYwOTI5MDY0NDAxWhcNMjYwOTI5MDY0NDAxWjB3MQswCQYD\n"
                    + "VQQGEwJJTjESMBAGA1UECAwJS2FybmF0YWthMRgwFgYDVQQKDA9lTXVkaHJhIExp\n"
                    + "bWl0ZWQxEzARBgNVBAsMClRlY2hub2xvZ3kxJTAjBgNVBAMMHGVNdWRocmEgTGlj\n"
                    + "ZW5zZSBQcm90ZWN0aW9uIDEwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAASo2Hlz\n"
                    + "HnlGNiEKG0RoNEMEOr7sPcoWOK5PqTaVqoIeLF36BjKhIXXSS1y+AaO5UutBitsv\n"
                    + "sf4wKnWbXEPjDzuyow8wDTALBgNVHQ8EBAMCBPAwCgYIKoZIzj0EAwIDSAAwRQIg\n"
                    + "OdwZuqCMuZSDgw3WfCxNDe6izAYqo2FSf7jJWM7nmggCIQCxArpFjiB2atyeyAfN\n"
                    + "ZlAVdHB1AfZO1ZT/G+rLt+JX2Q==\n"
                    + "-----END CERTIFICATE-----";
            byte[] signedByte = Base64.decode(signeddata);
            Security.addProvider(new emCastleProvider());
            CMSSignedData s = new CMSSignedData(new CMSProcessableByteArray(toVerify.getBytes()), signedByte);
            SignerInformationStore signers = s.getSignerInfos();
            SignerInformation signerInfo = (SignerInformation) signers.getSigners().iterator().next();
            X509Certificate cert = getX509CertificateFromPublicKey(Certificate);
            if (cert != null) {
                result = signerInfo.verify(cert.getPublicKey(), "EM");
            } else {
                throw new NullPointerException();
            }

        } catch (Exception ex) {
            LOGGER.warning("Exception in verifySignature : Exception "+ ex);
            LOGGER.info("Exception in verifySignature : Exception " + ex);
        }
        return result;
    }

    private static X509Certificate getX509CertificateFromPublicKey(String data) {
        InputStream fis = null;
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            fis = new ByteArrayInputStream(data.getBytes());
            return (X509Certificate) certFactory.generateCertificate(fis);

        } catch (Exception ex) {
            LOGGER.warning("Exception in getX509CertificateFromPublicKey : Exception "+ ex);
            LOGGER.info("Exception in getX509CertificateFromPublicKey : Exception " + ex);
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    LOGGER.warning("Exception in getX509CertificateFromPublicKey : Exception "+ ex);
                    LOGGER.info("Exception in getX509CertificateFromPublicKey : Exception " + ex);
                }
            }
        }
    }
}
