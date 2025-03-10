package org.emcastle.crypto.tls;

import java.io.IOException;

import org.emcastle.crypto.params.AsymmetricKeyParameter;

public interface TlsAgreementCredentials
    extends TlsCredentials
{

    byte[] generateAgreement(AsymmetricKeyParameter peerPublicKey)
        throws IOException;
}
