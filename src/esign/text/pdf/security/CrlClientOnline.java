package esign.text.pdf.security;

import esign.text.error_messages.MessageLocalization;
import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CrlClientOnline
        implements CrlClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrlClientOnline.class);

    protected List<URL> urls = new ArrayList<URL>();

    public CrlClientOnline() {
    }

    public CrlClientOnline(String... crls) {
        for (String url : crls) {
            addUrl(url);
        }
    }

    public CrlClientOnline(URL... crls) {
        for (URL url : this.urls) {
            addUrl(url);
        }
    }

    public CrlClientOnline(Certificate[] chain) {
        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = (X509Certificate) chain[i];
            LOGGER.info("Checking certificate: " + cert.getSubjectDN());
            try {
                addUrl(CertificateUtil.getCRLURL(cert));
            } catch (CertificateParsingException e) {
                LOGGER.info("Skipped CRL url (certificate could not be parsed)");
            }
        }
    }

    protected void addUrl(String url) {
        try {
            addUrl(new URL(url));
        } catch (MalformedURLException e) {
            LOGGER.info("Skipped CRL url (malformed): " + url);
        }
    }

    protected void addUrl(URL url) {
        if (this.urls.contains(url)) {
            LOGGER.info("Skipped CRL url (duplicate): " + url);
            return;
        }
        this.urls.add(url);
        LOGGER.info("Added CRL url: " + url);
    }

    public Collection<byte[]> getEncoded(X509Certificate checkCert, String url) {
        if (checkCert == null) {
            return null;
        }
        List<URL> urllist = new ArrayList<URL>(this.urls);
        if (urllist.size() == 0) {
            LOGGER.info("Looking for CRL for certificate " + checkCert.getSubjectDN());
            try {
                if (url == null) {
                    url = CertificateUtil.getCRLURL(checkCert);
                }
                if (url == null) {
                    throw new NullPointerException();
                }
                urllist.add(new URL(url));
                LOGGER.info("Found CRL url: " + url);
            } catch (Exception e) {
                LOGGER.info("Skipped CRL url: " + e.getMessage());
            }
        }
        ArrayList<byte[]> ar = (ArrayList) new ArrayList<byte[]>();
        for (URL urlt : urllist) {
            try {
                LOGGER.info("Checking CRL: " + urlt);
                HttpURLConnection con = (HttpURLConnection) urlt.openConnection();
                if (con.getResponseCode() / 100 != 2) {
                    throw new IOException(MessageLocalization.getComposedMessage("invalid.http.response.1", con.getResponseCode()));
                }

                InputStream inp = (InputStream) con.getContent();
                byte[] buf = new byte[1024];
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                while (true) {
                    int n = inp.read(buf, 0, buf.length);
                    if (n <= 0) {
                        break;
                    }
                    bout.write(buf, 0, n);
                }
                inp.close();
                ar.add(bout.toByteArray());
                LOGGER.info("Added CRL found at: " + urlt);
            } catch (Exception e) {
                LOGGER.info("Skipped CRL: " + e.getMessage() + " for " + urlt);
            }
        }
        return (Collection<byte[]>) ar;
    }
}
