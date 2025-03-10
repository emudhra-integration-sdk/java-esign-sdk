package org.emcastle.dvcs;

import java.io.IOException;

import org.emcastle.asn1.ASN1Encoding;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.CMSProcessableByteArray;
import org.emcastle.cms.CMSSignedData;
import org.emcastle.cms.CMSSignedDataGenerator;

public class SignedDVCSMessageGenerator
{
    private final CMSSignedDataGenerator signedDataGen;

    public SignedDVCSMessageGenerator(CMSSignedDataGenerator signedDataGen)
    {
        this.signedDataGen = signedDataGen;
    }

    /**
     * Creates a CMSSignedData object containing the passed in DVCSMessage
     *
     * @param message the request to be signed.
     * @return an encapsulating SignedData object.
     * @throws DVCSException in the event of failure to encode the request or sign it.
     */
    public CMSSignedData build(DVCSMessage message)
        throws DVCSException
    {
        try
        {
            byte[] encapsulatedData = message.getContent().toASN1Primitive().getEncoded(ASN1Encoding.DER);

            return signedDataGen.generate(new CMSProcessableByteArray(message.getContentType(), encapsulatedData), true);
        }
        catch (CMSException e)
        {
            throw new DVCSException("Could not sign DVCS request", e);
        }
        catch (IOException e)
        {
            throw new DVCSException("Could not encode DVCS request", e);
        }
    }
}
