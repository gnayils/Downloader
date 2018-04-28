package org.gnayils.downloader2.channel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

public abstract class BaseChannel<T extends SelectableChannel> implements Channel {

    protected InetAddress ip;
    protected int port;

    protected T channel;

    public BaseChannel(String ip, int port) throws IOException {
        this(InetAddress.getByName(ip), port);
    }

    public BaseChannel(InetAddress ip, int port) throws IOException {
        this.ip = ip;
        this.port = port;
    }

    public BaseChannel(T channel) throws IOException {
        this.channel = channel;
    }


    @Override
    public SelectableChannel getRealChannel() {
        return channel;
    }

    public void close() {
        try {
            if(channel != null) {
                channel.close();
                channel = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
