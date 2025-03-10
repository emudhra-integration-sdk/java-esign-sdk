package org.emcastle.cms.jcajce;

import java.math.BigInteger;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.emcastle.asn1.x500.X500Name;
import org.emcastle.cms.KeyAgreeRecipientId;

public class JceKeyAgreeRecipientId
    extends KeyAgreeRecipientId
{
    public JceKeyAgreeRecipientId(X509Certificate certificate)
    {
        this(certificate.getIssuerX500Principal(), certificate.getSerialNumber());
    }

    public JceKeyAgreeRecipientId(X500Principal issuer, BigInteger serialNumber)
    {
        super(X500Name.getInstance(issuer.getEncoded()), serialNumber);
    }
}
