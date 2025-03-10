package org.emcastle.operator.bc;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.crypto.ExtendedDigest;
import org.emcastle.operator.OperatorCreationException;

public interface BcDigestProvider
{
    ExtendedDigest get(AlgorithmIdentifier digestAlgorithmIdentifier)
        throws OperatorCreationException;
}
