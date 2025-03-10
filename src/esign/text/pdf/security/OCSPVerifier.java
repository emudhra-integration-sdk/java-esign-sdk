package esign.text.pdf.security;

import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import org.emcastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.emcastle.cert.X509CertificateHolder;
import org.emcastle.cert.jcajce.JcaX509CertificateConverter;
import org.emcastle.cert.ocsp.BasicOCSPResp;
import org.emcastle.cert.ocsp.CertificateStatus;
import org.emcastle.cert.ocsp.OCSPException;
import org.emcastle.cert.ocsp.SingleResp;
import org.emcastle.operator.ContentVerifierProvider;
import org.emcastle.operator.DigestCalculatorProvider;
import org.emcastle.operator.OperatorCreationException;
import org.emcastle.operator.bc.BcDigestCalculatorProvider;
import org.emcastle.operator.jcajce.JcaContentVerifierProviderBuilder;

public class OCSPVerifier
        extends RootStoreVerifier {

    protected static final Logger LOGGER = LoggerFactory.getLogger(OCSPVerifier.class);

    protected static final String id_kp_OCSPSigning = "1.3.6.1.5.5.7.3.9";

    protected List<BasicOCSPResp> ocsps;

    public OCSPVerifier(CertificateVerifier verifier, List<BasicOCSPResp> ocsps) {
        super(verifier);
        this.ocsps = ocsps;
    }

    public List<VerificationOK> verify(X509Certificate signCert, X509Certificate issuerCert, Date signDate) throws GeneralSecurityException, IOException {
        List<VerificationOK> result = new ArrayList<VerificationOK>();
        int validOCSPsFound = 0;

        if (this.ocsps != null) {
            for (BasicOCSPResp ocspResp : this.ocsps) {
                if (verify(ocspResp, signCert, issuerCert, signDate)) {
                    validOCSPsFound++;
                }
            }
        }
        boolean online = false;
        if (this.onlineCheckingAllowed && validOCSPsFound == 0
                && verify(getOcspResponse(signCert, issuerCert), signCert, issuerCert, signDate)) {
            validOCSPsFound++;
            online = true;
        }

        LOGGER.info("Valid OCSPs found: " + validOCSPsFound);
        if (validOCSPsFound > 0) {
            result.add(new VerificationOK(signCert, (Class) getClass(), "Valid OCSPs Found: " + validOCSPsFound + (online ? " (online)" : "")));
        }
        if (this.verifier != null) {
            result.addAll(this.verifier.verify(signCert, issuerCert, signDate));
        }
        return result;
    }

    public boolean verify(BasicOCSPResp ocspResp, X509Certificate signCert, X509Certificate issuerCert, Date signDate) throws GeneralSecurityException, IOException {
        if (ocspResp == null) {
            return false;
        }
        SingleResp[] resp = ocspResp.getResponses();
        for (int i = 0; i < resp.length; i++) {

            if (signCert.getSerialNumber().equals(resp[i].getCertID().getSerialNumber())) {
                try {

                    if (issuerCert == null) {
                        issuerCert = signCert;
                    }
                    if (!resp[i].getCertID().matchesIssuer(new X509CertificateHolder(issuerCert.getEncoded()), (DigestCalculatorProvider) new BcDigestCalculatorProvider())) {
                        LOGGER.info("OCSP: Issuers doesn't match.");

                    } else {

                        Date date = resp[i].getNextUpdate();
                        if (date == null) {
                            date = new Date(resp[i].getThisUpdate().getTime() + 180000L);
                            LOGGER.info(String.format("No 'next update' for OCSP Response; assuming %s", new Object[]{date}));
                        }
                        if (signDate.after(date)) {
                            LOGGER.info(String.format("OCSP no longer valid: %s after %s", new Object[]{signDate, date}));
                        } else {
                            Object status = resp[i].getCertStatus();
                            if (status == CertificateStatus.GOOD) {
                                isValidResponse(ocspResp, issuerCert);
                                return true;
                            }
                        }
                    }
                } catch (OCSPException oCSPException) {
                }
            }
        }
        return false;
    }

    public void isValidResponse(BasicOCSPResp ocspResp, X509Certificate issuerCert) throws GeneralSecurityException, IOException {
        X509Certificate responderCert = null;

        if (isSignatureValid(ocspResp, issuerCert)) {
            responderCert = issuerCert;
        }

        if (responderCert == null) {
            if (ocspResp.getCerts() != null) {

                X509CertificateHolder[] certs = ocspResp.getCerts();
                for (X509CertificateHolder cert : certs) {
                    X509Certificate tempCert = null;
                    try {
                        tempCert = (new JcaX509CertificateConverter()).getCertificate(cert);
                    } catch (Exception ex) {
                    }

                    List<String> keyPurposes = null;
                    try {
                        keyPurposes = tempCert.getExtendedKeyUsage();
                        if (keyPurposes != null && keyPurposes.contains("1.3.6.1.5.5.7.3.9") && isSignatureValid(ocspResp, tempCert)) {
                            responderCert = tempCert;
                            break;
                        }
                    } catch (CertificateParsingException certificateParsingException) {
                    }
                }

                if (responderCert == null) {
                    throw new VerificationException(issuerCert, "OCSP response could not be verified");
                }
            } else {

                if (this.rootStore != null) {
                    try {
                        for (Enumeration<String> aliases = this.rootStore.aliases(); aliases.hasMoreElements();) {
                            String alias = aliases.nextElement();
                            try {
                                if (!this.rootStore.isCertificateEntry(alias)) {
                                    continue;
                                }
                                X509Certificate anchor = (X509Certificate) this.rootStore.getCertificate(alias);
                                if (isSignatureValid(ocspResp, anchor)) {
                                    responderCert = anchor;
                                    break;
                                }
                            } catch (GeneralSecurityException generalSecurityException) {
                            }
                        }

                    } catch (KeyStoreException e) {
                        responderCert = null;
                    }
                }

                if (responderCert == null) {
                    throw new VerificationException(issuerCert, "OCSP response could not be verified");
                }
            }
        }

        responderCert.verify(issuerCert.getPublicKey());

        if (responderCert.getExtensionValue(OCSPObjectIdentifiers.id_pkix_ocsp_nocheck.getId()) == null) {
            CRL crl;
            try {
                crl = CertificateUtil.getCRL(responderCert);
            } catch (Exception ignored) {
                crl = null;
            }
            if (crl != null && crl instanceof X509CRL) {
                CRLVerifier crlVerifier = new CRLVerifier(null, null);
                crlVerifier.setRootStore(this.rootStore);
                crlVerifier.setOnlineCheckingAllowed(this.onlineCheckingAllowed);
                crlVerifier.verify((X509CRL) crl, responderCert, issuerCert, new Date());

                return;
            }
        }

        responderCert.checkValidity();
    }

    @Deprecated
    public boolean verifyResponse(BasicOCSPResp ocspResp, X509Certificate issuerCert) {
        try {
            isValidResponse(ocspResp, issuerCert);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isSignatureValid(BasicOCSPResp ocspResp, Certificate responderCert) {
        try {
            ContentVerifierProvider verifierProvider = (new JcaContentVerifierProviderBuilder()).setProvider("BC").build(responderCert.getPublicKey());
            return ocspResp.isSignatureValid(verifierProvider);
        } catch (OperatorCreationException e) {
            return false;
        } catch (OCSPException e) {
            return false;
        }
    }

    public BasicOCSPResp getOcspResponse(X509Certificate signCert, X509Certificate issuerCert) {
        if (signCert == null && issuerCert == null) {
            return null;
        }
        OcspClientBouncyCastle ocsp = new OcspClientBouncyCastle();
        BasicOCSPResp ocspResp = ocsp.getBasicOCSPResp(signCert, issuerCert, null);
        if (ocspResp == null) {
            return null;
        }
        SingleResp[] resp = ocspResp.getResponses();
        for (int i = 0; i < resp.length; i++) {
            Object status = resp[i].getCertStatus();
            if (status == CertificateStatus.GOOD) {
                return ocspResp;
            }
        }
        return null;
    }
}

