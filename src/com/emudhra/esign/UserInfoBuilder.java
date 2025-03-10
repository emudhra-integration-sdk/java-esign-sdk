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
public class UserInfoBuilder {

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

    public static UserInfoBuilder init() {
        return new UserInfoBuilder();
    }

    public UserInfoBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public UserInfoBuilder setMobile(String mobile) {
        this.mobile = mobile;
        return this;
    }

    public UserInfoBuilder setEmail(String email) {
        this.email = email;
        return this;
    }

    public UserInfoBuilder setAddress(String address) {
        this.address = address;
        return this;
    }

    public UserInfoBuilder setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
        return this;
    }

    public UserInfoBuilder setCountry(String country) {
        this.country = country;
        return this;
    }

    public UserInfoBuilder setPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public UserInfoBuilder setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public UserInfoBuilder setGender(String gender) {
        this.gender = gender;
        return this;
    }

    public UserInfoBuilder setPan(String pan) {
        this.pan = pan;
        return this;
    }

    public UserInfoBuilder setAadhaar(String aadhaar) {
        this.aadhaar = aadhaar;
        return this;
    }

    public UserInfoBuilder setPhotoFormat(String photoFormat) {
        this.photoFormat = photoFormat;
        return this;
    }

    public UserInfoBuilder setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
        return this;

    }

    public UserInfo build() {
        return new UserInfo(this.name,
                 this.mobile, this.email,
                 this.address, this.stateProvince,
                 this.country, this.postalCode,
                 this.dateOfBirth, this.gender,
                 this.pan, this.aadhaar,
                 this.photoFormat, this.photoBase64
        );
    }
}
