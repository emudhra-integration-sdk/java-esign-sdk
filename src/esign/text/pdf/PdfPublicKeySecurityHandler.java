package esign.text.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.emcastle.asn1.ASN1Encodable;
import org.emcastle.asn1.ASN1InputStream;
import org.emcastle.asn1.ASN1ObjectIdentifier;
import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.ASN1Primitive;
import org.emcastle.asn1.ASN1Set;
import org.emcastle.asn1.DEROctetString;
import org.emcastle.asn1.DEROutputStream;
import org.emcastle.asn1.DERSet;
import org.emcastle.asn1.cms.ContentInfo;
import org.emcastle.asn1.cms.EncryptedContentInfo;
import org.emcastle.asn1.cms.EnvelopedData;
import org.emcastle.asn1.cms.IssuerAndSerialNumber;
import org.emcastle.asn1.cms.KeyTransRecipientInfo;
import org.emcastle.asn1.cms.RecipientIdentifier;
import org.emcastle.asn1.cms.RecipientInfo;
import org.emcastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.asn1.x509.TBSCertificateStructure;

public class PdfPublicKeySecurityHandler {

    static final int SEED_LENGTH = 20;
    private ArrayList<PdfPublicKeyRecipient> recipients = null;

    private byte[] seed = new byte[20];

    public PdfPublicKeySecurityHandler() {
        try {
            KeyGenerator key = KeyGenerator.getInstance("AES");
            key.init(192, new SecureRandom());
            SecretKey sk = key.generateKey();
            System.arraycopy(sk.getEncoded(), 0, this.seed, 0, 20);
        } catch (NoSuchAlgorithmException e) {
            this.seed = SecureRandom.getSeed(20);
        }

        this.recipients = new ArrayList<PdfPublicKeyRecipient>();
    }

    public void addRecipient(PdfPublicKeyRecipient recipient) {
        this.recipients.add(recipient);
    }

    protected byte[] getSeed() {
        return (byte[]) this.seed.clone();
    }

    public int getRecipientsSize() {
        return this.recipients.size();
    }

    public byte[] getEncodedRecipient(int index) throws IOException, GeneralSecurityException {
        PdfPublicKeyRecipient recipient = this.recipients.get(index);
        byte[] cms = recipient.getCms();

        if (cms != null) {
            return cms;
        }

        Certificate certificate = recipient.getCertificate();
        int permission = recipient.getPermission();
        int revision = 3;

        permission |= (revision == 3) ? -3904 : -64;
        permission &= 0xFFFFFFFC;
        permission++;

        byte[] pkcs7input = new byte[24];

        byte one = (byte) permission;
        byte two = (byte) (permission >> 8);
        byte three = (byte) (permission >> 16);
        byte four = (byte) (permission >> 24);

        System.arraycopy(this.seed, 0, pkcs7input, 0, 20);

        pkcs7input[20] = four;
        pkcs7input[21] = three;
        pkcs7input[22] = two;
        pkcs7input[23] = one;

        ASN1Primitive obj = createDERForRecipient(pkcs7input, (X509Certificate) certificate);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        DEROutputStream k = new DEROutputStream(baos);

        k.writeObject((ASN1Encodable) obj);

        cms = baos.toByteArray();

        recipient.setCms(cms);

        return cms;
    }

    public PdfArray getEncodedRecipients() throws IOException, GeneralSecurityException {
        PdfArray EncodedRecipients = new PdfArray();
        byte[] cms = null;
        for (int i = 0; i < this.recipients.size(); i++) {
            try {
                cms = getEncodedRecipient(i);
                EncodedRecipients.add(new PdfLiteral(StringUtils.escapeString(cms)));
            } catch (GeneralSecurityException e) {
                EncodedRecipients = null;
            } catch (IOException e) {
                EncodedRecipients = null;
            }
        }
        return EncodedRecipients;
    }

    private ASN1Primitive createDERForRecipient(byte[] in, X509Certificate cert) throws IOException, GeneralSecurityException {
        String s = "1.2.840.113549.3.2";

        AlgorithmParameterGenerator algorithmparametergenerator = AlgorithmParameterGenerator.getInstance(s);
        AlgorithmParameters algorithmparameters = algorithmparametergenerator.generateParameters();
        ByteArrayInputStream bytearrayinputstream = new ByteArrayInputStream(algorithmparameters.getEncoded("ASN.1"));
        ASN1InputStream asn1inputstream = new ASN1InputStream(bytearrayinputstream);
        ASN1Primitive derobject = asn1inputstream.readObject();
        KeyGenerator keygenerator = KeyGenerator.getInstance(s);
        keygenerator.init(128);
        SecretKey secretkey = keygenerator.generateKey();
        Cipher cipher = Cipher.getInstance(s);
        cipher.init(1, secretkey, algorithmparameters);
        byte[] abyte1 = cipher.doFinal(in);
        DEROctetString deroctetstring = new DEROctetString(abyte1);
        KeyTransRecipientInfo keytransrecipientinfo = computeRecipientInfo(cert, secretkey.getEncoded());
        DERSet derset = new DERSet((ASN1Encodable) new RecipientInfo(keytransrecipientinfo));
        AlgorithmIdentifier algorithmidentifier = new AlgorithmIdentifier(new ASN1ObjectIdentifier(s), (ASN1Encodable) derobject);
        EncryptedContentInfo encryptedcontentinfo = new EncryptedContentInfo(PKCSObjectIdentifiers.data, algorithmidentifier, (ASN1OctetString) deroctetstring);

        ASN1Set set = null;
        EnvelopedData env = new EnvelopedData(null, (ASN1Set) derset, encryptedcontentinfo, set);
        ContentInfo contentinfo = new ContentInfo(PKCSObjectIdentifiers.envelopedData, (ASN1Encodable) env);

        return contentinfo.toASN1Primitive();
    }

    private KeyTransRecipientInfo computeRecipientInfo(X509Certificate x509certificate, byte[] abyte0) throws GeneralSecurityException, IOException {
        ASN1InputStream asn1inputstream = new ASN1InputStream(new ByteArrayInputStream(x509certificate.getTBSCertificate()));

        TBSCertificateStructure tbscertificatestructure = TBSCertificateStructure.getInstance(asn1inputstream.readObject());
        AlgorithmIdentifier algorithmidentifier = tbscertificatestructure.getSubjectPublicKeyInfo().getAlgorithm();

        IssuerAndSerialNumber issuerandserialnumber = new IssuerAndSerialNumber(tbscertificatestructure.getIssuer(), tbscertificatestructure.getSerialNumber().getValue());
        Cipher cipher = Cipher.getInstance(algorithmidentifier.getAlgorithm().getId());
        try {
            cipher.init(1, x509certificate);
        } catch (InvalidKeyException e) {
            cipher.init(1, x509certificate.getPublicKey());
        }
        DEROctetString deroctetstring = new DEROctetString(cipher.doFinal(abyte0));
        RecipientIdentifier recipId = new RecipientIdentifier(issuerandserialnumber);
        return new KeyTransRecipientInfo(recipId, algorithmidentifier, (ASN1OctetString) deroctetstring);
    }
}
