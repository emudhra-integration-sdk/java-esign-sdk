package org.emcastle.cert.ocsp.jcajce;

import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.emcastle.cert.jcajce.JcaX509CertificateHolder;
import org.emcastle.cert.ocsp.CertificateID;
import org.emcastle.cert.ocsp.OCSPException;
import org.emcastle.operator.DigestCalculator;

public class JcaCertificateID
    extends CertificateID
{
    public JcaCertificateID(DigestCalculator digestCalculator, X509Certificate issuerCert, BigInteger number)
        throws OCSPException, CertificateEncodingException
    {
        super(digestCalculator, new JcaX509CertificateHolder(issuerCert), number);
    }
}
