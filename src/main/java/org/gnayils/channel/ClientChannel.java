package org.gnayils.channel;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
        connect(0);
    }

    public void connect(int timeout) throws IOException {
        channel.register(selector, SelectionKey.OP_CONNECT);
        channel.connect(new InetSocketAddress(ip, port));
        SelectionKey key = selectKey(SelectionKey.OP_CONNECT, timeout);
        if(key == null) {
            throw new IOException("connection to the host failed due to selected a null OP_CONNECT key");
        }
        if(channel.getClass().cast(key.channel()).finishConnect()) {
            key.channel().register(selector, SelectionKey.OP_READ);
        } else {
            throw new IOException("connection to the host failed");
        }
    }

    public ByteBufferReadResult read(ByteBuffer byteBuffer) throws IOException {
        return read(byteBuffer, 0);
    }

    public ByteBufferReadResult read(ByteBuffer byteBuffer, int timeout) throws IOException {
        SelectionKey key = selectKey(SelectionKey.OP_READ, timeout);
        try {
            if(key == null) {
                return null;
            }
            int numberOfBytes = channel.getClass().cast(key.channel()).read(byteBuffer);
            if(numberOfBytes == -1) {
                throw new EOFException("the channel has reached end-of-stream");
            } else {
                return new ByteBufferReadResult(numberOfBytes, channel.getRemoteAddress());
            }
        } catch (IOException e) {
            key.cancel();
            throw e;
        }
    }

    public int write(ByteBuffer byteBuffer) throws IOException {
        return channel.write(byteBuffer);
    }
}
