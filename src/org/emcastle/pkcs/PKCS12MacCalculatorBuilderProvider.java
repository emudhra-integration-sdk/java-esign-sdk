package org.emcastle.pkcs;

import org.emcastle.asn1.x509.AlgorithmIdentifier;

public interface PKCS12MacCalculatorBuilderProvider
{
    PKCS12MacCalculatorBuilder get(AlgorithmIdentifier algorithmIdentifier);
}
