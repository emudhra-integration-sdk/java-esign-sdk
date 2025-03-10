package esign.text.pdf.security;

import esign.text.pdf.PdfDictionary;
import java.io.InputStream;
import java.security.GeneralSecurityException;

public interface ExternalSignatureContainer {
  byte[] sign(InputStream paramInputStream) throws GeneralSecurityException;
  
  void modifySigningDictionary(PdfDictionary paramPdfDictionary);
}


