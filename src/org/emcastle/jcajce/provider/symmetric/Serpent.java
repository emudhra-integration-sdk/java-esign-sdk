package org.emcastle.jcajce.provider.symmetric;

import org.emcastle.crypto.BlockCipher;
import org.emcastle.crypto.CipherKeyGenerator;
import org.emcastle.crypto.engines.SerpentEngine;
import org.emcastle.crypto.macs.GMac;
import org.emcastle.crypto.modes.GCMBlockCipher;
import org.emcastle.jcajce.provider.config.ConfigurableProvider;
import org.emcastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.emcastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.emcastle.jcajce.provider.symmetric.util.BaseMac;
import org.emcastle.jcajce.provider.symmetric.util.BlockCipherProvider;
import org.emcastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;

public final class Serpent
{
    private Serpent()
    {
    }
    
    public static class ECB
        extends BaseBlockCipher
    {
        public ECB()
        {
            super(new BlockCipherProvider()
            {
                public BlockCipher get()
                {
                    return new SerpentEngine();
                }
            });
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            super("Serpent", 192, new CipherKeyGenerator());
        }
    }

    public static class SerpentGMAC
        extends BaseMac
    {
        public SerpentGMAC()
        {
            super(new GMac(new GCMBlockCipher(new SerpentEngine())));
        }
    }

    public static class AlgParams
        extends IvAlgorithmParameters
    {
        protected String engineToString()
        {
            return "Serpent IV";
        }
    }

    public static class Mappings
        extends SymmetricAlgorithmProvider
    {
        private static final String PREFIX = Serpent.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {

            provider.addAlgorithm("Cipher.Serpent", PREFIX + "$ECB");
            provider.addAlgorithm("KeyGenerator.Serpent", PREFIX + "$KeyGen");
            provider.addAlgorithm("AlgorithmParameters.Serpent", PREFIX + "$AlgParams");

            addGMacAlgorithm(provider, "SERPENT", PREFIX + "$SerpentGMAC", PREFIX + "$KeyGen");
        }
    }
}
