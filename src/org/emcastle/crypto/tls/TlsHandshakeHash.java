package org.emcastle.crypto.tls;

import org.emcastle.crypto.Digest;

interface TlsHandshakeHash
    extends Digest
{

    void init(TlsContext context);

    TlsHandshakeHash commit();

    TlsHandshakeHash fork();
}
