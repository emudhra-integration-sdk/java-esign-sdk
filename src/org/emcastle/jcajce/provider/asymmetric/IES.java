package org.emcastle.jcajce.provider.asymmetric;

import org.emcastle.jcajce.provider.config.ConfigurableProvider;
import org.emcastle.jcajce.provider.util.AsymmetricAlgorithmProvider;

public class IES
{
    private static final String PREFIX = "org.emcastle.jcajce.provider.asymmetric" + ".ies.";

    public static class Mappings
        extends AsymmetricAlgorithmProvider
    {
        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("AlgorithmParameters.IES", PREFIX + "AlgorithmParametersSpi");
            provider.addAlgorithm("Cipher.IES", PREFIX + "CipherSpi$IES");
        }
    }
}
