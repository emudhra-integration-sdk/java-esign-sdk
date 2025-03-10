/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author 20476
 */
public final class EmdhaSignerInfo {

    /**
     * @return the rkaName
     */
    public String getRkaName() {
        return rkaName;
    }

    /**
     * @param rkaName the rkaName to set
     */
    public void setRkaName(String rkaName) {
        this.rkaName = rkaName;
    }

    /**
     * @return the kycId
     */
    public String getKycId() {
        return kycId;
    }

    /**
     * @param kycId the kycId to set
     */
    public void setKycId(String kycId) {
        this.kycId = kycId;
    }

    /**
     * @return the englishName
     */
    public String getEnglishName() {
        return englishName;
    }

    /**
     * @param englishName the englishName to set
     */
    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    /**
     * @return the arabicName
     */
    public String getArabicName() {
        return arabicName;
    }

    /**
     * @param arabicName the arabicName to set
     */
    public void setArabicName(String arabicName) {
        this.arabicName = arabicName;
    }

    /**
     * @return the mobile
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * @param mobile the mobile to set
     */
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @return the regionProvince
     */
    public String getRegionProvince() {
        return regionProvince;
    }

    /**
     * @param regionProvince the regionProvince to set
     */
    public void setRegionProvince(String regionProvince) {
        this.regionProvince = regionProvince;
    }

    /**
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country the country to set
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * @return the photoBase64
     */
    public String getPhotoBase64() {
        return photoBase64;
    }

    /**
     * @param photoBase64 the photoBase64 to set
     */
    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
    }

    private String rkaName;
    private String kycId;
    private String englishName;
    private String arabicName;
    private String mobile;
    private String email;
    private String address;
    private String regionProvince;
    private String country;
    private String photoBase64;

    public EmdhaSignerInfo(String rkaName, String kycId, String englishName, String arabicName, String mobile, String email, String address, String regionProvince, String country, String photoBase64) {
        this.rkaName = rkaName;
        this.kycId = kycId;
        this.englishName = englishName;
        this.arabicName = arabicName;
        this.mobile = mobile;
        this.email = email;
        this.address = address;
        this.regionProvince = regionProvince;
        this.country = country;
        this.photoBase64 = photoBase64;
    }

    public String getSignerInfoXMLBase64() throws Exception {
        try {
            String signerInfoXML = getSignerInfoXML();
            return org.emcastle.util.encoders.Base64.toBase64String(signerInfoXML.getBytes(StandardCharsets.UTF_8));
            
        } catch (Exception e) {
            throw e;
        }
    }

    private String validateSignerInfo() {
        String errorMessage = "";
        try {
            if (eSignUtility.isNullOrWhitespace(rkaName)) {
                errorMessage += "Parameter rkaName in signer info cannot be empty.\n";
            }
            if (eSignUtility.isNullOrWhitespace(kycId)) {
                errorMessage += "Parameter kycId in signer info cannot be empty.\n";
            }
            if (eSignUtility.isNullOrWhitespace(englishName)) {
                errorMessage += "Parameter englishName in signer info cannot be empty.\n";
            }
            if (eSignUtility.isNullOrWhitespace(arabicName)) {
                errorMessage += "Parameter arabicName in signer info cannot be empty.\n";
            }
            if (eSignUtility.isNullOrWhitespace(regionProvince)) {
                errorMessage += "Parameter regionProvince in signer info cannot be empty.\n";
            }
            if (eSignUtility.isNullOrWhitespace(country)) {
                errorMessage += "Parameter country in signer info cannot be empty.\n";
            }
            return errorMessage.trim();
        } catch (Exception e) {
            throw e;
        }
    }

    public String getSignerInfoXML() throws Exception {
        try {
            String errorMessage = validateSignerInfo();
            if (!eSignUtility.isNullOrWhitespace(errorMessage)) {
                throw new IllegalArgumentException(errorMessage);
            }
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element SignerInfoTag = document.createElement("SignerInfo");

            Attr verAttr = document.createAttribute("version");
            verAttr.setValue("1.00");
            SignerInfoTag.setAttributeNode(verAttr);

            Attr rkaNameAttr = document.createAttribute("rkaName");
            rkaNameAttr.setValue(this.rkaName);
            SignerInfoTag.setAttributeNode(rkaNameAttr);

            Attr kycIdAttr = document.createAttribute("kycId");
            kycIdAttr.setValue(this.kycId);
            SignerInfoTag.setAttributeNode(kycIdAttr);

            Element kycDataTag = document.createElement("kycData");

            Attr kycDataEnglishNameAttr = document.createAttribute("englishName");
            kycDataEnglishNameAttr.setValue(this.englishName);
            kycDataTag.setAttributeNode(kycDataEnglishNameAttr);

            Attr kycDataArabicNameAttr = document.createAttribute("arabicName");
            kycDataArabicNameAttr.setValue(this.arabicName);
            kycDataTag.setAttributeNode(kycDataArabicNameAttr);

            Attr kycDataRegionProvinceAttr = document.createAttribute("regionProvince");
            kycDataRegionProvinceAttr.setValue(this.regionProvince);
            kycDataTag.setAttributeNode(kycDataRegionProvinceAttr);

            Attr kycDataCountryAttr = document.createAttribute("country");
            kycDataCountryAttr.setValue(this.country);
            kycDataTag.setAttributeNode(kycDataCountryAttr);

            if (!eSignUtility.isNullOrWhitespace(this.mobile)) {
                Attr kycDataMobileAttr = document.createAttribute("mobile");
                kycDataMobileAttr.setValue(this.mobile);
                kycDataTag.setAttributeNode(kycDataMobileAttr);
            }
            if (!eSignUtility.isNullOrWhitespace(this.email)) {
                Attr kycDataEmailAttr = document.createAttribute("email");
                kycDataEmailAttr.setValue(this.email);
                kycDataTag.setAttributeNode(kycDataEmailAttr);
            }
            if (!eSignUtility.isNullOrWhitespace(this.address)) {
                Attr kycDataAddressAttr = document.createAttribute("address");
                kycDataAddressAttr.setValue(this.address);
                kycDataTag.setAttributeNode(kycDataAddressAttr);
            }
            SignerInfoTag.appendChild(kycDataTag);

            if (!eSignUtility.isNullOrWhitespace(this.photoBase64)) {
                Element photoTag = document.createElement("Photo");
                photoTag.appendChild(document.createTextNode(this.photoBase64.trim()));
                SignerInfoTag.appendChild(photoTag);
            }

            document.appendChild(SignerInfoTag);

            return eSignUtility.convertDocumentToString(document, true);
        } catch (Exception e) {
            throw e;
        }
    }
}
