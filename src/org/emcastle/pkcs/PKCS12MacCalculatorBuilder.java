package org.emcastle.pkcs;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.operator.MacCalculator;
import org.emcastle.operator.OperatorCreationException;

public interface PKCS12MacCalculatorBuilder
{
    MacCalculator build(char[] password)
        throws OperatorCreationException;

    AlgorithmIdentifier getDigestAlgorithmIdentifier();
}
