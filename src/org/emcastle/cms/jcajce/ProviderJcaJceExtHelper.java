package org.emcastle.cms.jcajce;

import java.security.PrivateKey;
import java.security.Provider;

import javax.crypto.SecretKey;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.jcajce.ProviderJcaJceHelper;
import org.emcastle.operator.SymmetricKeyUnwrapper;
import org.emcastle.operator.jcajce.JceAsymmetricKeyUnwrapper;
import org.emcastle.operator.jcajce.JceSymmetricKeyUnwrapper;

class ProviderJcaJceExtHelper
    extends ProviderJcaJceHelper
    implements JcaJceExtHelper
{
    public ProviderJcaJceExtHelper(Provider provider)
    {
        super(provider);
    }

    public JceAsymmetricKeyUnwrapper createAsymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, PrivateKey keyEncryptionKey)
    {
        return new JceAsymmetricKeyUnwrapper(keyEncryptionAlgorithm, keyEncryptionKey).setProvider(provider);
    }

    public SymmetricKeyUnwrapper createSymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, SecretKey keyEncryptionKey)
    {
        return new JceSymmetricKeyUnwrapper(keyEncryptionAlgorithm, keyEncryptionKey).setProvider(provider);
    }
}