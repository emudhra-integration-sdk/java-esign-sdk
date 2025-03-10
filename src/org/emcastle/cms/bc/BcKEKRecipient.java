package org.emcastle.cms.bc;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.KEKRecipient;
import org.emcastle.crypto.CipherParameters;
import org.emcastle.operator.OperatorException;
import org.emcastle.operator.SymmetricKeyUnwrapper;
import org.emcastle.operator.bc.BcSymmetricKeyUnwrapper;

public abstract class BcKEKRecipient
    implements KEKRecipient
{
    private SymmetricKeyUnwrapper unwrapper;

    public BcKEKRecipient(BcSymmetricKeyUnwrapper unwrapper)
    {
        this.unwrapper = unwrapper;
    }

    protected CipherParameters extractSecretKey(AlgorithmIdentifier keyEncryptionAlgorithm, AlgorithmIdentifier contentEncryptionAlgorithm, byte[] encryptedContentEncryptionKey)
        throws CMSException
    {
        try
        {
            return CMSUtils.getBcKey(unwrapper.generateUnwrappedKey(contentEncryptionAlgorithm, encryptedContentEncryptionKey));
        }
        catch (OperatorException e)
        {
            throw new CMSException("exception unwrapping key: " + e.getMessage(), e);
        }
    }
}
