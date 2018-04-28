package org.gnayils.downloader;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    static {
        Utilities.resetLoggerFormat();
        Utilities.trustAllCertificates();
        Utilities.setLoggerLevel(Level.ALL);
    }

    public static void main(String[] args) throws IOException {
        MasterDownloader master = new MasterDownloader("10.189.132.188", 9000, "230.0.0.1", 8000,"https://download-cf.jetbrains.com/cpp/CLion-2018.1.2.zip", 3);
        master.start();

//        SlaveDownloader slave = new SlaveDownloader("10.189.132.188", "230.0.0.1", 8000);
//        slave.start();
    }
}
