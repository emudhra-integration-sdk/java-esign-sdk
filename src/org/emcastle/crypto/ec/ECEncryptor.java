package org.emcastle.crypto.ec;

import org.emcastle.crypto.CipherParameters;
import org.emcastle.math.ec.ECPoint;

public interface ECEncryptor
{
    void init(CipherParameters params);

    ECPair encrypt(ECPoint point);
}
