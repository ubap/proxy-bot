package jtrzebiatowski.networkmessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Message {
    public static final int HEADER_LENGTH = 6; // 2 bytes - length, 4 bytes - sequence

    private final ByteBuffer buffer;

    private Message() {
        buffer = ByteBuffer.allocate(1024 * 1024).order(ByteOrder.LITTLE_ENDIAN);
    }

    public Message(byte[] backingArray, int limit) {

        this.buffer = ByteBuffer.wrap(Arrays.copyOf(backingArray, limit), 0, limit).order(ByteOrder.LITTLE_ENDIAN);

        buffer.position(HEADER_LENGTH);
    }

    public static Message readFromStream(InputStream inputStream) throws IOException {
        Message message = new Message();
        inputStream.readNBytes(message.getBackingArray(), 0, Message.HEADER_LENGTH);
        inputStream.readNBytes(message.getBackingArray(), Message.HEADER_LENGTH, message.dataLength());

        message.buffer.position(HEADER_LENGTH);
        message.buffer.limit(message.messageLength());
        return message;
    }

    public Message duplicate() {
        return new Message(buffer.array(), messageLength());
    }

    public byte[] getBackingArray() {
        return buffer.array();
    }

    /**
     * @return Total message length, including header.
     */
    public int messageLength() {
        return dataLength() + HEADER_LENGTH;
    }

    /**
     * @return The length of data segment, excluding header.
     */
    public int dataLength() {
        return buffer.getShort(0) * 8;
    }

    public ByteBuffer dataBuffer() {
        return buffer.duplicate().position(HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
    }

    public ByteBuffer headerBuffer() {
        return buffer.duplicate().position(0).limit(HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
    }

    public boolean isZlibCompressed() {
        return (buffer.getInt(2) >>> 31) == 1;
    }

    public int getSequence() {
        return buffer.getInt(2) & (0xFFFFFFFF >>> 1);
    }

    public void setSequence(int seq) {
        buffer.putInt(2, seq);
    }

    // data accessors below

    public byte getByte() {
        return buffer.get();
    }

    public boolean getBool() {
        return buffer.get() == 0x01;
    }

    public short getShort() {
        return buffer.getShort();
    }

    public int getInt32() {
        return buffer.getInt();
    }

    public long getInt64() {
        return buffer.getLong();
    }

    public String getString() {
        int  strlen = getShort();
        byte[] stringBackingArray = new byte[strlen];
        buffer.get(stringBackingArray);
        return new String(stringBackingArray);
    }

    public void skipBytes(int n) {
        buffer.position(buffer.position() + n);
    }

    public ByteBuffer next128Bytes() {
        return ByteBuffer.wrap(buffer.array(), buffer.position(), 128);
    }

}
