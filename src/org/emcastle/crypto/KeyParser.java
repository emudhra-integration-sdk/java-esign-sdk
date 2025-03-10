package org.emcastle.crypto;

import java.io.IOException;
import java.io.InputStream;

import org.emcastle.crypto.params.AsymmetricKeyParameter;

public interface KeyParser
{
    AsymmetricKeyParameter readKey(InputStream stream)
        throws IOException;
}
