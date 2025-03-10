/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

/**
 *
 * @author 21685
 */
public class BankKYCInfo {

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the mobile
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return the stateProvince
     */
    public String getStateProvince() {
        return stateProvince;
    }

    /**
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @return the postalCode
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * @return the dateOfBirth
     */
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * @return the gender
     */
    public String getGender() {
        return gender;
    }

    /**
     * @return the pan
     */
    public String getPan() {
        return pan;
    }

    /**
     * @return the aadhaar
     */
    public String getAadhaar() {
        return aadhaar;
    }

    /**
     * @return the photoFormat
     */
    public String getPhotoFormat() {
        return photoFormat;
    }

    /**
     * @return the photoBase64
     */
    public String getPhotoBase64() {
        return photoBase64;
    }
    private final String name;
    private final String mobile;
    private final String email;
    private final String address;
    private final String stateProvince;
    private final String country;
    private final String postalCode;
    private final String dateOfBirth;
    private final String gender;
    private final String pan;
    private final String aadhaar;
    private final String photoFormat;
    private final String photoBase64;

    public BankKYCInfo(String name, String mobile, String email, String address, String stateProvince, String country, String postalCode, String dateOfBirth, String gender, String pan, String aadhaar, String photoFormat, String photoBase64) {
        this.name = name;
        this.mobile = mobile;
        this.email = email;
        this.address = address;
        this.stateProvince = stateProvince;
        this.country = country;
        this.postalCode = postalCode;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.pan = pan;
        this.aadhaar = aadhaar;
        this.photoFormat = photoFormat;
        this.photoBase64 = photoBase64;
    }
    
    
}

