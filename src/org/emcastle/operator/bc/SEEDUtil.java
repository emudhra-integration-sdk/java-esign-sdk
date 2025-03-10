package org.emcastle.operator.bc;

import org.emcastle.asn1.kisa.KISAObjectIdentifiers;
import org.emcastle.asn1.x509.AlgorithmIdentifier;

class SEEDUtil
{
    static AlgorithmIdentifier determineKeyEncAlg()
    {
        // parameters absent
        return new AlgorithmIdentifier(
            KISAObjectIdentifiers.id_npki_app_cmsSeed_wrap);
    }
}
