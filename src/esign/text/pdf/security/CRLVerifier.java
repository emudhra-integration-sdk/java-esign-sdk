package esign.text.pdf.security;

import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

public class CRLVerifier
        extends RootStoreVerifier {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CRLVerifier.class);

    List<X509CRL> crls;

    public CRLVerifier(CertificateVerifier verifier, List<X509CRL> crls) {
        super(verifier);
        this.crls = crls;
    }

    public List<VerificationOK> verify(X509Certificate signCert, X509Certificate issuerCert, Date signDate) throws GeneralSecurityException, IOException {
        List<VerificationOK> result = new ArrayList<VerificationOK>();
        int validCrlsFound = 0;

        if (this.crls != null) {
            for (X509CRL crl : this.crls) {
                if (verify(crl, signCert, issuerCert, signDate)) {
                    validCrlsFound++;
                }
            }
        }
        boolean online = false;
        if (this.onlineCheckingAllowed && validCrlsFound == 0
                && verify(getCRL(signCert, issuerCert), signCert, issuerCert, signDate)) {
            validCrlsFound++;
            online = true;
        }

        LOGGER.info("Valid CRLs found: " + validCrlsFound);
        if (validCrlsFound > 0) {
            result.add(new VerificationOK(signCert, (Class) getClass(), "Valid CRLs found: " + validCrlsFound + (online ? " (online)" : "")));
        }
        if (this.verifier != null) {
            result.addAll(this.verifier.verify(signCert, issuerCert, signDate));
        }
        return result;
    }

    public boolean verify(X509CRL crl, X509Certificate signCert, X509Certificate issuerCert, Date signDate) throws GeneralSecurityException {
        if (crl == null || signDate == null) {
            return false;
        }
        if (crl.getIssuerX500Principal().equals(signCert.getIssuerX500Principal()) && signDate
                .after(crl.getThisUpdate()) && signDate.before(crl.getNextUpdate())) {

            if (isSignatureValid(crl, issuerCert) && crl.isRevoked(signCert)) {
                throw new VerificationException(signCert, "The certificate has been revoked.");
            }
            return true;
        }
        return false;
    }

    public X509CRL getCRL(X509Certificate signCert, X509Certificate issuerCert) {
        if (issuerCert == null) {
            issuerCert = signCert;
        }
        try {
            String crlurl = CertificateUtil.getCRLURL(signCert);
            if (crlurl == null) {
                return null;
            }
            LOGGER.info("Getting CRL from " + crlurl);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            return (X509CRL) cf.generateCRL((new URL(crlurl)).openStream());
        } catch (IOException e) {
            return null;
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    public boolean isSignatureValid(X509CRL crl, X509Certificate crlIssuer) {
        if (crlIssuer != null) {
            try {
                crl.verify(crlIssuer.getPublicKey());
                return true;
            } catch (GeneralSecurityException e) {
                LOGGER.warn("CRL not issued by the same authority as the certificate that is being checked");
            }
        }

        if (this.rootStore == null) {
            return false;
        }
        try {
            for (Enumeration<String> aliases = this.rootStore.aliases(); aliases.hasMoreElements();) {
                String alias = aliases.nextElement();
                try {
                    if (!this.rootStore.isCertificateEntry(alias)) {
                        continue;
                    }
                    X509Certificate anchor = (X509Certificate) this.rootStore.getCertificate(alias);
                    crl.verify(anchor.getPublicKey());
                    return true;
                } catch (GeneralSecurityException e) {
                }

            }

        } catch (GeneralSecurityException e) {
            return false;
        }
        return false;
    }
}
