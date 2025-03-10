package esign.text.pdf.security;

import esign.text.error_messages.MessageLocalization;
import esign.text.io.StreamUtil;
import esign.text.log.Level;
import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import esign.text.pdf.PdfEncryption;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.DEROctetString;
import org.emcastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.emcastle.asn1.x509.Extension;
import org.emcastle.asn1.x509.Extensions;
import org.emcastle.cert.X509CertificateHolder;
import org.emcastle.cert.jcajce.JcaX509CertificateHolder;
import org.emcastle.cert.ocsp.BasicOCSPResp;
import org.emcastle.cert.ocsp.CertificateID;
import org.emcastle.cert.ocsp.CertificateStatus;
import org.emcastle.cert.ocsp.OCSPException;
import org.emcastle.cert.ocsp.OCSPReq;
import org.emcastle.cert.ocsp.OCSPReqBuilder;
import org.emcastle.cert.ocsp.OCSPResp;
import org.emcastle.cert.ocsp.SingleResp;
import org.emcastle.jce.provider.emCastleProvider;
import org.emcastle.operator.OperatorException;
import org.emcastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

public class OcspClientBouncyCastle
        implements OcspClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcspClientBouncyCastle.class);

    private final OCSPVerifier verifier;

    @Deprecated
    public OcspClientBouncyCastle() {
        this.verifier = null;
    }

    public OcspClientBouncyCastle(OCSPVerifier verifier) {
        this.verifier = verifier;
    }

    public BasicOCSPResp getBasicOCSPResp(X509Certificate checkCert, X509Certificate rootCert, String url) {
        try {
            OCSPResp ocspResponse = getOcspResponse(checkCert, rootCert, url);
            if (ocspResponse == null) {
                return null;
            }
            if (ocspResponse.getStatus() != 0) {
                return null;
            }
            BasicOCSPResp basicResponse = (BasicOCSPResp) ocspResponse.getResponseObject();
            if (this.verifier != null) {
                this.verifier.isValidResponse(basicResponse, rootCert);
            }
            return basicResponse;
        } catch (Exception ex) {
            if (LOGGER.isLogging(Level.ERROR)) {
                LOGGER.error(ex.getMessage());
            }
            return null;
        }
    }

    public byte[] getEncoded(X509Certificate checkCert, X509Certificate rootCert, String url) {
        try {
            BasicOCSPResp basicResponse = getBasicOCSPResp(checkCert, rootCert, url);
            if (basicResponse != null) {
                SingleResp[] responses = basicResponse.getResponses();
                if (responses.length == 1) {
                    SingleResp resp = responses[0];
                    Object status = resp.getCertStatus();
                    if (status == CertificateStatus.GOOD) {
                        return basicResponse.getEncoded();
                    }
                    if (status instanceof org.emcastle.ocsp.RevokedStatus) {
                        throw new IOException(MessageLocalization.getComposedMessage("ocsp.status.is.revoked", new Object[0]));
                    }
                    throw new IOException(MessageLocalization.getComposedMessage("ocsp.status.is.unknown", new Object[0]));
                }

            }
        } catch (Exception ex) {
            if (LOGGER.isLogging(Level.ERROR)) {
                LOGGER.error(ex.getMessage());
            }
        }
        return null;
    }

    private static OCSPReq generateOCSPRequest(X509Certificate issuerCert, BigInteger serialNumber) throws OCSPException, IOException, OperatorException, CertificateEncodingException {
        Security.addProvider((Provider) new emCastleProvider());

        CertificateID id = new CertificateID((new JcaDigestCalculatorProviderBuilder()).build().get(CertificateID.HASH_SHA1), (X509CertificateHolder) new JcaX509CertificateHolder(issuerCert), serialNumber);

        OCSPReqBuilder gen = new OCSPReqBuilder();
        gen.addRequest(id);

        Extension ext = new Extension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false, (ASN1OctetString) new DEROctetString((new DEROctetString(PdfEncryption.createDocumentId())).getEncoded()));
        gen.setRequestExtensions(new Extensions(new Extension[]{ext}));
        return gen.build();
    }

    private OCSPResp getOcspResponse(X509Certificate checkCert, X509Certificate rootCert, String url) throws GeneralSecurityException, OCSPException, IOException, OperatorException {
        if (checkCert == null || rootCert == null) {
            return null;
        }
        if (url == null) {
            url = CertificateUtil.getOCSPURL(checkCert);
        }
        if (url == null) {
            return null;
        }
        LOGGER.info("Getting OCSP from " + url);
        OCSPReq request = generateOCSPRequest(rootCert, checkCert.getSerialNumber());
        byte[] array = request.getEncoded();
        URL urlt = new URL(url);
        HttpURLConnection con = (HttpURLConnection) urlt.openConnection();
        con.setRequestProperty("Content-Type", "application/ocsp-request");
        con.setRequestProperty("Accept", "application/ocsp-response");
        con.setDoOutput(true);
        OutputStream out = con.getOutputStream();
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out));
        dataOut.write(array);
        dataOut.flush();
        dataOut.close();
        if (con.getResponseCode() / 100 != 2) {
            throw new IOException(MessageLocalization.getComposedMessage("invalid.http.response.1", con.getResponseCode()));
        }

        InputStream in = (InputStream) con.getContent();
        return new OCSPResp(StreamUtil.inputStreamToArray(in));
    }
}
