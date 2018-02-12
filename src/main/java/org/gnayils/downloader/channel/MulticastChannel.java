package org.gnayils.downloader.channel;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.util.Timer;
import java.util.TimerTask;

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
            if(key != null) {
                int bufferPosition = byteBuffer.position();
                SocketAddress sourceAddress = channel.getClass().cast(key.channel()).receive(byteBuffer);
                int readNumberOfBytes = byteBuffer.position() - bufferPosition;
                return new ByteBufferReadResult(readNumberOfBytes, sourceAddress);
            }
        } catch (IOException e) {
            if(key != null) {
                key.cancel();
            }
            throw e;
        }
        return null;
    }

    public int write(ByteBuffer byteBuffer) throws IOException {
        return channel.send(byteBuffer, multicastAddress);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Timer timer4send = new Timer();
        timer4send.schedule(new TimerTask() {

            MulticastChannel multicaster = new MulticastChannel("192.168.0.106", 13232,"230.0.0.1");

            @Override
            public void run() {
                try {
                    multicaster.write(ByteBuffer.wrap("hello, everyone!".getBytes()));
                    System.out.println("send multicast...");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 500);

        Timer timer4recv = new Timer();
        timer4recv.schedule(new TimerTask() {

            ByteBuffer byteBuffer = ByteBuffer.allocate(128);
            MulticastChannel  multicaster = new MulticastChannel("192.168.0.106", 13232, "230.0.0.1");
            int cycleCount = 0;
            boolean isJoinedGroup = false;
            boolean isLastJoinedGroup = false;

            {
                multicaster.joinGroup();
                isJoinedGroup = true;
                isLastJoinedGroup = true;
            }

            @Override
            public void run() {
                try {
                    if(!isLastJoinedGroup && isJoinedGroup) {
                        System.out.println("---------------------------------------join group");
                        multicaster.joinGroup();
                        isLastJoinedGroup = isJoinedGroup;
                    } else if(isLastJoinedGroup && !isJoinedGroup){
                        System.out.println("---------------------------------------leave group");
                        multicaster.leaveGroup();
                        isLastJoinedGroup = isJoinedGroup;
                    }
                    byteBuffer.clear();
                    ByteBufferReadResult byteBufferReadResult = multicaster.read(byteBuffer, 500);
                    byteBuffer.flip();
                    System.out.print("recv multicast: " + new String(byteBuffer.array(), 0, byteBuffer.remaining()));
                    if(byteBufferReadResult == null) {
                        System.out.println();
                    } else {
                        System.out.println(", " + byteBufferReadResult.numberOfBytes + ", " + byteBufferReadResult.sourceAddress);
                    }

                } catch (IOException e) {
                    if(e instanceof SocketTimeoutException) {
                        System.out.println("recv multicast time out");
                    } else {
                        e.printStackTrace();
                    }
                }
                cycleCount++;
                isJoinedGroup = cycleCount % 5 == 0 ? !isJoinedGroup : isJoinedGroup;
            }
        }, 0, 500);
    }
}
