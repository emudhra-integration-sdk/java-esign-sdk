/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import com.emudhra.esign.eSign.AppearanceType;

/**
 *
 * @author 20730
 *
 */
public class eSignInputBuilder {

    private String docBase64 = "";
    private String docHash = "";
    private String docInfo = "";
    private String docURL = "";
    private String signedBy = "";
    private String location = "";
    private String reason = "";
    private String appearanceText = "";
    private boolean coSign = true;
    private boolean rightOrigin = false;
    private eSign.PageTobeSigned pageTobeSigned;
    private eSign.Coordinates coordinates;
    private String pageNumbers;
    private String pageLevelCoordinates;
    private ContentSearch contentSearch;
    private int signatureFontSize;
    private String signatureImage;
    private AppearanceType appearanceType;
    private String oneLiner;
    private AdvanceSignature advanceSignature;
    private ColoredGraphicInputs coloredGraphicInputs;
    private CustomStyle customStyle;
    private boolean borderRequired;
    private boolean tickRequired;
    private String pdfPassword;

    private eSign.InputType inputType = eSign.InputType.PDF;

    public static eSignInputBuilder init() {
        return new eSignInputBuilder();
    }

    public eSignInputBuilder setDocBase64(String docBase64) {
        this.docBase64 = docBase64;
        return this;
    }

    public eSignInputBuilder setBorderRequired(boolean borderRequired) {
        if (appearanceType == eSign.AppearanceType.ColoredGraphic) {
            this.borderRequired = true;
        } else {
            this.borderRequired = borderRequired;
        }
        return this;
    }

    public eSignInputBuilder setCustomStyle(CustomStyle customStyle) {
        this.customStyle = customStyle;
        return this;

    }

    public eSignInputBuilder setAdvanceSignature(AdvanceSignature advanceSignature) {
        this.advanceSignature = advanceSignature;
        return this;
    }

    public eSignInputBuilder setColoredGraphicInputs(ColoredGraphicInputs coloredGraphicInputs) {
        this.coloredGraphicInputs = coloredGraphicInputs;
        return this;
    }

    public eSignInputBuilder setOneLiner(String oneLiner) {
        this.oneLiner = oneLiner;
        return this;
    }

    public eSignInputBuilder setDocHash(String docHash) {
        this.docHash = docHash;
        return this;
    }

    public eSignInputBuilder setDocInfo(String docInfo) {
        this.docInfo = docInfo;
        return this;
    }

    public eSignInputBuilder setSignatureFontSize(int signatureFontSize) {
        this.signatureFontSize = signatureFontSize;
        return this;
    }

    public eSignInputBuilder setSignatureImage(String signatureImage) {
        this.signatureImage = signatureImage;
        return this;
    }

    public eSignInputBuilder setDocURL(String docURL) {
        this.docURL = docURL;
        return this;
    }

    public eSignInputBuilder setSignedBy(String signedBy) {
        this.signedBy = signedBy;
        return this;
    }

    public eSignInputBuilder setLocation(String location) {
        this.location = location;
        return this;
    }

    public eSignInputBuilder setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public eSignInputBuilder setAppearanceText(String appearanceText) {
        this.appearanceText = appearanceText;
        return this;
    }

    public eSignInputBuilder setCoSign(boolean coSign) {
        this.coSign = coSign;
        return this;
    }

    public eSignInputBuilder setPageTobeSigned(eSign.PageTobeSigned pageTobeSigned) {
        this.pageTobeSigned = pageTobeSigned;
        return this;
    }

    public eSignInputBuilder setCoordinates(eSign.Coordinates coordinates) {
        this.coordinates = coordinates;
        return this;
    }

    public eSignInputBuilder setPageNumbers(String pageNumbers) {
        this.pageNumbers = pageNumbers;
        return this;
    }

    public eSignInputBuilder setPageLevelCoordinates(String pageLevelCoordinates) {
        this.pageLevelCoordinates = pageLevelCoordinates;
        return this;
    }

    public eSignInputBuilder setContentSearch(ContentSearch contentSearch) {
        this.contentSearch = contentSearch;
        return this;
    }

    public eSignInputBuilder setInputType(eSign.InputType inputType) {
        this.inputType = inputType;
        return this;
    }

    public eSignInputBuilder isRightOrigin(boolean rightOrigin) {
        this.rightOrigin = rightOrigin;
        return this;
    }

    public eSignInputBuilder setAppearanceType(AppearanceType appearanceType) {
        this.appearanceType = appearanceType;
        return this;
    }

    public eSignInputBuilder setTickRequired(boolean tickRequired) {
        this.tickRequired = tickRequired;
        return this;
    }

    public eSignInputBuilder setPdfPassword(String pdfPassword) {
        this.pdfPassword = pdfPassword;
        return this;
    }

    public eSignInput build() {
        return new eSignInput(this.docBase64, this.docInfo, this.docURL, this.location, this.reason, this.signedBy, this.coSign, this.pageTobeSigned, this.coordinates, this.pageNumbers, this.pageLevelCoordinates, this.appearanceText, this.docHash, this.inputType, this.rightOrigin, this.contentSearch, this.signatureFontSize, this.signatureImage, this.appearanceType, this.oneLiner, this.advanceSignature, this.coloredGraphicInputs, this.customStyle, this.borderRequired, this.tickRequired,this.pdfPassword);
    }
}
