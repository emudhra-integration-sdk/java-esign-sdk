package org.emcastle.operator.bc;

import org.emcastle.asn1.ASN1ObjectIdentifier;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.crypto.AsymmetricBlockCipher;
import org.emcastle.crypto.encodings.PKCS1Encoding;
import org.emcastle.crypto.engines.RSAEngine;
import org.emcastle.crypto.params.AsymmetricKeyParameter;

public class BcRSAAsymmetricKeyUnwrapper
    extends BcAsymmetricKeyUnwrapper
{
    public BcRSAAsymmetricKeyUnwrapper(AlgorithmIdentifier encAlgId, AsymmetricKeyParameter privateKey)
    {
        super(encAlgId, privateKey);
    }

    protected AsymmetricBlockCipher createAsymmetricUnwrapper(ASN1ObjectIdentifier algorithm)
    {
        return new PKCS1Encoding(new RSAEngine());
    }
}
