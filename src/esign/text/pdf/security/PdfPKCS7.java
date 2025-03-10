package esign.text.pdf.security;

import esign.text.ExceptionConverter;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PdfName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import org.emcastle.asn1.ASN1Encodable;
import org.emcastle.asn1.ASN1EncodableVector;
import org.emcastle.asn1.ASN1Enumerated;
import org.emcastle.asn1.ASN1InputStream;
import org.emcastle.asn1.ASN1Integer;
import org.emcastle.asn1.ASN1ObjectIdentifier;
import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.ASN1OutputStream;
import org.emcastle.asn1.ASN1Primitive;
import org.emcastle.asn1.ASN1Sequence;
import org.emcastle.asn1.ASN1Set;
import org.emcastle.asn1.ASN1TaggedObject;
import org.emcastle.asn1.DERNull;
import org.emcastle.asn1.DEROctetString;
import org.emcastle.asn1.DERSequence;
import org.emcastle.asn1.DERSet;
import org.emcastle.asn1.DERTaggedObject;
import org.emcastle.asn1.cms.Attribute;
import org.emcastle.asn1.cms.AttributeTable;
import org.emcastle.asn1.cms.ContentInfo;
import org.emcastle.asn1.esf.SignaturePolicyIdentifier;
import org.emcastle.asn1.ess.ESSCertID;
import org.emcastle.asn1.ess.ESSCertIDv2;
import org.emcastle.asn1.ess.SigningCertificate;
import org.emcastle.asn1.ess.SigningCertificateV2;
import org.emcastle.asn1.ocsp.BasicOCSPResponse;
import org.emcastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.emcastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.emcastle.asn1.tsp.MessageImprint;
import org.emcastle.asn1.x509.AlgorithmIdentifier;
import org.emcastle.cert.X509CertificateHolder;
import org.emcastle.cert.jcajce.JcaX509CertificateHolder;
import org.emcastle.cert.ocsp.BasicOCSPResp;
import org.emcastle.cert.ocsp.CertificateID;
import org.emcastle.cert.ocsp.SingleResp;
import org.emcastle.jce.X509Principal;
import org.emcastle.jce.provider.X509CertParser;
import org.emcastle.operator.DigestCalculator;
import org.emcastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.emcastle.tsp.TimeStampToken;
import org.emcastle.tsp.TimeStampTokenInfo;

public class PdfPKCS7 {

    private String provider;
    private SignaturePolicyIdentifier signaturePolicyIdentifier;
    private String signName;
    private String reason;
    private String location;
    private Calendar signDate;
    private int version;
    private int signerversion;
    private String digestAlgorithmOid;
    private MessageDigest messageDigest;
    private Set<String> digestalgos;
    private byte[] digestAttr;
    private PdfName filterSubtype;
    private String digestEncryptionAlgorithmOid;
    private ExternalDigest interfaceDigest;
    private byte[] externalDigest;
    private byte[] externalRSAdata;
    private Signature sig;
    private byte[] digest;
    private byte[] RSAdata;
    private byte[] sigAttr;
    private byte[] sigAttrDer;
    private MessageDigest encContDigest;
    private boolean verified;
    private boolean verifyResult;
    private Collection<Certificate> certs;
    private Collection<Certificate> signCerts;
    private X509Certificate signCert;
    private Collection<CRL> crls;
    private BasicOCSPResp basicResp;
    private boolean isTsp;
    private boolean isCades;
    private TimeStampToken timeStampToken;

    public PdfPKCS7(PrivateKey privKey, Certificate[] certChain, String hashAlgorithm, String provider, ExternalDigest interfaceDigest, boolean hasRSAdata) throws InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
        this.version = 1;

