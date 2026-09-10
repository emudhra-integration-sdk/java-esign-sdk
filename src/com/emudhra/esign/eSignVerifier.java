package com.emudhra.esign;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.emcastle.asn1.ASN1ObjectIdentifier;
import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.ASN1Primitive;
import org.emcastle.asn1.ASN1String;
import org.emcastle.asn1.cms.Attribute;
import org.emcastle.asn1.cms.AttributeTable;
import org.emcastle.asn1.cms.CMSAttributes;
import org.emcastle.asn1.cms.Time;
import org.emcastle.asn1.x509.AccessDescription;
import org.emcastle.asn1.x509.AuthorityInformationAccess;
import org.emcastle.asn1.x509.CRLDistPoint;
import org.emcastle.asn1.x509.DistributionPoint;
import org.emcastle.asn1.x509.DistributionPointName;
import org.emcastle.asn1.x509.GeneralName;
import org.emcastle.asn1.x509.GeneralNames;
import org.emcastle.cert.X509CertificateHolder;
import org.emcastle.cert.jcajce.JcaX509CertificateConverter;
import org.emcastle.cert.ocsp.BasicOCSPResp;
import org.emcastle.cert.ocsp.CertificateID;
import org.emcastle.cert.ocsp.CertificateStatus;
import org.emcastle.cert.ocsp.OCSPReq;
import org.emcastle.cert.ocsp.OCSPReqBuilder;
import org.emcastle.cert.ocsp.OCSPResp;
import org.emcastle.cert.ocsp.RevokedStatus;
import org.emcastle.cert.ocsp.SingleResp;
import org.emcastle.cert.ocsp.jcajce.JcaCertificateID;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.CMSProcessableByteArray;
import org.emcastle.cms.CMSSignedData;
import org.emcastle.cms.SignerInformation;
import org.emcastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.emcastle.jce.provider.emCastleProvider;
import org.emcastle.operator.DigestCalculatorProvider;
import org.emcastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.emcastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.emcastle.util.Store;

/**
 * Verifies detached PKCS#7 / CMS signatures - typically the {@code signedData} returned
 * by hash-based eSign - against either the original data or just the SHA-256 that was
 * sent for signing, and checks the signer certificate for revocation (OCSP first, CRL
 * as fallback). Uses only the crypto provider bundled with the SDK.
 *
 * <p>All methods are non-throwing; problems are reported in the result.
 */
public final class eSignVerifier {

    private static final String OID_SHA1 = "1.3.14.3.2.26";
    private static final String OID_SHA256 = "2.16.840.1.101.3.4.2.1";
    private static final String OID_SHA384 = "2.16.840.1.101.3.4.2.2";
    private static final String OID_SHA512 = "2.16.840.1.101.3.4.2.3";
    private static final String OID_EXT_AIA = "1.3.6.1.5.5.7.1.1";
    private static final String OID_EXT_CRL_DP = "2.5.29.31";
    private static final ASN1ObjectIdentifier AD_OCSP = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.48.1");
    private static final ASN1ObjectIdentifier AD_CA_ISSUERS = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.48.2");
    // eMudhra's UAT OCSP responder has been measured taking ~20 s to answer, hence the generous read timeout.
    private static volatile int connectTimeoutMs = 15000;
    private static volatile int readTimeoutMs = 45000;

    private static final Provider EMC = new emCastleProvider();

    static {
        if (Security.getProvider(EMC.getName()) == null) {
            Security.addProvider(EMC);
        }
    }

    private eSignVerifier() {
    }

    /** Network timeouts (milliseconds) used for OCSP, CRL and CA-issuer downloads. Defaults: 15 s connect, 45 s read. */
    public static void setHttpTimeouts(int connectMillis, int readMillis) {
        if (connectMillis > 0) connectTimeoutMs = connectMillis;
        if (readMillis > 0) readTimeoutMs = readMillis;
    }

    /**
     * Verifies the PKCS#7 for {@code sha256Hex}, taking either the gateway's EsignResp XML or a
     * bare Base64/PEM PKCS#7. Entry point behind {@link eSign#verifyEsignResponseHash}.
     */
    static eSignVerificationResult verifyResponseHash(String sha256Hex, String esignRespXmlOrPkcs7, boolean checkRevocation) {
        return doVerifyResponse(sha256Hex, esignRespXmlOrPkcs7, checkRevocation);
    }

