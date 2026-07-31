/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import com.emudhra.esign.eSign.AppearanceType;
import com.emudhra.esign.eSign.Coordinates;
import com.emudhra.esign.eSign.PageTobeSigned;

/**
 *
 * @author 20476
 */
public final class eSignInput {

    private String docBase64;
    private String docHash;
    private String docInfo;
    private String docURL;
    private String signedBy;
    private String location;
    private String reason;
    private String appearanceText;
    private boolean coSign;
    private boolean rightOrigin;
    private PageTobeSigned pageTobeSigned;
    private Coordinates coordinates;
    private String pageNumbers;
    private String pageLevelCoordinates;
    private eSign.InputType inputType;
    private ContentSearch contentSearch;
    private int signatureFontSize;
    private String signatureImage;
    private String oneLiner;
    private AppearanceType appearanceType;
    private AdvanceSignature advanceSignature;
    private CustomStyle customStyle;
    private boolean BorderRequired;
    private boolean TickRequired;
    private ColoredGraphicInputs coloredGraphicInputs;
    private String pdfPassword;
    private boolean showAadhaarOnSignature;
    private eSign.Coordinates textContentPosition = eSign.Coordinates.TopLeft;
    private EncryptedAadhaarConfig encryptedAadhaarConfig;
    private boolean encryptedAadhaarFlowEnabled = false;

    public String getPdfPassword() {
        return pdfPassword;
    }

    public boolean isShowAadhaarOnSignature() {
        return showAadhaarOnSignature;
    }

    public eSign.Coordinates getTextContentPosition() {
        return textContentPosition != null ? textContentPosition : eSign.Coordinates.TopLeft;
    }

    public EncryptedAadhaarConfig getEncryptedAadhaarConfig() {
        return encryptedAadhaarConfig;
    }

    void setEncryptedAadhaarConfig(EncryptedAadhaarConfig encryptedAadhaarConfig) {
        this.encryptedAadhaarConfig = encryptedAadhaarConfig;
    }

    public boolean isEncryptedAadhaarFlowEnabled() {
        return encryptedAadhaarFlowEnabled;
    }

    void setEncryptedAadhaarFlowEnabled(boolean encryptedAadhaarFlowEnabled) {
        this.encryptedAadhaarFlowEnabled = encryptedAadhaarFlowEnabled;
    }

    public boolean isBorderRequired() {
        return BorderRequired;
    }

    public ColoredGraphicInputs getColoredGraphicInputs() {
        return coloredGraphicInputs;
    }

    public AdvanceSignature getAdvanceSignature() {
        return advanceSignature;
    }

    public CustomStyle getCustomStyle() {
        return customStyle;
    }

    public eSign.InputType getInputType() {
        return inputType;
    }

    public void setInputType(eSign.InputType inputType) {
        this.inputType = inputType;
    }

    public String getOneLiner() {
        return oneLiner;
    }

    /**
     * @return the appearanceText
     */
    public String getAppearanceText() {
        return appearanceText;
    }

    /**
     * @return the docBase64
     */
    public String getDocBase64() {
        return docBase64;
    }

