package org.emcastle.pkcs.bc;

import org.emcastle.asn1.DERNull;
import org.emcastle.asn1.pkcs.PKCS12PBEParams;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.operator.MacCalculator;
import org.emcastle.operator.OperatorCreationException;
import org.emcastle.operator.bc.BcDigestProvider;
import org.emcastle.pkcs.PKCS12MacCalculatorBuilder;
import org.emcastle.pkcs.PKCS12MacCalculatorBuilderProvider;

public class BcPKCS12MacCalculatorBuilderProvider
    implements PKCS12MacCalculatorBuilderProvider
{
    private BcDigestProvider digestProvider;

    public BcPKCS12MacCalculatorBuilderProvider(BcDigestProvider digestProvider)
    {
        this.digestProvider = digestProvider;
    }

    public PKCS12MacCalculatorBuilder get(final AlgorithmIdentifier algorithmIdentifier)
    {
        return new PKCS12MacCalculatorBuilder()
        {
            public MacCalculator build(final char[] password)
                throws OperatorCreationException
            {
                PKCS12PBEParams pbeParams = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());

                return PKCS12PBEUtils.createMacCalculator(algorithmIdentifier.getAlgorithm(), digestProvider.get(algorithmIdentifier), pbeParams, password);
            }

            public AlgorithmIdentifier getDigestAlgorithmIdentifier()
            {
                return new AlgorithmIdentifier(algorithmIdentifier.getAlgorithm(), DERNull.INSTANCE);
            }
        };
    }
}
