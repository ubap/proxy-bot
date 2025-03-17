import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PacketInterpreter {


    int[] expandedKey;

    public void initialFromClient(NetworkMessage networkMessage) {
        networkMessage.getShort(); // idk what it is, maybe encoded length;
        int systemVersion = networkMessage.getShort();
        int clientVersionShort = networkMessage.getShort();
        networkMessage.skipBytes(4); // client version 32 bit
        String clientVersion = networkMessage.getString();
        String datVersion = networkMessage.getString();
        boolean previewState = networkMessage.getBool();

        RSADecode.decode(networkMessage.access128Bytes());

        if (networkMessage.getByte() != 0) {
            throw new IllegalStateException("RSA decryption failed. Shutting down.");
        }
        int[] xteaKey = new int[4]; // 128-bit key as an array of four 32-bit integers
        xteaKey[0] = networkMessage.getInt32();
        xteaKey[1] = networkMessage.getInt32();
        xteaKey[2] = networkMessage.getInt32();
        xteaKey[3] = networkMessage.getInt32();
        expandedKey = XTEA.expandKey(xteaKey);

        int gamemaster = networkMessage.getByte(); //gamemaster flag

        String sessionTokenBase64 = networkMessage.getString();
        String charName = networkMessage.getString();
        networkMessage.skipBytes(4); // timestamp
        networkMessage.skipBytes(1); // randNumber


        System.out.println(charName);
    }

    public void fromClient(NetworkMessage networkMessage) {
        XTEA.decrypt(networkMessage.accessPayload(), expandedKey);
        System.out.println(byteBufferToHex(networkMessage.accessPayload()));

        ByteBuffer header = networkMessage.header();
        header.getShort();
        int checksum = header.getInt();

        byte fill = networkMessage.getByte();

        byte opCode = networkMessage.getByte();
        if (opCode == (byte) 0x96) { // say

            short speakClass = networkMessage.getByte(); // speak class
            if (speakClass == 1 /*say*/) {

                String text = networkMessage.getString();
                System.out.println(text);
            }
        }

    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b)); // %02X ensures 2-digit uppercase hex
        }
        return sb.toString().trim(); // Remove last space
    }

    public static String byteBufferToHex(ByteBuffer bytes) {
        StringBuilder sb = new StringBuilder();
        try {
            while (true) {
                sb.append(String.format("%02X ", bytes.get())); // %02X ensures 2-digit uppercase hex
            }
        } catch (BufferUnderflowException e) {

        }
        return sb.toString().trim(); // Remove last space
    }


}
