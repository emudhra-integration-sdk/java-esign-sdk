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
public class UserInfo {

    private String name;
    private String mobile;
    private String email;
    private String address;
    private String stateProvince;
    private String country;
    private String postalCode;
    private String dateOfBirth;
    private String gender;
    private String pan;
    private String aadhaar;
    private String photoFormat;
    private String photoBase64;

    public UserInfo(String name, String mobile, String email, String address, String stateProvince, String country, String postalCode, String dateOfBirth, String gender, String pan, String aadhaar, String photoFormat, String photoBase64) {
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



    public String getName() {
        return name;
    }

    public String getMobile() {
        return mobile;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public String getCountry() {
        return country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public String getPan() {
        return pan;
    }

    public String getAadhaar() {
        return aadhaar;
    }

    public String getPhotoFormat() {
        return photoFormat;
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

}
