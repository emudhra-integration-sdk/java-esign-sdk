package org.emcastle.openssl.jcajce;

import java.security.PrivateKey;

import org.emcastle.asn1.pkcs.PrivateKeyInfo;
import org.emcastle.openssl.PKCS8Generator;
import org.emcastle.operator.OutputEncryptor;
import org.emcastle.util.io.pem.PemGenerationException;

public class JcaPKCS8Generator
    extends PKCS8Generator
{
    public JcaPKCS8Generator(PrivateKey key, OutputEncryptor encryptor)
         throws PemGenerationException
    {
         super(PrivateKeyInfo.getInstance(key.getEncoded()), encryptor);
    }
}
