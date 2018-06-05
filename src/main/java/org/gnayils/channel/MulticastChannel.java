package org.gnayils.channel;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;

public class MulticastChannel extends BaseChannel<DatagramChannel> {

    private NetworkInterface networkInterface;
    private InetAddress groupIp;
    private SocketAddress multicastAddress;
    private MembershipKey membershipKey;

    public MulticastChannel(String ip, int port, String groupIp) throws IOException {
        super(ip, port);
        this.groupIp = InetAddress.getByName(groupIp);
        multicastAddress = new InetSocketAddress(groupIp, port);
        networkInterface = NetworkInterface.getByInetAddress(this.ip);
        channel = DatagramChannel.open(StandardProtocolFamily.INET);
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface)
                .bind(new InetSocketAddress("0.0.0.0", port));
        channel.register(selector, SelectionKey.OP_READ);
    }

    public void joinGroup() throws IOException {
        if(membershipKey == null) {
            membershipKey = channel.join(groupIp, networkInterface);
        }
    }

    public void leaveGroup() {
        if(membershipKey != null) {
            membershipKey.drop();
            membershipKey = null;
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
            int bufferPosition = byteBuffer.position();
            SocketAddress sourceAddress = channel.getClass().cast(key.channel()).receive(byteBuffer);
            int readNumberOfBytes = byteBuffer.position() - bufferPosition;
            return new ByteBufferReadResult(readNumberOfBytes, sourceAddress);
        } catch (IOException e) {
            key.cancel();
            throw e;
        }
    }

    public int write(ByteBuffer byteBuffer) throws IOException {
        return channel.send(byteBuffer, multicastAddress);
    }
}
