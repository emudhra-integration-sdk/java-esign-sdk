package esign.text.pdf.security;

import java.security.cert.X509Certificate;

public interface OcspClient {
  byte[] getEncoded(X509Certificate paramX509Certificate1, X509Certificate paramX509Certificate2, String paramString);
}


