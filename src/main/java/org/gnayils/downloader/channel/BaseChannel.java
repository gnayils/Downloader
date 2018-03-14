package org.gnayils.downloader.channel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public abstract class BaseChannel<T extends SelectableChannel> {

    protected InetAddress ip;
    protected int port;

    protected Selector selector;
    protected T channel;

    public BaseChannel(String ip, int port) throws IOException {
        this(InetAddress.getByName(ip), port);
    }

    public BaseChannel(InetAddress ip, int port) throws IOException {
        this.ip = ip;
        this.port = port;
        this.selector = Selector.open();
    }

    public BaseChannel(T channel) throws IOException {
        this.channel = channel;
        this.selector = Selector.open();
    }

    public abstract ByteBufferReadResult read(ByteBuffer byteBuffer) throws IOException;

    public abstract ByteBufferReadResult read(ByteBuffer bytebuffer, int timeout) throws IOException;

    public abstract int write(ByteBuffer byteBuffer) throws IOException;

    public T channel() {
        return channel;
    }

    protected SelectionKey selectKey(int keyOption, int timeout) throws IOException {
        SelectionKey theKey = null;
        do {
            int selectedCount = selector.select(timeout);
            if(selectedCount == 0) {
                return null;
            }
            Set<SelectionKey> keySet = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keySet.iterator();
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if(!key.isValid()) {
                    continue;
                }
                if((key.readyOps() & keyOption) != 0) {
                    theKey = key;
                    break;
                }
            }
        } while(theKey == null);
        return theKey;
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
        try {
            if(selector != null) {
                selector.close();
                selector = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ByteBufferReadResult {

        public int numberOfBytes;
        public SocketAddress sourceAddress;

        public ByteBufferReadResult(int numberOfBytes, SocketAddress sourceAddress) {
            this.numberOfBytes = numberOfBytes;
            this.sourceAddress = sourceAddress;
        }
    }
}
