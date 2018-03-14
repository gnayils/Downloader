package org.gnayils.downloader.channel;

import java.io.*;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.gnayils.downloader.channel.BaseChannel.ByteBufferReadResult;

public class ObjectChannel<T extends BaseChannel> {

    private T channel;
    private ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
    private ByteBuffer objectBuffer;
    private ObjectReadResult objectReadResult;

    public ObjectChannel(T channel) {
        this.channel = channel;
    }

    public T getChannel() {
        return channel;
    }

    public ObjectReadResult readObject() throws IOException, ClassNotFoundException {
        return readObject(0);
    }

    public ObjectReadResult readObject(int timeout) throws IOException, ClassNotFoundException {
        while (lengthBuffer.hasRemaining()) {
            channel.read(lengthBuffer, timeout);
            if(timeout == 0) {
                continue;
            } else if(lengthBuffer.hasRemaining()) {
                return null;
            }
        }
        if(objectBuffer == null) {
            lengthBuffer.flip();
            objectBuffer = ByteBuffer.allocate(lengthBuffer.getInt());
            objectReadResult = new ObjectReadResult();
        }
        while (objectBuffer.hasRemaining()) {
            ByteBufferReadResult byteBufferReadResult = channel.read(objectBuffer, timeout);
            if(byteBufferReadResult != null) {
                objectReadResult.sourceAddress = byteBufferReadResult.sourceAddress;
            }
            if (timeout == 0) {
                continue;
            } else if(objectBuffer.hasRemaining()){
                return null;
            }
        }
        objectReadResult.object = parseObject(objectBuffer);
        objectBuffer = null;
        lengthBuffer.clear();
        return objectReadResult;
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

        public ObjectReadResult() {

        }

        public ObjectReadResult(Object object, SocketAddress sourceAddress) {
            this.object = object;
            this.sourceAddress = sourceAddress;
        }
    }
}
