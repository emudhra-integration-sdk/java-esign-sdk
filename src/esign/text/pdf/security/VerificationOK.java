package esign.text.pdf.security;

import java.security.cert.X509Certificate;

public class VerificationOK {

    protected X509Certificate certificate;
    protected Class<? extends CertificateVerifier> verifierClass;
    protected String message;

    public VerificationOK(X509Certificate certificate, Class<? extends CertificateVerifier> verifierClass, String message) {
        this.certificate = certificate;
        this.verifierClass = verifierClass;
        this.message = message;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.certificate != null) {
            sb.append(this.certificate.getSubjectDN().getName());
            sb.append(" verified with ");
        }
        sb.append(this.verifierClass.getName());
        sb.append(": ");
        sb.append(this.message);
        return sb.toString();
    }
}
