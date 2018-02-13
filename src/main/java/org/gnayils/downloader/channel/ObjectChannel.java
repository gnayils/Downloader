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

        public SocketAddress sourceAddress;
        public Object object;

        public ObjectReadResult(Object object, SocketAddress sourceAddress) {
            this.object = object;
            this.sourceAddress = sourceAddress;
        }
    }
}