        this.signerversion = 1;
        this.provider = provider;
        this.interfaceDigest = interfaceDigest;
        this.digestAlgorithmOid = DigestAlgorithms.getAllowedDigests(hashAlgorithm);
        if (this.digestAlgorithmOid == null) {
            throw new NoSuchAlgorithmException(MessageLocalization.getComposedMessage("unknown.hash.algorithm.1", new Object[]{hashAlgorithm}));
        }
        this.signCert = (X509Certificate) certChain[0];
        this.certs = new ArrayList<Certificate>();
        for (Certificate element : certChain) {
            this.certs.add(element);
        }
        this.digestalgos = new HashSet<String>();
        this.digestalgos.add(this.digestAlgorithmOid);
        if (privKey != null) {
            this.digestEncryptionAlgorithmOid = privKey.getAlgorithm();
            if (this.digestEncryptionAlgorithmOid.equals("RSA")) {
                this.digestEncryptionAlgorithmOid = "1.2.840.113549.1.1.1";
            } else if (this.digestEncryptionAlgorithmOid.equals("DSA")) {
                this.digestEncryptionAlgorithmOid = "1.2.840.10040.4.1";
            } else {
                throw new NoSuchAlgorithmException(MessageLocalization.getComposedMessage("unknown.key.algorithm.1", new Object[]{this.digestEncryptionAlgorithmOid}));
            }
        }
        if (hasRSAdata) {
            this.RSAdata = new byte[0];
            this.messageDigest = DigestAlgorithms.getMessageDigest(getHashAlgorithm(), provider);
        }
        if (privKey != null) {
            this.sig = initSignature(privKey);
        }
    }

    public PdfPKCS7(byte[] contentsKey, byte[] certsKey, String provider) {
        this.version = 1;
        this.signerversion = 1;
        try {
            this.provider = provider;
            X509CertParser cr = new X509CertParser();
            cr.engineInit(new ByteArrayInputStream(certsKey));
            this.certs = cr.engineReadAll();
            this.signCerts = this.certs;
            this.signCert = (X509Certificate) this.certs.iterator().next();
            this.crls = new ArrayList<CRL>();
            ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(contentsKey));
            this.digest = ((ASN1OctetString) in.readObject()).getOctets();
            if (provider == null) {
                this.sig = Signature.getInstance("SHA1withRSA");
            } else {
                this.sig = Signature.getInstance("SHA1withRSA", provider);
            }
            this.sig.initVerify(this.signCert.getPublicKey());
            this.digestAlgorithmOid = "1.2.840.10040.4.3";
            this.digestEncryptionAlgorithmOid = "1.3.36.3.3.1.2";
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public PdfPKCS7(byte[] contentsKey, PdfName filterSubtype, String provider) {
        this.version = 1;
        this.signerversion = 1;
        this.filterSubtype = filterSubtype;
        this.isTsp = PdfName.ETSI_RFC3161.equals(filterSubtype);
        this.isCades = PdfName.ETSI_CADES_DETACHED.equals(filterSubtype);
        try {
            ASN1Primitive pkcs;
            this.provider = provider;
            ASN1InputStream din = new ASN1InputStream(new ByteArrayInputStream(contentsKey));
            try {
                pkcs = din.readObject();
            } catch (IOException iOException) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("can.t.decode.pkcs7signeddata.object", new Object[0]));
            }
            if (!(pkcs instanceof ASN1Sequence)) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("not.a.valid.pkcs.7.object.not.a.sequence", new Object[0]));
            }
            ASN1Sequence signedData = (ASN1Sequence) pkcs;
            ASN1ObjectIdentifier objId = (ASN1ObjectIdentifier) signedData.getObjectAt(0);
            if (!objId.getId().equals("1.2.840.113549.1.7.2")) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("not.a.valid.pkcs.7.object.not.signed.data", new Object[0]));
            }
            ASN1Sequence content = (ASN1Sequence) ((ASN1TaggedObject) signedData.getObjectAt(1)).getObject();
            this.version = ((ASN1Integer) content.getObjectAt(0)).getValue().intValue();
            this.digestalgos = new HashSet<String>();
            Enumeration<ASN1Sequence> e = ((ASN1Set) content.getObjectAt(1)).getObjects();
            while (e.hasMoreElements()) {
                ASN1Sequence s = e.nextElement();
                ASN1ObjectIdentifier o = (ASN1ObjectIdentifier) s.getObjectAt(0);
                this.digestalgos.add(o.getId());
            }
            ASN1Sequence rsaData = (ASN1Sequence) content.getObjectAt(2);
            if (rsaData.size() > 1) {
                ASN1OctetString rsaDataContent = (ASN1OctetString) ((ASN1TaggedObject) rsaData.getObjectAt(1)).getObject();
                this.RSAdata = rsaDataContent.getOctets();
            }
            int next = 3;
            while (content.getObjectAt(next) instanceof ASN1TaggedObject) {
                next++;
            }
            X509CertParser cr = new X509CertParser();
            cr.engineInit(new ByteArrayInputStream(contentsKey));
            this.certs = cr.engineReadAll();
            ASN1Set signerInfos = (ASN1Set) content.getObjectAt(next);
            if (signerInfos.size() != 1) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("this.pkcs.7.object.has.multiple.signerinfos.only.one.is.supported.at.this.time", new Object[0]));
            }
            ASN1Sequence signerInfo = (ASN1Sequence) signerInfos.getObjectAt(0);
            this.signerversion = ((ASN1Integer) signerInfo.getObjectAt(0)).getValue().intValue();
            ASN1Sequence issuerAndSerialNumber = (ASN1Sequence) signerInfo.getObjectAt(1);
            X509Principal issuer = new X509Principal(issuerAndSerialNumber.getObjectAt(0).toASN1Primitive().getEncoded());
            BigInteger serialNumber = ((ASN1Integer) issuerAndSerialNumber.getObjectAt(1)).getValue();
            for (Certificate element : this.certs) {
                X509Certificate cert = (X509Certificate) element;
                if (cert.getIssuerDN().equals(issuer) && serialNumber.equals(cert.getSerialNumber())) {
                    this.signCert = cert;
                    break;
                }
            }
            if (this.signCert == null) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("can.t.find.signing.certificate.with.serial.1", new Object[]{issuer.getName() + " / " + serialNumber.toString(16)}));
            }
            signCertificateChain();
            this.digestAlgorithmOid = ((ASN1ObjectIdentifier) ((ASN1Sequence) signerInfo.getObjectAt(2)).getObjectAt(0)).getId();
            next = 3;
            boolean foundCades = false;
            if (signerInfo.getObjectAt(next) instanceof ASN1TaggedObject) {
                ASN1TaggedObject tagsig = (ASN1TaggedObject) signerInfo.getObjectAt(next);
                ASN1Set sseq = ASN1Set.getInstance(tagsig, false);
                this.sigAttr = sseq.getEncoded();
                this.sigAttrDer = sseq.getEncoded("DER");
                for (int k = 0; k < sseq.size(); k++) {
                    ASN1Sequence seq2 = (ASN1Sequence) sseq.getObjectAt(k);
                    String idSeq2 = ((ASN1ObjectIdentifier) seq2.getObjectAt(0)).getId();
                    if (idSeq2.equals("1.2.840.113549.1.9.4")) {
                        ASN1Set set = (ASN1Set) seq2.getObjectAt(1);
                        this.digestAttr = ((ASN1OctetString) set.getObjectAt(0)).getOctets();
                    } else if (idSeq2.equals("1.2.840.113583.1.1.8")) {
                        ASN1Set setout = (ASN1Set) seq2.getObjectAt(1);
                        ASN1Sequence seqout = (ASN1Sequence) setout.getObjectAt(0);
                        for (int j = 0; j < seqout.size(); j++) {
                            ASN1TaggedObject tg = (ASN1TaggedObject) seqout.getObjectAt(j);
                            if (tg.getTagNo() == 0) {
                                ASN1Sequence seqin = (ASN1Sequence) tg.getObject();
                                findCRL(seqin);
                            }
                            if (tg.getTagNo() == 1) {
                                ASN1Sequence seqin = (ASN1Sequence) tg.getObject();
                                findOcsp(seqin);
                            }
                        }
                    } else if (this.isCades && idSeq2.equals("1.2.840.113549.1.9.16.2.12")) {
                        ASN1Set setout = (ASN1Set) seq2.getObjectAt(1);
                        ASN1Sequence seqout = (ASN1Sequence) setout.getObjectAt(0);
                        SigningCertificate sv2 = SigningCertificate.getInstance(seqout);
                        ESSCertID[] cerv2m = sv2.getCerts();
                        ESSCertID cerv2 = cerv2m[0];
                        byte[] enc2 = this.signCert.getEncoded();
                        MessageDigest m2 = (new BouncyCastleDigest()).getMessageDigest("SHA-1");
                        byte[] signCertHash = m2.digest(enc2);
                        byte[] hs2 = cerv2.getCertHash();
                        if (!Arrays.equals(signCertHash, hs2)) {
                            throw new IllegalArgumentException("Signing certificate doesn't match the ESS information.");
                        }
                        foundCades = true;
                    } else if (this.isCades && idSeq2.equals("1.2.840.113549.1.9.16.2.47")) {
                        ASN1Set setout = (ASN1Set) seq2.getObjectAt(1);
                        ASN1Sequence seqout = (ASN1Sequence) setout.getObjectAt(0);
                        SigningCertificateV2 sv2 = SigningCertificateV2.getInstance(seqout);
                        ESSCertIDv2[] cerv2m = sv2.getCerts();
                        ESSCertIDv2 cerv2 = cerv2m[0];
                        AlgorithmIdentifier ai2 = cerv2.getHashAlgorithm();
                        byte[] enc2 = this.signCert.getEncoded();
                        MessageDigest m2 = (new BouncyCastleDigest()).getMessageDigest(DigestAlgorithms.getDigest(ai2.getAlgorithm().getId()));
                        byte[] signCertHash = m2.digest(enc2);
                        byte[] hs2 = cerv2.getCertHash();
                        if (!Arrays.equals(signCertHash, hs2)) {
                            throw new IllegalArgumentException("Signing certificate doesn't match the ESS information.");
                        }
                        foundCades = true;
                    }
                }
                if (this.digestAttr == null) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("authenticated.attribute.is.missing.the.digest", new Object[0]));
                }
                next++;
            }
            if (this.isCades && !foundCades) {
                throw new IllegalArgumentException("CAdES ESS information missing.");
            }
            this.digestEncryptionAlgorithmOid = ((ASN1ObjectIdentifier) ((ASN1Sequence) signerInfo.getObjectAt(next++)).getObjectAt(0)).getId();
            this.digest = ((ASN1OctetString) signerInfo.getObjectAt(next++)).getOctets();
            if (next < signerInfo.size() && signerInfo.getObjectAt(next) instanceof ASN1TaggedObject) {
                ASN1TaggedObject taggedObject = (ASN1TaggedObject) signerInfo.getObjectAt(next);
                ASN1Set unat = ASN1Set.getInstance(taggedObject, false);
                AttributeTable attble = new AttributeTable(unat);
                Attribute ts = attble.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
                if (ts != null && ts.getAttrValues().size() > 0) {
                    ASN1Set attributeValues = ts.getAttrValues();
                    ASN1Sequence tokenSequence = ASN1Sequence.getInstance(attributeValues.getObjectAt(0));
                    ContentInfo contentInfo = new ContentInfo(tokenSequence);
                    this.timeStampToken = new TimeStampToken(contentInfo);
                }
            }
            if (this.isTsp) {
                ContentInfo contentInfoTsp = new ContentInfo(signedData);
                this.timeStampToken = new TimeStampToken(contentInfoTsp);
                TimeStampTokenInfo info = this.timeStampToken.getTimeStampInfo();
                String algOID = info.getMessageImprintAlgOID().getId();
                this.messageDigest = DigestAlgorithms.getMessageDigestFromOid(algOID, null);
            } else {
                if (this.RSAdata != null || this.digestAttr != null) {
                    if (PdfName.ADBE_PKCS7_SHA1.equals(getFilterSubtype())) {
                        this.messageDigest = DigestAlgorithms.getMessageDigest("SHA1", provider);
                    } else {
                        this.messageDigest = DigestAlgorithms.getMessageDigest(getHashAlgorithm(), provider);
                    }
                    this.encContDigest = DigestAlgorithms.getMessageDigest(getHashAlgorithm(), provider);
                }
                this.sig = initSignature(this.signCert.getPublicKey());
            }
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public void setSignaturePolicy(SignaturePolicyInfo signaturePolicy) {
        this.signaturePolicyIdentifier = signaturePolicy.toSignaturePolicyIdentifier();
    }

    public void setSignaturePolicy(SignaturePolicyIdentifier signaturePolicy) {
        this.signaturePolicyIdentifier = signaturePolicy;
    }

    public int getVersion() {
        return this.version;
    }

    public String getSignName() {
        return this.signName;
    }

    public void setSignName(String signName) {
        this.signName = signName;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Calendar getSignDate() {
        Calendar dt = getTimeStampDate();
        if (dt == null) {
            return this.signDate;
        }
        return dt;
    }

    public void setSignDate(Calendar signDate) {
        this.signDate = signDate;
    }

    public int getSigningInfoVersion() {
        return this.signerversion;
    }

    public String getDigestAlgorithmOid() {
        return this.digestAlgorithmOid;
    }

    public String getHashAlgorithm() {
        return DigestAlgorithms.getDigest(this.digestAlgorithmOid);
    }

    public String getDigestEncryptionAlgorithmOid() {
        return this.digestEncryptionAlgorithmOid;
    }

    public String getDigestAlgorithm() {
        return getHashAlgorithm() + "with" + getEncryptionAlgorithm();
    }

    public void setExternalDigest(byte[] digest, byte[] RSAdata, String digestEncryptionAlgorithm) {
        this.externalDigest = digest;
        this.externalRSAdata = RSAdata;
        if (digestEncryptionAlgorithm != null) {
            if (digestEncryptionAlgorithm.equals("RSA")) {
                this.digestEncryptionAlgorithmOid = "1.2.840.113549.1.1.1";
            } else if (digestEncryptionAlgorithm.equals("DSA")) {
                this.digestEncryptionAlgorithmOid = "1.2.840.10040.4.1";
            } else if (digestEncryptionAlgorithm.equals("ECDSA")) {
                this.digestEncryptionAlgorithmOid = "1.2.840.10045.2.1";
            } else {

                throw new ExceptionConverter(new NoSuchAlgorithmException(MessageLocalization.getComposedMessage("unknown.key.algorithm.1", new Object[]{digestEncryptionAlgorithm})));
            }
        }
    }

    private Signature initSignature(PrivateKey key) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
        Signature signature;
        if (this.provider == null) {
            signature = Signature.getInstance(getDigestAlgorithm());
        } else {
            signature = Signature.getInstance(getDigestAlgorithm(), this.provider);
        }
        signature.initSign(key);
        return signature;
    }

    private Signature initSignature(PublicKey key) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
        Signature signature;
        String digestAlgorithm = getDigestAlgorithm();
        if (PdfName.ADBE_X509_RSA_SHA1.equals(getFilterSubtype())) {
            digestAlgorithm = "SHA1withRSA";
        }
        if (this.provider == null) {
            signature = Signature.getInstance(digestAlgorithm);
        } else {
            signature = Signature.getInstance(digestAlgorithm, this.provider);
        }
        signature.initVerify(key);
        return signature;
    }

    public void update(byte[] buf, int off, int len) throws SignatureException {
        if (this.RSAdata != null || this.digestAttr != null || this.isTsp) {
            this.messageDigest.update(buf, off, len);
        } else {
            this.sig.update(buf, off, len);
        }
    }

    public byte[] getEncodedPKCS1() {
        try {
            if (this.externalDigest != null) {
                this.digest = this.externalDigest;
            } else {
                this.digest = this.sig.sign();
            }
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();

            ASN1OutputStream dout = new ASN1OutputStream(bOut);
            dout.writeObject((ASN1Encodable) new DEROctetString(this.digest));
            dout.close();

            return bOut.toByteArray();
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public byte[] getEncodedPKCS7() {
        return getEncodedPKCS7(null, null, null, null, MakeSignature.CryptoStandard.CMS);
    }

    public byte[] getEncodedPKCS7(byte[] secondDigest) {
        return getEncodedPKCS7(secondDigest, null, null, null, MakeSignature.CryptoStandard.CMS);
    }

    public byte[] getEncodedPKCS7(byte[] secondDigest, TSAClient tsaClient, byte[] ocsp, Collection<byte[]> crlBytes, MakeSignature.CryptoStandard sigtype) {
        try {
            if (this.externalDigest != null) {
                this.digest = this.externalDigest;
                if (this.RSAdata != null) {
                    this.RSAdata = this.externalRSAdata;
                }
            } else if (this.externalRSAdata != null && this.RSAdata != null) {
                this.RSAdata = this.externalRSAdata;
                this.sig.update(this.RSAdata);
                this.digest = this.sig.sign();
            } else {

                if (this.RSAdata != null) {
                    this.RSAdata = this.messageDigest.digest();
                    this.sig.update(this.RSAdata);
                }
                this.digest = this.sig.sign();
            }

            ASN1EncodableVector digestAlgorithms = new ASN1EncodableVector();
            for (String element : this.digestalgos) {
                ASN1EncodableVector algos = new ASN1EncodableVector();
                algos.add((ASN1Encodable) new ASN1ObjectIdentifier(element));
                algos.add((ASN1Encodable) DERNull.INSTANCE);
                digestAlgorithms.add((ASN1Encodable) new DERSequence(algos));
            }

            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add((ASN1Encodable) new ASN1ObjectIdentifier("1.2.840.113549.1.7.1"));
            if (this.RSAdata != null) {
                v.add((ASN1Encodable) new DERTaggedObject(0, (ASN1Encodable) new DEROctetString(this.RSAdata)));
            }
            DERSequence contentinfo = new DERSequence(v);

            v = new ASN1EncodableVector();
            for (Certificate element : this.certs) {
                ASN1InputStream tempstream = new ASN1InputStream(new ByteArrayInputStream(((X509Certificate) element).getEncoded()));
                v.add((ASN1Encodable) tempstream.readObject());
            }

            DERSet dercertificates = new DERSet(v);

            ASN1EncodableVector signerinfo = new ASN1EncodableVector();

            signerinfo.add((ASN1Encodable) new ASN1Integer(this.signerversion));

            v = new ASN1EncodableVector();
            v.add((ASN1Encodable) CertificateInfo.getIssuer(this.signCert.getTBSCertificate()));
            v.add((ASN1Encodable) new ASN1Integer(this.signCert.getSerialNumber()));
            signerinfo.add((ASN1Encodable) new DERSequence(v));

            v = new ASN1EncodableVector();
            v.add((ASN1Encodable) new ASN1ObjectIdentifier(this.digestAlgorithmOid));
            v.add((ASN1Encodable) new DERNull());
            signerinfo.add((ASN1Encodable) new DERSequence(v));

            if (secondDigest != null) {
                signerinfo.add((ASN1Encodable) new DERTaggedObject(false, 0, (ASN1Encodable) getAuthenticatedAttributeSet(secondDigest, ocsp, crlBytes, sigtype)));
            }

            v = new ASN1EncodableVector();
            v.add((ASN1Encodable) new ASN1ObjectIdentifier(this.digestEncryptionAlgorithmOid));
            v.add((ASN1Encodable) new DERNull());
            signerinfo.add((ASN1Encodable) new DERSequence(v));

            signerinfo.add((ASN1Encodable) new DEROctetString(this.digest));

            if (tsaClient != null) {
                byte[] tsImprint = tsaClient.getMessageDigest().digest(this.digest);
                byte[] tsToken = tsaClient.getTimeStampToken(tsImprint);
                if (tsToken != null) {
                    ASN1EncodableVector unauthAttributes = buildUnauthenticatedAttributes(tsToken);
                    if (unauthAttributes != null) {
                        signerinfo.add((ASN1Encodable) new DERTaggedObject(false, 1, (ASN1Encodable) new DERSet(unauthAttributes)));
                    }
                }
            }

            ASN1EncodableVector body = new ASN1EncodableVector();
            body.add((ASN1Encodable) new ASN1Integer(this.version));
            body.add((ASN1Encodable) new DERSet(digestAlgorithms));
            body.add((ASN1Encodable) contentinfo);
            body.add((ASN1Encodable) new DERTaggedObject(false, 0, (ASN1Encodable) dercertificates));

            body.add((ASN1Encodable) new DERSet((ASN1Encodable) new DERSequence(signerinfo)));

            ASN1EncodableVector whole = new ASN1EncodableVector();
            whole.add((ASN1Encodable) new ASN1ObjectIdentifier("1.2.840.113549.1.7.2"));
            whole.add((ASN1Encodable) new DERTaggedObject(0, (ASN1Encodable) new DERSequence(body)));

            ByteArrayOutputStream bOut = new ByteArrayOutputStream();

            ASN1OutputStream dout = new ASN1OutputStream(bOut);
            dout.writeObject((ASN1Encodable) new DERSequence(whole));
            dout.close();

            return bOut.toByteArray();
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    private ASN1EncodableVector buildUnauthenticatedAttributes(byte[] timeStampToken) throws IOException {
        if (timeStampToken == null) {
            return null;
        }

        String ID_TIME_STAMP_TOKEN = "1.2.840.113549.1.9.16.2.14";

        ASN1InputStream tempstream = new ASN1InputStream(new ByteArrayInputStream(timeStampToken));
        ASN1EncodableVector unauthAttributes = new ASN1EncodableVector();

        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add((ASN1Encodable) new ASN1ObjectIdentifier(ID_TIME_STAMP_TOKEN));
        ASN1Sequence seq = (ASN1Sequence) tempstream.readObject();
        v.add((ASN1Encodable) new DERSet((ASN1Encodable) seq));

        unauthAttributes.add((ASN1Encodable) new DERSequence(v));
        return unauthAttributes;
    }

    public byte[] getAuthenticatedAttributeBytes(byte[] secondDigest, byte[] ocsp, Collection<byte[]> crlBytes, MakeSignature.CryptoStandard sigtype) {
        try {
            return getAuthenticatedAttributeSet(secondDigest, ocsp, crlBytes, sigtype).getEncoded("DER");
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    private DERSet getAuthenticatedAttributeSet(byte[] secondDigest, byte[] ocsp, Collection<byte[]> crlBytes, MakeSignature.CryptoStandard sigtype) {
        try {
            ASN1EncodableVector attribute = new ASN1EncodableVector();
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add((ASN1Encodable) new ASN1ObjectIdentifier("1.2.840.113549.1.9.3"));
            v.add((ASN1Encodable) new DERSet((ASN1Encodable) new ASN1ObjectIdentifier("1.2.840.113549.1.7.1")));
            attribute.add((ASN1Encodable) new DERSequence(v));
            v = new ASN1EncodableVector();
            v.add((ASN1Encodable) new ASN1ObjectIdentifier("1.2.840.113549.1.9.4"));
            v.add((ASN1Encodable) new DERSet((ASN1Encodable) new DEROctetString(secondDigest)));
            attribute.add((ASN1Encodable) new DERSequence(v));
            boolean haveCrl = false;
            if (crlBytes != null) {
                for (byte[] bCrl : crlBytes) {
                    if (bCrl != null) {
                        haveCrl = true;
                        break;
                    }
                }
            }
            if (ocsp != null || haveCrl) {
                v = new ASN1EncodableVector();
                v.add((ASN1Encodable) new ASN1ObjectIdentifier("1.2.840.113583.1.1.8"));

                ASN1EncodableVector revocationV = new ASN1EncodableVector();

                if (haveCrl) {
                    ASN1EncodableVector v2 = new ASN1EncodableVector();
                    for (byte[] bCrl : crlBytes) {
                        if (bCrl == null) {
                            continue;
                        }
                        ASN1InputStream t = new ASN1InputStream(new ByteArrayInputStream(bCrl));
                        v2.add((ASN1Encodable) t.readObject());
                    }
                    revocationV.add((ASN1Encodable) new DERTaggedObject(true, 0, (ASN1Encodable) new DERSequence(v2)));
                }

                if (ocsp != null) {
                    DEROctetString doctet = new DEROctetString(ocsp);
                    ASN1EncodableVector vo1 = new ASN1EncodableVector();
                    ASN1EncodableVector v2 = new ASN1EncodableVector();
                    v2.add((ASN1Encodable) OCSPObjectIdentifiers.id_pkix_ocsp_basic);
                    v2.add((ASN1Encodable) doctet);
                    ASN1Enumerated den = new ASN1Enumerated(0);
                    ASN1EncodableVector v3 = new ASN1EncodableVector();
                    v3.add((ASN1Encodable) den);
                    v3.add((ASN1Encodable) new DERTaggedObject(true, 0, (ASN1Encodable) new DERSequence(v2)));
                    vo1.add((ASN1Encodable) new DERSequence(v3));
                    revocationV.add((ASN1Encodable) new DERTaggedObject(true, 1, (ASN1Encodable) new DERSequence(vo1)));
                }

                v.add((ASN1Encodable) new DERSet((ASN1Encodable) new DERSequence(revocationV)));
                attribute.add((ASN1Encodable) new DERSequence(v));
            }
            if (sigtype == MakeSignature.CryptoStandard.CADES) {
                v = new ASN1EncodableVector();
                v.add((ASN1Encodable) new ASN1ObjectIdentifier("1.2.840.113549.1.9.16.2.47"));

                ASN1EncodableVector aaV2 = new ASN1EncodableVector();
                String sha256Oid = DigestAlgorithms.getAllowedDigests("SHA-256");

                if (!sha256Oid.equals(this.digestAlgorithmOid)) {
                    AlgorithmIdentifier algoId = new AlgorithmIdentifier(new ASN1ObjectIdentifier(this.digestAlgorithmOid));
                    aaV2.add((ASN1Encodable) algoId);
                }

                MessageDigest md = this.interfaceDigest.getMessageDigest(getHashAlgorithm());
                byte[] dig = md.digest(this.signCert.getEncoded());
                aaV2.add((ASN1Encodable) new DEROctetString(dig));

                v.add((ASN1Encodable) new DERSet((ASN1Encodable) new DERSequence((ASN1Encodable) new DERSequence((ASN1Encodable) new DERSequence(aaV2)))));
                attribute.add((ASN1Encodable) new DERSequence(v));
            }

            if (this.signaturePolicyIdentifier != null) {
                attribute.add((ASN1Encodable) new Attribute(PKCSObjectIdentifiers.id_aa_ets_sigPolicyId, (ASN1Set) new DERSet((ASN1Encodable) this.signaturePolicyIdentifier)));
            }

            return new DERSet(attribute);
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public boolean verify() throws GeneralSecurityException {
        if (this.verified) {
            return this.verifyResult;
        }
        if (this.isTsp) {
            TimeStampTokenInfo info = this.timeStampToken.getTimeStampInfo();
            MessageImprint imprint = info.toASN1Structure().getMessageImprint();
            byte[] md = this.messageDigest.digest();
            byte[] imphashed = imprint.getHashedMessage();
            this.verifyResult = Arrays.equals(md, imphashed);

        } else if (this.sigAttr != null || this.sigAttrDer != null) {
            byte[] msgDigestBytes = this.messageDigest.digest();
            boolean verifyRSAdata = true;

            boolean encContDigestCompare = false;
            if (this.RSAdata != null) {
                verifyRSAdata = Arrays.equals(msgDigestBytes, this.RSAdata);
                this.encContDigest.update(this.RSAdata);
                encContDigestCompare = Arrays.equals(this.encContDigest.digest(), this.digestAttr);
            }
            boolean absentEncContDigestCompare = Arrays.equals(msgDigestBytes, this.digestAttr);
            boolean concludingDigestCompare = (absentEncContDigestCompare || encContDigestCompare);
            boolean sigVerify = (verifySigAttributes(this.sigAttr) || verifySigAttributes(this.sigAttrDer));
            this.verifyResult = (concludingDigestCompare && sigVerify && verifyRSAdata);
        } else {

            if (this.RSAdata != null) {
                this.sig.update(this.messageDigest.digest());
            }
            this.verifyResult = this.sig.verify(this.digest);
        }

        this.verified = true;
        return this.verifyResult;
    }

    private boolean verifySigAttributes(byte[] attr) throws GeneralSecurityException {
        Signature signature = initSignature(this.signCert.getPublicKey());
        signature.update(attr);
        return signature.verify(this.digest);
    }

    public boolean verifyTimestampImprint() throws GeneralSecurityException {
        if (this.timeStampToken == null) {
            return false;
        }
        TimeStampTokenInfo info = this.timeStampToken.getTimeStampInfo();
        MessageImprint imprint = info.toASN1Structure().getMessageImprint();
        String algOID = info.getMessageImprintAlgOID().getId();
        byte[] md = (new BouncyCastleDigest()).getMessageDigest(DigestAlgorithms.getDigest(algOID)).digest(this.digest);
        byte[] imphashed = imprint.getHashedMessage();
        boolean res = Arrays.equals(md, imphashed);
        return res;
    }

    public Certificate[] getCertificates() {
        return this.certs.<Certificate>toArray((Certificate[]) new X509Certificate[this.certs.size()]);
    }

    public Certificate[] getSignCertificateChain() {
        return this.signCerts.<Certificate>toArray((Certificate[]) new X509Certificate[this.signCerts.size()]);
    }

    public X509Certificate getSigningCertificate() {
        return this.signCert;
    }

    private void signCertificateChain() {
        ArrayList<Certificate> cc = new ArrayList<Certificate>();
        cc.add(this.signCert);
        ArrayList<Certificate> oc = new ArrayList<Certificate>(this.certs);
        for (int k = 0; k < oc.size(); k++) {
            if (this.signCert.equals(oc.get(k))) {
                oc.remove(k);
                k--;
            }
        }

        boolean found = true;
        while (found) {
            X509Certificate v = (X509Certificate) cc.get(cc.size() - 1);
            found = false;
            for (int i = 0; i < oc.size(); i++) {
                X509Certificate issuer = (X509Certificate) oc.get(i);
                try {
                    if (this.provider == null) {
                        v.verify(issuer.getPublicKey());
                    } else {
                        v.verify(issuer.getPublicKey(), this.provider);
                    }
                    found = true;
                    cc.add(oc.get(i));
                    oc.remove(i);

                    break;
                } catch (Exception exception) {
                }
            }
        }

        this.signCerts = cc;
    }

    public Collection<CRL> getCRLs() {
        return this.crls;
    }

    private void findCRL(ASN1Sequence seq) {
        try {
            this.crls = new ArrayList<CRL>();
            for (int k = 0; k < seq.size(); k++) {
                ByteArrayInputStream ar = new ByteArrayInputStream(seq.getObjectAt(k).toASN1Primitive().getEncoded("DER"));
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509CRL crl = (X509CRL) cf.generateCRL(ar);
                this.crls.add(crl);
            }

        } catch (Exception exception) {
        }
    }

    public BasicOCSPResp getOcsp() {
        return this.basicResp;
    }

    public boolean isRevocationValid() {
        if (this.basicResp == null) {
            return false;
        }
        if (this.signCerts.size() < 2) {
            return false;
        }
        try {
            X509Certificate[] cs = (X509Certificate[]) getSignCertificateChain();
            SingleResp sr = this.basicResp.getResponses()[0];
            CertificateID cid = sr.getCertID();
            DigestCalculator digestalg = (new JcaDigestCalculatorProviderBuilder()).build().get(new AlgorithmIdentifier(cid.getHashAlgOID(), (ASN1Encodable) DERNull.INSTANCE));
            X509Certificate sigcer = getSigningCertificate();
            X509Certificate isscer = cs[1];

            CertificateID tis = new CertificateID(digestalg, (X509CertificateHolder) new JcaX509CertificateHolder(isscer), sigcer.getSerialNumber());
            return tis.equals(cid);
        } catch (Exception exception) {

            return false;
        }
    }

    private void findOcsp(ASN1Sequence seq) throws IOException {
        this.basicResp = null;
        boolean ret = false;

        while (!(seq.getObjectAt(0) instanceof ASN1ObjectIdentifier)
                || !((ASN1ObjectIdentifier) seq.getObjectAt(0)).getId().equals(OCSPObjectIdentifiers.id_pkix_ocsp_basic.getId())) {

            ret = true;
            for (int k = 0; k < seq.size(); k++) {
                if (seq.getObjectAt(k) instanceof ASN1Sequence) {
                    seq = (ASN1Sequence) seq.getObjectAt(0);
                    ret = false;
                    break;
                }
                if (seq.getObjectAt(k) instanceof ASN1TaggedObject) {
                    ASN1TaggedObject tag = (ASN1TaggedObject) seq.getObjectAt(k);
                    if (tag.getObject() instanceof ASN1Sequence) {
                        seq = (ASN1Sequence) tag.getObject();
                        ret = false;

                        break;
                    }
                    return;
                }
            }
            if (ret) {
                return;
            }
        }
        ASN1OctetString os = (ASN1OctetString) seq.getObjectAt(1);
        ASN1InputStream inp = new ASN1InputStream(os.getOctets());
        BasicOCSPResponse resp = BasicOCSPResponse.getInstance(inp.readObject());
        this.basicResp = new BasicOCSPResp(resp);
    }

    public boolean isTsp() {
        return this.isTsp;
    }

    public TimeStampToken getTimeStampToken() {
        return this.timeStampToken;
    }

    public Calendar getTimeStampDate() {
        if (this.timeStampToken == null) {
            return null;
        }
        Calendar cal = new GregorianCalendar();
        Date date = this.timeStampToken.getTimeStampInfo().getGenTime();
        cal.setTime(date);
        return cal;
    }

    public PdfName getFilterSubtype() {
        return this.filterSubtype;
    }

    public String getEncryptionAlgorithm() {
        String encryptAlgo = EncryptionAlgorithms.getAlgorithm(this.digestEncryptionAlgorithmOid);
        if (encryptAlgo == null) {
            encryptAlgo = this.digestEncryptionAlgorithmOid;
        }
        return encryptAlgo;
    }
}
