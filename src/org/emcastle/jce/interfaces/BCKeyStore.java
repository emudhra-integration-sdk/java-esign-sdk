package org.emcastle.jce.interfaces;

import java.security.SecureRandom;

/**
 * all EM provider keystores implement this interface.
 */
public interface BCKeyStore
{
    /**
     * set the random source for the key store
     */
    public void setRandom(SecureRandom random);
}
