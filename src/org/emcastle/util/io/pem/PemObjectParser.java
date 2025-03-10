package org.emcastle.util.io.pem;

import java.io.IOException;

public interface PemObjectParser
{
    Object parseObject(PemObject obj)
            throws IOException;
}
