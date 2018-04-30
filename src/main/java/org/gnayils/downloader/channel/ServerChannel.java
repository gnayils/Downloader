package org.gnayils.downloader.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class ServerChannel extends BaseChannel<ServerSocketChannel> {

    public ServerChannel(String ip, int port) throws IOException {
        super(ip, port);
        channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
    }

    public void bind() throws IOException {
        channel.socket().bind(new InetSocketAddress(ip, port), 1024);
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public PeerChannel accept() throws IOException {
        return accept(0);
    }

    public PeerChannel accept(int timeout) throws IOException {
        SelectionKey key = selectKey(SelectionKey.OP_ACCEPT, timeout);
        try {
            return key == null ? null : new PeerChannel(channel.getClass().cast(key.channel()).accept());
        } catch (IOException e) {
            key.cancel();
            throw e;
        }
    }

    @Override
    public ByteBufferReadResult read(ByteBuffer byteBuffer) throws IOException {
        throw new UnsupportedOperationException("unsupported operation");
    }

    @Override
    public ByteBufferReadResult read(ByteBuffer bytebuffer, int timeout) throws IOException {
        throw new UnsupportedOperationException("unsupported operation");
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        throw new UnsupportedOperationException("unsupported operation");
    }
}
