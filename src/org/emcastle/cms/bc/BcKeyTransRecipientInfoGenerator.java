package org.emcastle.cms.bc;

import org.emcastle.asn1.cms.IssuerAndSerialNumber;
import org.emcastle.cert.X509CertificateHolder;
import org.emcastle.cms.KeyTransRecipientInfoGenerator;
import org.emcastle.operator.bc.BcAsymmetricKeyWrapper;

public abstract class BcKeyTransRecipientInfoGenerator
    extends KeyTransRecipientInfoGenerator
{
    public BcKeyTransRecipientInfoGenerator(X509CertificateHolder recipientCert, BcAsymmetricKeyWrapper wrapper)
    {
        super(new IssuerAndSerialNumber(recipientCert.toASN1Structure()), wrapper);
    }

    public BcKeyTransRecipientInfoGenerator(byte[] subjectKeyIdentifier, BcAsymmetricKeyWrapper wrapper)
    {
        super(subjectKeyIdentifier, wrapper);
    }
}