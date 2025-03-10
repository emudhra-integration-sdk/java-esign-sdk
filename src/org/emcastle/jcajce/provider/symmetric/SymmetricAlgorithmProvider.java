package org.emcastle.jcajce.provider.symmetric;

import org.emcastle.jcajce.provider.config.ConfigurableProvider;
import org.emcastle.jcajce.provider.util.AlgorithmProvider;

abstract class SymmetricAlgorithmProvider
    extends AlgorithmProvider
{
    protected void addGMacAlgorithm(
        ConfigurableProvider provider,
        String algorithm,
        String algorithmClassName,
        String keyGeneratorClassName)
    {
        provider.addAlgorithm("Mac." + algorithm + "-GMAC", algorithmClassName);
        provider.addAlgorithm("Alg.Alias.Mac." + algorithm + "GMAC", algorithm + "-GMAC");

        provider.addAlgorithm("KeyGenerator." + algorithm + "-GMAC", keyGeneratorClassName);
        provider.addAlgorithm("Alg.Alias.KeyGenerator." + algorithm + "GMAC",  algorithm + "-GMAC");
    }
}
