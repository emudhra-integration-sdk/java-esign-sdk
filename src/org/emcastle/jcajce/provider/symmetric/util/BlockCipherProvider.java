package org.emcastle.jcajce.provider.symmetric.util;

import org.emcastle.crypto.BlockCipher;

public interface BlockCipherProvider
{
    BlockCipher get();
}
