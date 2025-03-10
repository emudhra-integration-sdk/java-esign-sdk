package org.emcastle.cms;

import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.asn1.x509.SubjectPublicKeyInfo;

public interface KeyAgreeRecipient
    extends Recipient
{
    RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncAlg, AlgorithmIdentifier contentEncryptionAlgorithm, SubjectPublicKeyInfo senderPublicKey, ASN1OctetString userKeyingMaterial, byte[] encryptedContentKey)
        throws CMSException;

    AlgorithmIdentifier getPrivateKeyAlgorithmIdentifier();
}
