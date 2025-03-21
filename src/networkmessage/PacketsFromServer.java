package networkmessage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * XTEA decoded
 * zlib inflated
 */
public class PacketsFromServer {

    private final Message message;
    private final ByteBuffer packetsData;
    private final boolean wasZlibCompressed;

    public PacketsFromServer(Message message, ByteBuffer packetsData, boolean wasZlibCompressed) {
        this.message = message;
        this.packetsData = packetsData;
        this.wasZlibCompressed = wasZlibCompressed;
    }

    public ByteBuffer getPacketsData() {
        return packetsData.duplicate().order(ByteOrder.LITTLE_ENDIAN);
    }

    public Message getMessage() {
        return message;
    }
}
