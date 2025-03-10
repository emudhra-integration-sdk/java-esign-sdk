package esign.text.pdf.security;

import esign.text.DocumentException;
import esign.text.Rectangle;
import esign.text.pdf.PdfDeveloperExtension;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfSignature;
import esign.text.pdf.PdfSignatureAppearance;
import esign.text.pdf.PdfString;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;

public class LtvTimestamp {

    public static void timestamp(PdfSignatureAppearance sap, TSAClient tsa, String signatureName) throws IOException, DocumentException, GeneralSecurityException {
        byte[] tsToken;
        int contentEstimated = tsa.getTokenSizeEstimate();
        sap.addDeveloperExtension(PdfDeveloperExtension.ESIC_1_7_EXTENSIONLEVEL5);
        sap.setVisibleSignature(new Rectangle(0.0F, 0.0F, 0.0F, 0.0F), 1, signatureName);

        PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, PdfName.ETSI_RFC3161);
        dic.put(PdfName.TYPE, (PdfObject) PdfName.DOCTIMESTAMP);
        sap.setCryptoDictionary((PdfDictionary) dic);

        HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
        exc.put(PdfName.CONTENTS, new Integer(contentEstimated * 2 + 2));
        sap.preClose(exc);
        InputStream data = sap.getRangeStream();
        MessageDigest messageDigest = tsa.getMessageDigest();
        byte[] buf = new byte[4096];
        int n;
        while ((n = data.read(buf)) > 0) {
            messageDigest.update(buf, 0, n);
        }
        byte[] tsImprint = messageDigest.digest();

        try {
            tsToken = tsa.getTimeStampToken(tsImprint);
        } catch (Exception e) {
            throw new GeneralSecurityException(e);
        }

        if (contentEstimated + 2 < tsToken.length) {
            throw new IOException("Not enough space");
        }
        byte[] paddedSig = new byte[contentEstimated];
        System.arraycopy(tsToken, 0, paddedSig, 0, tsToken.length);

        PdfDictionary dic2 = new PdfDictionary();
        dic2.put(PdfName.CONTENTS, (PdfObject) (new PdfString(paddedSig)).setHexWriting(true));
        sap.close(dic2);
    }
}
