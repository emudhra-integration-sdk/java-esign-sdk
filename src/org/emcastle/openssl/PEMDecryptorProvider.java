package org.emcastle.openssl;

import org.emcastle.operator.OperatorCreationException;

public interface PEMDecryptorProvider
{
    PEMDecryptor get(String dekAlgName)
        throws OperatorCreationException;
}
