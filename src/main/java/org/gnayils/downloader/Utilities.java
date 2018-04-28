package org.gnayils.downloader;

import javax.net.ssl.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.*;

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
                port = Integer.parseInt(proxyPort);
                return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, port));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void resetLoggerFormat() {
        //System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        Logger rootLogger = Logger.getLogger("");
        for(Handler handler : rootLogger.getHandlers()) rootLogger.removeHandler(handler);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {

            final String LINE_SEPARATOR = System.getProperty("line.separator");

            @Override
            public String format(LogRecord record) {
                StringBuilder message = new StringBuilder();
                message.append(new Date(record.getMillis()))
                        .append(" ")
                        .append(record.getLevel().getLocalizedName())
                        .append(": ")
                        .append(formatMessage(record))
                        .append(LINE_SEPARATOR);
                if (record.getThrown() != null) {
                    try {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        record.getThrown().printStackTrace(pw);
                        pw.close();
                        message.append(sw.toString());
                    } catch (Exception ex) {
                    }
                }
                return message.toString();
            }
        });
        rootLogger.addHandler(handler);
    }

    public static void setLoggerLevel(Level level) {
        Logger.getLogger("").setLevel(level);
    }

    public static void main(String[] args) {
        System.out.println(getQualifiedFileName("/tigervnc/stable/download_file?file_path=TigerVNC-1.8.0.dmg"));
    }
}
