package org.emcastle.cert.ocsp.jcajce;

import java.security.PublicKey;

import javax.security.auth.x500.X500Principal;

import org.emcastle.asn1.x500.X500Name;
import org.emcastle.asn1.x509.SubjectPublicKeyInfo;
import org.emcastle.cert.ocsp.OCSPException;
import org.emcastle.cert.ocsp.RespID;
import org.emcastle.operator.DigestCalculator;

public class JcaRespID
    extends RespID
{
    public JcaRespID(X500Principal name)
    {
        super(X500Name.getInstance(name.getEncoded()));
    }

    public JcaRespID(PublicKey pubKey, DigestCalculator digCalc)
        throws OCSPException
    {
        super(SubjectPublicKeyInfo.getInstance(pubKey.getEncoded()), digCalc);
    }
}
