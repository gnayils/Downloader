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

    //https://bintray.com/tigervnc/stable/download_file?file_path=TigerVNC-1.8.0.dmg
    //https://download-cf.jetbrains.com/cpp/CLion-2018.1.2.zip
    public static void main(String[] args) throws IOException {
        MasterDownloader master = new MasterDownloader("10.189.132.32", 9000, "230.0.0.1", 8000,"https://bintray.com/tigervnc/stable/download_file?file_path=TigerVNC-1.8.0.dmg", 3);
        master.setListener(new TerminalUI());
        master.start();


    }
}
