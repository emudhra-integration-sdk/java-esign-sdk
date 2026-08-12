package com.emudhra.esign;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import org.emcastle.asn1.x500.RDN;
import org.emcastle.asn1.x500.X500Name;
import org.emcastle.asn1.x500.style.BCStyle;
import org.emcastle.asn1.x500.style.IETFUtils;
import org.emcastle.asn1.x509.X509CertificateStructure;

/**
 * Details of the signer certificate the eSign gateway returns in
 * {@code UserX509Certificate}.
 *
 * <p>Populated on {@link eSignServiceReturn#getSignerCertificateInfo()} after
 * {@code getSigedDocument}. It is {@code null} when the gateway response
 * carried no certificate, or when the certificate could not be parsed.
 */
public final class SignerCertificateInfo {

    private String subjectCommonName;
    private String aadhaarNumber;
    private String subjectDN;
    private String issuerCommonName;
    private String issuerDN;
    private String serialNumber;
    private Date notBefore;
    private Date notAfter;
    private String signatureAlgorithm;
    private String publicKeyAlgorithm;
    private int keySize;
    private String sha256Thumbprint;
    private String certificateBase64;

    SignerCertificateInfo() {
    }

    /**
     * Parses a Base64 DER certificate. Returns {@code null} rather than throwing —
     * certificate details are supplementary, and a parse failure must never fail
     * an otherwise successful signing.
     */
    protected static SignerCertificateInfo fromBase64(String certBase64) {
        if (certBase64 == null || certBase64.trim().isEmpty()) {
            return null;
        }
        try {
            String trimmed = certBase64.trim();
            byte[] der = org.emcastle.util.encoders.Base64.decode(trimmed);

            SignerCertificateInfo info = new SignerCertificateInfo();
            info.certificateBase64 = trimmed;

            X509Certificate cert = (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));

            info.subjectDN = cert.getSubjectX500Principal().getName();
            info.issuerDN = cert.getIssuerX500Principal().getName();
            info.serialNumber = cert.getSerialNumber().toString(16).toUpperCase();
            info.notBefore = cert.getNotBefore();
            info.notAfter = cert.getNotAfter();
            info.signatureAlgorithm = cert.getSigAlgName();

            PublicKey key = cert.getPublicKey();
            if (key != null) {
                info.publicKeyAlgorithm = key.getAlgorithm();
                if (key instanceof RSAPublicKey) {
                    info.keySize = ((RSAPublicKey) key).getModulus().bitLength();
                } else if (key instanceof ECPublicKey) {
                    info.keySize = ((ECPublicKey) key).getParams().getCurve().getField().getFieldSize();
                }
            }
            info.sha256Thumbprint = toHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));

            // CN and the Title OID (2.5.4.12), where eMudhra stores the Aadhaar digits
            X500Name subject = X509CertificateStructure
                    .getInstance(org.emcastle.asn1.ASN1Primitive.fromByteArray(der))
                    .getSubject();
            info.subjectCommonName = firstRDN(subject, BCStyle.CN);
            info.aadhaarNumber = firstRDN(subject, BCStyle.T);

            X500Name issuer = X509CertificateStructure
                    .getInstance(org.emcastle.asn1.ASN1Primitive.fromByteArray(der))
                    .getIssuer();
            info.issuerCommonName = firstRDN(issuer, BCStyle.CN);

            return info;
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstRDN(X500Name name, org.emcastle.asn1.ASN1ObjectIdentifier oid) {
        try {
            RDN[] rdns = name.getRDNs(oid);
            if (rdns != null && rdns.length > 0) {
                return IETFUtils.valueToString(rdns[0].getFirst().getValue());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /** CN of the signer, i.e. the Aadhaar name held in the certificate. */
    public String getSubjectCommonName() { return subjectCommonName; }

    /** Aadhaar digits from the certificate Title OID (2.5.4.12); null when absent. */
    public String getAadhaarNumber() { return aadhaarNumber; }

    public String getSubjectDN() { return subjectDN; }
    public String getIssuerCommonName() { return issuerCommonName; }
    public String getIssuerDN() { return issuerDN; }
    /** Certificate serial number in uppercase hex. */
    public String getSerialNumber() { return serialNumber; }
    public Date getNotBefore() { return notBefore; }
    public Date getNotAfter() { return notAfter; }
    public String getSignatureAlgorithm() { return signatureAlgorithm; }
    public String getPublicKeyAlgorithm() { return publicKeyAlgorithm; }
    /** Key size in bits; 0 when the algorithm is neither RSA nor EC. */
    public int getKeySize() { return keySize; }
    /** SHA-256 fingerprint of the DER certificate, uppercase hex. */
    public String getSha256Thumbprint() { return sha256Thumbprint; }
    public String getCertificateBase64() { return certificateBase64; }
}
