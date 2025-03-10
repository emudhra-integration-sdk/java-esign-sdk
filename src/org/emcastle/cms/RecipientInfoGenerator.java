package org.emcastle.cms;

import org.emcastle.asn1.cms.RecipientInfo;
import org.emcastle.operator.GenericKey;

public interface RecipientInfoGenerator
{
    RecipientInfo generate(GenericKey contentEncryptionKey)
        throws CMSException;
}
