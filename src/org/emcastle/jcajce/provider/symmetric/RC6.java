package org.emcastle.jcajce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.spec.IvParameterSpec;

import org.emcastle.crypto.BlockCipher;
import org.emcastle.crypto.BufferedBlockCipher;
import org.emcastle.crypto.CipherKeyGenerator;
import org.emcastle.crypto.engines.RC6Engine;
import org.emcastle.crypto.macs.GMac;
import org.emcastle.crypto.modes.CBCBlockCipher;
import org.emcastle.crypto.modes.CFBBlockCipher;
import org.emcastle.crypto.modes.GCMBlockCipher;
import org.emcastle.crypto.modes.OFBBlockCipher;
import org.emcastle.jcajce.provider.config.ConfigurableProvider;
import org.emcastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.emcastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.emcastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.emcastle.jcajce.provider.symmetric.util.BaseMac;
import org.emcastle.jcajce.provider.symmetric.util.BlockCipherProvider;
import org.emcastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;
import org.emcastle.jce.provider.emCastleProvider;

public final class RC6
{
    private RC6()
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
                    return new RC6Engine();
                }
            });
        }
    }

    public static class CBC
       extends BaseBlockCipher
    {
        public CBC()
        {
            super(new CBCBlockCipher(new RC6Engine()), 128);
        }
    }

    static public class CFB
        extends BaseBlockCipher
    {
        public CFB()
        {
            super(new BufferedBlockCipher(new CFBBlockCipher(new RC6Engine(), 128)), 128);
        }
    }

    static public class OFB
        extends BaseBlockCipher
    {
        public OFB()
        {
            super(new BufferedBlockCipher(new OFBBlockCipher(new RC6Engine(), 128)), 128);
        }
    }

    public static class GMAC
        extends BaseMac
    {
        public GMAC()
        {
            super(new GMac(new GCMBlockCipher(new RC6Engine())));
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            super("RC6", 256, new CipherKeyGenerator());
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
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for RC6 parameter generation.");
        }

        protected AlgorithmParameters engineGenerateParameters()
        {
            byte[]  iv = new byte[16];

            if (random == null)
            {
                random = new SecureRandom();
            }

            random.nextBytes(iv);

            AlgorithmParameters params;

            try
            {
                params = AlgorithmParameters.getInstance("RC6", emCastleProvider.PROVIDER_NAME);
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
            return "RC6 IV";
        }
    }

    public static class Mappings
        extends SymmetricAlgorithmProvider
    {
        private static final String PREFIX = RC6.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {

            provider.addAlgorithm("Cipher.RC6", PREFIX + "$ECB");
            provider.addAlgorithm("KeyGenerator.RC6", PREFIX + "$KeyGen");
            provider.addAlgorithm("AlgorithmParameters.RC6", PREFIX + "$AlgParams");

            addGMacAlgorithm(provider, "RC6", PREFIX + "$GMAC", PREFIX + "$KeyGen");
        }
    }
}
