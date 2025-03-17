import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NetworkMessage {

    private static final int HEADER_LENGTH = 6;

    private final ByteBuffer header;
    private final ByteBuffer payload;


    /**
     * Header consists of 6 bytes. Therefore we start reading payload from index 6.
     *
     * @param bytes
     */
    public NetworkMessage(ByteBuffer header, byte[] bytes) {
        this.header = header;
        payload = ByteBuffer.wrap(bytes, HEADER_LENGTH, payloadBlocks() * 8).order(ByteOrder.LITTLE_ENDIAN);
        //payload.position();
    }

    public ByteBuffer header() {
        return header;
    }

    public int payloadBlocks() {
        return header.getShort(0);
    }

    public byte getByte() {
       return payload.get();
    }

    public short getShort() {
        return payload.getShort();
    }

    public int getInt32() {
        return payload.getInt();
    }

    public String getString() {
        int  strlen = getShort();
        byte[] stringBackingArray = new byte[strlen];
        payload.get(stringBackingArray);
        return new String(stringBackingArray);
    }

    public boolean getBool() {
        return payload.get() == 0x01;
    }

    public void skipBytes(int n) {
        payload.position(payload.position() + n);
    }

    public ByteBuffer access128Bytes() {
        return ByteBuffer.wrap(payload.array(), payload.position(), 128);
    }

    public ByteBuffer accessPayload() {
        return ByteBuffer.wrap(payload.array(), HEADER_LENGTH, payloadBlocks() * 8).order(ByteOrder.LITTLE_ENDIAN);
    }
}
