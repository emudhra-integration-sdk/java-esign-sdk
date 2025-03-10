package org.emcastle.cert.crmf.jcajce;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.emcastle.asn1.crmf.EncryptedValue;
import org.emcastle.cert.crmf.CRMFException;
import org.emcastle.cert.crmf.EncryptedValueBuilder;
import org.emcastle.cert.jcajce.JcaX509CertificateHolder;
import org.emcastle.operator.KeyWrapper;
import org.emcastle.operator.OutputEncryptor;

public class JcaEncryptedValueBuilder
    extends EncryptedValueBuilder
{
    public JcaEncryptedValueBuilder(KeyWrapper wrapper, OutputEncryptor encryptor)
    {
        super(wrapper, encryptor);
    }

    public EncryptedValue build(X509Certificate certificate)
        throws CertificateEncodingException, CRMFException
    {
        return build(new JcaX509CertificateHolder(certificate));
    }
}
