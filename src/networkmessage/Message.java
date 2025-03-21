package networkmessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Message {
    public static final int HEADER_LENGTH = 6; // 2 bytes - length, 4 bytes - sequence

    private final ByteBuffer buffer;

    private Message() {
        buffer = ByteBuffer.allocate(1024 * 1024).order(ByteOrder.LITTLE_ENDIAN);
    }

    public static Message readFromStream(InputStream inputStream) throws IOException {
        Message message = new Message();
        inputStream.readNBytes(message.getBackingArray(), 0, Message.HEADER_LENGTH);
        inputStream.readNBytes(message.getBackingArray(), Message.HEADER_LENGTH, message.dataLength());

        message.buffer.position(HEADER_LENGTH);
        message.buffer.limit(message.dataLength());
        return message;
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
