package org.emcastle.cms.jcajce;

import java.io.OutputStream;
import java.security.Key;
import java.security.PrivateKey;

import javax.crypto.Mac;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.RecipientOperator;
import org.emcastle.jcajce.io.MacOutputStream;
import org.emcastle.operator.GenericKey;
import org.emcastle.operator.MacCalculator;


/**
 * the KeyTransRecipientInformation class for a recipient who has been sent a secret
 * key encrypted using their public key that needs to be used to
 * extract the message.
 */
public class JceKeyTransAuthenticatedRecipient
    extends JceKeyTransRecipient
{
    public JceKeyTransAuthenticatedRecipient(PrivateKey recipientKey)
    {
        super(recipientKey);
    }

    public RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncryptionAlgorithm, final AlgorithmIdentifier contentMacAlgorithm, byte[] encryptedContentEncryptionKey)
        throws CMSException
    {
        final Key secretKey = extractSecretKey(keyEncryptionAlgorithm, contentMacAlgorithm, encryptedContentEncryptionKey);

        final Mac dataMac = contentHelper.createContentMac(secretKey, contentMacAlgorithm);

        return new RecipientOperator(new MacCalculator()
        {
            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return contentMacAlgorithm;
            }

            public GenericKey getKey()
            {
                return new GenericKey(secretKey);
            }

            public OutputStream getOutputStream()
            {
                return new MacOutputStream(dataMac);
            }

            public byte[] getMac()
            {
                return dataMac.doFinal();
            }
        });
    }
}
