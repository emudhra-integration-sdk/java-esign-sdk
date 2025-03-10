package org.emcastle.crypto.util;

import java.io.IOException;

import org.emcastle.asn1.ASN1Integer;
import org.emcastle.asn1.DERNull;
import org.emcastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.emcastle.asn1.pkcs.PrivateKeyInfo;
import org.emcastle.asn1.pkcs.RSAPrivateKey;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.asn1.x509.DSAParameter;
import org.emcastle.asn1.x9.X9ObjectIdentifiers;
import org.emcastle.crypto.params.AsymmetricKeyParameter;
import org.emcastle.crypto.params.DSAParameters;
import org.emcastle.crypto.params.DSAPrivateKeyParameters;
import org.emcastle.crypto.params.RSAKeyParameters;
import org.emcastle.crypto.params.RSAPrivateCrtKeyParameters;

/**
 * Factory to create ASN.1 private key info objects from lightweight private keys.
 */
public class PrivateKeyInfoFactory
{
    /**
     * Create a PrivateKeyInfo representation of a private key.
     *
     * @param privateKey the SubjectPublicKeyInfo encoding
     * @return the appropriate key parameter
     * @throws java.io.IOException on an error encoding the key
     */
    public static PrivateKeyInfo createPrivateKeyInfo(AsymmetricKeyParameter privateKey) throws IOException
    {
        if (privateKey instanceof RSAKeyParameters)
        {
            RSAPrivateCrtKeyParameters priv = (RSAPrivateCrtKeyParameters)privateKey;

            return new PrivateKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE), new RSAPrivateKey(priv.getModulus(), priv.getPublicExponent(), priv.getExponent(), priv.getP(), priv.getQ(), priv.getDP(), priv.getDQ(), priv.getQInv()));
        }
        else if (privateKey instanceof DSAPrivateKeyParameters)
        {
            DSAPrivateKeyParameters priv = (DSAPrivateKeyParameters)privateKey;
            DSAParameters params = priv.getParameters();

            return new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa, new DSAParameter(params.getP(), params.getQ(), params.getG())), new ASN1Integer(priv.getX()));
        }
        else
        {
            throw new IOException("key parameters not recognised.");
        }
    }
}
