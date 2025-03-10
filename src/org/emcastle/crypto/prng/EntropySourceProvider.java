package org.emcastle.crypto.prng;

public interface EntropySourceProvider
{
    EntropySource get(final int bitsRequired);
}
