package org.emcastle.jcajce.provider.symmetric;

import org.emcastle.crypto.CipherKeyGenerator;
import org.emcastle.crypto.engines.Grainv1Engine;
import org.emcastle.jcajce.provider.config.ConfigurableProvider;
import org.emcastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.emcastle.jcajce.provider.symmetric.util.BaseStreamCipher;
import org.emcastle.jcajce.provider.util.AlgorithmProvider;

public final class Grainv1
{
    private Grainv1()
    {
    }
    
    public static class Base
        extends BaseStreamCipher
    {
        public Base()
        {
            super(new Grainv1Engine(), 8);
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            super("Grainv1", 80, new CipherKeyGenerator());
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = Grainv1.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("Cipher.Grainv1", PREFIX + "$Base");
            provider.addAlgorithm("KeyGenerator.Grainv1", PREFIX + "$KeyGen");
        }
    }
}
