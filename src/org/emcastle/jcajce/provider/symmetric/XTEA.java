package org.emcastle.jcajce.provider.symmetric;

import org.emcastle.crypto.CipherKeyGenerator;
import org.emcastle.crypto.engines.XTEAEngine;
import org.emcastle.jcajce.provider.config.ConfigurableProvider;
import org.emcastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.emcastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.emcastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;
import org.emcastle.jcajce.provider.util.AlgorithmProvider;

public final class XTEA
{
    private XTEA()
    {
    }
    
    public static class ECB
        extends BaseBlockCipher
    {
        public ECB()
        {
            super(new XTEAEngine());
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            super("XTEA", 128, new CipherKeyGenerator());
        }
    }

    public static class AlgParams
        extends IvAlgorithmParameters
    {
        protected String engineToString()
        {
            return "XTEA IV";
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = XTEA.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {

            provider.addAlgorithm("Cipher.XTEA", PREFIX + "$ECB");
            provider.addAlgorithm("KeyGenerator.XTEA", PREFIX + "$KeyGen");
            provider.addAlgorithm("AlgorithmParameters.XTEA", PREFIX + "$AlgParams");

        }
    }
}
