/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emudhra.esign;

/**
 *
 * @author 21032
 */
public class Enums {

//    public enum AppearanceType {
//        /// <summary>
//        /// Standard for signature appearance containing Name, Reason, Location and  Time.
//        /// </summary>
//        Standard,
//        /// <summary>
//        /// One line string as signature appearance.
//        /// </summary>
//        OneLiner,
//        /// <summary>
//        /// Image as signature appearance.
//        /// </summary>
//        SignatureImage,
//        /// <summary>
//        /// Custom Content string as signature appearance.
//        /// </summary>
//        CustomContent,
//        /// <summary>
//        /// Advanced signature appearance will contain background image and left and right side text.
//        /// </summary>
//        Advanced
//    }

    public enum Status {
        Failure(0),
        Success(1);
        
        private final int val;
        private Status(int val) {
            this.val = val;
        }
        public int getVal(){
            return val;
        }
    }
    
    public enum ImageType{
        SVG, Other
    }
    
//    public enum Coordinates {
//        /// <summary>
//        /// Makes signature appearance to be on Top and Left corner of Page.
//        /// </summary>
//         Top_Left(1),
//        /// <summary>
//        /// Makes signature appearance to be on Top and Center corner of Page.
//        /// </summary>
//       
//        Top_Center(2),
//        /// <summary>
//        /// Makes signature appearance to be on Top and Right corner of Page.
//        /// </summary>
//       
//        Top_Right(3),
//        /// <summary>
//        /// Makes signature appearance to be on Middle and Left corner of Page.
//        /// </summary>
//        
//        Middle_Left(4),
//        /// <summary>
//        /// Makes signature appearance to be on Middle and Center corner of Page.
//        /// </summary>
//        
//        Middle_Center(5),
//        /// <summary>
//        /// Makes signature appearance to be on Middle and Right corner of Page.
//        /// </summary>
//        
//        Middle_Right(6),
//        /// <summary>
//        /// Makes signature appearance to be on Bottom and Left corner of Page.
//        /// </summary>
//        Bottom_Left(7),
//        /// <summary>
//        /// Makes signature appearance to be on Bottom and Center corner of Page.
//        /// </summary>
//       
//        Bottom_Center(8),
//        /// <summary>
//        /// Makes signature appearance to be on Bottom and Right corner of Page.
//        /// </summary>
//        
//        Bottom_Right(9);
//
//        private final int val;
//
//        private Coordinates(int val) {
//            this.val = val;
//        }
//        public int getVal(){
//            return val;
//        }
//    }

//    public enum Page {
//        /// <summary>
//        /// Makes signature appearance to be on First Page of PDF.
//        /// </summary>
//        FIRST,
//        /// <summary>
//        /// Makes signature appearance to be on LAST Page of PDF.
//        /// </summary>
//        LAST,
//        /// <summary>
//        /// Makes signature appearance to be on EVEN Page of PDF.
//        /// </summary>
//        EVEN,
//        /// <summary>
//        /// Makes signature appearance to be on ODD Page of PDF.
//        /// </summary>
//        ODD,
//         /// <summary>
//        /// Makes signature appearance to be on ALL Page of PDF.
//        /// </summary>
//        ALL,
//        /// <summary>
//        /// Makes signature appearance to be on specify Page of PDF.
//        /// </summary>
//        SPECIFY,
//        /// <summary>
//        /// Makes signature appearance to be on pages of PDF.
//        /// </summary>
//        PAGE_LEVEL
//    }

//    public enum LogType {
//        /// <summary>
//        /// No Logs will be written 
//        /// </summary>
//        NoLog,
//        /// <summary>
//        /// No Debug Logs will be written 
//        /// </summary>
//        NoDebugLog,
//        /// <summary>
//        /// ALL Logs will be written 
//        /// </summary>
//        AllLog
//    }
//    
    public enum DocumentType{
        PDF, Other
    }
}
