package esign.text.pdf.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public interface TSAClient {
  int getTokenSizeEstimate();
  
  MessageDigest getMessageDigest() throws GeneralSecurityException;
  
  byte[] getTimeStampToken(byte[] paramArrayOfbyte) throws Exception;
}


