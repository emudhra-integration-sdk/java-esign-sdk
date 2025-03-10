package org.emcastle.jcajce.provider.asymmetric.dh;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Hashtable;

import javax.crypto.spec.DHParameterSpec;

import org.emcastle.crypto.AsymmetricCipherKeyPair;
import org.emcastle.crypto.generators.DHBasicKeyPairGenerator;
import org.emcastle.crypto.generators.DHParametersGenerator;
import org.emcastle.crypto.params.DHKeyGenerationParameters;
import org.emcastle.crypto.params.DHParameters;
import org.emcastle.crypto.params.DHPrivateKeyParameters;
import org.emcastle.crypto.params.DHPublicKeyParameters;
import org.emcastle.jce.provider.emCastleProvider;
import org.emcastle.util.Integers;

public class KeyPairGeneratorSpi
    extends java.security.KeyPairGenerator
{
    private static Hashtable params = new Hashtable();
    private static Object    lock = new Object();

    DHKeyGenerationParameters param;
    DHBasicKeyPairGenerator engine = new DHBasicKeyPairGenerator();
    int strength = 1024;
    int certainty = 20;
    SecureRandom random = new SecureRandom();
    boolean initialised = false;

    public KeyPairGeneratorSpi()
    {
        super("DH");
    }

    public void initialize(
        int strength,
        SecureRandom random)
    {
        this.strength = strength;
        this.random = random;
    }

    public void initialize(
        AlgorithmParameterSpec params,
        SecureRandom random)
        throws InvalidAlgorithmParameterException
    {
        if (!(params instanceof DHParameterSpec))
        {
            throw new InvalidAlgorithmParameterException("parameter object not a DHParameterSpec");
        }
        DHParameterSpec dhParams = (DHParameterSpec)params;

        param = new DHKeyGenerationParameters(random, new DHParameters(dhParams.getP(), dhParams.getG(), null, dhParams.getL()));

        engine.init(param);
        initialised = true;
    }

    public KeyPair generateKeyPair()
    {
        if (!initialised)
        {
            Integer paramStrength = Integers.valueOf(strength);

            if (params.containsKey(paramStrength))
            {
                param = (DHKeyGenerationParameters)params.get(paramStrength);
            }
            else
            {
                DHParameterSpec dhParams = emCastleProvider.CONFIGURATION.getDHDefaultParameters(strength);

                if (dhParams != null)
                {
                    param = new DHKeyGenerationParameters(random, new DHParameters(dhParams.getP(), dhParams.getG(), null, dhParams.getL()));
                }
                else
                {
                    synchronized (lock)
                    {
                        // we do the check again in case we were blocked by a generator for
                        // our key size.
                        if (params.containsKey(paramStrength))
                        {
                            param = (DHKeyGenerationParameters)params.get(paramStrength);
                        }
                        else
                        {

                            DHParametersGenerator pGen = new DHParametersGenerator();

                            pGen.init(strength, certainty, random);

                            param = new DHKeyGenerationParameters(random, pGen.generateParameters());

                            params.put(paramStrength, param);
                        }
                    }
                }
            }

            engine.init(param);

            initialised = true;
        }

        AsymmetricCipherKeyPair pair = engine.generateKeyPair();
        DHPublicKeyParameters pub = (DHPublicKeyParameters)pair.getPublic();
        DHPrivateKeyParameters priv = (DHPrivateKeyParameters)pair.getPrivate();

        return new KeyPair(new BCDHPublicKey(pub),
            new BCDHPrivateKey(priv));
    }
}
