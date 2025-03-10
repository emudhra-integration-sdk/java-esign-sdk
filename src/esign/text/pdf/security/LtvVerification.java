package esign.text.pdf.security;

import esign.text.Utilities;
import esign.text.error_messages.MessageLocalization;
import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import esign.text.pdf.AcroFields;
import esign.text.pdf.PRIndirectReference;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDeveloperExtension;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfIndirectReference;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfReader;
import esign.text.pdf.PdfStamper;
import esign.text.pdf.PdfStream;
import esign.text.pdf.PdfString;
import esign.text.pdf.PdfWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.emcastle.asn1.ASN1Encodable;
import org.emcastle.asn1.ASN1EncodableVector;
import org.emcastle.asn1.ASN1Enumerated;
import org.emcastle.asn1.ASN1InputStream;
import org.emcastle.asn1.ASN1Primitive;
import org.emcastle.asn1.DEROctetString;
import org.emcastle.asn1.DERSequence;
import org.emcastle.asn1.DERTaggedObject;
import org.emcastle.asn1.ocsp.OCSPObjectIdentifiers;

public class LtvVerification {

    private Logger LOGGER = LoggerFactory.getLogger(LtvVerification.class);

    private PdfStamper stp;
    private PdfWriter writer;
    private PdfReader reader;
    private AcroFields acroFields;
    private Map<PdfName, ValidationData> validated = new HashMap<PdfName, ValidationData>();

    private boolean used = false;

    public enum Level {
        OCSP,
        CRL,
        OCSP_CRL,
        OCSP_OPTIONAL_CRL;
    }

    public enum CertificateOption {
        SIGNING_CERTIFICATE,
        WHOLE_CHAIN;
    }

    public enum CertificateInclusion {
        YES,
        NO;
    }

    public LtvVerification(PdfStamper stp) {
        this.stp = stp;
        this.writer = stp.getWriter();
        this.reader = stp.getReader();
        this.acroFields = stp.getAcroFields();
    }