    // ------------------------------------------------------------------ core

    private static eSignVerificationResult doVerifyResponse(String sha256Hex, String input, boolean checkRevocation) {
        eSignVerificationResult r = new eSignVerificationResult();
        String text = input == null ? "" : input.trim();
        if (text.isEmpty()) {
            r.setErrorMessage("pkcs7 / EsignResp XML is empty");
            return r;
        }
        if (!text.startsWith("<")) {
            // A bare PKCS#7 (Base64 or PEM) rather than the gateway response.
            return doVerify(null, sha256Hex, text.getBytes(java.nio.charset.StandardCharsets.US_ASCII), checkRevocation);
        }

        eSignResponseInfo info;
        try {
            info = eSignResponseInfo.parse(text);
        } catch (Exception e) {
            r.setErrorMessage("EsignResp XML could not be parsed: " + e.getMessage());
            return r;
        }
        copyResponseFields(r, info, null);

        if (!"1".equals(info.status)) {
            StringBuilder m = new StringBuilder("EsignResp status=").append(info.status.isEmpty() ? "(missing)" : info.status);
            if (!info.errorCode.isEmpty()) m.append(", errorCode=").append(info.errorCode);
            if (!info.errorMessage.isEmpty()) m.append(", errorMessage=").append(info.errorMessage);
            r.setErrorMessage(m.toString());
            return r;
        }
        if (info.docSignatures.isEmpty()) {
            r.setErrorMessage("EsignResp contains no DocSignature element");
            return r;
        }

        // One document is the norm; with several, pick the DocSignature whose signed digest is
        // the hash asked about, so no document id has to be passed in.
        String id = info.docSignatures.keySet().iterator().next();
        if (info.docSignatures.size() > 1) {
            for (Map.Entry<String, String> e : info.docSignatures.entrySet()) {
                if (signedDigestMatches(e.getValue(), sha256Hex)) {
                    id = e.getKey();
                    break;
                }
            }
        }
        r.setDocId(id);
        String docError = info.docErrors.get(id);
        if (docError != null && !docError.isEmpty()) {
            r.setErrorMessage("DocSignature id=" + id + " reports error: " + docError);
            return r;
        }
        String pkcs7 = info.docSignatures.get(id);
        if (pkcs7.isEmpty()) {
            r.setErrorMessage("DocSignature id=" + id + " is empty - no PKCS#7 in the response");
            return r;
        }

        eSignVerificationResult v = doVerify(null, sha256Hex,
                pkcs7.getBytes(java.nio.charset.StandardCharsets.US_ASCII), checkRevocation);
        copyResponseFields(v, info, id);

        // The response names the signer certificate separately; it must be the one inside the PKCS#7.
        try {
            java.security.cert.X509Certificate uc = info.userCertificate();
            if (uc != null && v.getSignerCertificate() != null) {
                boolean same = Arrays.equals(uc.getEncoded(), v.getSignerCertificate().getEncoded());
                v.setUserCertificateMatches(Boolean.valueOf(same));
                if (!same && v.getErrorMessage() == null) {
                    v.setErrorMessage("UserX509Certificate in EsignResp differs from the PKCS#7 signer certificate");
                }
            }
        } catch (Exception ignore) {
        }
        return v;
    }

