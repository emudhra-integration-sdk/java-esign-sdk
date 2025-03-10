package esign.text.pdf.security;

import esign.text.DocumentException;
import esign.text.io.RASInputStream;
import esign.text.io.RandomAccessSource;
import esign.text.io.RandomAccessSourceFactory;
import esign.text.io.StreamUtil;
import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import esign.text.pdf.AcroFields;
import esign.text.pdf.ByteBuffer;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDate;
import esign.text.pdf.PdfDeveloperExtension;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfReader;
import esign.text.pdf.PdfSignature;
import esign.text.pdf.PdfSignatureAppearance;
import esign.text.pdf.PdfString;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.emcastle.asn1.esf.SignaturePolicyIdentifier;

public class MakeSignature {

    private static final Logger LOGGER = LoggerFactory.getLogger(MakeSignature.class);

    public enum CryptoStandard {
        CMS, CADES;
    }

    public static void signDetached(PdfSignatureAppearance sap, ExternalDigest externalDigest, ExternalSignature externalSignature, Certificate[] chain, Collection<CrlClient> crlList, OcspClient ocspClient, TSAClient tsaClient, int estimatedSize, CryptoStandard sigtype) throws IOException, DocumentException, GeneralSecurityException {
        signDetached(sap, externalDigest, externalSignature, chain, crlList, ocspClient, tsaClient, estimatedSize, sigtype, (SignaturePolicyIdentifier) null);
    }

    public static void signDetached(PdfSignatureAppearance sap, ExternalDigest externalDigest, ExternalSignature externalSignature, Certificate[] chain, Collection<CrlClient> crlList, OcspClient ocspClient, TSAClient tsaClient, int estimatedSize, CryptoStandard sigtype, SignaturePolicyInfo signaturePolicy) throws IOException, DocumentException, GeneralSecurityException {
        signDetached(sap, externalDigest, externalSignature, chain, crlList, ocspClient, tsaClient, estimatedSize, sigtype, signaturePolicy.toSignaturePolicyIdentifier());
    }

    public static void signDetached(PdfSignatureAppearance sap, ExternalDigest externalDigest, ExternalSignature externalSignature, Certificate[] chain, Collection<CrlClient> crlList, OcspClient ocspClient, TSAClient tsaClient, int estimatedSize, CryptoStandard sigtype, SignaturePolicyIdentifier signaturePolicy) throws IOException, DocumentException, GeneralSecurityException {
        Collection<byte[]> crlBytes = null;
        int i = 0;
        while (crlBytes == null && i < chain.length) {
            crlBytes = processCrl(chain[i++], crlList);
        }
        if (estimatedSize == 0) {
            estimatedSize = 8192;
            if (crlBytes != null) {
                for (byte[] element : crlBytes) {
                    estimatedSize += element.length + 10;
                }
            }
            if (ocspClient != null) {
                estimatedSize += 4192;
            }
            if (tsaClient != null) {
                estimatedSize += 4192;
            }
        }
        sap.setCertificate(chain[0]);
        if (sigtype == CryptoStandard.CADES) {
            sap.addDeveloperExtension(PdfDeveloperExtension.ESIC_1_7_EXTENSIONLEVEL2);
        }
        PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, (sigtype == CryptoStandard.CADES) ? PdfName.ETSI_CADES_DETACHED : PdfName.ADBE_PKCS7_DETACHED);
        dic.setReason(sap.getReason());
        dic.setLocation(sap.getLocation());
        dic.setSignatureCreator(sap.getSignatureCreator());
        dic.setContact(sap.getContact());
        dic.setDate(new PdfDate(sap.getSignDate()));
        sap.setCryptoDictionary((PdfDictionary) dic);

        HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
        exc.put(PdfName.CONTENTS, new Integer(estimatedSize * 2 + 2));
        sap.preClose(exc);

        String hashAlgorithm = externalSignature.getHashAlgorithm();
        PdfPKCS7 sgn = new PdfPKCS7(null, chain, hashAlgorithm, null, externalDigest, false);
        if (signaturePolicy != null) {
            sgn.setSignaturePolicy(signaturePolicy);
        }
        InputStream data = sap.getRangeStream();
        byte[] hash = DigestAlgorithms.digest(data, externalDigest.getMessageDigest(hashAlgorithm));
        byte[] ocsp = null;
        if (chain.length >= 2 && ocspClient != null) {
            ocsp = ocspClient.getEncoded((X509Certificate) chain[0], (X509Certificate) chain[1], null);
        }
        byte[] sh = sgn.getAuthenticatedAttributeBytes(hash, ocsp, crlBytes, sigtype);
        byte[] extSignature = externalSignature.sign(sh);
        sgn.setExternalDigest(extSignature, null, externalSignature.getEncryptionAlgorithm());

