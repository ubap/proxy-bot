package jtrzebiatowski.networkmessage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static jtrzebiatowski.networkmessage.Message.HEADER_LENGTH;

public class PacketToServer {
    ByteBuffer buffer;

    public PacketToServer() {
        this.buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(HEADER_LENGTH + 1); // 1 for fill byte
    }

    public void addByte(byte b) {
        buffer.put(b);
    }

    public void addText(String text) {
        short len = (short) text.length();
        buffer.putShort(len);
        buffer.put(text.getBytes(StandardCharsets.UTF_8), 0, len);
    }

    private void setFillBytes() {
        int positionWithinDataSegment = buffer.position() - HEADER_LENGTH;
        byte fillBytes = (byte) (positionWithinDataSegment % 8);
        buffer.putShort(6, fillBytes);
    }

    public ByteBuffer dataBuffer() {
        return buffer.duplicate().position(HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
    }
}
