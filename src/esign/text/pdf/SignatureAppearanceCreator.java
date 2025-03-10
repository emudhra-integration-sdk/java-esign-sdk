package esign.text.pdf;

import esign.text.BadElementException;
import esign.text.BaseColor;
import esign.text.DocumentException;
import esign.text.Element;
import esign.text.Font;
import esign.text.FontFactory;
import esign.text.Image;
import esign.text.Paragraph;
import esign.text.Phrase;
import esign.text.Rectangle;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.emcastle.util.encoders.Base64;

public class SignatureAppearanceCreator {

    public static String getSignedByWithTs(String signedBy) {
        Calendar cals = Calendar.getInstance();
        SimpleDateFormat simpleformat = new SimpleDateFormat("dd-MMM-yyyy (hh:mm:ss)");
        StringBuilder sb = new StringBuilder();
        sb.append("\nSigned By: ");
        sb.append(signedBy);
        sb.append("\nDate: ");
        sb.append(simpleformat.format(cals.getTime()));
        return sb.toString();
    }

    public static PdfPTable getIconPTable(Image image) throws DocumentException {
        float[] widthOrg = {75};
        PdfPTable iconTable = new PdfPTable(1);
        iconTable.setWidthPercentage(100);
        iconTable.setTotalWidth(widthOrg);
        iconTable.setSpacingAfter(0);
        iconTable.setSpacingAfter(0);
        PdfPCell iconCell = new PdfPCell(image, true);
        iconCell.setBorder(Rectangle.NO_BORDER);
        iconCell.setVerticalAlignment(esign.text.Element.ALIGN_MIDDLE);
        iconCell.setFixedHeight(114);
        iconCell.setPadding(0);
        iconTable.addCell(iconCell);
        return iconTable;
    }

    public static PdfPTable getSigTable(Image image) throws DocumentException {

        float[] width = {215};
        PdfPTable sigTable = new PdfPTable(1);
        sigTable.setWidthPercentage(100);
        sigTable.setTotalWidth(width);
        sigTable.setSpacingAfter(0);
        sigTable.setSpacingAfter(0);
        PdfPCell cell = new PdfPCell(image, true);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(esign.text.Element.ALIGN_CENTER);
        cell.setVerticalAlignment(esign.text.Element.ALIGN_MIDDLE);
        cell.setFixedHeight(115);
        cell.setPadding(0);
        sigTable.addCell(cell);

        return sigTable;
    }

    public static void insertQR(PdfTemplate pdfTemplate, Image qrImage) throws DocumentException {
        ColumnText ctQr = new ColumnText(pdfTemplate);
        ctQr.setSimpleColumn(363, 21, 489, 147);
        qrImage.scaleAbsolute(126, 126);
        ctQr.addElement(qrImage);
        ctQr.go();
    }

    public static Image getImageInstance(String base64Image) throws BadElementException, IOException {
        byte[] decodedImageByte = Base64.decode(base64Image);
        Image image = Image.getInstance(decodedImageByte);
        image.setAlignment(Image.ALIGN_CENTER);
        return image;
    }

    public static void insertSignatureHeading(String heading, PdfTemplate pdfTemplate) throws DocumentException {
        ColumnText ctNp = new ColumnText(pdfTemplate);
        Phrase headingNp = new Phrase(heading, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, Font.NORMAL, new BaseColor(0, 0, 0)));
        headingNp.setLeading(0);
        ctNp.setSimpleColumn(headingNp, 17, 120, 300, 140, 0, esign.text.Element.ALIGN_LEFT);
        ctNp.go();
    }

