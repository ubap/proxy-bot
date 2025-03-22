package jtrzebiatowski.networkmessage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * XTEA decoded
 * zlib inflated
 */
public class PacketsFromServer {

    private final jtrzebiatowski.networkmessage.Message message;
    private final ByteBuffer packetsData;
    private final boolean wasZlibCompressed;

    public PacketsFromServer(jtrzebiatowski.networkmessage.Message message, ByteBuffer packetsData, boolean wasZlibCompressed) {
        this.message = message;
        this.packetsData = packetsData;
        this.wasZlibCompressed = wasZlibCompressed;
    }

    /**
     * The position is set to the first byte after fill byte.
     *
     * @return
     */
    public ByteBuffer getPacketsData() {
        return packetsData.duplicate().order(ByteOrder.LITTLE_ENDIAN);
    }

    public jtrzebiatowski.networkmessage.Message getMessage() {
        return message;
    }
}
