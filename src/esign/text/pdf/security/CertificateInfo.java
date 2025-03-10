package esign.text.pdf.security;

import esign.text.ExceptionConverter;
import esign.text.error_messages.MessageLocalization;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.emcastle.asn1.ASN1InputStream;
import org.emcastle.asn1.ASN1ObjectIdentifier;
import org.emcastle.asn1.ASN1Primitive;
import org.emcastle.asn1.ASN1Sequence;
import org.emcastle.asn1.ASN1Set;
import org.emcastle.asn1.ASN1String;

public class CertificateInfo {

    public static class X500Name {

        public static final ASN1ObjectIdentifier C = new ASN1ObjectIdentifier("2.5.4.6");

        public static final ASN1ObjectIdentifier O = new ASN1ObjectIdentifier("2.5.4.10");

        public static final ASN1ObjectIdentifier OU = new ASN1ObjectIdentifier("2.5.4.11");

        public static final ASN1ObjectIdentifier T = new ASN1ObjectIdentifier("2.5.4.12");

        public static final ASN1ObjectIdentifier CN = new ASN1ObjectIdentifier("2.5.4.3");

        public static final ASN1ObjectIdentifier SN = new ASN1ObjectIdentifier("2.5.4.5");

        public static final ASN1ObjectIdentifier L = new ASN1ObjectIdentifier("2.5.4.7");

        public static final ASN1ObjectIdentifier ST = new ASN1ObjectIdentifier("2.5.4.8");

        public static final ASN1ObjectIdentifier SURNAME = new ASN1ObjectIdentifier("2.5.4.4");

        public static final ASN1ObjectIdentifier GIVENNAME = new ASN1ObjectIdentifier("2.5.4.42");

        public static final ASN1ObjectIdentifier INITIALS = new ASN1ObjectIdentifier("2.5.4.43");

        public static final ASN1ObjectIdentifier GENERATION = new ASN1ObjectIdentifier("2.5.4.44");

        public static final ASN1ObjectIdentifier UNIQUE_IDENTIFIER = new ASN1ObjectIdentifier("2.5.4.45");

        public static final ASN1ObjectIdentifier EmailAddress = new ASN1ObjectIdentifier("1.2.840.113549.1.9.1");

        public static final ASN1ObjectIdentifier E = EmailAddress;

        public static final ASN1ObjectIdentifier DC = new ASN1ObjectIdentifier("0.9.2342.19200300.100.1.25");

        public static final ASN1ObjectIdentifier UID = new ASN1ObjectIdentifier("0.9.2342.19200300.100.1.1");

        public static final Map<ASN1ObjectIdentifier, String> DefaultSymbols = new HashMap<ASN1ObjectIdentifier, String>();

        static {
            DefaultSymbols.put(C, "C");
            DefaultSymbols.put(O, "O");
            DefaultSymbols.put(T, "T");
            DefaultSymbols.put(OU, "OU");
            DefaultSymbols.put(CN, "CN");
            DefaultSymbols.put(L, "L");
            DefaultSymbols.put(ST, "ST");
            DefaultSymbols.put(SN, "SN");
            DefaultSymbols.put(EmailAddress, "E");
            DefaultSymbols.put(DC, "DC");
            DefaultSymbols.put(UID, "UID");
            DefaultSymbols.put(SURNAME, "SURNAME");
            DefaultSymbols.put(GIVENNAME, "GIVENNAME");
            DefaultSymbols.put(INITIALS, "INITIALS");
            DefaultSymbols.put(GENERATION, "GENERATION");
        }

        public Map<String, ArrayList<String>> values = new HashMap<String, ArrayList<String>>();

        public X500Name(ASN1Sequence seq) {
            Enumeration<ASN1Set> e = seq.getObjects();

            while (e.hasMoreElements()) {
                ASN1Set set = e.nextElement();

                for (int i = 0; i < set.size(); i++) {
                    ASN1Sequence s = (ASN1Sequence) set.getObjectAt(i);
                    String id = DefaultSymbols.get(s.getObjectAt(0));
                    if (id != null) {

                        ArrayList<String> vs = this.values.get(id);
                        if (vs == null) {
                            vs = new ArrayList<String>();
                            this.values.put(id, vs);
                        }
                        vs.add(((ASN1String) s.getObjectAt(1)).getString());
                    }
                }
            }
        }

        public X500Name(String dirName) {
            CertificateInfo.X509NameTokenizer nTok = new CertificateInfo.X509NameTokenizer(dirName);

            while (nTok.hasMoreTokens()) {
                String token = nTok.nextToken();
                int index = token.indexOf('=');

                if (index == -1) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("badly.formated.directory.string", new Object[0]));
                }

                String id = token.substring(0, index).toUpperCase();
                String value = token.substring(index + 1);
                ArrayList<String> vs = this.values.get(id);
                if (vs == null) {
                    vs = new ArrayList<String>();
                    this.values.put(id, vs);
                }
                vs.add(value);
            }
        }

        public String getField(String name) {
            List<String> vs = this.values.get(name);
            return (vs == null) ? null : vs.get(0);
        }

        public List<String> getFieldArray(String name) {
            return this.values.get(name);
        }

        public Map<String, ArrayList<String>> getFields() {
            return this.values;
        }

        public String toString() {
            return this.values.toString();
        }
    }

    public static class X509NameTokenizer {

        private String oid;

        private int index;

        private StringBuffer buf = new StringBuffer();

        public X509NameTokenizer(String oid) {
            this.oid = oid;
            this.index = -1;
        }

        public boolean hasMoreTokens() {
            return (this.index != this.oid.length());
        }

        public String nextToken() {
            if (this.index == this.oid.length()) {
                return null;
            }

            int end = this.index + 1;
            boolean quoted = false;
            boolean escaped = false;

            this.buf.setLength(0);

            while (end != this.oid.length()) {
                char c = this.oid.charAt(end);

                if (c == '"') {
                    if (!escaped) {
                        quoted = !quoted;
                    } else {

                        this.buf.append(c);
                    }
                    escaped = false;

                } else if (escaped || quoted) {
                    this.buf.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else {
                    if (c == ',') {
                        break;
                    }

                    this.buf.append(c);
                }

                end++;
            }

            this.index = end;
            return this.buf.toString().trim();
        }
    }

    public static X500Name getIssuerFields(X509Certificate cert) {
        try {
            return new X500Name((ASN1Sequence) getIssuer(cert.getTBSCertificate()));
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public static ASN1Primitive getIssuer(byte[] enc) {
        try {
            ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(enc));
            ASN1Sequence seq = (ASN1Sequence) in.readObject();
            return (ASN1Primitive) seq.getObjectAt((seq.getObjectAt(0) instanceof org.emcastle.asn1.ASN1TaggedObject) ? 3 : 2);
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    public static X500Name getSubjectFields(X509Certificate cert) {
        try {
            if (cert != null) {
                return new X500Name((ASN1Sequence) getSubject(cert.getTBSCertificate()));
            }
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
        return null;
    }

    public static ASN1Primitive getSubject(byte[] enc) {
        try {
            ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(enc));
            ASN1Sequence seq = (ASN1Sequence) in.readObject();
            return (ASN1Primitive) seq.getObjectAt((seq.getObjectAt(0) instanceof org.emcastle.asn1.ASN1TaggedObject) ? 5 : 4);
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }
}

