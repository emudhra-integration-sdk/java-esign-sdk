package org.emcastle.cms.jcajce;

import java.security.PrivateKey;

import javax.crypto.SecretKey;

import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.jcajce.JcaJceHelper;
import org.emcastle.operator.SymmetricKeyUnwrapper;
import org.emcastle.operator.jcajce.JceAsymmetricKeyUnwrapper;

public interface JcaJceExtHelper
    extends JcaJceHelper
{
    JceAsymmetricKeyUnwrapper createAsymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, PrivateKey keyEncryptionKey);

    SymmetricKeyUnwrapper createSymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, SecretKey keyEncryptionKey);
}
