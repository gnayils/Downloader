package org.gnayils.downloads.channels;
import org.gnayils.channel.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class ChannelTest {

    static final String ip = "127.0.0.1";
    static final int port = 23333;
    static final String groupIp = "230.0.0.1";

    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

    @Test
    public void testMulticastChannel() throws IOException {
        MulticastChannel multicastChannelA = new MulticastChannel(ip, port, groupIp);
        MulticastChannel multicastChannelB = new MulticastChannel(ip, port, groupIp);

        multicastChannelA.joinGroup();
        multicastChannelB.joinGroup();

        String messageA = "multicast message from A";
        multicastChannelA.write(ByteBuffer.wrap(messageA.getBytes()));
        byteBuffer.clear();
        BaseChannel.ByteBufferReadResult result = multicastChannelB.read(byteBuffer);
        assertEquals(messageA, new String(byteBuffer.array(), 0, result.numberOfBytes));
        byteBuffer.clear();
        result = multicastChannelA.read(byteBuffer);
        assertEquals(messageA, new String(byteBuffer.array(), 0, result.numberOfBytes));

        String messageB = "multicast message from B";
        multicastChannelB.write(ByteBuffer.wrap(messageB.getBytes()));
        byteBuffer.clear();
        result = multicastChannelA.read(byteBuffer);
        assertEquals(messageB, new String(byteBuffer.array(), 0, result.numberOfBytes));
        byteBuffer.clear();
        result = multicastChannelB.read(byteBuffer);
        assertEquals(messageB, new String(byteBuffer.array(), 0, result.numberOfBytes));

        multicastChannelB.leaveGroup();

        multicastChannelA.write(ByteBuffer.wrap(messageA.getBytes()));
        byteBuffer.clear();
        result = multicastChannelB.read(byteBuffer, 100);
        assertNull(result);
        assertEquals(byteBuffer.remaining(), byteBuffer.capacity());

        multicastChannelB.close();
        multicastChannelA.leaveGroup();
        multicastChannelA.close();
    }

    @Test
    public void testSocketChannel() throws IOException {
        ClientChannel client = new ClientChannel(ip, port);
        ServerChannel server = new ServerChannel(ip, port);

        server.bind();
        client.connect();

        PeerChannel peer = server.accept();
        String clientMessage = "message from client";
        client.write(ByteBuffer.wrap(clientMessage.getBytes()));
        byteBuffer.clear();
        BaseChannel.ByteBufferReadResult result = peer.read(byteBuffer);
        byteBuffer.flip();
        assertEquals(clientMessage, new String(byteBuffer.array(), 0, result.numberOfBytes));

        String serverMessage = "message from server";
        peer.write(ByteBuffer.wrap(serverMessage.getBytes()));
        byteBuffer.clear();
        result = client.read(byteBuffer);
        byteBuffer.flip();
        assertEquals(serverMessage, new String(byteBuffer.array(), 0, result.numberOfBytes));

        peer.close();
        client.close();
        server.close();
    }

    @Test
    public void testObjectChannelWithSocketChannel() throws IOException, ClassNotFoundException {
        ClientChannel client = new ClientChannel(ip, port);
        ServerChannel server = new ServerChannel(ip, port);

        server.bind();
        client.connect();

        ObjectChannel<ClientChannel> clientObjectChannel = new ObjectChannel<>(client);
        ObjectChannel<PeerChannel> peerObjectChannel = new ObjectChannel<>(server.accept());

        Object clientObject = "object from client";
        clientObjectChannel.writeObject(clientObject);

        assertEquals(clientObject, peerObjectChannel.readObject().object);

        Object serverObject = "object from server";
        peerObjectChannel.writeObject(serverObject);
        assertEquals(serverObject, clientObjectChannel.readObject().object);

        peerObjectChannel.getChannel().close();
        clientObjectChannel.getChannel().close();
        server.close();
    }

    @Test
    public void testObjectChannelWithMulticastChannel() throws IOException, ClassNotFoundException {
        MulticastChannel multicasterA = new MulticastChannel(ip, port,groupIp);
        MulticastChannel multicasterB = new MulticastChannel(ip, port,groupIp);

        ObjectChannel<MulticastChannel> multicastObjectChannelA = new ObjectChannel<>(multicasterA);
        ObjectChannel<MulticastChannel> multicastObjectChannelB = new ObjectChannel<>(multicasterB);

        multicastObjectChannelA.getChannel().joinGroup();
        multicastObjectChannelB.getChannel().joinGroup();

        Object objectA = "object A";
        multicastObjectChannelA.writeObject(objectA);
        assertEquals(objectA, multicastObjectChannelB.readObject().object);
        assertEquals(objectA, multicastObjectChannelA.readObject().object);

        Object objectB = "object B";
        multicastObjectChannelB.writeObject(objectB);
        assertEquals(objectB, multicastObjectChannelA.readObject().object);
        assertEquals(objectB, multicastObjectChannelB.readObject().object);

        multicasterA.leaveGroup();
        multicasterB.leaveGroup();
        multicasterA.close();
        multicasterB.close();
    }
}
