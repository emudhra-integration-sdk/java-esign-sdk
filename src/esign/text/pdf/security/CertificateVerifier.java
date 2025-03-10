package esign.text.pdf.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CertificateVerifier {

    protected CertificateVerifier verifier;
    protected boolean onlineCheckingAllowed = true;

    public CertificateVerifier(CertificateVerifier verifier) {
        this.verifier = verifier;
    }

    public void setOnlineCheckingAllowed(boolean onlineCheckingAllowed) {
        this.onlineCheckingAllowed = onlineCheckingAllowed;
    }

    public List<VerificationOK> verify(X509Certificate signCert, X509Certificate issuerCert, Date signDate) throws GeneralSecurityException, IOException {
        if (signDate != null) {
            signCert.checkValidity(signDate);
        }
        if (issuerCert != null) {
            signCert.verify(issuerCert.getPublicKey());
        } else {

            signCert.verify(signCert.getPublicKey());
        }
        List<VerificationOK> result = new ArrayList<VerificationOK>();
        if (this.verifier != null) {
            result.addAll(this.verifier.verify(signCert, issuerCert, signDate));
        }
        return result;
    }
}
