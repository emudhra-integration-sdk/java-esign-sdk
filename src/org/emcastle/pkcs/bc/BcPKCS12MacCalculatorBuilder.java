package org.emcastle.pkcs.bc;

import java.security.SecureRandom;

import org.emcastle.asn1.DERNull;
import org.emcastle.asn1.oiw.OIWObjectIdentifiers;
import org.emcastle.asn1.pkcs.PKCS12PBEParams;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.crypto.ExtendedDigest;
import org.emcastle.crypto.digests.SHA1Digest;
import org.emcastle.operator.MacCalculator;
import org.emcastle.pkcs.PKCS12MacCalculatorBuilder;

public class BcPKCS12MacCalculatorBuilder
    implements PKCS12MacCalculatorBuilder
{
    private ExtendedDigest digest;
    private AlgorithmIdentifier algorithmIdentifier;

    private SecureRandom  random;
    private int    saltLength;
    private int    iterationCount = 1024;

    public BcPKCS12MacCalculatorBuilder()
    {
        this(new SHA1Digest(), new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1, DERNull.INSTANCE));
    }

    public BcPKCS12MacCalculatorBuilder(ExtendedDigest digest, AlgorithmIdentifier algorithmIdentifier)
    {
        this.digest = digest;
        this.algorithmIdentifier = algorithmIdentifier;
        this.saltLength = digest.getDigestSize();
    }

    public AlgorithmIdentifier getDigestAlgorithmIdentifier()
    {
        return algorithmIdentifier;
    }

    public MacCalculator build(final char[] password)
    {
        if (random == null)
        {
            random = new SecureRandom();
        }

        byte[] salt = new byte[saltLength];

        random.nextBytes(salt);

        return PKCS12PBEUtils.createMacCalculator(algorithmIdentifier.getAlgorithm(), digest, new PKCS12PBEParams(salt, iterationCount), password);
    }
}
