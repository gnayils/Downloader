package org.gnayils.gui;

import org.gnayils.downloader.MasterDownloaderListener;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TerminalUI implements MasterDownloaderListener {

    private Timer timer = new Timer();

    @Override
    public void onDownloadStart(URL downloadUrl) {
        System.out.println("Start Download: " + downloadUrl);
        LoadingPrinter.print("get download file size");
    }

    @Override
    public void onGetDownloadSize(int downloadContentLength) {
        LoadingPrinter.stopCurrentPrint();
        System.out.printf("\rget download file size：%dKB\n", downloadContentLength / 1024);
    }

    @Override
    public void onHireSlaveDownloaderStart() {
        LoadingPrinter.print("start hire slave downloader");
    }

    @Override
    public void onHireSlaveDownloaderDone(int slaveDownloaderCount) {
        LoadingPrinter.stopCurrentPrint();
        System.out.printf("\rhire slave downloader done, %d accept the download task\n", slaveDownloaderCount);
    }

    @Override
    public void onSlaveDownloadStart(Map<String, int[]> downloadProgressMap) {

        final Map<String, int[]> downloadInfoMap = new HashMap<>();
        for(Map.Entry<String, int[]> entry : downloadProgressMap.entrySet()) {
            downloadInfoMap.put(entry.getKey(), new int[] {entry.getValue()[0], entry.getValue()[0], entry.getValue()[1] - entry.getValue()[0] + 1});
        }

        timer.schedule(new TimerTask() {

            final int MAX_PROGRESS_CHAR_COUNT = 50;
            final String FORMAT_1 = "%" + MAX_PROGRESS_CHAR_COUNT + "c";
            final String FORMAT_2 = "%-" + MAX_PROGRESS_CHAR_COUNT + "s";

            @Override
            public void run() {
                for(Map.Entry<String, int[]> entry : downloadInfoMap.entrySet()) {
                    int startPosition = entry.getValue()[0];
                    int lastPosition = entry.getValue()[1];
                    int totalLength = entry.getValue()[2];

                    int[] progressInfo = downloadProgressMap.get(entry.getKey());
                    int currentPosition;
                    int speed;
                    if(progressInfo == null) {
                        currentPosition = startPosition + totalLength - 1;
                        speed = 0;
                    } else {
                        currentPosition = progressInfo[0];
                        speed = (currentPosition - lastPosition) * 2;
                    }

                    float progressRatio = ((float) currentPosition - startPosition + 1) / totalLength;
                    int progressCharCount = (int) (progressRatio * MAX_PROGRESS_CHAR_COUNT);
                    String progressString = progressCharCount < 1 ? String.format(FORMAT_1, ' ') : String.format(FORMAT_2, String.format("%" + progressCharCount + "c", ' ').replace(' ', '='));
                    int progressPercent = (int) (progressRatio * 100);

                    System.out.printf("%15s: [%s] %3d%% %4dKB/s%n", entry.getKey(), progressString, progressPercent, speed / 1024);

                    entry.getValue()[1] = currentPosition;
                }
                if(downloadProgressMap.isEmpty()) {
                    synchronized (TerminalUI.this) {
                        TerminalUI.this.notifyAll();
                    }
                } else {
                    System.out.print("\u001b[" + downloadInfoMap.size() + "A");
                }
            }
        }, 0, 500);
    }

    @Override
    public void onDownloadDone(boolean isDownloadSuccessful, File downloadFile) {
        if(isDownloadSuccessful) {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        timer.cancel();
        LoadingPrinter.teardown();
        System.out.printf("%ndownload %s%s%n",
                isDownloadSuccessful ? "successful" : "failed",
                downloadFile == null ? "" : ": " + downloadFile.getAbsolutePath());
    }

    public static void main(String[] args)  {
        System.out.println(System.getProperty("os.name"));
    }
}
