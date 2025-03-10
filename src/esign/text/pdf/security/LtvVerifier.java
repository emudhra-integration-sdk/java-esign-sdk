package esign.text.pdf.security;

import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import esign.text.pdf.AcroFields;
import esign.text.pdf.PRStream;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.emcastle.cert.ocsp.BasicOCSPResp;
import org.emcastle.cert.ocsp.OCSPException;
import org.emcastle.cert.ocsp.OCSPResp;

public class LtvVerifier
        extends RootStoreVerifier {

    protected static final Logger LOGGER = LoggerFactory.getLogger(LtvVerifier.class);

    protected LtvVerification.CertificateOption option = LtvVerification.CertificateOption.SIGNING_CERTIFICATE;

    protected boolean verifyRootCertificate = true;

    protected PdfReader reader;

    protected AcroFields fields;

    protected Date signDate;

    protected String signatureName;

    protected PdfPKCS7 pkcs7;

    protected boolean latestRevision = true;

    protected PdfDictionary dss;

    public LtvVerifier(PdfReader reader) throws GeneralSecurityException {
        super((CertificateVerifier) null);
        this.reader = reader;
        this.fields = reader.getAcroFields();
        List<String> names = this.fields.getSignatureNames();
        this.signatureName = names.get(names.size() - 1);
        this.signDate = new Date();
        this.pkcs7 = coversWholeDocument();
        LOGGER.info(String.format("Checking %ssignature %s", new Object[]{this.pkcs7.isTsp() ? "document-level timestamp " : "", this.signatureName}));
    }

    public void setVerifier(CertificateVerifier verifier) {
        this.verifier = verifier;
    }

    public void setCertificateOption(LtvVerification.CertificateOption option) {
        this.option = option;
    }

    public void setVerifyRootCertificate(boolean verifyRootCertificate) {
        this.verifyRootCertificate = verifyRootCertificate;
    }

    protected PdfPKCS7 coversWholeDocument() throws GeneralSecurityException {
        PdfPKCS7 pkcs7 = this.fields.verifySignature(this.signatureName);
        if (this.fields.signatureCoversWholeDocument(this.signatureName)) {
            LOGGER.info("The timestamp covers whole document.");
        } else {

            throw new VerificationException(null, "Signature doesn't cover whole document.");
        }
        if (pkcs7.verify()) {
            LOGGER.info("The signed document has not been modified.");
            return pkcs7;
        }

        throw new VerificationException(null, "The document was altered after the final signature was applied.");
    }

    public List<VerificationOK> verify(List<VerificationOK> result) throws IOException, GeneralSecurityException {
        if (result == null) {
            result = new ArrayList<VerificationOK>();
        }
        while (this.pkcs7 != null) {
            result.addAll(verifySignature());
        }
        return result;
    }

    public List<VerificationOK> verifySignature() throws GeneralSecurityException, IOException {
        LOGGER.info("Verifying signature.");
        List<VerificationOK> result = new ArrayList<VerificationOK>();

        Certificate[] chain = this.pkcs7.getSignCertificateChain();
        verifyChain(chain);

        int total = 1;
        if (LtvVerification.CertificateOption.WHOLE_CHAIN.equals(this.option)) {
            total = chain.length;
        }

        for (int i = 0; i < total;) {

            X509Certificate signCert = (X509Certificate) chain[i++];

            X509Certificate issuerCert = null;
            if (i < chain.length) {
                issuerCert = (X509Certificate) chain[i];
            }
            LOGGER.info(signCert.getSubjectDN().getName());
            List<VerificationOK> list = verify(signCert, issuerCert, this.signDate);
            if (list.size() == 0) {
                try {
                    signCert.verify(signCert.getPublicKey());
                    if (this.latestRevision && chain.length > 1) {
                        list.add(new VerificationOK(signCert, (Class) getClass(), "Root certificate in final revision"));
                    }
                    if (list.size() == 0 && this.verifyRootCertificate) {
                        throw new GeneralSecurityException();
                    }
                    if (chain.length > 1) {
                        list.add(new VerificationOK(signCert, (Class) getClass(), "Root certificate passed without checking"));
                    }
                } catch (GeneralSecurityException e) {
                    throw new VerificationException(signCert, "Couldn't verify with CRL or OCSP or trusted anchor");
                }
            }
            result.addAll(list);
        }

        switchToPreviousRevision();
        return result;
    }

    public void verifyChain(Certificate[] chain) throws GeneralSecurityException {
        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = (X509Certificate) chain[i];

            cert.checkValidity(this.signDate);

            if (i > 0) {
                chain[i - 1].verify(chain[i].getPublicKey());
            }
        }
        LOGGER.info("All certificates are valid on " + this.signDate.toString());
    }

    public List<VerificationOK> verify(X509Certificate signCert, X509Certificate issuerCert, Date signDate) throws GeneralSecurityException, IOException {
        RootStoreVerifier rootStoreVerifier = new RootStoreVerifier(this.verifier);
        rootStoreVerifier.setRootStore(this.rootStore);

        CRLVerifier crlVerifier = new CRLVerifier(rootStoreVerifier, getCRLsFromDSS());
        crlVerifier.setRootStore(this.rootStore);
        crlVerifier.setOnlineCheckingAllowed((this.latestRevision || this.onlineCheckingAllowed));

        OCSPVerifier ocspVerifier = new OCSPVerifier(crlVerifier, getOCSPResponsesFromDSS());
        ocspVerifier.setRootStore(this.rootStore);
        ocspVerifier.setOnlineCheckingAllowed((this.latestRevision || this.onlineCheckingAllowed));

        return ocspVerifier.verify(signCert, issuerCert, signDate);
    }

    public void switchToPreviousRevision() throws IOException, GeneralSecurityException {
        LOGGER.info("Switching to previous revision.");
        this.latestRevision = false;
        this.dss = this.reader.getCatalog().getAsDict(PdfName.DSS);
        Calendar cal = this.pkcs7.getTimeStampDate();
        if (cal == null) {
            cal = this.pkcs7.getSignDate();
        }
        this.signDate = cal.getTime();
        List<String> names = this.fields.getSignatureNames();
        if (names.size() > 1) {
            this.signatureName = names.get(names.size() - 2);
            this.reader = new PdfReader(this.fields.extractRevision(this.signatureName));
            this.fields = this.reader.getAcroFields();
            names = this.fields.getSignatureNames();
            this.signatureName = names.get(names.size() - 1);
            this.pkcs7 = coversWholeDocument();
            LOGGER.info(String.format("Checking %ssignature %s", new Object[]{this.pkcs7.isTsp() ? "document-level timestamp " : "", this.signatureName}));
        } else {

            LOGGER.info("No signatures in revision");
            this.pkcs7 = null;
        }
    }

    public List<X509CRL> getCRLsFromDSS() throws GeneralSecurityException, IOException {
        List<X509CRL> crls = new ArrayList<X509CRL>();
        if (this.dss == null) {
            return crls;
        }
        PdfArray crlarray = this.dss.getAsArray(PdfName.CRLS);
        if (crlarray == null) {
            return crls;
        }
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        for (int i = 0; i < crlarray.size(); i++) {
            PRStream stream = (PRStream) crlarray.getAsStream(i);
            X509CRL crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(PdfReader.getStreamBytes(stream)));
            crls.add(crl);
        }
        return crls;
    }

    public List<BasicOCSPResp> getOCSPResponsesFromDSS() throws IOException, GeneralSecurityException {
        List<BasicOCSPResp> ocsps = new ArrayList<BasicOCSPResp>();
        if (this.dss == null) {
            return ocsps;
        }
        PdfArray ocsparray = this.dss.getAsArray(PdfName.OCSPS);
        if (ocsparray == null) {
            return ocsps;
        }
        for (int i = 0; i < ocsparray.size(); i++) {
            PRStream stream = (PRStream) ocsparray.getAsStream(i);
            OCSPResp ocspResponse = new OCSPResp(PdfReader.getStreamBytes(stream));
            if (ocspResponse.getStatus() == 0) {
                try {
                    ocsps.add((BasicOCSPResp) ocspResponse.getResponseObject());
                } catch (OCSPException e) {
                    throw new GeneralSecurityException(e);
                }
            }
        }
        return ocsps;
    }
}
