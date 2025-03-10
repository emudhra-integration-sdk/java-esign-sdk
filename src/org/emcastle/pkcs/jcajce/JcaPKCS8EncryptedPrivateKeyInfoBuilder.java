package org.emcastle.pkcs.jcajce;

import java.security.PrivateKey;

import org.emcastle.asn1.pkcs.PrivateKeyInfo;
import org.emcastle.pkcs.PKCS8EncryptedPrivateKeyInfoBuilder;

public class JcaPKCS8EncryptedPrivateKeyInfoBuilder
    extends PKCS8EncryptedPrivateKeyInfoBuilder
{
    public JcaPKCS8EncryptedPrivateKeyInfoBuilder(PrivateKey privateKey)
    {
         super(PrivateKeyInfo.getInstance(privateKey.getEncoded()));
    }
}
