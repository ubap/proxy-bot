import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class XTEA {

    private static final int DELTA = 0x9E3779B9;
    private static final int ROUNDS = 32; // XTEA does 32 cycles, 64 rounds

    // Expand the key using the XTEA key expansion algorithm
    public static int[] expandKey(int[] k) {
        int[] expanded = new int[ROUNDS * 2];
        for (int i = 0, sum = 0; i < expanded.length; i += 2) {
            expanded[i] = sum + k[sum & 3];
            sum += DELTA;
            expanded[i + 1] = sum + k[(sum >>> 11) & 3];
        }
        return expanded;
    }

    public static void decrypt(ByteBuffer dataBuffer, int[] k) {
        int count = dataBuffer.remaining();
        int startPosition = dataBuffer.position();
        for (int i = k.length; i > 0; i -= 2) {
            for (int offset = 0; offset < count; offset += 8) {
                ByteBuffer buffer = ByteBuffer.wrap(dataBuffer.array(), startPosition + offset, 8);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                int left = buffer.getInt();
                int right = buffer.getInt();

                right -= ((left << 4 ^ left >>> 5) + left) ^ k[i - 1];
                left -= ((right << 4 ^ right >>> 5) + right) ^ k[i - 2];

                buffer.position(startPosition + offset);
                buffer.putInt(left);
                buffer.putInt(right);
            }
        }
        dataBuffer.position(startPosition);
    }

    public static void encrypt(ByteBuffer dataBuffer, int[] k) {
        int count = dataBuffer.remaining();
        int startPosition = dataBuffer.position();

        for (int i = 0; i < k.length; i += 2) {
            for (int offset = 0; offset < count; offset += 8) {

                ByteBuffer buffer = ByteBuffer.wrap(dataBuffer.array(), startPosition + offset, 8);
                buffer.order(ByteOrder.LITTLE_ENDIAN); // Important: Specify byte order

                int left = buffer.getInt();
                int right = buffer.getInt();

                left += ((right << 4 ^ right >>> 5) + right) ^ k[i];
                right += ((left << 4 ^ left >>> 5) + left) ^ k[i + 1];

                buffer.position(startPosition + offset);
                buffer.putInt(left);
                buffer.putInt(right);
            }
        }
    }

    // Helper method to print byte array in hex format
    public static void printByteArray(byte[] data) {
        for (byte b : data) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }
}
