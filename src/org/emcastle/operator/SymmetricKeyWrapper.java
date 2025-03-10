package org.emcastle.operator;

import org.emcastle.asn1.x509.AlgorithmIdentifier;

public abstract class SymmetricKeyWrapper
    implements KeyWrapper
{
    private AlgorithmIdentifier algorithmId;

    protected SymmetricKeyWrapper(AlgorithmIdentifier algorithmId)
    {
        this.algorithmId = algorithmId;
    }

    public AlgorithmIdentifier getAlgorithmIdentifier()
    {
        return algorithmId;
    }
}
