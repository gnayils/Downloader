package org.gnayils;

import org.gnayils.Utilities;
import org.gnayils.downloader.MasterDownloader;
import org.gnayils.downloader.SlaveDownloader;
import org.gnayils.gui.TerminalUI;

import java.io.IOException;
import java.util.logging.Level;

public class Main {

    static {
        Utilities.resetLoggerFormat();
        Utilities.trustAllCertificates();
        Utilities.setLoggerLevel(Level.ALL);
    }

    public static String startupType;
    public static String ip;
    public static int port;
    public static String multicastGroupIp;
    public static int multicastPort;
    public static String downloadUrl;
    public static int waitSlaveTimeout;

    public static void main(String[] args) throws IOException {
        if(!fetchArgs()) {
            printUsage();
            return;
        }
        if("master".equals(startupType)) {
            MasterDownloader master = new MasterDownloader(ip, port, multicastGroupIp, multicastPort,downloadUrl, waitSlaveTimeout);
            master.setListener(new TerminalUI());
            master.start();
        } else if("slave".equals(startupType)) {
            SlaveDownloader slave = new SlaveDownloader(ip, multicastGroupIp, multicastPort);
            slave.start();
        }
    }

    private static boolean fetchArgs() {
        startupType = System.getProperty("startupType");
        if(startupType == null || startupType.trim().isEmpty() || !("master".equals(startupType.trim()) || "slave".equals(startupType.trim()))) {
            System.out.println("please specify the startup type");
            return false;
        }
        ip = System.getProperty("ip");
        if(ip == null || ip.trim().isEmpty()) {
            System.out.println("please specify the host ip");
            return false;
        }
        if("master".equals(startupType)) {
            String portStr = System.getProperty("port");
            if (portStr == null || portStr.trim().isEmpty()) {
                System.out.println("please specify the host port");
                return false;
            }
            try {
                port = Integer.valueOf(portStr.trim());
            } catch (NumberFormatException e) {
                System.out.println("please specify a invalid host port");
                return false;
            }
        }
        multicastGroupIp = System.getProperty("multicastGroupIp");
        if(multicastGroupIp == null || multicastGroupIp.trim().isEmpty()) {
            System.out.println("please specify the multicast group ip");
            return false;
        }
        String multicastPortStr = System.getProperty("multicastPort");
        if(multicastPortStr == null || multicastPortStr.trim().isEmpty()) {
            System.out.println("please specify the multicast port");
            return false;
        }
        try {
            multicastPort = Integer.valueOf(multicastPortStr.trim());
        } catch (NumberFormatException e) {
            System.out.println("please specify a valid multicast port");
            return false;
        }
        if("master".equals(startupType)) {
            downloadUrl = System.getProperty("downloadUrl");
            if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
                System.out.println("please specify the download url");
                return false;
            }
            String waitSlaveTimeoutStr = System.getProperty("waitSlaveTimeout", "5000");
            try {
                waitSlaveTimeout = Integer.valueOf(waitSlaveTimeoutStr);
            } catch (NumberFormatException e) {
                System.out.println("please specify a valid timeout for wait the slave connect");
                return false;
            }
        }
        return true;
    }

    private static void printUsage() {
        System.out.println(
                "Usage:\n" +
                "    java -jar -DstartupType=master -Dip=192.168.1.100 -Dport=9000 -DmulticastGroupIp=230.0.0.1 -DmulticastPort=9500 -DdownloadUrl=http://google.com/archive.zip -DwaitSlaveTimeout=5 downloader.jar\n" +
                "    java -jar -DstartupType=slave -Dip=192.168.1.101 -DmulticastGroupIp=230.0.0.1 -DmulticastPort=9500 downloader.jar"
        );
    }
}