    /** True when the signed messageDigest attribute of this PKCS#7 equals the given hex. Local only. */
    private static boolean signedDigestMatches(String pkcs7Base64, String sha256Hex) {
        try {
            if (pkcs7Base64 == null || pkcs7Base64.isEmpty() || sha256Hex == null) {
                return false;
            }
            CMSSignedData cms = new CMSSignedData(toDer(pkcs7Base64.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
            SignerInformation si = (SignerInformation) cms.getSignerInfos().getSigners().iterator().next();
            AttributeTable at = si.getSignedAttributes();
            if (at == null) {
                return false;
            }
            Attribute md = at.get(CMSAttributes.messageDigest);
            if (md == null) {
                return false;
            }
            byte[] bytes = ASN1OctetString.getInstance(md.getAttrValues().getObjectAt(0)).getOctets();
            return toHex(bytes).equalsIgnoreCase(sha256Hex.trim());
        } catch (Exception e) {
            return false;
        }
    }

    private static void copyResponseFields(eSignVerificationResult r, eSignResponseInfo info, String docId) {
        r.setTransactionId(info.transactionId.isEmpty() ? null : info.transactionId);
        r.setResponseTimestamp(info.timestamp.isEmpty() ? null : info.timestamp);
        r.setResponseStatus(info.status.isEmpty() ? null : info.status);
        r.setDocId(docId);
        r.setResponseSignatureValid(info.xmlSignatureValid);
        r.setResponseSignatureNote(info.xmlSignatureNote);
    }

    private static eSignVerificationResult doVerify(byte[] data, String sha256Hex, byte[] pkcs7, boolean checkRevocation) {
        eSignVerificationResult r = new eSignVerificationResult();
        try {
            if (pkcs7 == null || pkcs7.length == 0) {
                r.setErrorMessage("pkcs7 is empty");
                return r;
            }
            pkcs7 = toDer(pkcs7);

            byte[] expectedDigest;
            if (data != null) {
                expectedDigest = MessageDigest.getInstance("SHA-256").digest(data);
            } else {
                if (sha256Hex == null || !sha256Hex.matches("^[a-fA-F0-9]{64}$")) {
                    r.setErrorMessage("sha256Hex must be 64 hex characters (SHA-256)");
                    return r;
                }
                expectedDigest = fromHex(sha256Hex);
            }
            r.setExpectedDigestHex(toHex(expectedDigest));

            // --- read signer + certificate
            CMSSignedData info = new CMSSignedData(pkcs7);
            Collection signers = info.getSignerInfos().getSigners();
            if (signers.isEmpty()) {
                r.setErrorMessage("PKCS#7 contains no SignerInfo");
                return r;
            }
            SignerInformation signer = (SignerInformation) signers.iterator().next();
            Store certs = info.getCertificates();
            Collection matches = certs.getMatches(signer.getSID());
            if (matches.isEmpty()) {
                r.setErrorMessage("signer certificate is not included in the PKCS#7");
                return r;
            }
            X509CertificateHolder holder = (X509CertificateHolder) matches.iterator().next();
            JcaX509CertificateConverter conv = new JcaX509CertificateConverter().setProvider(EMC);
            X509Certificate signerCert = conv.getCertificate(holder);
            fillCertificate(r, signerCert);

            String digestOid = signer.getDigestAlgOID();
            r.setDigestAlgorithm(digestName(digestOid));

            AttributeTable at = signer.getSignedAttributes();
            if (at != null) {
                Attribute md = at.get(CMSAttributes.messageDigest);
                if (md != null) {
                    byte[] mdBytes = ASN1OctetString.getInstance(md.getAttrValues().getObjectAt(0)).getOctets();
                    r.setMessageDigestHex(toHex(mdBytes));
                    r.setHashMatched(Arrays.equals(mdBytes, expectedDigest));
                }
                Attribute st = at.get(CMSAttributes.signingTime);
                if (st != null) {
                    r.setSigningTime(Time.getInstance(st.getAttrValues().getObjectAt(0)).getDate());
                }
            }
            // eSign (Aadhaar / PAN OTP) certificates are short-lived - typically 30 minutes -
            // so the validity window is judged at the signing time carried in the CMS, not now.
            Date ref = r.getSigningTime() != null ? r.getSigningTime() : new Date();
            r.setValidityCheckedAt(ref);
            r.setCertificateTimeValid(!ref.before(signerCert.getNotBefore()) && !ref.after(signerCert.getNotAfter()));

            // --- cryptographic verification
            CMSSignedData cms;
            if (info.getSignedContent() != null) {
                cms = info;                                                          // content embedded
            } else if (data != null) {
                cms = new CMSSignedData(new CMSProcessableByteArray(data), pkcs7);   // detached, data known
            } else {
                if (!OID_SHA256.equals(digestOid)) {
                    r.setErrorMessage("signature digest is " + r.getDigestAlgorithm()
                            + "; hash-only verification needs SHA-256 - pass the original data instead");
                    return r;
                }
                Map<String, byte[]> hashes = new HashMap<String, byte[]>();
                hashes.put(digestOid, expectedDigest);
                cms = new CMSSignedData(hashes, pkcs7);                              // detached, precomputed digest
            }
            SignerInformation s2 = (SignerInformation) cms.getSignerInfos().getSigners().iterator().next();
            boolean ok;
            try {
                ok = s2.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(EMC).build(holder));
            } catch (CMSException e) {
                ok = false;                       // e.g. message-digest attribute does not match
                r.setRevocationMessage(null);
                r.setErrorMessage(e.getMessage());
            }
            r.setSignatureValid(ok);
            if (r.getMessageDigestHex() == null) {
                r.setHashMatched(ok);             // no signed attributes: verify() compared the digest directly
            }

            // --- issuer + revocation
            X509Certificate issuer = findIssuer(certs, signerCert, conv);
            if (checkRevocation) {
                try {
                    if (issuer == null) {
                        issuer = downloadIssuer(signerCert);
                    }
                    r.setIssuerCertificate(issuer);
                    revocation(r, signerCert, issuer);
                } catch (Exception e) {
                    r.setRevocationStatus(eSignVerificationResult.RevocationStatus.NOT_CHECKED);
                    r.setRevocationMessage(e.toString());
                }
            } else {
                r.setIssuerCertificate(issuer);
                r.setRevocationMessage("not requested");
            }
        } catch (Exception e) {
            r.setErrorMessage(e.toString());
        }
        return r;
    }

    private static void fillCertificate(eSignVerificationResult r, X509Certificate cert) {
        r.setSignerCertificate(cert);
        r.setSignerSubject(cert.getSubjectX500Principal().getName());
        r.setIssuerSubject(cert.getIssuerX500Principal().getName());
        r.setSerialNumberHex(cert.getSerialNumber().toString(16));
        r.setCertNotBefore(cert.getNotBefore());
        r.setCertNotAfter(cert.getNotAfter());
        Date now = new Date();
        r.setValidityCheckedAt(now);
        r.setCertificateTimeValid(!now.before(cert.getNotBefore()) && !now.after(cert.getNotAfter()));
    }

    // ------------------------------------------------------------ revocation

    private static void revocation(eSignVerificationResult r, X509Certificate cert, X509Certificate issuer) throws Exception {
        if (issuer == null) {
            r.setRevocationStatus(eSignVerificationResult.RevocationStatus.NOT_CHECKED);
            r.setRevocationMessage("issuer certificate not available (not in PKCS#7 and no AIA caIssuers URL)");
            return;
        }
        String ocspUrl = firstAccessLocation(cert, AD_OCSP);
        String ocspFailure = null;
        if (ocspUrl != null) {
            try {
                ocsp(r, cert, issuer, ocspUrl);
                return;
            } catch (Exception e) {
                ocspFailure = "OCSP failed (" + e + ")";
            }
        }
        List<String> crlUrls = crlUrls(cert);
        if (!crlUrls.isEmpty()) {
            crl(r, cert, issuer, crlUrls);
            if (ocspFailure != null) {
                r.setRevocationMessage(ocspFailure + "; " + r.getRevocationMessage());
            }
            return;
        }
        r.setRevocationStatus(eSignVerificationResult.RevocationStatus.NOT_CHECKED);
        r.setRevocationMessage(ocspFailure != null ? ocspFailure + "; no CRL distribution point"
                : "certificate carries no OCSP responder or CRL distribution point");
    }

    private static void ocsp(eSignVerificationResult r, X509Certificate cert, X509Certificate issuer, String url) throws Exception {
        DigestCalculatorProvider dcp = new JcaDigestCalculatorProviderBuilder().setProvider(EMC).build();
        CertificateID id = new JcaCertificateID(dcp.get(CertificateID.HASH_SHA1), issuer, cert.getSerialNumber());
        OCSPReq req = new OCSPReqBuilder().addRequest(id).build();

        byte[] respBytes = http(url, "POST", "application/ocsp-request", req.getEncoded());
        OCSPResp resp = new OCSPResp(respBytes);
        if (resp.getStatus() != OCSPResp.SUCCESSFUL) {
            throw new Exception("responder status " + resp.getStatus());
        }
        BasicOCSPResp basic = (BasicOCSPResp) resp.getResponseObject();

        boolean sigOk = false;
        try {
            sigOk = basic.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider(EMC).build(issuer.getPublicKey()));
        } catch (Exception ignore) {
        }
        if (!sigOk) {
            X509CertificateHolder[] rc = basic.getCerts();
            for (int i = 0; rc != null && i < rc.length && !sigOk; i++) {
                try {
                    sigOk = basic.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider(EMC).build(rc[i]));
                } catch (Exception ignore) {
                }
            }
        }

