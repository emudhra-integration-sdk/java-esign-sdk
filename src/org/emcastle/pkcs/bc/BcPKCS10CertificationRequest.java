package org.emcastle.pkcs.bc;

import java.io.IOException;

import org.emcastle.asn1.pkcs.CertificationRequest;
import org.emcastle.crypto.params.AsymmetricKeyParameter;
import org.emcastle.crypto.util.PublicKeyFactory;
import org.emcastle.pkcs.PKCS10CertificationRequest;
import org.emcastle.pkcs.PKCSException;

public class BcPKCS10CertificationRequest
    extends PKCS10CertificationRequest
{
    public BcPKCS10CertificationRequest(CertificationRequest certificationRequest)
    {
        super(certificationRequest);
    }

    public BcPKCS10CertificationRequest(byte[] encoding)
        throws IOException
    {
        super(encoding);
    }

    public BcPKCS10CertificationRequest(PKCS10CertificationRequest requestHolder)
    {
        super(requestHolder.toASN1Structure());
    }

    public AsymmetricKeyParameter getPublicKey()
        throws PKCSException
    {
        try
        {
            return PublicKeyFactory.createKey(this.getSubjectPublicKeyInfo());
        }
        catch (IOException e)
        {
            throw new PKCSException("error extracting key encoding: " + e.getMessage(), e);
        }
    }
}
