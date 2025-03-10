package org.emcastle.cert.crmf;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.operator.InputDecryptor;

public interface ValueDecryptorGenerator
{
    InputDecryptor getValueDecryptor(AlgorithmIdentifier keyAlg, AlgorithmIdentifier symmAlg, byte[] encKey)
        throws CRMFException;
}
