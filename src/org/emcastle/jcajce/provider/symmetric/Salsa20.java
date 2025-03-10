package org.emcastle.jcajce.provider.symmetric;

import org.emcastle.crypto.CipherKeyGenerator;
import org.emcastle.crypto.engines.Salsa20Engine;
import org.emcastle.jcajce.provider.config.ConfigurableProvider;
import org.emcastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.emcastle.jcajce.provider.symmetric.util.BaseStreamCipher;
import org.emcastle.jcajce.provider.util.AlgorithmProvider;

public final class Salsa20
{
    private Salsa20()
    {
    }
    
    public static class Base
        extends BaseStreamCipher
    {
        public Base()
        {
            super(new Salsa20Engine(), 8);
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            super("Salsa20", 128, new CipherKeyGenerator());
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = Salsa20.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {

            provider.addAlgorithm("Cipher.SALSA20", PREFIX + "$Base");
            provider.addAlgorithm("KeyGenerator.SALSA20", PREFIX + "$KeyGen");

        }
    }
}
