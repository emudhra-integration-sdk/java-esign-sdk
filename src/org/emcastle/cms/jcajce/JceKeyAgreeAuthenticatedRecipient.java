package org.emcastle.cms.jcajce;

import java.io.OutputStream;
import java.security.Key;
import java.security.PrivateKey;

import javax.crypto.Mac;

import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.asn1.x509.SubjectPublicKeyInfo;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.RecipientOperator;
import org.emcastle.jcajce.io.MacOutputStream;
import org.emcastle.operator.GenericKey;
import org.emcastle.operator.MacCalculator;
import org.emcastle.operator.jcajce.JceGenericKey;

public class JceKeyAgreeAuthenticatedRecipient
    extends JceKeyAgreeRecipient
{
    public JceKeyAgreeAuthenticatedRecipient(PrivateKey recipientKey)
    {
        super(recipientKey);
    }

    public RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncryptionAlgorithm, final AlgorithmIdentifier contentMacAlgorithm, SubjectPublicKeyInfo senderPublicKey, ASN1OctetString userKeyingMaterial, byte[] encryptedContentKey)
        throws CMSException
    {
        final Key secretKey = extractSecretKey(keyEncryptionAlgorithm, contentMacAlgorithm, senderPublicKey, userKeyingMaterial, encryptedContentKey);

        final Mac dataMac = contentHelper.createContentMac(secretKey, contentMacAlgorithm);

        return new RecipientOperator(new MacCalculator()
        {
            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return contentMacAlgorithm;
            }

            public GenericKey getKey()
            {
                return new JceGenericKey(contentMacAlgorithm, secretKey);
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
