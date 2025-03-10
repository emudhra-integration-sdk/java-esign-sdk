/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

import com.emudhra.esign.Enums.ImageType;



/**
 *
 * @author 20730
 */
public class AdvanceSignature {

    private ImageType imageType;
    private String imagebase64;
    private String leftSideText;
    private String rightSideText;

    public ImageType getImageType() {
        return imageType;
    }

    public void setImageType(ImageType imageType) {
        this.imageType = imageType;
    }

    public String getImagebase64() {
        return imagebase64;
    }

    public void setImagebase64(String imagebase64) {
        this.imagebase64 = imagebase64;
    }

    public String getLeftSideText() {
        return leftSideText;
    }

    public void setLeftSideText(String leftSideText) {
        this.leftSideText = leftSideText;
    }

    public String getRightSideText() {
        return rightSideText;
    }

    public void setRightSideText(String rightSideText) {
        this.rightSideText = rightSideText;
    }
    
    

}
