package org.emcastle.operator;

import org.emcastle.asn1.x509.AlgorithmIdentifier;

public interface SecretKeySizeProvider
{
    int getKeySize(AlgorithmIdentifier algorithmIdentifier);
}
