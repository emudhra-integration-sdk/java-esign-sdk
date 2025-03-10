package org.emcastle.pqc.crypto.gmss;

import org.emcastle.crypto.Digest;

public interface GMSSDigestProvider
{
    Digest get();
}
