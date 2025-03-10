package esign.text.pdf.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import org.emcastle.asn1.ASN1InputStream;
import org.emcastle.asn1.ASN1ObjectIdentifier;
import org.emcastle.asn1.ASN1OctetString;
import org.emcastle.asn1.ASN1Primitive;
import org.emcastle.asn1.ASN1Sequence;
import org.emcastle.asn1.ASN1TaggedObject;
import org.emcastle.asn1.DERIA5String;
import org.emcastle.asn1.DEROctetString;
import org.emcastle.asn1.x509.CRLDistPoint;
import org.emcastle.asn1.x509.DistributionPoint;
import org.emcastle.asn1.x509.DistributionPointName;
import org.emcastle.asn1.x509.Extension;
import org.emcastle.asn1.x509.GeneralName;
import org.emcastle.asn1.x509.GeneralNames;

public class CertificateUtil {

    public static CRL getCRL(X509Certificate certificate) throws CertificateException, CRLException, IOException {
        return getCRL(getCRLURL(certificate));
    }

    public static String getCRLURL(X509Certificate certificate) throws CertificateParsingException {
        ASN1Primitive obj;
        try {
            obj = getExtensionValue(certificate, Extension.cRLDistributionPoints.getId());
        } catch (IOException e) {
            obj = null;
        }
        if (obj == null) {
            return null;
        }
        CRLDistPoint dist = CRLDistPoint.getInstance(obj);
        DistributionPoint[] dists = dist.getDistributionPoints();
        for (DistributionPoint p : dists) {
            DistributionPointName distributionPointName = p.getDistributionPoint();
            if (0 == distributionPointName.getType()) {

                GeneralNames generalNames = (GeneralNames) distributionPointName.getName();
                GeneralName[] names = generalNames.getNames();
                GeneralName[] arrayOfGeneralName1;
                int i;
                byte b;
                for (arrayOfGeneralName1 = names, i = arrayOfGeneralName1.length, b = 0; b < i;) {
                    GeneralName name = arrayOfGeneralName1[b];
                    if (name.getTagNo() != 6) {
                        b++;
                        continue;
                    }
                    DERIA5String derStr = DERIA5String.getInstance((ASN1TaggedObject) name.toASN1Primitive(), false);
                    return derStr.getString();
                }

            }
        }
        return null;
    }

    public static CRL getCRL(String url) throws IOException, CertificateException, CRLException {
        if (url == null) {
            return null;
        }
        InputStream is = (new URL(url)).openStream();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCRL(is);
    }

    public static String getOCSPURL(X509Certificate certificate) {
        try {
            ASN1Primitive obj = getExtensionValue(certificate, Extension.authorityInfoAccess.getId());
            if (obj == null) {
                return null;
            }
            ASN1Sequence AccessDescriptions = (ASN1Sequence) obj;
            for (int i = 0; i < AccessDescriptions.size(); i++) {
                ASN1Sequence AccessDescription = (ASN1Sequence) AccessDescriptions.getObjectAt(i);
                if (AccessDescription.size() == 2) {

                    if (AccessDescription.getObjectAt(0) instanceof ASN1ObjectIdentifier) {
                        ASN1ObjectIdentifier id = (ASN1ObjectIdentifier) AccessDescription.getObjectAt(0);
                        if ("1.3.6.1.5.5.7.48.1".equals(id.getId())) {
                            ASN1Primitive description = (ASN1Primitive) AccessDescription.getObjectAt(1);
                            String AccessLocation = getStringFromGeneralName(description);
                            if (AccessLocation == null) {
                                return "";
                            }

                            return AccessLocation;
                        }
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public static String getTSAURL(X509Certificate certificate) {
        byte[] der = certificate.getExtensionValue("1.2.840.113583.1.1.9.1");
        if (der == null) {
            return null;
        }
        try {
            ASN1Primitive asn1obj = ASN1Primitive.fromByteArray(der);
            DEROctetString octets = (DEROctetString) asn1obj;
            asn1obj = ASN1Primitive.fromByteArray(octets.getOctets());
            ASN1Sequence asn1seq = ASN1Sequence.getInstance(asn1obj);
            return getStringFromGeneralName(asn1seq.getObjectAt(1).toASN1Primitive());
        } catch (IOException e) {
            return null;
        }
    }

    private static ASN1Primitive getExtensionValue(X509Certificate certificate, String oid) throws IOException {
        byte[] bytes = certificate.getExtensionValue(oid);
        if (bytes == null) {
            return null;
        }
        ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(bytes));
        ASN1OctetString octs = (ASN1OctetString) aIn.readObject();
        aIn = new ASN1InputStream(new ByteArrayInputStream(octs.getOctets()));
        return aIn.readObject();
    }

    private static String getStringFromGeneralName(ASN1Primitive names) throws IOException {
        ASN1TaggedObject taggedObject = (ASN1TaggedObject) names;
        return new String(ASN1OctetString.getInstance(taggedObject, false).getOctets(), "ISO-8859-1");
    }
}
