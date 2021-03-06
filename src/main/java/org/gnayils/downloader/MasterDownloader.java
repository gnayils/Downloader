package org.gnayils.downloader;

import org.gnayils.Packet;
import org.gnayils.Utilities;
import org.gnayils.channel.MulticastChannel;
import org.gnayils.channel.ObjectChannel;
import org.gnayils.channel.PeerChannel;
import org.gnayils.channel.ServerChannel;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.*;

public class MasterDownloader implements Runnable {

    public static final int POLLING_TIMEOUT = 100;

    private String ip;
    private int port;
    private String multicastGroupIp;
    private int multicastPort;
    private int waitSlaveTimeout;

    private MasterDownloaderListener listener;

    private Map<String, ObjectChannel<PeerChannel>> slaveChannelMap = new HashMap<>();

    private Timer timer;

    private URL downloadUrl;
    private int downloadContentLength;
    private File downloadFile;
    private RandomAccessFile randomAccessFile;
    private Map<String, int[]> downloadProgressMap = new HashMap<>();

    private ObjectChannel<MulticastChannel> multicastObjectChannel;
    private ServerChannel serverChannel;

    private boolean isDownloadSuccessful = false;

    private volatile boolean isRunning;

    private Thread thread;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    public MasterDownloader(String ip, int port, String multicastGroupIp, int multicastPort, String downloadUrl, int waitSlaveTimeout) throws IOException {
        this.ip = ip;
        this.port = port;
        this.multicastGroupIp = multicastGroupIp;
        this.multicastPort = multicastPort;
        this.downloadUrl = new URL(downloadUrl);
        this.waitSlaveTimeout = waitSlaveTimeout;
        this.multicastObjectChannel = new ObjectChannel<>(new MulticastChannel(ip, multicastPort, multicastGroupIp));
        this.serverChannel =  new ServerChannel(ip, port);
        this.timer = new Timer();
    }

    public void setListener(MasterDownloaderListener listener) {
        this.listener = listener;
    }


