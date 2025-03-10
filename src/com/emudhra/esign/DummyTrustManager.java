package com.emudhra.esign;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public final class DummyTrustManager implements X509TrustManager {

    public boolean isClientTrusted() {
        return true;
    }

    public boolean isServerTrusted() {
        return true;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        // Over ridden method of X509TrustManager
    }

    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        // Over ridden method of X509TrustManager
    }

}
