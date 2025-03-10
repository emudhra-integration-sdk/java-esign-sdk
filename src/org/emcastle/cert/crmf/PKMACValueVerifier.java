package org.emcastle.cert.crmf;

import java.io.IOException;
import java.io.OutputStream;

import org.emcastle.asn1.ASN1Encoding;
import org.emcastle.asn1.cmp.PBMParameter;
import org.emcastle.asn1.crmf.PKMACValue;
import org.emcastle.asn1.x509.SubjectPublicKeyInfo;
import org.emcastle.operator.MacCalculator;
import org.emcastle.util.Arrays;

class PKMACValueVerifier
{
    private final PKMACBuilder builder;

    public PKMACValueVerifier(PKMACBuilder builder)
    {
        this.builder = builder;
    }

    public boolean isValid(PKMACValue value, char[] password, SubjectPublicKeyInfo keyInfo)
        throws CRMFException
    {
        builder.setParameters(PBMParameter.getInstance(value.getAlgId().getParameters()));
        MacCalculator calculator = builder.build(password);

        OutputStream macOut = calculator.getOutputStream();

        try
        {
            macOut.write(keyInfo.getEncoded(ASN1Encoding.DER));

            macOut.close();
        }
        catch (IOException e)
        {
            throw new CRMFException("exception encoding mac input: " + e.getMessage(), e);
        }

        return Arrays.areEqual(calculator.getMac(), value.getValue().getBytes());
    }
}