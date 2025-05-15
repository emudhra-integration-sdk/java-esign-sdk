
import com.emudhra.esign.eSign;
import com.emudhra.esign.eSignInput;
import com.emudhra.esign.eSignInputBuilder;
import com.emudhra.esign.eSignServiceReturn;
import esign.text.pdf.codec.Base64;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class Test {
    public static void main(String[] args) throws IOException {
         String   appearenceContent = "signed by B S Abhishek";
        eSign.eSignAPIVersion type = eSign.eSignAPIVersion.V2;

        // Configurations
        String tempFolder = "D:\\env\\eSign\\Temp";
        String licenceFilePath = "D:\\env\\eSign\\Qa.lic";
        String pdfURL = "https://";
        String resposeURL = (type == eSign.eSignAPIVersion.V2) ? "" : "";
        String redirectUrl = "";
        String pdfPath_1 = "D:\\3test pdfs\\Test.pdf";

//        Initializing eSign object

//String licenceFilePath, String pfxpath, String password, String pfxAlias, boolean proxyreq,
//            String proxyIp, int proxyPort, int sessionTimeout, eSignSettings.LogType logType, String ProxyUserID, String ProxyUserPassword, String pdfViewerLicence
//has context menu
//        eSign eSignObj = new eSign(licenceFilePath, "D:\\env\\eSign\\API.pfx", "Q1w2e3/", "CTCAG2", false, 
//                "43.205.25.216", 443);
//        eSign eSignObj = new eSign(licenceFilePath, "D:\\env\\eSign\\API.pfx", "Q1w2e3/", "CTCAG2", 21000);
//        eSign eSignObj = new eSign(licenceFilePath, pfxPath, pfxPassword, pfxAlias, false,
//                "", 0, 0, eSignSettings.LogType.AllLog);


        byte[] array_1 = Files.readAllBytes(new File(pdfPath_1).toPath());
//        String pdfBase64_1 = Base64.encodeBytes(array_1);
        
        byte[] image = Files.readAllBytes(new File("D:\\env\\RSDS\\image.jpg").toPath());
        String imageBase64 = Base64.encodeBytes(image);
        eSignInput bulider = eSignInputBuilder.init()
                .setDocBase64(Base64.encodeBytes(array_1))
                .setPdfPassword("a")
                .setAppearanceType(eSign.AppearanceType.StandardSignature)
//                .setSignatureImage(imageBase64)
                .setDocInfo("test")
                .setDocURL(pdfURL)
                .setLocation("test")
                .setReason("test")
                .setSignedBy("Abhishek BS")                
                .setCoSign(true)
                .setPageTobeSigned(eSign.PageTobeSigned.Last)
                .setPageLevelCoordinates("1-100,100,50,200;2-100,100,50,200;")
                                .setCoordinates(eSign.Coordinates.BottomLeft)
                .isRightOrigin(true)
//                .setTickRequired(true)
                .setBorderRequired(true)
                //                .setAppearanceText("test")
                //                .setContentSearch(contentSearch)
//                .setOneLiner("hi")
                .build();

//            eSignInput eSignInput1 = new eSignInput(pdfBase64_1, "INFO 1", pdfURL, "Bengaluru", "Test Signing", "", true, pageLevelCoordinates, appearenceContent);
//        eSignInput eSignInput1 = new eSignInput(pdfBase64_1, "test", pdfURL, "Bengaluru", "Test Signing", "", true, "1-50,15,250,43", appearenceContent);
        ArrayList<eSignInput> eSignInputs = new ArrayList<>();
        eSignInputs.add(bulider);
        eSignInputs.add(bulider);

        //To sign pdfs
//        eSignServiceReturn serviceReturn = eSignObj.getGatewayParameter(eSignInputs, "abhibs", "", "https://abhishek.free.beeceptor.com", "https://abhishek.free.beeceptor.com", tempFolder, eSign.eSignAPIVersion.V3, eSign.AuthMode.FingerPrint);

//        System.out.println(serviceReturn.getGatewayParameter());
//        System.out.println(serviceReturn.getErrorMessage());
    }
}
