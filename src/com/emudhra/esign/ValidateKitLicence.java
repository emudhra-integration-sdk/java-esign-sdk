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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
//import org.apache.logging.log4j.core.Logger;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.CMSProcessableByteArray;
import org.emcastle.cms.CMSSignedData;
import org.emcastle.cms.SignerInformation;
import org.emcastle.cms.SignerInformationStore;
import org.emcastle.jce.provider.emCastleProvider;
import org.emcastle.util.encoders.Base64;
import org.emcastle.util.encoders.Hex;

/**
 *
 * @author 20476
 */
public class ValidateKitLicence {

    private static final Logger LOGGER = EsignLoggerFactory.getLogger(eSignSettings.class);
    private static final String PRODUCTCODE = "PDF_VIEWER_KIT_101";
    private static String ASPID;
    private static String SignedData;
    private static boolean isValidLicence;
    private static String validationError;
    private static String encryptionKey;

    /**
     * @return the ASPID
     */
    protected static String getASPID() {
        return ASPID;
    }

    public static String getEncryptionKey() {
        return encryptionKey;
    }

    private static void setIsValidLicence(boolean validity) {
        isValidLicence = validity;
    }

    /**
     * @return the IsValidLicence
     */
    public static boolean IsValidLicence() {
        return isValidLicence;
    }

    public static String getValidationError() {
        return validationError;
    }

    protected static void validateKitLicence(String LicFilePath) {
        try {
            LOGGER.info("Reading licence file :" + LicFilePath);
            File file = new File(LicFilePath);
            if (!file.exists() && file.isDirectory()) {
                validationError = "Invalid File Path";
                setIsValidLicence(false);
            }
            int Length = LicFilePath.length();
            String Extension = LicFilePath.substring(Length - 4, Length);
            if (!Extension.toLowerCase().equals(".lic")) {
                validationError = "Invalid File Path";
                setIsValidLicence(false);
            }
            String targetFileStr;
            try (FileInputStream fisTargetFile = new FileInputStream(new File(LicFilePath))) {
                targetFileStr = IOUtils.toString(fisTargetFile, "UTF-8");
            }
            String sArry[] = targetFileStr.split("\\|");
            if (sArry.length <= 1) {
            }
            String decodedString;
            try {
                decodedString = new String(Base64.decode(sArry[0]));
                SignedData = sArry[1];
                try {
                    setIsValidLicence(verifySignature(decodedString, SignedData));
                } catch (Exception ex) {
                    validationError = ex.toString();
                    setIsValidLicence(false);
                }
                if (isValidLicence && !(decodedString.equals(""))) {
                    String[] Arry = decodedString.split("\\|");
                    if (Arry.length > 1) {
                        ASPID = Arry[0];
                        long unixTime = System.currentTimeMillis() / 1000L;
                        if (PRODUCTCODE.equals(Arry[1])) {
                            if (unixTime < Long.parseLong(Arry[2])) {
                                encryptionKey = getSha256("PDF_VIEWER_KIT_101" + ASPID);
                                setIsValidLicence(true);
                            } else {
                                setIsValidLicence(false);
                                validationError = "Licence period completed";
                            }
                        } else {
                            setIsValidLicence(false);
                            validationError = "Invalid Licence for this product";
                        }
                    }
                }
            } catch (NoSuchAlgorithmException | NumberFormatException e) {
                validationError = e.toString();
                setIsValidLicence(false);
            }
        } catch (IOException e) {
            validationError = e.toString();
            setIsValidLicence(false);
        }
    }

    private static String getSha256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return Hex.toHexString(hash);
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

        } catch (NoSuchAlgorithmException | NoSuchProviderException | CMSException | NullPointerException ex) {
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
