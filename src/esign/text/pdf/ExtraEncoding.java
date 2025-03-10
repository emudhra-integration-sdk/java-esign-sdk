package esign.text.pdf;

public interface ExtraEncoding {

    byte[] charToByte(String paramString1, String paramString2);

    byte[] charToByte(char paramChar, String paramString);

    String byteToChar(byte[] paramArrayOfbyte, String paramString);
}
