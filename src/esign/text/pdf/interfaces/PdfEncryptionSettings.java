package esign.text.pdf.interfaces;

import esign.text.DocumentException;
import java.security.cert.Certificate;

public interface PdfEncryptionSettings {

    void setEncryption(byte[] paramArrayOfbyte1, byte[] paramArrayOfbyte2, int paramInt1, int paramInt2) throws DocumentException;

    void setEncryption(Certificate[] paramArrayOfCertificate, int[] paramArrayOfint, int paramInt) throws DocumentException;
}
