package org.emcastle.pkcs.bc;

import java.io.InputStream;

import org.emcastle.asn1.pkcs.PKCS12PBEParams;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.crypto.CipherParameters;
import org.emcastle.crypto.ExtendedDigest;
import org.emcastle.crypto.digests.SHA1Digest;
import org.emcastle.crypto.generators.PKCS12ParametersGenerator;
import org.emcastle.crypto.io.CipherInputStream;
import org.emcastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.emcastle.operator.GenericKey;
import org.emcastle.operator.InputDecryptor;
import org.emcastle.operator.InputDecryptorProvider;

public class BcPKCS12PBEInputDecryptorProviderBuilder
{
    private ExtendedDigest digest;

    public BcPKCS12PBEInputDecryptorProviderBuilder()
    {
         this(new SHA1Digest());
    }

    public BcPKCS12PBEInputDecryptorProviderBuilder(ExtendedDigest digest)
    {
         this.digest = digest;
    }

    public InputDecryptorProvider build(final char[] password)
    {
        return new InputDecryptorProvider()
        {
            public InputDecryptor get(final AlgorithmIdentifier algorithmIdentifier)
            {
                final PaddedBufferedBlockCipher engine = PKCS12PBEUtils.getEngine(algorithmIdentifier.getAlgorithm());

                PKCS12PBEParams           pbeParams = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());

                CipherParameters params = PKCS12PBEUtils.createCipherParameters(algorithmIdentifier.getAlgorithm(), digest, engine.getBlockSize(), pbeParams, password);

                engine.init(false, params);

                return new InputDecryptor()
                {
                    public AlgorithmIdentifier getAlgorithmIdentifier()
                    {
                        return algorithmIdentifier;
                    }

                    public InputStream getInputStream(InputStream input)
                    {
                        return new CipherInputStream(input, engine);
                    }

                    public GenericKey getKey()
                    {
                        return new GenericKey(PKCS12ParametersGenerator.PKCS12PasswordToBytes(password));
                    }
                };
            }
        };

    }
}