        byte[] encodedSig = sgn.getEncodedPKCS7(hash, tsaClient, ocsp, crlBytes, sigtype);

        if (estimatedSize < encodedSig.length) {
            throw new IOException("Not enough space");
        }
        byte[] paddedSig = new byte[estimatedSize];
        System.arraycopy(encodedSig, 0, paddedSig, 0, encodedSig.length);

        PdfDictionary dic2 = new PdfDictionary();
        dic2.put(PdfName.CONTENTS, (PdfObject) (new PdfString(paddedSig)).setHexWriting(true));
        sap.close(dic2);
    }

    public static Collection<byte[]> processCrl(Certificate cert, Collection<CrlClient> crlList) {
        if (crlList == null) {
            return null;
        }
        ArrayList<byte[]> crlBytes = (ArrayList) new ArrayList<byte[]>();
        for (CrlClient cc : crlList) {
            if (cc == null) {
                continue;
            }
            LOGGER.info("Processing " + cc.getClass().getName());
            Collection<byte[]> b = cc.getEncoded((X509Certificate) cert, null);
            if (b == null) {
                continue;
            }
            crlBytes.addAll((Collection) b);
        }
        if (crlBytes.isEmpty()) {
            return null;
        }
        return (Collection<byte[]>) crlBytes;
    }

    public static void signExternalContainer(PdfSignatureAppearance sap, ExternalSignatureContainer externalSignatureContainer, int estimatedSize) throws GeneralSecurityException, IOException, DocumentException {
        PdfSignature dic = new PdfSignature(null, null);
        dic.setReason(sap.getReason());
        dic.setLocation(sap.getLocation());
        dic.setSignatureCreator(sap.getSignatureCreator());
        dic.setContact(sap.getContact());
        dic.setDate(new PdfDate(sap.getSignDate()));
        externalSignatureContainer.modifySigningDictionary((PdfDictionary) dic);
        sap.setCryptoDictionary((PdfDictionary) dic);

        HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
        exc.put(PdfName.CONTENTS, new Integer(estimatedSize * 2 + 2));
        sap.preClose(exc);

        InputStream data = sap.getRangeStream();
        byte[] encodedSig = externalSignatureContainer.sign(data);

        if (estimatedSize < encodedSig.length) {
            throw new IOException("Not enough space");
        }
        byte[] paddedSig = new byte[estimatedSize];
        System.arraycopy(encodedSig, 0, paddedSig, 0, encodedSig.length);

        PdfDictionary dic2 = new PdfDictionary();
        dic2.put(PdfName.CONTENTS, (PdfObject) (new PdfString(paddedSig)).setHexWriting(true));
        sap.close(dic2);
    }

    public static void signDeferred(PdfReader reader, String fieldName, OutputStream outs, ExternalSignatureContainer externalSignatureContainer) throws DocumentException, IOException, GeneralSecurityException {
        AcroFields af = reader.getAcroFields();
        PdfDictionary v = af.getSignatureDictionary(fieldName);
        if (v == null) {
            throw new DocumentException("No field");
        }
        if (!af.signatureCoversWholeDocument(fieldName)) {
            throw new DocumentException("Not the last signature");
        }
        PdfArray b = v.getAsArray(PdfName.BYTERANGE);
        long[] gaps = b.asLongArray();
        if (b.size() != 4 || gaps[0] != 0L) {
            throw new DocumentException("Single exclusion space supported");
        }
        RandomAccessSource readerSource = reader.getSafeFile().createSourceView();
        RASInputStream rASInputStream = new RASInputStream((new RandomAccessSourceFactory()).createRanged(readerSource, gaps));
        byte[] signedContent = externalSignatureContainer.sign((InputStream) rASInputStream);
        int spaceAvailable = (int) (gaps[2] - gaps[1]) - 2;
        if ((spaceAvailable & 0x1) != 0) {
            throw new DocumentException("Gap is not a multiple of 2");
        }
        spaceAvailable /= 2;
        if (spaceAvailable < signedContent.length) {
            throw new DocumentException("Not enough space");
        }
        StreamUtil.CopyBytes(readerSource, 0L, gaps[1] + 1L, outs);
        ByteBuffer bb = new ByteBuffer(spaceAvailable * 2);
        for (byte bi : signedContent) {
            bb.appendHex(bi);
        }
        int remain = (spaceAvailable - signedContent.length) * 2;
        for (int k = 0; k < remain; k++) {
            bb.append((byte) 48);
        }
        bb.writeTo(outs);
        StreamUtil.CopyBytes(readerSource, gaps[2] - 1L, gaps[3] + 1L, outs);
    }
}

