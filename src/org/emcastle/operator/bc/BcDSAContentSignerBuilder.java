package org.emcastle.operator.bc;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.crypto.Digest;
import org.emcastle.crypto.Signer;
import org.emcastle.crypto.signers.DSADigestSigner;
import org.emcastle.crypto.signers.DSASigner;
import org.emcastle.operator.OperatorCreationException;

public class BcDSAContentSignerBuilder
    extends BcContentSignerBuilder
{
    public BcDSAContentSignerBuilder(AlgorithmIdentifier sigAlgId, AlgorithmIdentifier digAlgId)
    {
        super(sigAlgId, digAlgId);
    }

    protected Signer createSigner(AlgorithmIdentifier sigAlgId, AlgorithmIdentifier digAlgId)
        throws OperatorCreationException
    {
        Digest dig = digestProvider.get(digAlgId);

        return new DSADigestSigner(new DSASigner(), dig);
    }
}
