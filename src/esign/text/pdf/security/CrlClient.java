package esign.text.pdf.security;

import java.security.cert.X509Certificate;
import java.util.Collection;

public interface CrlClient {
  Collection<byte[]> getEncoded(X509Certificate paramX509Certificate, String paramString);
}


