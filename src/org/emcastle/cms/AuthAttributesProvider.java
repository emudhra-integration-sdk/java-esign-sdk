package org.emcastle.cms;

import org.emcastle.asn1.ASN1Set;

interface AuthAttributesProvider
{
    ASN1Set getAuthAttributes();
}
