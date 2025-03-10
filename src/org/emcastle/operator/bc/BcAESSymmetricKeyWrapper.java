package org.emcastle.operator.bc;

import org.emcastle.crypto.engines.AESWrapEngine;
import org.emcastle.crypto.params.KeyParameter;

public class BcAESSymmetricKeyWrapper
    extends BcSymmetricKeyWrapper
{
    public BcAESSymmetricKeyWrapper(KeyParameter wrappingKey)
    {
        super(AESUtil.determineKeyEncAlg(wrappingKey), new AESWrapEngine(), wrappingKey);
    }
}
