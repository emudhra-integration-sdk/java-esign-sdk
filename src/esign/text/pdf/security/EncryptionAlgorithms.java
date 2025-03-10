package esign.text.pdf.security;

import java.util.HashMap;

public class EncryptionAlgorithms {

    static final HashMap<String, String> algorithmNames = new HashMap<String, String>();

    static {
        algorithmNames.put("1.2.840.113549.1.1.1", "RSA");
        algorithmNames.put("1.2.840.10040.4.1", "DSA");
        algorithmNames.put("1.2.840.113549.1.1.2", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.4", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.5", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.14", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.11", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.12", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.13", "RSA");
        algorithmNames.put("1.2.840.10040.4.3", "DSA");
        algorithmNames.put("2.16.840.1.101.3.4.3.1", "DSA");
        algorithmNames.put("2.16.840.1.101.3.4.3.2", "DSA");
        algorithmNames.put("1.3.14.3.2.29", "RSA");
        algorithmNames.put("1.3.36.3.3.1.2", "RSA");
        algorithmNames.put("1.3.36.3.3.1.3", "RSA");
        algorithmNames.put("1.3.36.3.3.1.4", "RSA");
        algorithmNames.put("1.2.643.2.2.19", "ECGOST3410");
    }

    public static String getAlgorithm(String oid) {
        String ret = algorithmNames.get(oid);
        if (ret == null) {
            return oid;
        }
        return ret;
    }
}
