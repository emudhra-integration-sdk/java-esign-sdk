package org.emcastle.dvcs;

import java.io.IOException;

import org.emcastle.asn1.dvcs.DVCSRequestInformationBuilder;
import org.emcastle.asn1.dvcs.Data;
import org.emcastle.asn1.dvcs.ServiceType;

/**
 * Builder of DVCSRequests to CPD service (Certify Possession of Data).
 */
public class CPDRequestBuilder
    extends DVCSRequestBuilder
{
    public CPDRequestBuilder()
    {
        super(new DVCSRequestInformationBuilder(ServiceType.CPD));
    }

    /**
     * Build CPD request.
     *
     * @param messageBytes  - data to be certified
     * @return
     * @throws DVCSException
     */
    public DVCSRequest build(byte[] messageBytes)
        throws DVCSException, IOException
    {
        Data data = new Data(messageBytes);

        return createDVCRequest(data);
    }
}
