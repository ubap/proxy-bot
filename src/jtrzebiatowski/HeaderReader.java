package jtrzebiatowski;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HeaderReader {
    private final ByteBuffer buffer;

    public HeaderReader(byte[] readBuffer) {
        buffer = ByteBuffer.wrap(readBuffer).order(ByteOrder.LITTLE_ENDIAN);
    }

    public int getPayloadBlocks() {
        return buffer.getShort(0);
    }

    public int getSequence() {
        return buffer.getInt(2);
    }
}
