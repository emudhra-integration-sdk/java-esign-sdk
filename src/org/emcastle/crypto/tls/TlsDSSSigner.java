package org.emcastle.crypto.tls;

import org.emcastle.crypto.DSA;
import org.emcastle.crypto.params.AsymmetricKeyParameter;
import org.emcastle.crypto.params.DSAPublicKeyParameters;
import org.emcastle.crypto.signers.DSASigner;

public class TlsDSSSigner
    extends TlsDSASigner
{

    public boolean isValidPublicKey(AsymmetricKeyParameter publicKey)
    {
        return publicKey instanceof DSAPublicKeyParameters;
    }

    protected DSA createDSAImpl()
    {
        return new DSASigner();
    }
}
