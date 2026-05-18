/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author 20476
 */
public final class ReturnDocument {

    private String signedDocument;
    private String signedData;
//    private boolean isPDF;
    private String documentHash;
    private String preSignedDocument;
    private String docInfo;
    private String docURL;
    private int docId;
    private String errorMessage;
    private String errorCode;
    private int status;
    private eSign.InputType inputType;
    private boolean showAadhaarOnSignature;

    protected ReturnDocument(String signedDocument, int docId, String docInfo, String docURL, String documentHash, String preSignedDocument, eSign.InputType inputType) {
        this(signedDocument, docId, docInfo, docURL, documentHash, preSignedDocument, inputType, false);
    }

    protected ReturnDocument(String signedDocument, int docId, String docInfo, String docURL, String documentHash, String preSignedDocument, eSign.InputType inputType, boolean showAadhaarOnSignature) {
        this.signedDocument = signedDocument;
        this.documentHash = documentHash;
        this.docURL = docURL;
        this.docInfo = docInfo;
        this.preSignedDocument = preSignedDocument;
        this.docId = docId;
        this.inputType = inputType;
        this.showAadhaarOnSignature = showAadhaarOnSignature;
    }

    protected String getReturnDocumentObjBase64() throws UnsupportedEncodingException {
        String returnDocument = Integer.toString(docId) + "|" + docInfo + "|" + docURL + "|" + documentHash + "|" + preSignedDocument + "|" + showAadhaarOnSignature;
        return org.emcastle.util.encoders.Base64.toBase64String(returnDocument.getBytes("utf-8"));
    }

    public boolean isShowAadhaarOnSignature() {
        return showAadhaarOnSignature;
    }

    public ReturnDocument(String returnDocumentBase64) {
        try {
            byte[] decodedBytes = org.emcastle.util.encoders.Base64.decode(returnDocumentBase64);
            String returnDocument = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] returnDocumentValues = returnDocument.split("\\|");
            if (returnDocumentValues.length >= 5) {
                this.preSignedDocument = returnDocumentValues[4];
            }
            if (returnDocumentValues.length >= 6) {
                this.showAadhaarOnSignature = Boolean.parseBoolean(returnDocumentValues[5]);
            }
            int documentId = 0;
            if (eSignUtility.tryParseInt(returnDocumentValues[0])) {
                documentId = Integer.parseInt(returnDocumentValues[0]);
            }
            this.docId = documentId;
            this.docInfo = returnDocumentValues[1];
            this.docURL = returnDocumentValues[2];
            this.documentHash = returnDocumentValues[3];

            this.signedDocument = "";
        } catch (Exception e) {
            this.status = 0;
            this.docId = 0;
            this.preSignedDocument = returnDocumentBase64;
            this.errorMessage = "Unable to create Return Document - " + e.getMessage();
        }
    }

    protected ReturnDocument(int status, String errorMessage, String errorCode, int docId) {
        this.status = status;
        this.docId = docId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * @return the signedDocument
     */
    public String getSignedDocument() {
        return signedDocument;
    }

    /**
     * @return the documentHash
     */
    public String getDocumentHash() {
        return documentHash;
    }

    /**
     * @return the DocID
     */
    public int getDocId() {
        return docId;
    }

    /**
     * @param signedDocument the signedDocument to set
     */
    protected void setSignedDocument(String signedDocument) {
        this.signedDocument = signedDocument;
    }

    /**
     * @param documentHash the documentHash to set
     */
    protected void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
    }

    /**
     * @param docId the docId to set
     */
    protected void setDocId(int docId) {
        this.docId = docId;
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
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status the Status to set
     */
    protected void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return the preSignedDocument
     */
    public String getPreSignedDocument() {
        return preSignedDocument;
    }

    /**
     * @param preSignedDocument the preSignedDocument to set
     */
    protected void setPreSignedDocument(String preSignedDocument) {
        this.preSignedDocument = preSignedDocument;
    }

    /**
     * @return the docInfo
     */
    public String getDocInfo() {
        return docInfo;
    }

    /**
     * @param docInfo the docInfo to set
     */
    protected void setDocInfo(String docInfo) {
        this.docInfo = docInfo;
    }

    /**
     * @return the docURL
     */
    public String getDocURL() {
        return docURL;
    }

    /**
     * @param docURL the docURL to set
     */
    protected void setDocURL(String docURL) {
        this.docURL = docURL;
    }

    public String getSignedData() {
        return signedData;
    }

    public void setSignedData(String signedData) {
        this.signedData = signedData;
    }

    public eSign.InputType getInputType() {
        return inputType;
    }

    private Exception IllegalArgumentException(String invalid_return_Document) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