//    public static void insertSignatureBody(String heading, PdfTemplate pdfTemplate, float maxWidth, float maxHeight) throws DocumentException {
//        ColumnText ctNp = new ColumnText(pdfTemplate);
//        int inputStringLength = heading.length();
//        int defaultFontSize = 30; // Updated default font size
//
//        int fontSize;
//        if (inputStringLength <= 20) {
//            fontSize = 25; // Font size set to 15 if string length is within 30
//        } else {
//            fontSize = Math.min(defaultFontSize, (int) (maxWidth / inputStringLength)); // Dynamic font size calculation
//            if (fontSize < 15) { // Minimum font size to maintain visibility
//                fontSize = 15;
//            }
//            heading = heading.substring(0, 17) + "..."; // Truncate the string to 27 characters and append "..."
//        }
//
//        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fontSize, Font.NORMAL, new BaseColor(0, 0, 0));
//        Phrase headingNp = new Phrase(heading, font);
//
//        ctNp.setSimpleColumn(headingNp, 17, 0, maxWidth, maxHeight, 0, Element.ALIGN_LEFT);
//        ctNp.go();
//    }
    public static void insertSignatureBody(String heading, PdfTemplate pdfTemplate, float maxWidth, float maxHeight) throws DocumentException {
        ColumnText ctNp = new ColumnText(pdfTemplate);
        int inputStringLength = heading.length();
        int defaultFontSize = 30; // Updated default font size

        int fontSize;
        if (inputStringLength <= 20) {
            fontSize = 25; // Font size set if string length is within 20
        } else if (inputStringLength <= 25) {
            fontSize = Math.min(defaultFontSize, (int) (maxWidth / inputStringLength)); // Dynamic font size calculation
            if (fontSize < 19) { // Minimum font size to maintain visibility
                fontSize = 19;
            }
        } else {
            fontSize = 19; // Font size set if string length exceeds 30
            heading = heading.substring(0, 25) + "..."; // Truncate the string to 27 characters and append "..."
        }

        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fontSize, Font.NORMAL, new BaseColor(0, 0, 0));
        Phrase headingNp = new Phrase(heading, font);

        ctNp.setSimpleColumn(headingNp, 17, 0, maxWidth, maxHeight, 0, Element.ALIGN_LEFT);
        ctNp.go();
    }

    public static void insertSignatureLocation(String heading, PdfTemplate pdfTemplate, float maxWidth, float maxHeight) throws DocumentException {
        ColumnText ctNp = new ColumnText(pdfTemplate);
        int fontSize = 21;
        float requiredHeight = 0;
        Phrase headingNp;
        if (heading.length() > 26) {
            heading = heading.substring(0, 23) + "...";
        }
        if (heading.length() > 15) {
            for (int i = 15; i >= 15; i--) {
                if (Character.isUpperCase(heading.charAt(i - 1))) {
                    heading = heading.substring(0, i - 1) + "...";
                    break;
                }
            }
        }

        do {
            Font font = FontFactory.getFont("Open sans", fontSize, Font.ITALIC, new BaseColor(85, 85, 85));
            headingNp = new Phrase(heading, font);
            ctNp.setSimpleColumn(headingNp, 17, 0, 300, 70, 0, Element.ALIGN_LEFT);
            ctNp.go();
            requiredHeight = ctNp.getYLine();
            fontSize--;
        } while (requiredHeight > maxHeight && fontSize > 0);
        if (requiredHeight > maxHeight) {
            heading = heading.substring(0, 23) + "...";
            Font font = FontFactory.getFont("Open sans", fontSize + 1, Font.ITALIC, new BaseColor(85, 85, 85));
            headingNp = new Phrase(heading, font);
            ctNp.setSimpleColumn(headingNp, 17, 0, 300, 70, 0, Element.ALIGN_LEFT);
            ctNp.go();
        }
        headingNp.setLeading(0);
        ctNp.setSimpleColumn(headingNp, 17, 0, 300, 70, 0, Element.ALIGN_LEFT);
        ctNp.go();
    }

    public static void insertSignatureReason(String heading, PdfTemplate pdfTemplate, float maxWidth, float maxHeight) throws DocumentException {
        ColumnText ctNp = new ColumnText(pdfTemplate);
        int fontSize = 21;
        float requiredHeight = 0;
        Phrase headingNp;
        if (heading.length() > 26) {
            heading = heading.substring(0, 24) + "...";
        }
        if (heading.length() > 15) {
            for (int i = 15; i >= 15; i--) {
                if (Character.isUpperCase(heading.charAt(i - 1))) {
                    heading = heading.substring(0, i - 1) + "...";
                    break;
                }
            }
        }
        do {
            Font font = FontFactory.getFont(FontFactory.HELVETICA, fontSize, Font.ITALIC, new BaseColor(85, 85, 85));
            headingNp = new Phrase(heading, font);
            ctNp.setSimpleColumn(headingNp, 17, 0, 300, 44, 0, Element.ALIGN_LEFT);
            ctNp.go();
            requiredHeight = ctNp.getYLine();
            fontSize--;
        } while (requiredHeight > maxHeight && fontSize > 0);

        if (requiredHeight > maxHeight) {
            heading = heading.substring(0, 23) + "...";
            Font font = FontFactory.getFont(FontFactory.HELVETICA, fontSize + 1, Font.ITALIC, new BaseColor(85, 85, 85));
            headingNp = new Phrase(heading, font);
            ctNp.setSimpleColumn(headingNp, 17, 0, 300, 44, 0, Element.ALIGN_LEFT);
            ctNp.go();
        }
        headingNp.setLeading(0);
        ctNp.setSimpleColumn(headingNp, 17, 0, 300, 44, 0, Element.ALIGN_LEFT);
        ctNp.go();
    }

    public static void insertTimeStamp(PdfTemplate pdfTemplate) throws DocumentException {
        Calendar cals = Calendar.getInstance();
        SimpleDateFormat simpleformat = new SimpleDateFormat("dd-MMM-yyyy");
        Phrase timeStamPhrase = new Phrase("Date: " + simpleformat.format(cals.getTime()), FontFactory.getFont(FontFactory.HELVETICA, 21, Font.ITALIC, new BaseColor(85, 85, 85)));
        ColumnText timeStampColumnText = new ColumnText(pdfTemplate);
        timeStampColumnText.setSimpleColumn(timeStamPhrase, 17, 9, 250, 13, 0, esign.text.Element.ALIGN_LEFT);
        timeStampColumnText.go();
    }

    public static PdfTemplate createSignatureAppearance(PdfStamper stamper, String signedBy, String reason, String location) {
        Font regularFont = new Font(Font.FontFamily.HELVETICA, 15, Font.NORMAL);
        PdfTemplate templateTemp = null;
        try {
            String SpecialSignatureBase64 = "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";
            Image SpecialSignatureIcon = getImageInstance(SpecialSignatureBase64);
            templateTemp = PdfTemplate.createTemplate(stamper.getWriter(), 363, 168);
            float templateWidth = templateTemp.getWidth();

            insertSignatureHeading("Digitally Signed by:", templateTemp);
            insertSignatureBody(signedBy, templateTemp, templateWidth, 103);
            insertSignatureLocation(location, templateTemp, templateWidth, 103);
            insertSignatureReason(reason, templateTemp, templateWidth, 103);

            ColumnText ctSS = new ColumnText(templateTemp);
            ctSS.setSimpleColumn(225, 41, 585, 120);
            ctSS.addElement(getIconPTable(SpecialSignatureIcon));
            ctSS.go();

            if (!SpecialSignatureBase64.equals("")) {
                ctSS.setSimpleColumn(265, 5 + (2 * 0.14f), 385, 90);
                PdfTemplate templateForSplSig = PdfTemplate.createTemplate(stamper.getWriter(), 215, 163 - (163 * 0.2f));
                ColumnText innerColumnText = new ColumnText(templateForSplSig);
                innerColumnText.setSimpleColumn(0, 0, 215, 163 - (163 * 0.2f));
                innerColumnText.addElement(getSigTable(getImageInstance(SpecialSignatureBase64)));
                innerColumnText.go();

                Image innerTemplateImage = Image.getInstance(templateForSplSig);
                ctSS.addElement(innerTemplateImage);
                ctSS.go();
                insertTimeStamp(templateTemp);
            } else {
                ctSS.setSimpleColumn(20, 15, 238, 128);
                Paragraph signedByParagraph = new Paragraph(getSignedByWithTs(signedBy), regularFont);
                signedByParagraph.setIndentationLeft(10);
                ctSS.addElement(signedByParagraph);
            }
            ctSS.go();
        } catch (Exception ex) {
            return null;
        }
        return templateTemp;
    }
}
