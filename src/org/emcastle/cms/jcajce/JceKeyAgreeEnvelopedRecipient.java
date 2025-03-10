package org.emcastle.cms.jcajce;

import java.io.InputStream;
import java.security.Key;
import java.security.PrivateKey;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.asn1.x509.SubjectPublicKeyInfo;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.RecipientOperator;
import org.emcastle.operator.InputDecryptor;

public class JceKeyAgreeEnvelopedRecipient
    extends JceKeyAgreeRecipient
{
    public JceKeyAgreeEnvelopedRecipient(PrivateKey recipientKey)
    {
        super(recipientKey);
    }

    public RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncryptionAlgorithm, final AlgorithmIdentifier contentEncryptionAlgorithm, SubjectPublicKeyInfo senderPublicKey, ASN1OctetString userKeyingMaterial, byte[] encryptedContentKey)
        throws CMSException
    {
        Key secretKey = extractSecretKey(keyEncryptionAlgorithm, contentEncryptionAlgorithm, senderPublicKey, userKeyingMaterial, encryptedContentKey);

        final Cipher dataCipher = contentHelper.createContentCipher(secretKey, contentEncryptionAlgorithm);

        return new RecipientOperator(new InputDecryptor()
        {
            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return contentEncryptionAlgorithm;
            }

            public InputStream getInputStream(InputStream dataOut)
            {
                return new CipherInputStream(dataOut, dataCipher);
            }
        });
    }
}
