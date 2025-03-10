package com.emudhra.esign;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public final class DummyHostnameVerifier implements HostnameVerifier {

    public boolean verify(String urlHostname, String certHostname) {
        return true;
    }

    @Override
    public boolean verify(String arg0, SSLSession arg1) {
        return true;
    }

}
