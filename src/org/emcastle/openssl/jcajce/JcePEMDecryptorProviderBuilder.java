package org.emcastle.openssl.jcajce;

import java.security.Provider;

import org.emcastle.jcajce.DefaultJcaJceHelper;
import org.emcastle.jcajce.JcaJceHelper;
import org.emcastle.jcajce.NamedJcaJceHelper;
import org.emcastle.jcajce.ProviderJcaJceHelper;
import org.emcastle.openssl.PEMDecryptor;
import org.emcastle.openssl.PEMDecryptorProvider;
import org.emcastle.openssl.PEMException;
import org.emcastle.openssl.PasswordException;

public class JcePEMDecryptorProviderBuilder
{
    private JcaJceHelper helper = new DefaultJcaJceHelper();

    public JcePEMDecryptorProviderBuilder setProvider(Provider provider)
    {
        this.helper = new ProviderJcaJceHelper(provider);

        return this;
    }

    public JcePEMDecryptorProviderBuilder setProvider(String providerName)
    {
        this.helper = new NamedJcaJceHelper(providerName);

        return this;
    }

    public PEMDecryptorProvider build(final char[] password)
    {
        return new PEMDecryptorProvider()
        {
            public PEMDecryptor get(final String dekAlgName)
            {
                return new PEMDecryptor()
                {
                    public byte[] decrypt(byte[] keyBytes, byte[] iv)
                        throws PEMException
                    {
                        if (password == null)
                        {
                            throw new PasswordException("Password is null, but a password is required");
                        }

                        return PEMUtilities.crypt(false, helper, keyBytes, password, dekAlgName, iv);
                    }
                };
            }
        };
    }
}
