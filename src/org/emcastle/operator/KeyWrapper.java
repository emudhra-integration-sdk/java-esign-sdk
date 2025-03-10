package org.emcastle.operator;

import org.emcastle.asn1.x509.AlgorithmIdentifier;

public interface KeyWrapper
{
    AlgorithmIdentifier getAlgorithmIdentifier();

    byte[] generateWrappedKey(GenericKey encryptionKey)
        throws OperatorException;
}
