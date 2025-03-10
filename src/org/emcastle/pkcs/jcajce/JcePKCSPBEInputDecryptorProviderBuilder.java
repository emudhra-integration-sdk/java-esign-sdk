package org.emcastle.pkcs.jcajce;

import java.io.InputStream;
import java.security.Provider;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.emcastle.asn1.ASN1ObjectIdentifier;
import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.pkcs.PBES2Parameters;
import org.emcastle.asn1.pkcs.PBKDF2Params;
import org.emcastle.asn1.pkcs.PKCS12PBEParams;
import org.emcastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.jcajce.DefaultJcaJceHelper;
import org.emcastle.jcajce.JcaJceHelper;
import org.emcastle.jcajce.NamedJcaJceHelper;
import org.emcastle.jcajce.ProviderJcaJceHelper;
import org.emcastle.jcajce.provider.symmetric.util.BCPBEKey;
import org.emcastle.operator.DefaultSecretKeyProvider;
import org.emcastle.operator.GenericKey;
import org.emcastle.operator.InputDecryptor;
import org.emcastle.operator.InputDecryptorProvider;
import org.emcastle.operator.OperatorCreationException;
import org.emcastle.operator.SecretKeySizeProvider;
import org.emcastle.operator.jcajce.JceGenericKey;

public class JcePKCSPBEInputDecryptorProviderBuilder
{
    private JcaJceHelper helper = new DefaultJcaJceHelper();
    private boolean      wrongPKCS12Zero = false;
    private SecretKeySizeProvider keySizeProvider = DefaultSecretKeyProvider.INSTANCE;

    public JcePKCSPBEInputDecryptorProviderBuilder()
    {
    }

    public JcePKCSPBEInputDecryptorProviderBuilder setProvider(Provider provider)
    {
        this.helper = new ProviderJcaJceHelper(provider);

        return this;
    }

    public JcePKCSPBEInputDecryptorProviderBuilder setProvider(String providerName)
    {
        this.helper = new NamedJcaJceHelper(providerName);

        return this;
    }

    public JcePKCSPBEInputDecryptorProviderBuilder setTryWrongPKCS12Zero(boolean tryWrong)
    {
        this.wrongPKCS12Zero = tryWrong;

        return this;
    }

    /**
     * Set the lookup provider of AlgorithmIdentifier returning key_size_in_bits used to
     * handle PKCS5 decryption.
     *
     * @param keySizeProvider  a provider of integer secret key sizes.
     *
     * @return the current builder.
     */
    public JcePKCSPBEInputDecryptorProviderBuilder setKeySizeProvider(SecretKeySizeProvider keySizeProvider)
    {
        this.keySizeProvider = keySizeProvider;

        return this;
    }

    public InputDecryptorProvider build(final char[] password)
    {
        return new InputDecryptorProvider()
        {
            private Cipher cipher;
            private SecretKey key;
            private AlgorithmIdentifier encryptionAlg;

            public InputDecryptor get(final AlgorithmIdentifier algorithmIdentifier)
                throws OperatorCreationException
            {
                ASN1ObjectIdentifier algorithm = algorithmIdentifier.getAlgorithm();

                try
                {
                    if (algorithm.on(PKCSObjectIdentifiers.pkcs_12PbeIds))
                    {
                        PKCS12PBEParams pbeParams = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());

                        PBEKeySpec pbeSpec = new PBEKeySpec(password);

                        SecretKeyFactory keyFact = helper.createSecretKeyFactory(algorithm.getId());

                        PBEParameterSpec defParams = new PBEParameterSpec(
                            pbeParams.getIV(),
                            pbeParams.getIterations().intValue());

                        key = keyFact.generateSecret(pbeSpec);

                        if (key instanceof BCPBEKey)
                        {
                            ((BCPBEKey)key).setTryWrongPKCS12Zero(wrongPKCS12Zero);
                        }

                        cipher = helper.createCipher(algorithm.getId());

                        cipher.init(Cipher.DECRYPT_MODE, key, defParams);

                        encryptionAlg = algorithmIdentifier;
                    }
                    else if (algorithm.equals(PKCSObjectIdentifiers.id_PBES2))
                    {
                        PBES2Parameters alg = PBES2Parameters.getInstance(algorithmIdentifier.getParameters());
                        PBKDF2Params func = PBKDF2Params.getInstance(alg.getKeyDerivationFunc().getParameters());
                        AlgorithmIdentifier encScheme = AlgorithmIdentifier.getInstance(alg.getEncryptionScheme());

                        SecretKeyFactory keyFact = helper.createSecretKeyFactory(alg.getKeyDerivationFunc().getAlgorithm().getId());

                        key = keyFact.generateSecret(new PBEKeySpec(password, func.getSalt(), func.getIterationCount().intValue(), keySizeProvider.getKeySize(encScheme)));

                        cipher = helper.createCipher(alg.getEncryptionScheme().getAlgorithm().getId());

                        encryptionAlg = AlgorithmIdentifier.getInstance(alg.getEncryptionScheme());

                        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ASN1OctetString.getInstance(alg.getEncryptionScheme().getParameters()).getOctets()));
                    }
                }
                catch (Exception e)
                {
                    throw new OperatorCreationException("unable to create InputDecryptor: " + e.getMessage(), e);
                }

                return new InputDecryptor()
                {
                    public AlgorithmIdentifier getAlgorithmIdentifier()
                    {
                        return encryptionAlg;
                    }

                    public InputStream getInputStream(InputStream input)
                    {
                        return new CipherInputStream(input, cipher);
                    }

                    public GenericKey getKey()
                    {
                        return new JceGenericKey(encryptionAlg, key);
                    }
                };
            }
        };
    }
}
