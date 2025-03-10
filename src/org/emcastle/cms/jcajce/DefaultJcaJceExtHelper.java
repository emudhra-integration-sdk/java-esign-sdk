package org.emcastle.cms.jcajce;

import java.security.PrivateKey;

import javax.crypto.SecretKey;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.jcajce.DefaultJcaJceHelper;
import org.emcastle.operator.SymmetricKeyUnwrapper;
import org.emcastle.operator.jcajce.JceAsymmetricKeyUnwrapper;
import org.emcastle.operator.jcajce.JceSymmetricKeyUnwrapper;

class DefaultJcaJceExtHelper
    extends DefaultJcaJceHelper
    implements JcaJceExtHelper
{
    public JceAsymmetricKeyUnwrapper createAsymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, PrivateKey keyEncryptionKey)
    {
        return new JceAsymmetricKeyUnwrapper(keyEncryptionAlgorithm, keyEncryptionKey);
    }

    public SymmetricKeyUnwrapper createSymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, SecretKey keyEncryptionKey)
    {
        return new JceSymmetricKeyUnwrapper(keyEncryptionAlgorithm, keyEncryptionKey);
    }
}
