package org.emcastle.cms.bc;

import java.io.InputStream;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.RecipientOperator;
import org.emcastle.crypto.BufferedBlockCipher;
import org.emcastle.crypto.StreamCipher;
import org.emcastle.crypto.params.KeyParameter;
import org.emcastle.operator.InputDecryptor;
import org.emcastle.operator.bc.BcSymmetricKeyUnwrapper;

public class BcKEKEnvelopedRecipient
    extends BcKEKRecipient
{
    public BcKEKEnvelopedRecipient(BcSymmetricKeyUnwrapper unwrapper)
    {
        super(unwrapper);
    }

    public RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncryptionAlgorithm, final AlgorithmIdentifier contentEncryptionAlgorithm, byte[] encryptedContentEncryptionKey)
        throws CMSException
    {
        KeyParameter secretKey = (KeyParameter)extractSecretKey(keyEncryptionAlgorithm, contentEncryptionAlgorithm, encryptedContentEncryptionKey);

        final Object dataCipher = EnvelopedDataHelper.createContentCipher(false, secretKey, contentEncryptionAlgorithm);

        return new RecipientOperator(new InputDecryptor()
        {
            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return contentEncryptionAlgorithm;
            }

            public InputStream getInputStream(InputStream dataOut)
            {
                if (dataCipher instanceof BufferedBlockCipher)
                {
                    return new org.emcastle.crypto.io.CipherInputStream(dataOut, (BufferedBlockCipher)dataCipher);
                }
                else
                {
                    return new org.emcastle.crypto.io.CipherInputStream(dataOut, (StreamCipher)dataCipher);
                }
            }
        });
    }
}
