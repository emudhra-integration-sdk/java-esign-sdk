/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import java.util.logging.Logger;

/**
 *
 * @author 20476
 */
public final class eSignSettings {

    public enum LogType {
        NoLog,
        NoDebugLog,
        AllLog
    }

    private static final Logger LOGGER = EsignLoggerFactory.getLogger(eSignSettings.class);
    private static String ASPID;
    private static String ESIGNURL;
    private static String ESIGNURLV2;
    private static int SessionTimeout;
    private static String ProxyUserID;
    private static String ProxyUserPassword;
    private static String encryptionKey;

    public static String getEncryptionKey() {
        return encryptionKey;
    }

    public static void setEncryptionKey(String encryptionKey) {
        eSignSettings.encryptionKey = encryptionKey;
    }

    /**
     * @return the ASPID
     */
    protected static String getASPID() {
        return ASPID;
    }

    public static void setASPID(String ASPID) {
        eSignSettings.ASPID = ASPID;
    }

    public static void setESIGNURL(String ESIGNURL) {
        eSignSettings.ESIGNURL = ESIGNURL;
    }

    public static void setESIGNURLV2(String ESIGNURLV2) {
        eSignSettings.ESIGNURLV2 = ESIGNURLV2;
    }

    protected static String getESIGNURLV2() {
        return ESIGNURLV2;
    }

    /**
     * @return the ESIGNURL
     */
    protected static String getESIGNURL() {
        return ESIGNURL;
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

}
