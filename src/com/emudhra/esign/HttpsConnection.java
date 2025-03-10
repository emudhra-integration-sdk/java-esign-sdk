/**
 *
 */
package com.emudhra.esign;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
//import org.apache.logging.log4j.core.Logger;

/**
 * @author 20323
 *
 *
 */
public final class HttpsConnection {

    private static Logger logger = EsignLoggerFactory.getLogger(HttpsURLConnection.class);

    public static String excutePostHttpsXml(String targetURL, String urlParameters, String proxyIp, int proxyPort, boolean proxyreq, String txn) throws IOException, SQLException, Exception {
        logger.info("proxyreq ---------->" + proxyreq);
        logger.info("proxyIp ---------->" + proxyIp);
        logger.info("proxyPort ---------->" + proxyPort);
        logger.info("timeout ---------->" + eSignSettings.getSessionTimeout());
        URL url;
        HttpsURLConnection connection = null;
        SSLContext sslcontext = null;
        int sessionTimeout = eSignSettings.getSessionTimeout();
        try {

            sslcontext = SSLContext.getInstance("TLSv1.2");
            sslcontext.init(new KeyManager[0], new TrustManager[]{new DummyTrustManager()}, new SecureRandom());

        } catch (NoSuchAlgorithmException e) {
            logger.warning("Exception in SSLContext " + e);
            return e.toString();
        } catch (KeyManagementException e) {
            logger.warning("Exception in SSLContext " + e);
            return e.toString();
        }
        try {
            //Create connection
            SSLSocketFactory factory = sslcontext.getSocketFactory();
            url = new URL(targetURL);
            try {
                logger.info("HttpsConnection.excutePostHttpsXml Start ");
                if (proxyreq == true) // Updated by Munish @ 20323
                {
                    logger.info("Proxy Added for IP address :" + proxyIp + " and port number :" + proxyPort + " TxnID: " + txn);
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyIp, proxyPort));
                    connection = (HttpsURLConnection) url.openConnection(proxy);
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
                    connection = (HttpsURLConnection) url.openConnection();
                }
            } catch (IOException ee) {
                //logger.error("Exception in HttpsConnection.excutePostHttpsXml while openning connection " + Arrays.toString(ee.getStackTrace()));
                logger.warning("Exception in HttpsConnection.excutePostHttpsXml for session/read timeout :" + sessionTimeout + " mili second");
                //logger.info("Exception in HttpsConnection.excutePostHttpsXml while openning connection " + ee);
                throw ee;
            }
            if (sessionTimeout > 0) {
                connection.setConnectTimeout(sessionTimeout);
                connection.setReadTimeout(sessionTimeout);
            }
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setSSLSocketFactory(factory);
            connection.setHostnameVerifier(new DummyHostnameVerifier());
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            //logger.debug("responseCode ---------->" + responseCode);
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            logger.info("HttpsConnection.excutePostHttpsXml END");
            return response.toString();
        } catch (Exception e) {
            logger.warning("Exception in HttpsConnection.excutePostHttpsXml for session/read timeout :" + sessionTimeout + " mili second.");
            //logger.info("Exception in HttpsConnection.excutePostHttpsXml " + e);
            throw e;
            //return e.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
