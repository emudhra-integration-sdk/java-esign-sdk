package esign.text.pdf.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public class ProviderDigest
        implements ExternalDigest {

    private String provider;

    public ProviderDigest(String provider) {
        this.provider = provider;
    }

    public MessageDigest getMessageDigest(String hashAlgorithm) throws GeneralSecurityException {
        return DigestAlgorithms.getMessageDigest(hashAlgorithm, this.provider);
    }
}