    public void start() {
        if(!isRunning) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        if(isRunning)  {
            isRunning = false;
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        isRunning = true;
        try {
            getContentLength();
            hireSlaveDownloader();
            dispatchTask();
            receiveDataFromSlave();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isRunning = false;
            cleanup();
        }
    }

    private void getContentLength() throws IOException {
        if(listener != null) listener.onDownloadStart(downloadUrl);
        Proxy proxy = Utilities.getHttpProxy();
        HttpURLConnection httpURLConnection;
        if(proxy == null) {
            httpURLConnection = (HttpURLConnection) downloadUrl.openConnection();
        } else {
            httpURLConnection = (HttpURLConnection) downloadUrl.openConnection(proxy);
        }
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setConnectTimeout(10000);
        httpURLConnection.setReadTimeout(10000);
        if (httpURLConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            downloadContentLength = httpURLConnection.getContentLength();
            if(listener != null) listener.onGetDownloadSize(downloadContentLength);
            httpURLConnection.disconnect();
            downloadFile = new File(String.valueOf(Utilities.getQualifiedFileName(downloadUrl.getFile())));
            if(!downloadFile.exists()) downloadFile.createNewFile();
            randomAccessFile = new RandomAccessFile(downloadFile, "rwd");
            randomAccessFile.setLength(downloadContentLength);
            logger.log(Level.INFO, "master downloader started, begin to download {0}, the size is {1}", new Object[]{ downloadFile.getName(), downloadContentLength});
        } else {
            throw new IOException("get download file length failed, with response code: " + httpURLConnection.getResponseCode());
        }
    }

    private void hireSlaveDownloader() throws Exception {
        if(listener != null) listener.onHireSlaveDownloaderStart();
        serverChannel.bind();
        multicastObjectChannel.getChannel().joinGroup();
        TimerTask timerTask = new TimerTask() {

            Packet packet = new Packet(Packet.CONNECT_MASTER, port);

            @Override
            public void run() {
                try {
                    multicastObjectChannel.writeObject(packet);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "multicast the message for hire slave failed");
                    e.printStackTrace();
                    cancel();
                    stop();
                }
            }
        };
        logger.log(Level.INFO, "prepare hire slave downloader via sending broadcast to the local network, wait for {0} seconds", waitSlaveTimeout);
        timer.schedule(timerTask, 0, 1000);
        long expiredTime = System.currentTimeMillis() + waitSlaveTimeout * 1000;
        do {
            PeerChannel peerChannel = serverChannel.accept(1000);
            if(peerChannel != null) {
                logger.log(Level.INFO, "hired one slave downloader, come from {0}", peerChannel.channel().getRemoteAddress().toString());
                slaveChannelMap.put(peerChannel.channel().getRemoteAddress().toString(), new ObjectChannel<>(peerChannel));
            }
            if(!isRunning || Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
        } while (System.currentTimeMillis() < expiredTime);
        logger.log(Level.INFO, "hire slave downloader finished with {0} slave downloader", slaveChannelMap.size());
        timerTask.cancel();
        timer.purge();
        multicastObjectChannel.getChannel().leaveGroup();
        if(slaveChannelMap.isEmpty()) throw new Exception("no any slave downloader hired for download task");
        if(listener != null) listener.onHireSlaveDownloaderDone(slaveChannelMap.size());
    }

    private void dispatchTask() throws IOException {
        int rangeLength = downloadContentLength / slaveChannelMap.size();
        int i = 0;
        for(Map.Entry<String, ObjectChannel<PeerChannel>> entry : slaveChannelMap.entrySet()) {
            int startPosition = i * rangeLength;
            int endPosition = startPosition + rangeLength - 1;
            if (i == slaveChannelMap.size() - 1) {
                endPosition = downloadContentLength - 1;
            }
            try {
                entry.getValue().writeObject(new Packet(Packet.DOWNLOAD_PARAMS, String.format("%s %d/%d", downloadUrl.toString(), startPosition, endPosition)));downloadProgressMap.put(entry.getKey(), new int[] {startPosition, endPosition});
                downloadProgressMap.put(entry.getKey(), new int[] {startPosition, endPosition});
                i++;
                logger.log(Level.INFO, "dispatch download task [{0}~{1}] to the slave downloader {2}", new Object[] {startPosition, endPosition, entry.getKey()});
            } catch (IOException e) {
                logger.log(Level.SEVERE, "dispatch task to the slave downloader {0} failed", entry.getKey());
                throw e;
            }
        }
    }

    private void receiveDataFromSlave() throws IOException, ClassNotFoundException {
        if(listener != null) listener.onSlaveDownloadStart(downloadProgressMap);
        while(!downloadProgressMap.isEmpty() && !slaveChannelMap.isEmpty()) {
            Iterator<Map.Entry<String, ObjectChannel<PeerChannel>>> iterator = slaveChannelMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, ObjectChannel<PeerChannel>> entry = iterator.next();
                ObjectChannel.ObjectReadResult result = entry.getValue().readObject(POLLING_TIMEOUT);
                if(result == null)  continue;
                Packet packet = (Packet) result.object;
                int[] progress = downloadProgressMap.get(result.sourceAddress.toString());
                if(packet.type == Packet.DOWNLOADED_DATA) {
                    byte[] downloadedData = (byte[]) packet.payload;
                    randomAccessFile.seek(progress[0]);
                    randomAccessFile.write(downloadedData);
                    progress[0] += downloadedData.length;
                    logger.log(Level.INFO, "received data come from slave downloader {0} with length {1}", new Object[] {result.sourceAddress.toString(), downloadedData.length});
                } else if(packet.type == Packet.DOWNLOAD_COMPLETED){
                    if(progress[0] - 1 == progress[1]) {
                        downloadProgressMap.remove(result.sourceAddress.toString());
                        iterator.remove();
                    } else {
                        throw new IOException("although DOWNLOAD_COMPLETED received but progress[0] - 1 != progress[1]: " + result.sourceAddress.toString());
                    }
                } else if(packet.type == Packet.DOWNLOAD_ERROR) {
                    iterator.remove();
                    throw new IOException("download failed, error occurred at slave downloader side: " + result.sourceAddress.toString());
                } else {
                    throw new IOException("wrong type for the packet received from slave downloader");
                }
            }
        }
        isDownloadSuccessful = downloadProgressMap.isEmpty();
        if(isDownloadSuccessful) {
            logger.info("all data receive from slave downloader finished, download is successful");
        }
    }

    private void cleanup() {
        timer.cancel();
        multicastObjectChannel.getChannel().close();
        for(Map.Entry<String, ObjectChannel<PeerChannel>> entry : slaveChannelMap.entrySet()) {
            entry.getValue().getChannel().close();
        }
        if(randomAccessFile != null) {
            try {
                randomAccessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!isDownloadSuccessful) {
                downloadFile.delete();
            }
        }
        if(listener != null) listener.onDownloadDone(isDownloadSuccessful, downloadFile);
    }

}
