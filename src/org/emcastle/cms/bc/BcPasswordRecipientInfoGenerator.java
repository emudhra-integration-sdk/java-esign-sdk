package org.emcastle.cms.bc;

import org.emcastle.asn1.ASN1ObjectIdentifier;
import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.PasswordRecipientInfoGenerator;
import org.emcastle.crypto.Wrapper;
import org.emcastle.crypto.params.KeyParameter;
import org.emcastle.crypto.params.ParametersWithIV;
import org.emcastle.operator.GenericKey;

public class BcPasswordRecipientInfoGenerator
    extends PasswordRecipientInfoGenerator
{
    public BcPasswordRecipientInfoGenerator(ASN1ObjectIdentifier kekAlgorithm, char[] password)
    {
        super(kekAlgorithm, password);
    }

    public byte[] generateEncryptedBytes(AlgorithmIdentifier keyEncryptionAlgorithm, byte[] derivedKey, GenericKey contentEncryptionKey)
        throws CMSException
    {
        byte[] contentEncryptionKeySpec = ((KeyParameter)CMSUtils.getBcKey(contentEncryptionKey)).getKey();
        Wrapper keyEncryptionCipher = EnvelopedDataHelper.createRFC3211Wrapper(keyEncryptionAlgorithm.getAlgorithm());

        keyEncryptionCipher.init(true, new ParametersWithIV(new KeyParameter(derivedKey), ASN1OctetString.getInstance(keyEncryptionAlgorithm.getParameters()).getOctets()));

        return keyEncryptionCipher.wrap(contentEncryptionKeySpec, 0, contentEncryptionKeySpec.length);
    }
}
