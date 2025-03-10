package esign.text.pdf;

import esign.text.DocumentException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.util.HashMap;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.Recipient;
import org.emcastle.cms.RecipientInformation;
import org.emcastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.emcastle.cms.jcajce.JceKeyTransRecipient;

public final class PdfEncryptor {

    public static void encrypt(PdfReader reader, OutputStream os, byte[] userPassword, byte[] ownerPassword, int permissions, boolean strength128Bits) throws DocumentException, IOException {
        PdfStamper stamper = new PdfStamper(reader, os);
        stamper.setEncryption(userPassword, ownerPassword, permissions, strength128Bits);
        stamper.close();
    }

    public static void encrypt(PdfReader reader, OutputStream os, byte[] userPassword, byte[] ownerPassword, int permissions, boolean strength128Bits, HashMap<String, String> newInfo) throws DocumentException, IOException {
        PdfStamper stamper = new PdfStamper(reader, os);
        stamper.setEncryption(userPassword, ownerPassword, permissions, strength128Bits);
        stamper.setMoreInfo(newInfo);
        stamper.close();
    }

    public static void encrypt(PdfReader reader, OutputStream os, boolean strength, String userPassword, String ownerPassword, int permissions) throws DocumentException, IOException {
        PdfStamper stamper = new PdfStamper(reader, os);
        stamper.setEncryption(strength, userPassword, ownerPassword, permissions);
        stamper.close();
    }

    public static void encrypt(PdfReader reader, OutputStream os, boolean strength, String userPassword, String ownerPassword, int permissions, HashMap<String, String> newInfo) throws DocumentException, IOException {
        PdfStamper stamper = new PdfStamper(reader, os);
        stamper.setEncryption(strength, userPassword, ownerPassword, permissions);
        stamper.setMoreInfo(newInfo);
        stamper.close();
    }

    public static void encrypt(PdfReader reader, OutputStream os, int type, String userPassword, String ownerPassword, int permissions, HashMap<String, String> newInfo) throws DocumentException, IOException {
        PdfStamper stamper = new PdfStamper(reader, os);
        stamper.setEncryption(type, userPassword, ownerPassword, permissions);
        stamper.setMoreInfo(newInfo);
        stamper.close();
    }

    public static void encrypt(PdfReader reader, OutputStream os, int type, String userPassword, String ownerPassword, int permissions) throws DocumentException, IOException {
        PdfStamper stamper = new PdfStamper(reader, os);
        stamper.setEncryption(type, userPassword, ownerPassword, permissions);
        stamper.close();
    }

    public static String getPermissionsVerbose(int permissions) {
        StringBuffer buf = new StringBuffer("Allowed:");
        if ((0x804 & permissions) == 2052) {
            buf.append(" Printing");
        }
        if ((0x8 & permissions) == 8) {
            buf.append(" Modify contents");
        }
        if ((0x10 & permissions) == 16) {
            buf.append(" Copy");
        }
        if ((0x20 & permissions) == 32) {
            buf.append(" Modify annotations");
        }
        if ((0x100 & permissions) == 256) {
            buf.append(" Fill in");
        }
        if ((0x200 & permissions) == 512) {
            buf.append(" Screen readers");
        }
        if ((0x400 & permissions) == 1024) {
            buf.append(" Assembly");
        }
        if ((0x4 & permissions) == 4) {
            buf.append(" Degraded printing");
        }
        return buf.toString();
    }

    public static boolean isPrintingAllowed(int permissions) {
        return ((0x804 & permissions) == 2052);
    }

    public static boolean isModifyContentsAllowed(int permissions) {
        return ((0x8 & permissions) == 8);
    }

    public static boolean isCopyAllowed(int permissions) {
        return ((0x10 & permissions) == 16);
    }

    public static boolean isModifyAnnotationsAllowed(int permissions) {
        return ((0x20 & permissions) == 32);
    }

    public static boolean isFillInAllowed(int permissions) {
        return ((0x100 & permissions) == 256);
    }

    public static boolean isScreenReadersAllowed(int permissions) {
        return ((0x200 & permissions) == 512);
    }

    public static boolean isAssemblyAllowed(int permissions) {
        return ((0x400 & permissions) == 1024);
    }

    public static boolean isDegradedPrintingAllowed(int permissions) {
        return ((0x4 & permissions) == 4);
    }

    public static byte[] getContent(RecipientInformation recipientInfo, PrivateKey certificateKey, String certificateKeyProvider) throws CMSException {
        JceKeyTransRecipient jceKeyTransRecipient = (new JceKeyTransEnvelopedRecipient(certificateKey)).setProvider(certificateKeyProvider);
        return recipientInfo.getContent((Recipient) jceKeyTransRecipient);
    }
}
