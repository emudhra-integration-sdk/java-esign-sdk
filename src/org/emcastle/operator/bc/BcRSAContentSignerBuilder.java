package org.emcastle.operator.bc;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.crypto.Digest;
import org.emcastle.crypto.Signer;
import org.emcastle.crypto.signers.RSADigestSigner;
import org.emcastle.operator.OperatorCreationException;

public class BcRSAContentSignerBuilder
    extends BcContentSignerBuilder
{
    public BcRSAContentSignerBuilder(AlgorithmIdentifier sigAlgId, AlgorithmIdentifier digAlgId)
    {
        super(sigAlgId, digAlgId);
    }

    protected Signer createSigner(AlgorithmIdentifier sigAlgId, AlgorithmIdentifier digAlgId)
        throws OperatorCreationException
    {
        Digest dig = digestProvider.get(digAlgId);

        return new RSADigestSigner(dig);
    }
}
