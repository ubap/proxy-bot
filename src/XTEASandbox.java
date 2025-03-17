import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class XTEASandbox {

    private static final int DELTA = 0x9E3779B9;
    private static final int ROUNDS = 32; // XTEA does 32 cycles, 64 rounds

    // Expand the key using the XTEA key expansion algorithm
    private static int[] expandKey(int[] k) {
        int[] expanded = new int[ROUNDS * 2];
        for (int i = 0, sum = 0; i < expanded.length; i += 2) {
            expanded[i] = sum + k[sum & 3];
            sum += DELTA;
            expanded[i + 1] = sum + k[(sum >>> 11) & 3];
        }
        return expanded;
    }

    // Function to print the expanded key
    private static void printExpandedKey(int[] expanded) {
        System.out.println("Expanded Key:");
        for (int i = 0; i < expanded.length; i++) {
            System.out.printf("%08X ", expanded[i]);
            if ((i + 1) % 8 == 0) {
                System.out.println();
            }
        }
        System.out.println();
    }

    public static void decrypt(byte[] data, int[] k) {
        for (int i = k.length; i > 0; i -= 2) {
            for (int offset = 0; offset < data.length; offset += 8) {
                ByteBuffer buffer = ByteBuffer.wrap(data, offset, 8);
                buffer.order(ByteOrder.LITTLE_ENDIAN); // Important: Specify byte order

                int left = buffer.getInt();
                int right = buffer.getInt();

                right -= ((left << 4 ^ left >>> 5) + left) ^ k[i - 1];
                left -= ((right << 4 ^ right >>> 5) + right) ^ k[i - 2];

                buffer.position(offset);
                buffer.putInt(left);
                buffer.putInt(right);
            }
        }
    }

    public static void encrypt(byte[] data, int[] k) {

        for (int i = 0; i < k.length; i += 2) {
            for (int offset = 0; offset < data.length; offset += 8) {

                ByteBuffer buffer = ByteBuffer.wrap(data, offset, 8);
                buffer.order(ByteOrder.LITTLE_ENDIAN); // Important: Specify byte order

                int left = buffer.getInt();
                int right = buffer.getInt();

                left += ((right << 4 ^ right >>> 5) + right) ^ k[i];
                right += ((left << 4 ^ left >>> 5) + left) ^ k[i + 1];

                buffer.position(offset); // Go back to the beginning of the buffer
                buffer.putInt(left);
                buffer.putInt(right);
            }
        }
    }



    // Function to print data in hexadecimal format
    private static void printData(byte[] data) {
        for (byte b : data) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }


    public static void main(String[] args) {
        byte[] keyBytes = {
                0x78, 0x56, 0x34, 0x12,
                (byte) 0xF0, (byte) 0xDE, (byte) 0xBC, (byte) 0x9A,
                0x12, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB,
                (byte) 0x90, 0x78, 0x56, 0x34
        };

        // Convert key bytes to int array (little-endian)
        int[] keyArray = new int[4];
        ByteBuffer keyBuffer = ByteBuffer.wrap(keyBytes);
        keyBuffer.order(ByteOrder.LITTLE_ENDIAN); // Use Little Endian
        for (int i = 0; i < 4; i++) {
            keyArray[i] = keyBuffer.getInt();
        }
        // Expand the key
        int[] expandedKey = expandKey(keyArray);

        // Print the expanded key
        printExpandedKey(expandedKey);

        // Define some data to encrypt (8 bytes)
        byte[] data = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
        System.out.println("Original Data:");
        printData(data);

        // Encrypt the data
        encrypt(data, expandedKey);
        System.out.println("Encrypted Data:");
        printData(data);

        // Decrypt the data back to original
        decrypt(data, expandedKey);
        System.out.println("Decrypted Data (should match original):");
        printData(data);

        // Test with a larger data set
        byte[] largeData = new byte[32]; // Example: 32 bytes
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i * 3);
        }
        System.out.println("\nOriginal Large Data:");
        printData(largeData);

        encrypt(largeData, expandedKey);
        System.out.println("Encrypted Large Data:");
        printData(largeData);

        decrypt(largeData, expandedKey);
        System.out.println("Decrypted Large Data:");
        printData(largeData);
    }
}