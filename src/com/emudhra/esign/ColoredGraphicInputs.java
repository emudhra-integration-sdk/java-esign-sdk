/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.emudhra.esign;

/**
 *
 * @author 21701
 */
public class ColoredGraphicInputs {

    private int[] rightBorder = new int[]{148, 0, 211};
    private int[] leftBorder = new int[]{222, 35, 2};

    public int[] getRightBorder() {
        return rightBorder;
    }

    public int[] getLeftBorder() {
        return leftBorder;
    }

    public void setRightBorder(int[] rightBorder) {
        if (rightBorder == null || rightBorder.length == 0 || rightBorder.length > 3) {
            this.rightBorder = new int[]{148, 0, 211};
        } else {
            this.rightBorder = rightBorder;
        }
    }

    public void setLeftBorder(int[] leftBorder) {
        if (leftBorder == null || leftBorder.length == 0 || leftBorder.length > 3) {
            this.leftBorder = new int[]{222, 35, 2};
        } else {
            this.leftBorder = leftBorder;
        }

    }
}
