package org.gnayils.downloader;

import javax.net.ssl.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class Utilities {

    public static void trustAllCertificates() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{ new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                    } }, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public static String getQualifiedFileName(String text) {
        final String illegalChars = "\\/:*?\"<>|";
        int lastIllegalCharIndex = -1;
        for(int i = text.length() - 1; i >= 0; i--) {
            if(illegalChars.indexOf(text.charAt(i)) != -1) {
                lastIllegalCharIndex = i;
                break;
            }
        }
        if(lastIllegalCharIndex != -1) {
            return text.substring(lastIllegalCharIndex + 1);
        }
        return null;
    }

    public static Proxy getHttpProxy() {
        String proxyHost = System.getProperty("http.ProxyHost");
        String proxyPort = System.getProperty("http.ProxyPort");
        if(proxyHost != null && proxyPort != null) {
            int port;
            try {
                port = Integer.parseInt(proxyHost);
                return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, port));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
;
    public static void main(String[] args) {
        System.out.println(getQualifiedFileName("/tigervnc/stable/download_file?file_path=TigerVNC-1.8.0.dmg"));
    }
}
