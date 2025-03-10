package org.emcastle.operator;

import org.emcastle.asn1.x509.AlgorithmIdentifier;

public interface InputExpanderProvider
{
    InputExpander get(AlgorithmIdentifier algorithm);
}
