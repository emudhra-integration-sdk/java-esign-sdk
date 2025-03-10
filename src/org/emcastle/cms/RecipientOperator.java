package org.emcastle.cms;

import java.io.InputStream;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.operator.InputDecryptor;
import org.emcastle.operator.MacCalculator;
import org.emcastle.util.io.TeeInputStream;

public class RecipientOperator
{
    private final AlgorithmIdentifier algorithmIdentifier;
    private final Object operator;

    public RecipientOperator(InputDecryptor decryptor)
    {
        this.algorithmIdentifier = decryptor.getAlgorithmIdentifier();
        this.operator = decryptor;
    }

    public RecipientOperator(MacCalculator macCalculator)
    {
        this.algorithmIdentifier = macCalculator.getAlgorithmIdentifier();
        this.operator = macCalculator;
    }

    public InputStream getInputStream(InputStream dataIn)
    {
        if (operator instanceof InputDecryptor)
        {
            return ((InputDecryptor)operator).getInputStream(dataIn);
        }
        else
        {
            return new TeeInputStream(dataIn, ((MacCalculator)operator).getOutputStream());
        }
    }

    public boolean isMacBased()
    {
        return operator instanceof MacCalculator;
    }

    public byte[] getMac()
    {
        return ((MacCalculator)operator).getMac();
    }
}
