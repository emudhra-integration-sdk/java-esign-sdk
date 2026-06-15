package com.emudhra.esign;

/**
 * Configuration for the Encrypted Aadhaar eSign flow.
 *
 * <p>Create an instance, set the Aadhaar number and the UIDAI public-key
 * certificate, then pass it to the builder:
 * <pre>
 *   EncryptedAadhaarConfig cfg = new EncryptedAadhaarConfig();
 *   cfg.setAadhaarNumber("123456789012");
 *   cfg.setCerFilePath("/path/to/uidai-public.cer");   // OR
 *   cfg.setCerBase64(base64EncodedCerBytes);
 *
 *   eSignInput input = eSignInputBuilder.init()
 *       ...
 *       .setEncryptedAadhaarFlowEnabled(true)
 *       .setEncryptedAadhaarConfig(cfg)
 *       .build();
 * </pre>
 */
public class EncryptedAadhaarConfig {

    private String aadhaarNumber;
    private String cerFilePath;
    private String cerBase64;

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    /** @param aadhaarNumber 12-digit Aadhaar number, digits only, no spaces. */
    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    /** @return path to the UIDAI public-key CER file, or {@code null} if base64 was set. */
    public String getCerFilePath() {
        return cerFilePath;
    }

    /** @param cerFilePath absolute or relative path to the UIDAI public-key CER file. */
    public void setCerFilePath(String cerFilePath) {
        this.cerFilePath = cerFilePath;
    }

    /** @return Base64-encoded CER file bytes, or {@code null} if a file path was set. */
    public String getCerBase64() {
        return cerBase64;
    }

    /** @param cerBase64 Base64 encoding of the raw CER file bytes (DER or PEM). */
    public void setCerBase64(String cerBase64) {
        this.cerBase64 = cerBase64;
    }
}