    public boolean addVerification(String signatureName, OcspClient ocsp, CrlClient crl, CertificateOption certOption, Level level, CertificateInclusion certInclude) throws IOException, GeneralSecurityException {
        if (this.used) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("verification.already.output", new Object[0]));
        }
        PdfPKCS7 pk = this.acroFields.verifySignature(signatureName);
        this.LOGGER.info("Adding verification for " + signatureName);
        Certificate[] xc = pk.getCertificates();

        X509Certificate signingCert = pk.getSigningCertificate();
        ValidationData vd = new ValidationData();
        for (int k = 0; k < xc.length; k++) {
            X509Certificate cert = (X509Certificate) xc[k];
            this.LOGGER.info("Certificate: " + cert.getSubjectDN());
            if (certOption != CertificateOption.SIGNING_CERTIFICATE || cert
                    .equals(signingCert)) {

                byte[] ocspEnc = null;
                if (ocsp != null && level != Level.CRL) {
                    ocspEnc = ocsp.getEncoded(cert, getParent(cert, xc), null);
                    if (ocspEnc != null) {
                        vd.ocsps.add(buildOCSPResponse(ocspEnc));
                        this.LOGGER.info("OCSP added");
                    }
                }
                if (crl != null && (level == Level.CRL || level == Level.OCSP_CRL || (level == Level.OCSP_OPTIONAL_CRL && ocspEnc == null))) {
                    Collection<byte[]> cims = crl.getEncoded(cert, null);
                    if (cims != null) {
                        for (byte[] cim : cims) {
                            boolean dup = false;
                            for (byte[] b : vd.crls) {
                                if (Arrays.equals(b, cim)) {
                                    dup = true;
                                    break;
                                }
                            }
                            if (!dup) {
                                vd.crls.add(cim);
                                this.LOGGER.info("CRL added");
                            }
                        }
                    }
                }
                if (certInclude == CertificateInclusion.YES) {
                    vd.certs.add(cert.getEncoded());
                }
            }
        }
        if (vd.crls.isEmpty() && vd.ocsps.isEmpty()) {
            return false;
        }
        this.validated.put(getSignatureHashKey(signatureName), vd);
        return true;
    }

    private X509Certificate getParent(X509Certificate cert, Certificate[] certs) {
        for (int i = 0; i < certs.length; i++) {
            X509Certificate parent = (X509Certificate) certs[i];
            if (cert.getIssuerDN().equals(parent.getSubjectDN())) {

                try {
                    cert.verify(parent.getPublicKey());
                    return parent;
                } catch (Exception exception) {
                }
            }
        }

        return null;
    }

    public boolean addVerification(String signatureName, Collection<byte[]> ocsps, Collection<byte[]> crls, Collection<byte[]> certs) throws IOException, GeneralSecurityException {
        if (this.used) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("verification.already.output", new Object[0]));
        }
        ValidationData vd = new ValidationData();
        if (ocsps != null) {
            for (byte[] ocsp : ocsps) {
                vd.ocsps.add(buildOCSPResponse(ocsp));
            }
        }
        if (crls != null) {
            for (byte[] crl : crls) {
                vd.crls.add(crl);
            }
        }
        if (certs != null) {
            for (byte[] cert : certs) {
                vd.certs.add(cert);
            }
        }
        this.validated.put(getSignatureHashKey(signatureName), vd);
        return true;
    }

    private static byte[] buildOCSPResponse(byte[] BasicOCSPResponse) throws IOException {
        DEROctetString doctet = new DEROctetString(BasicOCSPResponse);
        ASN1EncodableVector v2 = new ASN1EncodableVector();
        v2.add((ASN1Encodable) OCSPObjectIdentifiers.id_pkix_ocsp_basic);
        v2.add((ASN1Encodable) doctet);
        ASN1Enumerated den = new ASN1Enumerated(0);
        ASN1EncodableVector v3 = new ASN1EncodableVector();
        v3.add((ASN1Encodable) den);
        v3.add((ASN1Encodable) new DERTaggedObject(true, 0, (ASN1Encodable) new DERSequence(v2)));
        DERSequence seq = new DERSequence(v3);
        return seq.getEncoded();
    }

    private PdfName getSignatureHashKey(String signatureName) throws NoSuchAlgorithmException, IOException {
        PdfDictionary dic = this.acroFields.getSignatureDictionary(signatureName);
        PdfString contents = dic.getAsString(PdfName.CONTENTS);
        byte[] bc = contents.getOriginalBytes();
        byte[] bt = null;
        if (PdfName.ETSI_RFC3161.equals(PdfReader.getPdfObject(dic.get(PdfName.SUBFILTER)))) {
            ASN1InputStream din = new ASN1InputStream(new ByteArrayInputStream(bc));
            ASN1Primitive pkcs = din.readObject();
            bc = pkcs.getEncoded();
        }
        bt = hashBytesSha1(bc);
        return new PdfName(Utilities.convertToHex(bt));
    }

    private static byte[] hashBytesSha1(byte[] b) throws NoSuchAlgorithmException {
        MessageDigest sh = MessageDigest.getInstance("SHA1");
        return sh.digest(b);
    }

    public void merge() throws IOException {
        if (this.used || this.validated.isEmpty()) {
            return;
        }
        this.used = true;
        PdfDictionary catalog = this.reader.getCatalog();
        PdfObject dss = catalog.get(PdfName.DSS);
        if (dss == null) {
            createDss();
        } else {
            updateDss();
        }
    }

    private void updateDss() throws IOException {
        PdfDictionary catalog = this.reader.getCatalog();
        this.stp.markUsed((PdfObject) catalog);
        PdfDictionary dss = catalog.getAsDict(PdfName.DSS);
        PdfArray ocsps = dss.getAsArray(PdfName.OCSPS);
        PdfArray crls = dss.getAsArray(PdfName.CRLS);
        PdfArray certs = dss.getAsArray(PdfName.CERTS);
        dss.remove(PdfName.OCSPS);
        dss.remove(PdfName.CRLS);
        dss.remove(PdfName.CERTS);
        PdfDictionary vrim = dss.getAsDict(PdfName.VRI);

        if (vrim != null) {
            for (PdfName n : vrim.getKeys()) {
                if (this.validated.containsKey(n)) {
                    PdfDictionary vri = vrim.getAsDict(n);
                    if (vri != null) {
                        deleteOldReferences(ocsps, vri.getAsArray(PdfName.OCSP));
                        deleteOldReferences(crls, vri.getAsArray(PdfName.CRL));
                        deleteOldReferences(certs, vri.getAsArray(PdfName.CERT));
                    }
                }
            }
        }
        if (ocsps == null) {
            ocsps = new PdfArray();
        }
        if (crls == null) {
            crls = new PdfArray();
        }
        if (certs == null) {
            certs = new PdfArray();
        }
        outputDss(dss, vrim, ocsps, crls, certs);
    }

    private static void deleteOldReferences(PdfArray all, PdfArray toDelete) {
        if (all == null || toDelete == null) {
            return;
        }
        for (PdfObject pi : toDelete) {
            if (!pi.isIndirect()) {
                continue;
            }
            PRIndirectReference pir = (PRIndirectReference) pi;
            for (int k = 0; k < all.size(); k++) {
                PdfObject po = all.getPdfObject(k);
                if (po.isIndirect()) {

                    PRIndirectReference pod = (PRIndirectReference) po;
                    if (pir.getNumber() == pod.getNumber()) {
                        all.remove(k);
                        k--;
                    }
                }
            }
        }
    }

    private void createDss() throws IOException {
        outputDss(new PdfDictionary(), new PdfDictionary(), new PdfArray(), new PdfArray(), new PdfArray());
    }

    private void outputDss(PdfDictionary dss, PdfDictionary vrim, PdfArray ocsps, PdfArray crls, PdfArray certs) throws IOException {
        this.writer.addDeveloperExtension(PdfDeveloperExtension.ESIC_1_7_EXTENSIONLEVEL5);
        PdfDictionary catalog = this.reader.getCatalog();
        this.stp.markUsed((PdfObject) catalog);
        for (PdfName vkey : this.validated.keySet()) {
            PdfArray ocsp = new PdfArray();
            PdfArray crl = new PdfArray();
            PdfArray cert = new PdfArray();
            PdfDictionary vri = new PdfDictionary();
            for (byte[] b : ((ValidationData) this.validated.get(vkey)).crls) {
                PdfStream ps = new PdfStream(b);
                ps.flateCompress();
                PdfIndirectReference iref = this.writer.addToBody((PdfObject) ps, false).getIndirectReference();
                crl.add((PdfObject) iref);
                crls.add((PdfObject) iref);
            }
            for (byte[] b : ((ValidationData) this.validated.get(vkey)).ocsps) {
                PdfStream ps = new PdfStream(b);
                ps.flateCompress();
                PdfIndirectReference iref = this.writer.addToBody((PdfObject) ps, false).getIndirectReference();
                ocsp.add((PdfObject) iref);
                ocsps.add((PdfObject) iref);
            }
            for (byte[] b : ((ValidationData) this.validated.get(vkey)).certs) {
                PdfStream ps = new PdfStream(b);
                ps.flateCompress();
                PdfIndirectReference iref = this.writer.addToBody((PdfObject) ps, false).getIndirectReference();
                cert.add((PdfObject) iref);
                certs.add((PdfObject) iref);
            }
            if (ocsp.size() > 0) {
                vri.put(PdfName.OCSP, (PdfObject) this.writer.addToBody((PdfObject) ocsp, false).getIndirectReference());
            }
            if (crl.size() > 0) {
                vri.put(PdfName.CRL, (PdfObject) this.writer.addToBody((PdfObject) crl, false).getIndirectReference());
            }
            if (cert.size() > 0) {
                vri.put(PdfName.CERT, (PdfObject) this.writer.addToBody((PdfObject) cert, false).getIndirectReference());
            }
            vrim.put(vkey, (PdfObject) this.writer.addToBody((PdfObject) vri, false).getIndirectReference());
        }
        dss.put(PdfName.VRI, (PdfObject) this.writer.addToBody((PdfObject) vrim, false).getIndirectReference());
        if (ocsps.size() > 0) {
            dss.put(PdfName.OCSPS, (PdfObject) this.writer.addToBody((PdfObject) ocsps, false).getIndirectReference());
        }
        if (crls.size() > 0) {
            dss.put(PdfName.CRLS, (PdfObject) this.writer.addToBody((PdfObject) crls, false).getIndirectReference());
        }
        if (certs.size() > 0) {
            dss.put(PdfName.CERTS, (PdfObject) this.writer.addToBody((PdfObject) certs, false).getIndirectReference());
        }
        catalog.put(PdfName.DSS, (PdfObject) this.writer.addToBody((PdfObject) dss, false).getIndirectReference());
    }

    private static class ValidationData {

        public List<byte[]> crls = (List) new ArrayList<byte[]>();

        private ValidationData() {
        }
        public List<byte[]> ocsps = (List) new ArrayList<byte[]>();
        public List<byte[]> certs = (List) new ArrayList<byte[]>();
    }
}
