package esign.text.pdf.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public interface ExternalDigest {
  MessageDigest getMessageDigest(String paramString) throws GeneralSecurityException;
}
