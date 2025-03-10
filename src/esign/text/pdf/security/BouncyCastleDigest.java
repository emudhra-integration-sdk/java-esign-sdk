package esign.text.pdf.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.emcastle.jcajce.provider.digest.GOST3411;
import org.emcastle.jcajce.provider.digest.MD2;
import org.emcastle.jcajce.provider.digest.MD5;
import org.emcastle.jcajce.provider.digest.RIPEMD128;
import org.emcastle.jcajce.provider.digest.RIPEMD160;
import org.emcastle.jcajce.provider.digest.RIPEMD256;
import org.emcastle.jcajce.provider.digest.SHA1;
import org.emcastle.jcajce.provider.digest.SHA224;
import org.emcastle.jcajce.provider.digest.SHA256;
import org.emcastle.jcajce.provider.digest.SHA384;
import org.emcastle.jcajce.provider.digest.SHA512;

public class BouncyCastleDigest
        implements ExternalDigest {

    public MessageDigest getMessageDigest(String hashAlgorithm) throws GeneralSecurityException {
        String oid = DigestAlgorithms.getAllowedDigests(hashAlgorithm);
        if (oid == null) {
            throw new NoSuchAlgorithmException(hashAlgorithm);
        }
        if (oid.equals("1.2.840.113549.2.2")) {
            return (MessageDigest) new MD2.Digest();
        }
        if (oid.equals("1.2.840.113549.2.5")) {
            return (MessageDigest) new MD5.Digest();
        }
        if (oid.equals("1.3.14.3.2.26")) {
            return (MessageDigest) new SHA1.Digest();
        }
        if (oid.equals("2.16.840.1.101.3.4.2.4")) {
            return (MessageDigest) new SHA224.Digest();
        }
        if (oid.equals("2.16.840.1.101.3.4.2.1")) {
            return (MessageDigest) new SHA256.Digest();
        }
        if (oid.equals("2.16.840.1.101.3.4.2.2")) {
            return (MessageDigest) new SHA384.Digest();
        }
        if (oid.equals("2.16.840.1.101.3.4.2.3")) {
            return (MessageDigest) new SHA512.Digest();
        }
        if (oid.equals("1.3.36.3.2.2")) {
            return (MessageDigest) new RIPEMD128.Digest();
        }
        if (oid.equals("1.3.36.3.2.1")) {
            return (MessageDigest) new RIPEMD160.Digest();
        }
        if (oid.equals("1.3.36.3.2.3")) {
            return (MessageDigest) new RIPEMD256.Digest();
        }
        if (oid.equals("1.2.643.2.2.9")) {
            return (MessageDigest) new GOST3411.Digest();
        }

        throw new NoSuchAlgorithmException(hashAlgorithm);
    }
}


/* Location:              D:\test\tp\itextpdf-5.5.10.jar!\com\itextpdf\text\pdf\security\BouncyCastleDigest.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */
