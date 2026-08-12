/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import java.util.ArrayList;

/**
 *
 * @author 20476
 */
public final class eSignServiceReturn {

    /**
     * @return the enCryptedPath
     */
    public String getEnCryptedPath() {
        return enCryptedPath;
    }

    /**
     * @param enCryptedPath the enCryptedPath to set
     */
    protected void setEnCryptedPath(String enCryptedPath) {
        this.enCryptedPath = enCryptedPath;
    }

    /**
     * @return the preSignedTempFile
     */
    public String getPreSignedTempFile() {
        return preSignedTempFile;
    }

    /**
     * @param preSignedTempFile the preSignedTempFile to set
     */
    protected void setPreSignedTempFile(String preSignedTempFile) {
        this.preSignedTempFile = preSignedTempFile;
    }

    private String transactionID;
    private String preSignedTempFile;
    private String requestXML;
    private String responseXML;
    private int status;
    private String responseCode;
    private String gatewayParameter;
    private String errorCode;
    private String errorMessage;
    private ArrayList<ReturnDocument> returnDocuments;
    private String enCryptedPath;
    private SignerCertificateInfo signerCertificateInfo;

    /**
     * Details of the signer certificate returned by the gateway in
     * {@code UserX509Certificate}. Populated by {@code getSigedDocument};
     * null in phase 1, and null when the response carried no parsable certificate.
     *
     * @return the signerCertificateInfo
     */
    public SignerCertificateInfo getSignerCertificateInfo() {
        return signerCertificateInfo;
    }

    /**
     * @param signerCertificateInfo the signerCertificateInfo to set
     */
    protected void setSignerCertificateInfo(SignerCertificateInfo signerCertificateInfo) {
        this.signerCertificateInfo = signerCertificateInfo;
    }

    /**
     * @return the transactionID
     */
    public String getTransactionID() {
        return transactionID;
    }

    /**
     * @param transactionID the transactionID to set
     */
    protected void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    /**
     * @return the requestXML
     */
    public String getRequestXML() {
        return requestXML;
    }

    /**
     * @param requestXML the requestXML to set
     */
    protected void setRequestXML(String requestXML) {
        this.requestXML = requestXML;
    }

    /**
     * @return the responseXML
     */
    public String getResponseXML() {
        return responseXML;
    }

    /**
     * @param responseXML the responseXML to set
     */
    protected void setResponseXML(String responseXML) {
        this.responseXML = responseXML;
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    protected void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return the errorCode
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode the errorCode to set
     */
    protected void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    protected void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the returnDocuments
     */
    public ArrayList<ReturnDocument> getReturnValues() {
        return getReturnDocuments();
    }

    /**
     * @param returnDocuments the returnValues to set
     */
    protected void setReturnValues(ArrayList<ReturnDocument> returnDocuments) {
        this.setReturnDocuments(returnDocuments);
    }

    /**
     * @return the responseCode
     */
    public String getResponseCode() {
        return responseCode;
    }

    /**
     * @param responseCode the responseCode to set
     */
    protected void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * @return the gateayParameter
     */
    public String getGatewayParameter() {
        return gatewayParameter;
    }

    /**
     * @param gatewayParameter the gatewayParameter to set
     */
    protected void setGatewayParameter(String gatewayParameter) {
        this.gatewayParameter = gatewayParameter;
    }

    /**
     * @return the returnDocuments
     */
    public ArrayList<ReturnDocument> getReturnDocuments() {
        return returnDocuments;
    }

    /**
     * @param returnDocuments the returnDocuments to set
     */
    protected void setReturnDocuments(ArrayList<ReturnDocument> returnDocuments) {
        this.returnDocuments = returnDocuments;
    }
}
