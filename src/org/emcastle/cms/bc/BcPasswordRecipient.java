package org.emcastle.cms.bc;

import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.cms.CMSException;
import org.emcastle.cms.PasswordRecipient;
import org.emcastle.crypto.InvalidCipherTextException;
import org.emcastle.crypto.Wrapper;
import org.emcastle.crypto.params.KeyParameter;
import org.emcastle.crypto.params.ParametersWithIV;

/**
 * the RecipientInfo class for a recipient who has been sent a message
 * encrypted using a password.
 */
public abstract class BcPasswordRecipient
    implements PasswordRecipient
{
    private int schemeID = PasswordRecipient.PKCS5_SCHEME2_UTF8;
    private char[] password;

    BcPasswordRecipient(
        char[] password)
    {
        this.password = password;
    }

    public BcPasswordRecipient setPasswordConversionScheme(int schemeID)
    {
        this.schemeID = schemeID;

        return this;
    }

    protected KeyParameter extractSecretKey(AlgorithmIdentifier keyEncryptionAlgorithm, AlgorithmIdentifier contentEncryptionAlgorithm, byte[] derivedKey, byte[] encryptedContentEncryptionKey)
        throws CMSException
    {
        Wrapper keyEncryptionCipher = EnvelopedDataHelper.createRFC3211Wrapper(keyEncryptionAlgorithm.getAlgorithm());

        keyEncryptionCipher.init(false, new ParametersWithIV(new KeyParameter(derivedKey), ASN1OctetString.getInstance(keyEncryptionAlgorithm.getParameters()).getOctets()));

        try
        {
            return new KeyParameter(keyEncryptionCipher.unwrap(encryptedContentEncryptionKey, 0, encryptedContentEncryptionKey.length));
        }
        catch (InvalidCipherTextException e)
        {
            throw new CMSException("unable to unwrap key: " + e.getMessage(), e);
        }
    }

    public int getPasswordConversionScheme()
    {
        return schemeID;
    }

    public char[] getPassword()
    {
        return password;
    }
}
