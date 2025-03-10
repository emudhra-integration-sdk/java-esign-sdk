package esign.text.pdf.security;

import java.security.GeneralSecurityException;

public interface ExternalSignature {
  String getHashAlgorithm();
  
  String getEncryptionAlgorithm();
  
  byte[] sign(byte[] paramArrayOfbyte) throws GeneralSecurityException;
}


