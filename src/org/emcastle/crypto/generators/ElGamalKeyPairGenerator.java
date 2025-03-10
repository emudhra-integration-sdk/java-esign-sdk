package org.emcastle.crypto.generators;

import java.math.BigInteger;

import org.emcastle.crypto.AsymmetricCipherKeyPair;
import org.emcastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.emcastle.crypto.KeyGenerationParameters;
import org.emcastle.crypto.params.DHParameters;
import org.emcastle.crypto.params.ElGamalKeyGenerationParameters;
import org.emcastle.crypto.params.ElGamalParameters;
import org.emcastle.crypto.params.ElGamalPrivateKeyParameters;
import org.emcastle.crypto.params.ElGamalPublicKeyParameters;

/**
 * a ElGamal key pair generator.
 * <p>
 * This generates keys consistent for use with ElGamal as described in
 * page 164 of "Handbook of Applied Cryptography".
 */
public class ElGamalKeyPairGenerator
    implements AsymmetricCipherKeyPairGenerator
{
    private ElGamalKeyGenerationParameters param;

    public void init(
        KeyGenerationParameters param)
    {
        this.param = (ElGamalKeyGenerationParameters)param;
    }

    public AsymmetricCipherKeyPair generateKeyPair()
    {
        DHKeyGeneratorHelper helper = DHKeyGeneratorHelper.INSTANCE;
        ElGamalParameters egp = param.getParameters();
        DHParameters dhp = new DHParameters(egp.getP(), egp.getG(), null, egp.getL());  

        BigInteger x = helper.calculatePrivate(dhp, param.getRandom()); 
        BigInteger y = helper.calculatePublic(dhp, x);

        return new AsymmetricCipherKeyPair(
            new ElGamalPublicKeyParameters(y, egp),
            new ElGamalPrivateKeyParameters(x, egp));
    }
}
