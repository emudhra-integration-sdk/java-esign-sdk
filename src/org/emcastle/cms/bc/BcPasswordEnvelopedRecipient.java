package org.emcastle.cms.bc;

import java.io.InputStream;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.RecipientOperator;
import org.emcastle.crypto.BufferedBlockCipher;
import org.emcastle.crypto.StreamCipher;
import org.emcastle.crypto.io.CipherInputStream;
import org.emcastle.crypto.params.KeyParameter;
import org.emcastle.operator.InputDecryptor;

public class BcPasswordEnvelopedRecipient
    extends BcPasswordRecipient
{
    public BcPasswordEnvelopedRecipient(char[] password)
    {
        super(password);
    }

    public RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncryptionAlgorithm, final AlgorithmIdentifier contentEncryptionAlgorithm, byte[] derivedKey, byte[] encryptedContentEncryptionKey)
        throws CMSException
    {
        KeyParameter secretKey = extractSecretKey(keyEncryptionAlgorithm, contentEncryptionAlgorithm, derivedKey, encryptedContentEncryptionKey);

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
                    return new CipherInputStream(dataOut, (BufferedBlockCipher)dataCipher);
                }
                else
                {
                    return new CipherInputStream(dataOut, (StreamCipher)dataCipher);
                }
            }
        });
    }
}
