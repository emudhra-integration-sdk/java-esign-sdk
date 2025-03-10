package esign.text.pdf.security;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;

public class PrivateKeySignature
        implements ExternalSignature {

    private PrivateKey pk;
    private String hashAlgorithm;
    private String encryptionAlgorithm;
    private String provider;

    public PrivateKeySignature(PrivateKey pk, String hashAlgorithm, String provider) {
        this.pk = pk;
        this.provider = provider;
        this.hashAlgorithm = DigestAlgorithms.getDigest(DigestAlgorithms.getAllowedDigests(hashAlgorithm));
        this.encryptionAlgorithm = pk.getAlgorithm();
        if (this.encryptionAlgorithm.startsWith("EC")) {
            this.encryptionAlgorithm = "ECDSA";
        }
    }

    public String getHashAlgorithm() {
        return this.hashAlgorithm;
    }

    public String getEncryptionAlgorithm() {
        return this.encryptionAlgorithm;
    }

    public byte[] sign(byte[] b) throws GeneralSecurityException {
        Signature sig;
        String signMode = this.hashAlgorithm + "with" + this.encryptionAlgorithm;

        if (this.provider == null) {
            sig = Signature.getInstance(signMode);
        } else {
            sig = Signature.getInstance(signMode, this.provider);
        }
        sig.initSign(this.pk);
        sig.update(b);
        return sig.sign();
    }
}
