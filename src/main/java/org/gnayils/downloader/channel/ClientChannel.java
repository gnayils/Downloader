package org.gnayils.downloader.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ClientChannel extends BaseChannel<SocketChannel> {

    public ClientChannel(String ip, int port) throws IOException {
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

    public ReadResult read(ByteBuffer byteBuffer) throws IOException {
        return read(byteBuffer, 0);
    }

    public ReadResult read(ByteBuffer byteBuffer, int timeout) throws IOException {
        SelectionKey key = selectKey(SelectionKey.OP_READ, timeout);
        try {
            return key == null ? null : new ReadResult(channel.getClass().cast(key.channel()).read(byteBuffer), channel.getRemoteAddress());
        } catch (IOException e) {
            if(key != null) {
                key.cancel();
            }
            throw e;
        }
    }

    public int write(ByteBuffer byteBuffer) throws IOException {
        return channel.write(byteBuffer);
    }

    public static void main(String[] args) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);

        ClientChannel client = new ClientChannel("192.168.0.106", 13232);
        ServerChannel server = new ServerChannel("192.168.0.106", 13232);

        server.bind();
        client.connect();

        PeerChannel peer = server.accept();
        client.write(ByteBuffer.wrap("hello, server".getBytes()));
        byteBuffer.clear();
        ReadResult readResult = peer.read(byteBuffer);
        System.out.println(readResult.numberOfBytes + ", " + readResult.sourceAddress);
        byteBuffer.flip();
        System.out.println(new String(byteBuffer.array(), 0, byteBuffer.remaining()));

        peer.write(ByteBuffer.wrap("hello, client".getBytes()));
        byteBuffer.clear();
        client.read(byteBuffer);
        byteBuffer.flip();
        System.out.printf(new String(byteBuffer.array(), 0, byteBuffer.remaining()));

        peer.close();
        client.close();
        server.close();
    }
}
