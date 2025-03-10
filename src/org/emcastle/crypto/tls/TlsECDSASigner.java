package org.emcastle.crypto.tls;

import org.emcastle.crypto.DSA;
import org.emcastle.crypto.params.AsymmetricKeyParameter;
import org.emcastle.crypto.params.ECPublicKeyParameters;
import org.emcastle.crypto.signers.ECDSASigner;

public class TlsECDSASigner
    extends TlsDSASigner
{

    public boolean isValidPublicKey(AsymmetricKeyParameter publicKey)
    {
        return publicKey instanceof ECPublicKeyParameters;
    }

    protected DSA createDSAImpl()
    {
        return new ECDSASigner();
    }
}
