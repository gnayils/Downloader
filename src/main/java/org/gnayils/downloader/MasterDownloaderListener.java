package org.gnayils.downloader;

import java.io.File;
import java.net.URL;
import java.util.Map;

public interface MasterDownloaderListener {

    void onDownloadStart(URL downloadUrl);

    void onGetDownloadSize(int downloadContentLength);

    void onHireSlaveDownloaderStart();

    void onHireSlaveDownloaderDone(int slaveDownloaderCount);

    void onSlaveDownloadStart(Map<String, int[]> downloadProgressMap);

    void onDownloadDone(boolean isDownloadSuccessful, File downloadFile);

}
