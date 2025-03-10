package org.emcastle.operator;

import org.emcastle.asn1.x509.AlgorithmIdentifier;

public interface InputDecryptorProvider
{
    public InputDecryptor get(AlgorithmIdentifier algorithm)
        throws OperatorCreationException;
}
