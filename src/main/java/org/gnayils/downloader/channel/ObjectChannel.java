package org.gnayils.downloader.channel;

import java.io.*;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.gnayils.downloader.channel.BaseChannel.ByteBufferReadResult;

public class ObjectChannel<T extends BaseChannel> {

    private T channel;

    public ObjectChannel(T channel) {
        this.channel = channel;
    }

    public T getChannel() {
        return channel;
    }

    public ObjectReadResult readObject() throws IOException, ClassNotFoundException {
        ByteBufferReadResult lastByteBufferReadResult = null;
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        while (lengthBuffer.hasRemaining()) {
            ByteBufferReadResult byteBufferReadResult = channel.read(lengthBuffer);
            if(byteBufferReadResult != null) {
                if (lastByteBufferReadResult == null || byteBufferReadResult.sourceAddress.equals(lastByteBufferReadResult.sourceAddress)) {
                    lastByteBufferReadResult = byteBufferReadResult;
                    continue;
                } else {
                    System.err.println("the received data from different source address");
                    return null;
                }
            }
        }
        lengthBuffer.flip();
        ByteBuffer objectBuffer = ByteBuffer.allocate(lengthBuffer.getInt());
        while (objectBuffer.hasRemaining()) {
            ByteBufferReadResult byteBufferReadResult = channel.read(objectBuffer);
            if(byteBufferReadResult != null) {
                if (byteBufferReadResult.sourceAddress.equals(lastByteBufferReadResult.sourceAddress)) {
                    continue;
                } else {
                    System.err.println("the received data from different source address");
                    return null;
                }
            }
        }
        return new ObjectReadResult(parseObject(objectBuffer), lastByteBufferReadResult.sourceAddress);
    }

    public void writeObject(Object object) throws IOException {
        ByteBuffer objectBuffer = toByteBuffer(object);
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        lengthBuffer.putInt(objectBuffer.capacity());
        lengthBuffer.flip();
        while (lengthBuffer.hasRemaining()) {
            channel.write(lengthBuffer);
        }
        while (objectBuffer.hasRemaining()) {
            channel.write(objectBuffer);
        }
    }

    private Object parseObject(ByteBuffer byteBuffer) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteBuffer.array()));
        Object object = objectInputStream.readObject();
        objectInputStream.close();
        return object;
    }

    private ByteBuffer toByteBuffer(Object object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
        objectOutputStream.close();
        return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
    }

    public static class ObjectReadResult {

        SocketAddress sourceAddress;
        Object object;

        public ObjectReadResult(Object object, SocketAddress sourceAddress) {
            this.object = object;
            this.sourceAddress = sourceAddress;
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        ClientChannel client = new ClientChannel("192.168.0.106", 13232);
        ServerChannel server = new ServerChannel("192.168.0.106", 13232);

        server.bind();
        client.connect();

        ObjectChannel<PeerChannel> peerObjectChannel = new ObjectChannel(server.accept());


        ObjectChannel<ClientChannel> clientObjectChannel = new ObjectChannel(client);
        clientObjectChannel.writeObject("client string object");

        String string = (String) peerObjectChannel.readObject().object;
        System.out.println(string);

        peerObjectChannel.writeObject("server string object");
        string = (String) clientObjectChannel.readObject().object;
        System.out.println(string);

        peerObjectChannel.getChannel().close();
        clientObjectChannel.getChannel().close();
        server.close();



        MulticastChannel multicasterA = new MulticastChannel("192.168.0.106", 13232,"230.0.0.1");
        MulticastChannel multicasterB = new MulticastChannel("192.168.0.106", 13232,"230.0.0.1");

        ObjectChannel<MulticastChannel> multicastObjectChannelA = new ObjectChannel(multicasterA);
        ObjectChannel<MulticastChannel> multicastObjectChannelB = new ObjectChannel(multicasterB);

        multicastObjectChannelA.getChannel().joinGroup();
        multicastObjectChannelB.getChannel().joinGroup();

        multicastObjectChannelA.writeObject("multicastObjectChannelA");
        multicastObjectChannelB.writeObject("multicastObjectChannelB");

        System.out.println(multicastObjectChannelA.readObject().object);
        System.out.println(multicastObjectChannelB.readObject().object);
        System.out.println(multicastObjectChannelA.readObject().object);
        System.out.println(multicastObjectChannelB.readObject().object);

        multicasterA.close();
        multicasterB.close();

    }
}
