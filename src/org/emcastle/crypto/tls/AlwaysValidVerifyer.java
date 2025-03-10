package org.emcastle.crypto.tls;

/**
 * A certificate verifyer, that will always return true.
 * <p/>
 * <pre>
 * DO NOT USE THIS FILE UNLESS YOU KNOW EXACTLY WHAT YOU ARE DOING.
 * </pre>
 *
 * @deprecated Perform certificate verification in TlsAuthentication implementation
 */
public class AlwaysValidVerifyer
    implements CertificateVerifyer
{
    /**
     * Return true.
     *
     * @see org.emcastle.crypto.tls.CertificateVerifyer#isValid(org.emcastle.asn1.x509.Certificate[])
     */
    public boolean isValid(org.emcastle.asn1.x509.Certificate[] certs)
    {
        return true;
    }
}
