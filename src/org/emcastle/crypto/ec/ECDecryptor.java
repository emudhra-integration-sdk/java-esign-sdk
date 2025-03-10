package org.emcastle.crypto.ec;

import org.emcastle.crypto.CipherParameters;
import org.emcastle.math.ec.ECPoint;

public interface ECDecryptor
{
    void init(CipherParameters params);

    ECPoint decrypt(ECPair cipherText);
}
