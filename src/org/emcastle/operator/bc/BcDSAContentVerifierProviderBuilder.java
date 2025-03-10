package org.emcastle.operator.bc;

import java.io.IOException;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.asn1.x509.SubjectPublicKeyInfo;
import org.emcastle.crypto.Digest;
import org.emcastle.crypto.Signer;
import org.emcastle.crypto.params.AsymmetricKeyParameter;
import org.emcastle.crypto.signers.DSADigestSigner;
import org.emcastle.crypto.signers.DSASigner;
import org.emcastle.crypto.util.PublicKeyFactory;
import org.emcastle.operator.DigestAlgorithmIdentifierFinder;
import org.emcastle.operator.OperatorCreationException;

public class BcDSAContentVerifierProviderBuilder
    extends BcContentVerifierProviderBuilder
{
    private DigestAlgorithmIdentifierFinder digestAlgorithmFinder;

    public BcDSAContentVerifierProviderBuilder(DigestAlgorithmIdentifierFinder digestAlgorithmFinder)
    {
        this.digestAlgorithmFinder = digestAlgorithmFinder;
    }

    protected Signer createSigner(AlgorithmIdentifier sigAlgId)
        throws OperatorCreationException
    {
        AlgorithmIdentifier digAlg = digestAlgorithmFinder.find(sigAlgId);
        Digest dig = digestProvider.get(digAlg);

        return new DSADigestSigner(new DSASigner(), dig);
    }

    protected AsymmetricKeyParameter extractKeyParameters(SubjectPublicKeyInfo publicKeyInfo)
        throws IOException
    {
        return PublicKeyFactory.createKey(publicKeyInfo);
    }
}
