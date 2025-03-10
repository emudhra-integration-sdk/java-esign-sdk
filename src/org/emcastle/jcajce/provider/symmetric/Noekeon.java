package org.emcastle.jcajce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.spec.IvParameterSpec;

import org.emcastle.crypto.CipherKeyGenerator;
import org.emcastle.crypto.engines.NoekeonEngine;
import org.emcastle.crypto.macs.GMac;
import org.emcastle.crypto.modes.GCMBlockCipher;
import org.emcastle.jcajce.provider.config.ConfigurableProvider;
import org.emcastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.emcastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.emcastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.emcastle.jcajce.provider.symmetric.util.BaseMac;
import org.emcastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;
import org.emcastle.jce.provider.emCastleProvider;

public final class Noekeon
{
    private Noekeon()
    {
    }

    public static class ECB
        extends BaseBlockCipher
    {
        public ECB()
        {
            super(new NoekeonEngine());
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            super("Noekeon", 128, new CipherKeyGenerator());
        }
    }

    public static class GMAC
        extends BaseMac
    {
        public GMAC()
        {
            super(new GMac(new GCMBlockCipher(new NoekeonEngine())));
        }
    }

    public static class AlgParamGen
        extends BaseAlgorithmParameterGenerator
    {
        protected void engineInit(
            AlgorithmParameterSpec genParamSpec,
            SecureRandom random)
            throws InvalidAlgorithmParameterException
        {
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for Noekeon parameter generation.");
        }

        protected AlgorithmParameters engineGenerateParameters()
        {
            byte[] iv = new byte[16];

            if (random == null)
            {
                random = new SecureRandom();
            }

            random.nextBytes(iv);

            AlgorithmParameters params;

            try
            {
                params = AlgorithmParameters.getInstance("Noekeon", emCastleProvider.PROVIDER_NAME);
                params.init(new IvParameterSpec(iv));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e.getMessage());
            }

            return params;
        }
    }

    public static class AlgParams
        extends IvAlgorithmParameters
    {
        protected String engineToString()
        {
            return "Noekeon IV";
        }
    }

    public static class Mappings
        extends SymmetricAlgorithmProvider
    {
        private static final String PREFIX = Noekeon.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {

            provider.addAlgorithm("AlgorithmParameters.NOEKEON", PREFIX + "$AlgParams");

            provider.addAlgorithm("AlgorithmParameterGenerator.NOEKEON", PREFIX + "$AlgParamGen");

            provider.addAlgorithm("Cipher.NOEKEON", PREFIX + "$ECB");

            provider.addAlgorithm("KeyGenerator.NOEKEON", PREFIX + "$KeyGen");

            addGMacAlgorithm(provider, "NOEKEON", PREFIX + "$GMAC", PREFIX + "$KeyGen");
        }
    }
}
