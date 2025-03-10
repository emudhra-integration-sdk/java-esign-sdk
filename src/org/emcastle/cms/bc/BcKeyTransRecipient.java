package org.emcastle.cms.bc;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.KeyTransRecipient;
import org.emcastle.crypto.CipherParameters;
import org.emcastle.crypto.params.AsymmetricKeyParameter;
import org.emcastle.operator.AsymmetricKeyUnwrapper;
import org.emcastle.operator.OperatorException;
import org.emcastle.operator.bc.BcRSAAsymmetricKeyUnwrapper;

public abstract class BcKeyTransRecipient
    implements KeyTransRecipient
{
    private AsymmetricKeyParameter recipientKey;

    public BcKeyTransRecipient(AsymmetricKeyParameter recipientKey)
    {
        this.recipientKey = recipientKey;
    }

    protected CipherParameters extractSecretKey(AlgorithmIdentifier keyEncryptionAlgorithm, AlgorithmIdentifier encryptedKeyAlgorithm, byte[] encryptedEncryptionKey)
        throws CMSException
    {
        AsymmetricKeyUnwrapper unwrapper = new BcRSAAsymmetricKeyUnwrapper(keyEncryptionAlgorithm, recipientKey);

        try
        {
            return CMSUtils.getBcKey(unwrapper.generateUnwrappedKey(encryptedKeyAlgorithm, encryptedEncryptionKey));
        }
        catch (OperatorException e)
        {
            throw new CMSException("exception unwrapping key: " + e.getMessage(), e);
        }
    }
}
