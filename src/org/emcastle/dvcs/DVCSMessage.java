package org.emcastle.dvcs;

import org.emcastle.asn1.ASN1Encodable;
import org.emcastle.asn1.ASN1ObjectIdentifier;
import org.emcastle.asn1.cms.ContentInfo;

public abstract class DVCSMessage
{
    private final ContentInfo contentInfo;

    protected DVCSMessage(ContentInfo contentInfo)
    {
        this.contentInfo = contentInfo;
    }

    public ASN1ObjectIdentifier getContentType()
    {
        return contentInfo.getContentType();
    }

    public abstract ASN1Encodable getContent();
}
