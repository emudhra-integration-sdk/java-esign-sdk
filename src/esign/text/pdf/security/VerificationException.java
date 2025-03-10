package esign.text.pdf.security;

import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class VerificationException
        extends GeneralSecurityException {

    private static final long serialVersionUID = 2978604513926438256L;

    public VerificationException(Certificate cert, String message) {
        super(String.format("Certificate %s failed: %s", new Object[]{(cert == null) ? "Unknown" : ((X509Certificate) cert).getSubjectDN().getName(), message}));
    }
}
