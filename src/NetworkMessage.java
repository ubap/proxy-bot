import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NetworkMessage {

    private static final int HEADER_LENGTH = 6;

    private final ByteBuffer header;
    private final ByteBuffer data;
    private int fill = 0;



    /**
     * Header consists of 6 bytes. Therefore we start reading payload from index 6.
     *
     * @param bytes
     */
    public NetworkMessage(ByteBuffer header, byte[] bytes) {
        this.header = header;
        data = ByteBuffer.wrap(bytes, HEADER_LENGTH, payloadBlocks() * 8).order(ByteOrder.LITTLE_ENDIAN);
    }

    public ByteBuffer header() {
        return header;
    }


    public void setFillBytes(int fillBytes) {
        if (fillBytes >= 8) {
            throw new RuntimeException(" fill bytes must be < 8");
        }
        data.limit(data.limit() - fillBytes);
        fill = fillBytes; // this can be somehow extracted from data buffer,
    }

    public void setLimit(int newLimit) {
        data.limit(newLimit);
    }

    public int payloadBlocks() {
        return header.getShort(0);
    }

    public byte getByte() {
       return data.get();
    }

    public short getShort() {
        return data.getShort();
    }

    public int getInt32() {
        return data.getInt();
    }

    public long getInt64() {
        return data.getLong();
    }

    public String getString() {
        int  strlen = getShort();
        byte[] stringBackingArray = new byte[strlen];
        data.get(stringBackingArray);
        return new String(stringBackingArray);
    }

    public boolean getBool() {
        return data.get() == 0x01;
    }

    public void skipBytes(int n) {
        data.position(data.position() + n);
    }

    public ByteBuffer access128Bytes() {
        return ByteBuffer.wrap(data.array(), data.position(), 128);
    }

    public ByteBuffer dataBuffer() {
        return ByteBuffer.wrap(data.array(), HEADER_LENGTH, data.limit()).order(ByteOrder.LITTLE_ENDIAN);
    }
}
