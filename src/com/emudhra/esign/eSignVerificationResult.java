package com.emudhra.esign;

import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Outcome of verifying a detached PKCS#7 / CMS signature (for example the result of
 * hash-based signing over JSON or any other data) together with the revocation status
 * of the signer certificate.
 *
 * Produced by {@link eSign#verifyEsignResponseHash} and
 * {@link eSignVerifier}. Verification never throws: check {@link #isValid()} and the
 * individual fields, and {@link #getErrorMessage()} when something could not be evaluated.
 */
public final class eSignVerificationResult {

    public enum RevocationStatus { GOOD, REVOKED, UNKNOWN, NOT_CHECKED }

    // signature
    private boolean signatureValid;
    private boolean hashMatched;
    private String expectedDigestHex;
    private String messageDigestHex;
    private String digestAlgorithm;
    private Date signingTime;

    // signer certificate
    private X509Certificate signerCertificate;
    private X509Certificate issuerCertificate;
    private String signerSubject;
    private String issuerSubject;
    private String serialNumberHex;
    private Date certNotBefore;
    private Date certNotAfter;
    private boolean certificateTimeValid;
    private Date validityCheckedAt;

    // revocation
    private RevocationStatus revocationStatus = RevocationStatus.NOT_CHECKED;
    private String revocationMethod;
    private String revocationSource;
    private Date revocationCheckedAt;
    private Date revocationTime;
    private String revocationMessage;

    // gateway response (only set when an EsignResp XML was the input)
    private String transactionId;
    private String responseTimestamp;
    private String responseStatus;
    private String docId;
    private Boolean responseSignatureValid;
    private String responseSignatureNote;
    private Boolean userCertificateMatches;

    private String errorMessage;

    /**
     * True when the signature verifies, the signed digest matches the data/hash supplied,
     * the signer certificate is within its validity period, and it is not known to be revoked.
     * A revocation status of NOT_CHECKED or UNKNOWN does not make this false - inspect
     * {@link #getRevocationStatus()} if your policy requires a positive revocation answer.
     */
    public boolean isValid() {
        return signatureValid && hashMatched && certificateTimeValid
                && revocationStatus != RevocationStatus.REVOKED && errorMessage == null;
    }

    public boolean isSignatureValid() { return signatureValid; }
    public boolean isHashMatched() { return hashMatched; }
    public String getExpectedDigestHex() { return expectedDigestHex; }
    public String getMessageDigestHex() { return messageDigestHex; }
    public String getDigestAlgorithm() { return digestAlgorithm; }
    public Date getSigningTime() { return signingTime; }

    public X509Certificate getSignerCertificate() { return signerCertificate; }
    public X509Certificate getIssuerCertificate() { return issuerCertificate; }
    public String getSignerSubject() { return signerSubject; }
    public String getIssuerSubject() { return issuerSubject; }
    public String getSerialNumberHex() { return serialNumberHex; }
    public Date getCertNotBefore() { return certNotBefore; }
    public Date getCertNotAfter() { return certNotAfter; }
    /**
     * Whether the signer certificate was within its validity period at {@link #getValidityCheckedAt()}:
     * the CMS signing time when present (eSign OTP certificates live only ~30 minutes, so
     * they are normally expired by the time anyone verifies), otherwise the current time.
     */
    public boolean isCertificateTimeValid() { return certificateTimeValid; }
    public Date getValidityCheckedAt() { return validityCheckedAt; }

    public RevocationStatus getRevocationStatus() { return revocationStatus; }
    /** "OCSP" or "CRL", null when revocation was not checked. */
    public String getRevocationMethod() { return revocationMethod; }
    /** The OCSP responder or CRL URL that answered. */
    public String getRevocationSource() { return revocationSource; }
    public Date getRevocationCheckedAt() { return revocationCheckedAt; }
    public Date getRevocationTime() { return revocationTime; }
    public String getRevocationMessage() { return revocationMessage; }

    public String getErrorMessage() { return errorMessage; }

    /** EsignResp @txn - only when the input was the gateway response XML. */
    public String getTransactionId() { return transactionId; }
    /** EsignResp @ts as sent by the gateway. */
    public String getResponseTimestamp() { return responseTimestamp; }
    /** EsignResp @status ("1" = success). */
    public String getResponseStatus() { return responseStatus; }
    /** The DocSignature id that was verified. */
    public String getDocId() { return docId; }
    /**
     * Core validation of the gateway's enveloped XML signature over the EsignResp, using the
     * certificate in its KeyInfo. TRUE / FALSE, or null when the response has no XML signature or
     * it could not be evaluated (see {@link #getResponseSignatureNote()}). Advisory: it says whether
     * the XML is exactly what the gateway signed; it is not part of {@link #isValid()}.
     */
    public Boolean getResponseSignatureValid() { return responseSignatureValid; }
    public String getResponseSignatureNote() { return responseSignatureNote; }
    /** Whether EsignResp/UserX509Certificate equals the PKCS#7 signer certificate; null when not compared. */
    public Boolean getUserCertificateMatches() { return userCertificateMatches; }

    void setSignatureValid(boolean v) { signatureValid = v; }
    void setHashMatched(boolean v) { hashMatched = v; }
    void setExpectedDigestHex(String v) { expectedDigestHex = v; }
    void setMessageDigestHex(String v) { messageDigestHex = v; }
    void setDigestAlgorithm(String v) { digestAlgorithm = v; }
    void setSigningTime(Date v) { signingTime = v; }
    void setSignerCertificate(X509Certificate v) { signerCertificate = v; }
    void setIssuerCertificate(X509Certificate v) {
        issuerCertificate = v;
        if (v != null && issuerSubject == null) issuerSubject = v.getSubjectX500Principal().getName();
    }
    void setSignerSubject(String v) { signerSubject = v; }
    void setIssuerSubject(String v) { issuerSubject = v; }
    void setSerialNumberHex(String v) { serialNumberHex = v; }
    void setCertNotBefore(Date v) { certNotBefore = v; }
    void setCertNotAfter(Date v) { certNotAfter = v; }
    void setCertificateTimeValid(boolean v) { certificateTimeValid = v; }
    void setValidityCheckedAt(Date v) { validityCheckedAt = v; }
    void setRevocationStatus(RevocationStatus v) { revocationStatus = v; }
    void setRevocationMethod(String v) { revocationMethod = v; }
    void setRevocationSource(String v) { revocationSource = v; }
    void setRevocationCheckedAt(Date v) { revocationCheckedAt = v; }
    void setRevocationTime(Date v) { revocationTime = v; }
    void setRevocationMessage(String v) { revocationMessage = v; }
    void setErrorMessage(String v) { errorMessage = v; }
    void setTransactionId(String v) { transactionId = v; }
    void setResponseTimestamp(String v) { responseTimestamp = v; }
    void setResponseStatus(String v) { responseStatus = v; }
    void setDocId(String v) { docId = v; }
    void setResponseSignatureValid(Boolean v) { responseSignatureValid = v; }
    void setResponseSignatureNote(String v) { responseSignatureNote = v; }
    void setUserCertificateMatches(Boolean v) { userCertificateMatches = v; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("valid=").append(isValid())
          .append(", signatureValid=").append(signatureValid)
          .append(", hashMatched=").append(hashMatched)
          .append(", digest=").append(digestAlgorithm)
          .append(", signer=").append(signerSubject)
          .append(", issuer=").append(issuerSubject)
          .append(", serial=").append(serialNumberHex)
          .append(", certValid=").append(certNotBefore).append("..").append(certNotAfter)
          .append(", certTimeValid=").append(certificateTimeValid).append("@").append(validityCheckedAt)
          .append(", signingTime=").append(signingTime)
          .append(", revocation=").append(revocationStatus);
        if (revocationMethod != null) sb.append(" via ").append(revocationMethod).append(" (").append(revocationSource).append(")");
        if (revocationTime != null) sb.append(", revokedAt=").append(revocationTime);
        if (revocationMessage != null) sb.append(", revocationNote=").append(revocationMessage);
        if (transactionId != null) sb.append(", txn=").append(transactionId).append(", ts=").append(responseTimestamp)
                .append(", responseStatus=").append(responseStatus).append(", docId=").append(docId);
        if (responseSignatureNote != null) sb.append(", responseXmlSignature=").append(responseSignatureValid).append(" (").append(responseSignatureNote).append(")");
        if (userCertificateMatches != null) sb.append(", userCertMatches=").append(userCertificateMatches);
        if (errorMessage != null) sb.append(", error=").append(errorMessage);
        return sb.toString();
    }
}
