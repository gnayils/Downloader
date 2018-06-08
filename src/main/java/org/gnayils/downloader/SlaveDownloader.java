package org.gnayils.downloader;

import org.gnayils.Packet;
import org.gnayils.Utilities;
import org.gnayils.channel.ClientChannel;
import org.gnayils.channel.MulticastChannel;
import org.gnayils.channel.ObjectChannel;
import org.gnayils.channel.ObjectChannel.ObjectReadResult;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SlaveDownloader implements Runnable {

    private String ip;
    private String multicastGroupIp;
    private int multicastPort;

    private ObjectChannel<MulticastChannel> multicastObjectChannel;
    private ObjectChannel<ClientChannel> clientObjectChannel;

    private String url;
    private int startDownloadPosition;
    private int endDownloadPosition;

    private volatile boolean isRunning;

    private Thread thread;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    public SlaveDownloader(String ip, String multicastGroupIp, int multicastPort) throws IOException {
        this.ip = ip;
        this.multicastGroupIp = multicastGroupIp;
        this.multicastPort = multicastPort;
        this.multicastObjectChannel = new ObjectChannel<>(new MulticastChannel(ip, multicastPort, multicastGroupIp));
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
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                if(!waitMasterRequest()) continue;
                if(!waitTaskDispatch()) continue;
                executeTask();
            }
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
            multicastObjectChannel.getChannel().leaveGroup();
            multicastObjectChannel.getChannel().close();
            if(clientObjectChannel != null) {
                clientObjectChannel.getChannel().close();
            }
        }
    }

    private boolean waitMasterRequest() throws IOException, ClassNotFoundException, InterruptedException {
        ObjectReadResult result = null;
        Packet packet = null;
        multicastObjectChannel.getChannel().joinGroup();
        logger.info("slave downloader start to wait for the master hire request");
        while(isRunning && !Thread.currentThread().isInterrupted()) {
            result = multicastObjectChannel.readObject(1000);
            if(result == null) continue;
            packet = (Packet) result.object;
            if (packet.type != Packet.CONNECT_MASTER) continue;
            break;
        }
        multicastObjectChannel.getChannel().leaveGroup();
        if(!isRunning || Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        logger.log(Level.INFO, "accepted the request from master downloader {0}", result.sourceAddress.toString());
        InetAddress masterIp = ((InetSocketAddress) result.sourceAddress).getAddress();
        int masterPort = (Integer) packet.payload;
        clientObjectChannel = new ObjectChannel<>(new ClientChannel(masterIp, masterPort));
        try {
            clientObjectChannel.getChannel().connect(2000);
            logger.log(Level.INFO, "established the data transfer connection to the master downloader {0}", result.sourceAddress.toString());
        } catch (IOException e) {//may be the master downloader has enough slave connected, thus, other connection request ignored.
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean waitTaskDispatch() throws ClassNotFoundException, InterruptedException {
        ObjectReadResult result;
        Packet packet = null;
        try {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                result = clientObjectChannel.readObject(1000);
                if (result == null) continue;
                packet = (Packet) result.object;
                if (packet.type != Packet.DOWNLOAD_PARAMS) {
                    throw new IOException("wrong type for the first packet received from server");
                }
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if(!isRunning || Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        String[] params = packet.payload.toString().split(" ");
        url = params[0];
        String[] downloadRange = params[1].split("/");
        startDownloadPosition = Integer.valueOf(downloadRange[0]);
        endDownloadPosition = Integer.valueOf(downloadRange[1]);
        logger.log(Level.INFO, "received the download task: {0} [{1}~{2}]", new Object[]{url, startDownloadPosition, endDownloadPosition});
        return true;
    }

    private void executeTask() throws InterruptedException {
        boolean errorOccurred = false;
        HttpURLConnection httpURLConnection = null;
        try {
            logger.info("start to execute the download task");
            URL downloadUrl = new URL(url);
            Proxy proxy = Utilities.getHttpProxy();
            if(proxy == null) {
                httpURLConnection = (HttpURLConnection) downloadUrl.openConnection();
            } else {
                httpURLConnection = (HttpURLConnection) downloadUrl.openConnection(proxy);
            }
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.setConnectTimeout(10000);
            httpURLConnection.setRequestProperty("Range", String.format("bytes=%d-%d", startDownloadPosition, endDownloadPosition));
            if (httpURLConnection.getResponseCode() == HttpsURLConnection.HTTP_PARTIAL) {
                InputStream inputStream = httpURLConnection.getInputStream();
                byte[] downloadBuffer = new byte[20480];
                int readLength, totalReadLength = 0;
                while ((readLength = inputStream.read(downloadBuffer)) != -1) {
                    byte[] downloadedData = new byte[readLength];
                    System.arraycopy(downloadBuffer, 0, downloadedData, 0, downloadedData.length);
                    clientObjectChannel.writeObject(new Packet(Packet.DOWNLOADED_DATA, downloadedData));
                    totalReadLength += readLength;
                    logger.log(Level.INFO, "data downloaded: {0}", totalReadLength);
                    if(!isRunning || Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                }
            } else {
                errorOccurred = true;
            }
        } catch (IOException e) {
            errorOccurred = true;
            e.printStackTrace();
        } catch (InterruptedException e) {
            errorOccurred = true;
            throw e;
        }  finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            try {
                clientObjectChannel.writeObject(new Packet(errorOccurred ? Packet.DOWNLOAD_ERROR : Packet.DOWNLOAD_COMPLETED));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clientObjectChannel.getChannel().close();
            }
            logger.log(Level.INFO, "download task finished with {0}", errorOccurred ? "error" : "successful");
        }
    }

    public static void main(String[] args) throws IOException {
        Utilities.trustAllCertificates();
        SlaveDownloader slave = new SlaveDownloader("10.189.132.32", "230.0.0.1", 8000);
        slave.start();
    }
}
