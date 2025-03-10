package org.emcastle.crypto.util;

import java.io.IOException;

import org.emcastle.asn1.ASN1Encodable;
import org.emcastle.asn1.ASN1Integer;
import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.DERNull;
import org.emcastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.emcastle.asn1.pkcs.RSAPublicKey;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.asn1.x509.SubjectPublicKeyInfo;
import org.emcastle.asn1.x9.X962Parameters;
import org.emcastle.asn1.x9.X9ECParameters;
import org.emcastle.asn1.x9.X9ECPoint;
import org.emcastle.asn1.x9.X9ObjectIdentifiers;
import org.emcastle.crypto.params.AsymmetricKeyParameter;
import org.emcastle.crypto.params.DSAPublicKeyParameters;
import org.emcastle.crypto.params.ECDomainParameters;
import org.emcastle.crypto.params.ECPublicKeyParameters;
import org.emcastle.crypto.params.RSAKeyParameters;

/**
 * Factory to create ASN.1 subject public key info objects from lightweight public keys.
 */
public class SubjectPublicKeyInfoFactory
{
    /**
     * Create a SubjectPublicKeyInfo public key.
     *
     * @param publicKey the SubjectPublicKeyInfo encoding
     * @return the appropriate key parameter
     * @throws java.io.IOException on an error encoding the key
     */
    public static SubjectPublicKeyInfo createSubjectPublicKeyInfo(AsymmetricKeyParameter publicKey) throws IOException
    {
        if (publicKey instanceof RSAKeyParameters)
        {
            RSAKeyParameters pub = (RSAKeyParameters)publicKey;

            return new SubjectPublicKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE), new RSAPublicKey(pub.getModulus(), pub.getExponent()));
        }
        else if (publicKey instanceof DSAPublicKeyParameters)
        {
            DSAPublicKeyParameters pub = (DSAPublicKeyParameters)publicKey;

            return new SubjectPublicKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa), new ASN1Integer(pub.getY()));
        }
        else if (publicKey instanceof ECPublicKeyParameters)
        {
            ECPublicKeyParameters pub = (ECPublicKeyParameters)publicKey;
            ECDomainParameters domainParams = pub.getParameters();
            ASN1Encodable      params;

            // TODO: need to handle named curves
            if (domainParams == null)
            {
                params = new X962Parameters(DERNull.INSTANCE);      // Implicitly CA
            }
            else
            {
                X9ECParameters ecP = new X9ECParameters(
                    domainParams.getCurve(),
                    domainParams.getG(),
                    domainParams.getN(),
                    domainParams.getH(),
                    domainParams.getSeed());

                params = new X962Parameters(ecP);
            }

            ASN1OctetString p = (ASN1OctetString)new X9ECPoint(pub.getQ()).toASN1Primitive();

            return new SubjectPublicKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, params), p.getOctets());
        }
        else
        {
            throw new IOException("key parameters not recognised.");
        }
    }
}
