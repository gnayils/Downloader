package org.gnayils.downloader.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class PeerChannel extends BaseChannel<SocketChannel> {

    public PeerChannel(SocketChannel channel) throws IOException {
        super(channel);
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
    }

    public ByteBufferReadResult read(ByteBuffer byteBuffer) throws IOException {
        return read(byteBuffer, 0);
    }

    public ByteBufferReadResult read(ByteBuffer byteBuffer, int timeout) throws IOException {
        SelectionKey key = selectKey(SelectionKey.OP_READ, timeout);
        try {
            return key == null ? null : new ByteBufferReadResult(channel.getClass().cast(key.channel()).read(byteBuffer), channel.getRemoteAddress());
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
}
