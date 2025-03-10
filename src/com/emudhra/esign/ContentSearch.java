/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

/**
 *
 * @author 20730
 */
public class ContentSearch {

    private String searchText;
    private int height;
    private int width;
    private String offset;
    private Position position;

    public static enum Position {
        /// <summary>
        /// Makes signature appearance to be on Outer Top Left.
        /// </summary>
        OTL,
        /// <summary>
        /// Makes signature appearance to be on Outer Top Middle.
        /// </summary>
        OTM,
        /// <summary>
        /// Makes signature appearance to be on Outer Top Right.
        /// </summary>
        OTR,
        /// <summary>
        /// Makes signature appearance to be on Outer Bottom Left.
        /// </summary>
        OBL,
        /// <summary>
        /// Makes signature appearance to be on Outer Bottom Middle.
        /// </summary>
        OBM,
        /// <summary>
        /// Makes signature appearance to be on Outer Bottom Right.
        /// </summary>
        OBR,
        /// <summary>
        /// Makes signature appearance to be on Inner Top Left.
        /// </summary>
        ITL,
        /// <summary>
        /// Makes signature appearance to be on Inner Top Middle.
        /// </summary>
        ITM,
        /// <summary>
        /// Makes signature appearance to be on Inner Top Right.
        /// </summary>
        ITR,
        /// <summary>
        /// Makes signature appearance to be on Inner Middle Left.
        /// </summary>
        IML,
        /// <summary>
        /// Makes signature appearance to be on Inner Middle Center.
        /// </summary>
        IMC,
        /// <summary>
        /// Makes signature appearance to be on Inner Middle Right.
        /// </summary>
        IMR,
        /// <summary>
        /// Makes signature appearance to be on Inner Bottom Left.
        /// </summary>
        IBL,
        /// <summary>
        /// Makes signature appearance to be on Inner Bottom Middle.
        /// </summary>
        IBM,
        /// <summary>
        /// Makes signature appearance to be on Inner Bottom Right.
        /// </summary>
        IBR;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

}
