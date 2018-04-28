package org.gnayils.downloader2.channel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ClientChannel extends BaseChannel<SocketChannel> {

    public ClientChannel(String ip, int port) throws IOException {
        this(InetAddress.getByName(ip), port);
    }

    public ClientChannel(InetAddress ip, int port) throws IOException {
        super(ip, port);
        this.channel = SocketChannel.open();
        this.channel.configureBlocking(false);
    }

    public void connect() throws IOException {
        SelectorThread.getInstance().register(this, SelectionKey.OP_CONNECT);
        channel.connect(new InetSocketAddress(ip, port));

    }

    @Override
    public void onKeyAvailable(SelectionKey key) {
        if(!key.isValid()) return ;
        if(key.isConnectable()) {
            try {
                if(channel.getClass().cast(key.channel()).finishConnect()) {
                    SelectorThread.getInstance().register(this, SelectionKey.OP_READ);
                } else {
                    throw new IOException("connection to the host failed");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if(key.isReadable()) {

        }

    }

    public void write(ByteBuffer byteBuffer) throws IOException {
        while(byteBuffer.hasRemaining()) {
            channel.write(byteBuffer);
        }
    }
}
