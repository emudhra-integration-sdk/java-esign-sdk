package org.emcastle.crypto.prng;

import org.emcastle.crypto.prng.drbg.SP80090DRBG;

interface DRBGProvider
{
    SP80090DRBG get(EntropySource entropySource);
}