        for (SingleResp sr : basic.getResponses()) {
            if (!sr.getCertID().getSerialNumber().equals(cert.getSerialNumber())) {
                continue;
            }
            CertificateStatus st = sr.getCertStatus();
            r.setRevocationMethod("OCSP");
            r.setRevocationSource(url);
            r.setRevocationCheckedAt(sr.getThisUpdate());
            if (st == CertificateStatus.GOOD) {
                r.setRevocationStatus(eSignVerificationResult.RevocationStatus.GOOD);
            } else if (st instanceof RevokedStatus) {
                r.setRevocationStatus(eSignVerificationResult.RevocationStatus.REVOKED);
                r.setRevocationTime(((RevokedStatus) st).getRevocationTime());
            } else {
                r.setRevocationStatus(eSignVerificationResult.RevocationStatus.UNKNOWN);
            }
            r.setRevocationMessage(sigOk ? "OCSP response signature verified" : "OCSP response signature NOT verified");
            return;
        }
        throw new Exception("OCSP response has no entry for this certificate");
    }

    private static void crl(eSignVerificationResult r, X509Certificate cert, X509Certificate issuer, List<String> urls) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Exception last = null;
        for (String url : urls) {
            try {
                byte[] bytes = http(url, "GET", null, null);
                X509CRL crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(bytes));
                boolean sigOk = true;
                try {
                    crl.verify(issuer.getPublicKey());
                } catch (Exception e) {
                    sigOk = false;
                }
                X509CRLEntry entry = crl.getRevokedCertificate(cert.getSerialNumber());
                r.setRevocationMethod("CRL");
                r.setRevocationSource(url);
                r.setRevocationCheckedAt(crl.getThisUpdate());
                if (entry != null) {
                    r.setRevocationStatus(eSignVerificationResult.RevocationStatus.REVOKED);
                    r.setRevocationTime(entry.getRevocationDate());
                } else {
                    r.setRevocationStatus(eSignVerificationResult.RevocationStatus.GOOD);
                }
                r.setRevocationMessage(sigOk ? "CRL signature verified" : "CRL signature NOT verified against issuer");
                return;
            } catch (Exception e) {
                last = e;
            }
        }
        throw last != null ? last : new Exception("no CRL could be fetched");
    }

    // ---------------------------------------------------------------- helpers

    private static X509Certificate findIssuer(Store certs, X509Certificate signerCert, JcaX509CertificateConverter conv) {
        try {
            Collection all = certs.getMatches(null);
            for (Iterator it = all.iterator(); it.hasNext();) {
                X509Certificate cand = conv.getCertificate((X509CertificateHolder) it.next());
                if (cand.getSubjectX500Principal().equals(signerCert.getIssuerX500Principal())) {
                    return cand;
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private static X509Certificate downloadIssuer(X509Certificate cert) throws Exception {
        String url = firstAccessLocation(cert, AD_CA_ISSUERS);
        if (url == null) {
            return null;
        }
        byte[] bytes = http(url, "GET", null, null);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try {
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            for (Object o : cf.generateCertificates(new ByteArrayInputStream(bytes))) {   // PKCS#7 bundle
                X509Certificate c = (X509Certificate) o;
                if (c.getSubjectX500Principal().equals(cert.getIssuerX500Principal())) {
                    return c;
                }
            }
            throw e;
        }
    }

    private static String firstAccessLocation(X509Certificate cert, ASN1ObjectIdentifier method) throws Exception {
        byte[] ext = cert.getExtensionValue(OID_EXT_AIA);
        if (ext == null) {
            return null;
        }
        ASN1OctetString octets = (ASN1OctetString) ASN1Primitive.fromByteArray(ext);
        AuthorityInformationAccess aia = AuthorityInformationAccess.getInstance(ASN1Primitive.fromByteArray(octets.getOctets()));
        for (AccessDescription ad : aia.getAccessDescriptions()) {
            if (method.equals(ad.getAccessMethod())
                    && ad.getAccessLocation().getTagNo() == GeneralName.uniformResourceIdentifier) {
                return ((ASN1String) ad.getAccessLocation().getName()).getString();
            }
        }
        return null;
    }

    private static List<String> crlUrls(X509Certificate cert) throws Exception {
        List<String> out = new ArrayList<String>();
        byte[] ext = cert.getExtensionValue(OID_EXT_CRL_DP);
        if (ext == null) {
            return out;
        }
        ASN1OctetString octets = (ASN1OctetString) ASN1Primitive.fromByteArray(ext);
        CRLDistPoint cdp = CRLDistPoint.getInstance(ASN1Primitive.fromByteArray(octets.getOctets()));
        for (DistributionPoint dp : cdp.getDistributionPoints()) {
            DistributionPointName dpn = dp.getDistributionPoint();
            if (dpn == null || dpn.getType() != DistributionPointName.FULL_NAME) {
                continue;
            }
            for (GeneralName gn : GeneralNames.getInstance(dpn.getName()).getNames()) {
                if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
                    out.add(((ASN1String) gn.getName()).getString());
                }
            }
        }
        return out;
    }

    private static byte[] http(String url, String method, String contentType, byte[] body) throws Exception {
        String current = url;
        for (int hop = 0; hop < 5; hop++) {
            HttpURLConnection c = open(current, method, contentType, body);
            int code = c.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) {
                    throw new Exception("HTTP " + code + " without Location from " + current);
                }
                current = new URL(new URL(current), loc).toString();
                continue;
            }
            if (code != 200) {
                throw new Exception("HTTP " + code + " from " + current);
            }
            InputStream is = c.getInputStream();
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                }
                return bos.toByteArray();
            } finally {
                is.close();
            }
        }
        throw new Exception("too many redirects from " + url);
    }

    private static HttpURLConnection open(String url, String method, String contentType, byte[] body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setRequestMethod(method);
        c.setConnectTimeout(connectTimeoutMs);
        c.setReadTimeout(readTimeoutMs);
        if (body != null) {
            c.setDoOutput(true);
            if (contentType != null) {
                c.setRequestProperty("Content-Type", contentType);
            }
            OutputStream os = c.getOutputStream();
            try {
                os.write(body);
            } finally {
                os.close();
            }
        }
        return c;
    }

    /**
     * DER-encoded CMS always begins with a SEQUENCE tag (0x30) and its Base64 form always begins
     * with "MI", so the first byte tells the two apart. Anything that is not DER is treated as
     * Base64 text, with PEM armour lines ("-----BEGIN PKCS7-----") removed first.
     */
    private static byte[] toDer(byte[] pkcs7) {
        if (pkcs7[0] == 0x30) {
            return pkcs7;
        }
        String text = new String(pkcs7, java.nio.charset.StandardCharsets.US_ASCII).trim();
        if (text.startsWith("-----")) {
            text = text.replaceAll("-----[^-]*-----", "");
        }
        return java.util.Base64.getMimeDecoder().decode(text.trim());
    }

    private static String digestName(String oid) {
        if (OID_SHA256.equals(oid)) return "SHA-256";
        if (OID_SHA384.equals(oid)) return "SHA-384";
        if (OID_SHA512.equals(oid)) return "SHA-512";
        if (OID_SHA1.equals(oid)) return "SHA-1";
        return oid;
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) {
            sb.append(Character.forDigit((x >> 4) & 0xF, 16)).append(Character.forDigit(x & 0xF, 16));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }
}
