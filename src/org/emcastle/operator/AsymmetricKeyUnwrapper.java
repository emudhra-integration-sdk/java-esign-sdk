package org.emcastle.operator;

import org.emcastle.asn1.x509.AlgorithmIdentifier;

public abstract class AsymmetricKeyUnwrapper
    implements KeyUnwrapper
{
    private AlgorithmIdentifier algorithmId;

    protected AsymmetricKeyUnwrapper(AlgorithmIdentifier algorithmId)
    {
        this.algorithmId = algorithmId;
    }

    public AlgorithmIdentifier getAlgorithmIdentifier()
    {
        return algorithmId;
    }
}
