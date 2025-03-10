package org.emcastle.cert.ocsp.jcajce;

import java.security.PublicKey;

import org.emcastle.asn1.x509.SubjectPublicKeyInfo;
import org.emcastle.cert.ocsp.BasicOCSPRespBuilder;
import org.emcastle.cert.ocsp.OCSPException;
import org.emcastle.operator.DigestCalculator;

public class JcaBasicOCSPRespBuilder
    extends BasicOCSPRespBuilder
{
    public JcaBasicOCSPRespBuilder(PublicKey key, DigestCalculator digCalc)
        throws OCSPException
    {
        super(SubjectPublicKeyInfo.getInstance(key.getEncoded()), digCalc);
    }
}
