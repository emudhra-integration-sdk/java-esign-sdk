package org.emcastle.operator.bc;

import java.security.SecureRandom;

import org.emcastle.asn1.ASN1ObjectIdentifier;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.crypto.AsymmetricBlockCipher;
import org.emcastle.crypto.CipherParameters;
import org.emcastle.crypto.InvalidCipherTextException;
import org.emcastle.crypto.params.AsymmetricKeyParameter;
import org.emcastle.crypto.params.ParametersWithRandom;
import org.emcastle.operator.AsymmetricKeyWrapper;
import org.emcastle.operator.GenericKey;
import org.emcastle.operator.OperatorException;

public abstract class BcAsymmetricKeyWrapper
    extends AsymmetricKeyWrapper
{
    private AsymmetricKeyParameter publicKey;
    private SecureRandom random;

    public BcAsymmetricKeyWrapper(AlgorithmIdentifier encAlgId, AsymmetricKeyParameter publicKey)
    {
        super(encAlgId);

        this.publicKey = publicKey;
    }

    public BcAsymmetricKeyWrapper setSecureRandom(SecureRandom random)
    {
        this.random = random;

        return this;
    }

    public byte[] generateWrappedKey(GenericKey encryptionKey)
        throws OperatorException
    {
        AsymmetricBlockCipher keyEncryptionCipher = createAsymmetricWrapper(getAlgorithmIdentifier().getAlgorithm());
        
        CipherParameters params = publicKey;
        if (random != null)
        {
            params = new ParametersWithRandom(params, random);
        }

        try
        {
            byte[] keyEnc = OperatorUtils.getKeyBytes(encryptionKey);
            keyEncryptionCipher.init(true, publicKey);
            return keyEncryptionCipher.processBlock(keyEnc, 0, keyEnc.length);
        }
        catch (InvalidCipherTextException e)
        {
            throw new OperatorException("unable to encrypt contents key", e);
        }
    }

    protected abstract AsymmetricBlockCipher createAsymmetricWrapper(ASN1ObjectIdentifier algorithm);
}
