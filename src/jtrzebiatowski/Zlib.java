package jtrzebiatowski;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Zlib {
    public static ByteBuffer inflate(ByteBuffer data) {
        Inflater inflater = new Inflater(true);

        int lengthBefore = data.remaining();

        inflater.setInput(data);

        byte[] output = new byte[1024 * 1024]; // Adjust size if needed
        int lengthAfter = 0;
        try {
            lengthAfter = inflater.inflate(output);
        } catch (DataFormatException e) {
            e.printStackTrace();
        }

        System.out.println("jtrzebiatowski.Zlib inflate, lengthBefore=%d, lengthAfter=%d".formatted(lengthBefore, lengthAfter));

        inflater.end();
        return ByteBuffer.wrap(output, 0, lengthAfter).order(ByteOrder.LITTLE_ENDIAN);
    }
}
