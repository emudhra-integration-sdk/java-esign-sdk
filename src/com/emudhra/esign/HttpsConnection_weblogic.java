/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.emudhra.esign;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.KeyManager;
import javax.net.ssl.HostnameVerifier;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Authenticator;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 *
 * @author 21729
 */
public class HttpsConnection_weblogic {

    private static Logger logger = EsignLoggerFactory.getLogger(HttpsURLConnection.class);

    public static String excutePostHttpsXml(String targetURL, String urlParameters, String proxyIp, int proxyPort, boolean proxyreq, String txn) throws IOException, SQLException, Exception {
        logger.info("proxyreq ---------->" + proxyreq);
        logger.info("proxyIp ---------->" + proxyIp);
        logger.info("proxyPort ---------->" + proxyPort);
        logger.info("timeout ---------->" + eSignSettings.getSessionTimeout());

        URL url;
        URLConnection connection = null;
        SSLContext sslcontext = null;
        int sessionTimeout = eSignSettings.getSessionTimeout();

        try {
            sslcontext = SSLContext.getInstance("TLSv1.2");
            sslcontext.init(new KeyManager[0], new TrustManager[]{new DummyTrustManager()}, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.warning("Exception in SSLContext " + e);
            return e.toString();
        }

        try {
            // Create connection
            url = new URL(targetURL);
            logger.info("HttpsConnection.excutePostHttpsXml Start ");

            if (proxyreq) {
                logger.info("Proxy Added for IP address :" + proxyIp + " and port number :" + proxyPort + " TxnID: " + txn);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyIp, proxyPort));
                connection = url.openConnection(proxy);
                if (eSignSettings.getProxyUserID() != null && !"".equals(eSignSettings.getProxyUserID())) {
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(eSignSettings.getProxyUserID(), eSignSettings.getProxyUserPassword().toCharArray());
                        }
                    });
                }
            } else {
                logger.info("Connection Start TxnID: " + txn);
                connection = url.openConnection();
            }

            if (sessionTimeout > 0) {
                connection.setConnectTimeout(sessionTimeout);
                connection.setReadTimeout(sessionTimeout);
            }

            // Cast and configure only if it's a HttpsURLConnection
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) connection;
                httpsConn.setSSLSocketFactory(sslcontext.getSocketFactory());
                httpsConn.setHostnameVerifier(new DummyHostnameVerifier());

                httpsConn.setRequestMethod("POST");
                httpsConn.setRequestProperty("Content-Type", "application/xml");
                httpsConn.setRequestProperty("Content-Length", "" + urlParameters.getBytes().length);
                httpsConn.setRequestProperty("Content-Language", "en-US");
                httpsConn.setUseCaches(false);
                httpsConn.setDoInput(true);
                httpsConn.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(httpsConn.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                InputStream is = httpsConn.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();
                logger.info("HttpsConnection.excutePostHttpsXml END");
                return response.toString();
            } else {
                throw new UnsupportedOperationException("Unsupported connection class: " + connection.getClass().getName());
            }

        } catch (Exception e) {
            logger.warning("Exception in HttpsConnection.excutePostHttpsXml for session/read timeout :" + sessionTimeout + " milli second.");
            throw e;
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
    }
}