    /**
     * @return the signedBy
     */
    public String getSignedBy() {
        return signedBy;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return the reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * @return the coSign
     */
    public boolean isCoSign() {
        return coSign;
    }

    /**
     * @return the pageTobeSigned
     */
    public PageTobeSigned getPage() {
        return pageTobeSigned;
    }

    /**
     * @return the coordinates
     */
    public Coordinates getCoordinates() {
        return coordinates;
    }

    /**
     * @return the pageNumbers
     */
    public String getPageNumbers() {
        return pageNumbers;
    }

    /**
     * @return the pageLevelCoordinates
     */
    public String getPageLevelCoordinates() {
        return pageLevelCoordinates;
    }

    protected void pageLevelCoordinates(String pageLevelCoordinates) {
        this.pageLevelCoordinates = pageLevelCoordinates;
//        this.pageTobeSigned = PageTobeSigned.PageLevel;
    }

    /**
     * @return the docInfo
     */
    public String getDocInfo() {
        return docInfo;
    }

    /**
     * @return the docURL
     */
    public String getDocURL() {
        return docURL;
    }

    /**
     * @return the docHash
     */
    public String getDocHash() {
        return docHash;
    }

    public boolean isRightOrigin() {
        return rightOrigin;
    }

    public PageTobeSigned getPageTobeSigned() {
        return pageTobeSigned;
    }

    public ContentSearch getContentSearch() {
        return contentSearch;
    }

    public int getSignatureFontSize() {
        return signatureFontSize;
    }

    public String getSignatureImage() {
        return signatureImage;
    }

    public AppearanceType getAppearanceType() {
        return appearanceType;
    }

    public boolean isTickRequired() {
        return TickRequired;
    }

    public eSignInput(String docInfo, String docHash, String docURL) {
        this("", docInfo, docURL, "", "", "", true, PageTobeSigned.Specify, Coordinates.TopRight, "", "", "", docHash, eSign.InputType.HASH, false, null, -1, "", eSign.AppearanceType.StandardSignature, "", null, null, null, false, false, "", false, eSign.Coordinates.TopLeft);
    }

    public eSignInput(String docBase64, String docInfo, String docURL, String location, String reason, String signedBy, boolean coSign, PageTobeSigned page, Coordinates coordinates, String appearanceText) {
        this(docBase64, docInfo, docURL, location, reason, signedBy, coSign, page, coordinates, "", "", appearanceText, "", eSign.InputType.PDF, false, null, -1, "", eSign.AppearanceType.StandardSignature, "", null, null, null, false, false, "", false, eSign.Coordinates.TopLeft);
    }

    public eSignInput(String docBase64, String docInfo, String docURL, String location, String reason, String signedBy, boolean coSign, Coordinates coordinates, String pageNumbers, String appearanceText) {
        this(docBase64, docInfo, docURL, location, reason, signedBy, coSign, PageTobeSigned.Specify, coordinates, pageNumbers, "", appearanceText, "", eSign.InputType.PDF, false, null, -1, "", eSign.AppearanceType.StandardSignature, "", null, null, null, false, false, "", false, eSign.Coordinates.TopLeft);
    }

    public eSignInput(String docBase64, String docInfo, String docURL, String location, String reason, String signedBy, boolean coSign, String pageLevelCoordinates, String appearanceText) {
        this(docBase64, docInfo, docURL, location, reason, signedBy, coSign, PageTobeSigned.PageLevel, null, "", pageLevelCoordinates, appearanceText, "", eSign.InputType.PDF, false, null, -1, "", eSign.AppearanceType.StandardSignature, "", null, null, null, false, false, "", false, eSign.Coordinates.TopLeft);
    }

    public eSignInput(String docBase64, String docInfo, String docURL, String location, String reason, String signedBy, boolean coSign, String pageLevelCoordinates, String appearanceText, boolean rightOrigin) {
        this(docBase64, docInfo, docURL, location, reason, signedBy, coSign, PageTobeSigned.PageLevel, null, "", pageLevelCoordinates, appearanceText, "", eSign.InputType.PDF, rightOrigin, null, -1, "", eSign.AppearanceType.StandardSignature, "", null, null, null, false, false, "", false, eSign.Coordinates.TopLeft);
    }

    protected eSignInput(String docBase64, String docInfo, String docURL, String location, String reason, String signedBy, boolean coSign, PageTobeSigned pageTobeSigned, Coordinates coordinates, String pageNumbers, String pageLevelCoordinates, String appearanceText, String docHash, eSign.InputType inputType, boolean rightOrigin, ContentSearch contentSearch, int signatureFontSize, String signatureImage, eSign.AppearanceType appearanceType, String oneLiner, AdvanceSignature advanceSignature, ColoredGraphicInputs coloredGraphicInputs, CustomStyle customStyle, boolean isBorderRequired, boolean isTickRequired, String pdfPassword, boolean showAadhaarOnSignature, eSign.Coordinates textContentPosition) {
        this.coSign = coSign;
        this.coordinates = coordinates;
        this.docBase64 = docBase64;
        this.docInfo = docInfo;
        this.docURL = docURL;
        this.location = location;
        this.pageTobeSigned = pageTobeSigned;
        this.pageLevelCoordinates = pageLevelCoordinates;
        this.pageNumbers = pageNumbers;
        this.reason = reason;
        this.signedBy = signedBy;
        this.appearanceText = appearanceText;
        this.docHash = docHash;
        this.inputType = inputType;
        this.rightOrigin = rightOrigin;
        this.contentSearch = contentSearch;
        this.signatureFontSize = signatureFontSize;
        this.signatureImage = signatureImage;
        this.appearanceType = appearanceType;
        this.oneLiner = oneLiner;
        this.advanceSignature = advanceSignature;
        this.coloredGraphicInputs = coloredGraphicInputs;
        this.customStyle = customStyle;
        this.BorderRequired = isBorderRequired;
        this.TickRequired = isTickRequired;
        this.pdfPassword = pdfPassword;
        this.showAadhaarOnSignature = showAadhaarOnSignature;
        this.textContentPosition = textContentPosition != null ? textContentPosition : eSign.Coordinates.TopLeft;
    }

}
