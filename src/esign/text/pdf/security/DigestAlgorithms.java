package esign.text.pdf.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;

public class DigestAlgorithms {

    public static final String SHA1 = "SHA-1";
    public static final String SHA256 = "SHA-256";
    public static final String SHA384 = "SHA-384";
    public static final String SHA512 = "SHA-512";
    public static final String RIPEMD160 = "RIPEMD160";
    private static final HashMap<String, String> digestNames = new HashMap<String, String>();

    private static final HashMap<String, String> fixNames = new HashMap<String, String>();

    private static final HashMap<String, String> allowedDigests = new HashMap<String, String>();

    static {
        digestNames.put("1.2.840.113549.2.5", "MD5");
        digestNames.put("1.2.840.113549.2.2", "MD2");
        digestNames.put("1.3.14.3.2.26", "SHA1");
        digestNames.put("2.16.840.1.101.3.4.2.4", "SHA224");
        digestNames.put("2.16.840.1.101.3.4.2.1", "SHA256");
        digestNames.put("2.16.840.1.101.3.4.2.2", "SHA384");
        digestNames.put("2.16.840.1.101.3.4.2.3", "SHA512");
        digestNames.put("1.3.36.3.2.2", "RIPEMD128");
        digestNames.put("1.3.36.3.2.1", "RIPEMD160");
        digestNames.put("1.3.36.3.2.3", "RIPEMD256");
        digestNames.put("1.2.840.113549.1.1.4", "MD5");
        digestNames.put("1.2.840.113549.1.1.2", "MD2");
        digestNames.put("1.2.840.113549.1.1.5", "SHA1");
        digestNames.put("1.2.840.113549.1.1.14", "SHA224");
        digestNames.put("1.2.840.113549.1.1.11", "SHA256");
        digestNames.put("1.2.840.113549.1.1.12", "SHA384");
        digestNames.put("1.2.840.113549.1.1.13", "SHA512");
        digestNames.put("1.2.840.113549.2.5", "MD5");
        digestNames.put("1.2.840.113549.2.2", "MD2");
        digestNames.put("1.2.840.10040.4.3", "SHA1");
        digestNames.put("2.16.840.1.101.3.4.3.1", "SHA224");
        digestNames.put("2.16.840.1.101.3.4.3.2", "SHA256");
        digestNames.put("2.16.840.1.101.3.4.3.3", "SHA384");
        digestNames.put("2.16.840.1.101.3.4.3.4", "SHA512");
        digestNames.put("1.3.36.3.3.1.3", "RIPEMD128");
        digestNames.put("1.3.36.3.3.1.2", "RIPEMD160");
        digestNames.put("1.3.36.3.3.1.4", "RIPEMD256");
        digestNames.put("1.2.643.2.2.9", "GOST3411");

        fixNames.put("SHA256", "SHA-256");
        fixNames.put("SHA384", "SHA-384");
        fixNames.put("SHA512", "SHA-512");

        allowedDigests.put("MD2", "1.2.840.113549.2.2");
        allowedDigests.put("MD-2", "1.2.840.113549.2.2");
        allowedDigests.put("MD5", "1.2.840.113549.2.5");
        allowedDigests.put("MD-5", "1.2.840.113549.2.5");
        allowedDigests.put("SHA1", "1.3.14.3.2.26");
        allowedDigests.put("SHA-1", "1.3.14.3.2.26");
        allowedDigests.put("SHA224", "2.16.840.1.101.3.4.2.4");
        allowedDigests.put("SHA-224", "2.16.840.1.101.3.4.2.4");
        allowedDigests.put("SHA256", "2.16.840.1.101.3.4.2.1");
        allowedDigests.put("SHA-256", "2.16.840.1.101.3.4.2.1");
        allowedDigests.put("SHA384", "2.16.840.1.101.3.4.2.2");
        allowedDigests.put("SHA-384", "2.16.840.1.101.3.4.2.2");
        allowedDigests.put("SHA512", "2.16.840.1.101.3.4.2.3");
        allowedDigests.put("SHA-512", "2.16.840.1.101.3.4.2.3");
        allowedDigests.put("RIPEMD128", "1.3.36.3.2.2");
        allowedDigests.put("RIPEMD-128", "1.3.36.3.2.2");
        allowedDigests.put("RIPEMD160", "1.3.36.3.2.1");
        allowedDigests.put("RIPEMD-160", "1.3.36.3.2.1");
        allowedDigests.put("RIPEMD256", "1.3.36.3.2.3");
        allowedDigests.put("RIPEMD-256", "1.3.36.3.2.3");
        allowedDigests.put("GOST3411", "1.2.643.2.2.9");
    }

    public static MessageDigest getMessageDigestFromOid(String digestOid, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        return getMessageDigest(getDigest(digestOid), provider);
    }

    public static MessageDigest getMessageDigest(String hashAlgorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.startsWith("SunPKCS11") || provider.startsWith("SunMSCAPI")) {
            return MessageDigest.getInstance(normalizeDigestName(hashAlgorithm));
        }
        return MessageDigest.getInstance(hashAlgorithm, provider);
    }

    public static byte[] digest(InputStream data, String hashAlgorithm, String provider) throws GeneralSecurityException, IOException {
        MessageDigest messageDigest = getMessageDigest(hashAlgorithm, provider);
        return digest(data, messageDigest);
    }

    public static byte[] digest(InputStream data, MessageDigest messageDigest) throws GeneralSecurityException, IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = data.read(buf)) > 0) {
            messageDigest.update(buf, 0, n);
        }
        return messageDigest.digest();
    }

    public static String getDigest(String oid) {
        String ret = digestNames.get(oid);
        if (ret == null) {
            return oid;
        }
        return ret;
    }

    public static String normalizeDigestName(String algo) {
        if (fixNames.containsKey(algo)) {
            return fixNames.get(algo);
        }
        return algo;
    }

    public static String getAllowedDigests(String name) {
        return allowedDigests.get(name.toUpperCase());
    }
}
