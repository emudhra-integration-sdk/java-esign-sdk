package org.emcastle.operator.bc;

import java.io.IOException;

import org.emcastle.asn1.ASN1ObjectIdentifier;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.asn1.x509.SubjectPublicKeyInfo;
import org.emcastle.crypto.AsymmetricBlockCipher;
import org.emcastle.crypto.encodings.PKCS1Encoding;
import org.emcastle.crypto.engines.RSAEngine;
import org.emcastle.crypto.params.AsymmetricKeyParameter;
import org.emcastle.crypto.util.PublicKeyFactory;

public class BcRSAAsymmetricKeyWrapper
    extends BcAsymmetricKeyWrapper
{
    public BcRSAAsymmetricKeyWrapper(AlgorithmIdentifier encAlgId, AsymmetricKeyParameter publicKey)
    {
        super(encAlgId, publicKey);
    }

    public BcRSAAsymmetricKeyWrapper(AlgorithmIdentifier encAlgId, SubjectPublicKeyInfo publicKeyInfo)
        throws IOException
    {
        super(encAlgId, PublicKeyFactory.createKey(publicKeyInfo));
    }

    protected AsymmetricBlockCipher createAsymmetricWrapper(ASN1ObjectIdentifier algorithm)
    {
        return new PKCS1Encoding(new RSAEngine());
    }
}
