import encryption.RSADecode;
import encryption.XTEA;
import networkmessage.Message;
import networkmessage.PacketsFromServer;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class MessageInterpreter {
    ArrayBlockingQueue<byte[]> queueToServer;

    int[] expandedKey;

    MessageInterpreter(ArrayBlockingQueue<byte[]> queueToServer) {
        this.queueToServer = queueToServer;
    }

    public void initialFromClient(Message message) {
        message.getShort(); // idk what it is, maybe encoded length; maybe - fill bytes?
        int systemVersion = message.getShort();
        int clientVersionShort = message.getShort();
        message.skipBytes(4); // client version 32 bit
        String clientVersion = message.getString();
        String datVersion = message.getString();
        boolean previewState = message.getBool();

        RSADecode.decode(message.next128Bytes());

        if (message.getByte() != 0) {
            throw new IllegalStateException("RSA decryption failed. Shutting down.");
        }
        int[] xteaKey = new int[4]; // 128-bit key as an array of four 32-bit integers
        xteaKey[0] = message.getInt32();
        xteaKey[1] = message.getInt32();
        xteaKey[2] = message.getInt32();
        xteaKey[3] = message.getInt32();
        expandedKey = XTEA.expandKey(xteaKey);

        int gamemaster = message.getByte(); //gamemaster flag

        String sessionTokenBase64 = message.getString();
        String charName = message.getString();
        message.skipBytes(4); // timestamp
        message.skipBytes(1); // randNumber


        System.out.println("initial packet from client parsed. CharName = " + charName);
    }

    public void fromClient(NetworkMessage networkMessage) {
        XTEA.decrypt(networkMessage.dataBuffer(), expandedKey);
        // System.out.println(byteBufferToHex(networkMessage.accessPayload()));

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
                if (text.equals("hej")) {
                    XTEA.encrypt(networkMessage.dataBuffer(), expandedKey);
                    byte[] bytes = Arrays.copyOfRange(networkMessage.dataBuffer().array(), 0, networkMessage.payloadBlocks() * 8 + 6);
                    queueToServer.add(bytes);
                }
            }
        }
    }

    public PacketsFromServer fromServer(Message networkMessage) {
        if (expandedKey == null) {
            System.out.println("XTEA key not set yet.");
            return null;
        }
        XTEA.decrypt(networkMessage.dataBuffer(), expandedKey);

        if (networkMessage.isZlibCompressed()) {
            System.out.println("zlib compressed");
            Inflater inflater = new Inflater(true);

            ByteBuffer byteBuffer = networkMessage.dataBuffer();
            int fillBytes = byteBuffer.get(); // skip fill
            // big question ?! - do we want to decrompress fill bytes? I asumme not.
            byteBuffer.limit( byteBuffer.limit() - fillBytes);

            int lengthBefore = byteBuffer.remaining();

            inflater.setInput(byteBuffer);

            byte[] output = new byte[1024 * 1024]; // Adjust size if needed
            int lengthAfter = 0;
            try {
                lengthAfter = inflater.inflate(output);
            } catch (DataFormatException e) {
                e.printStackTrace();
            }

            inflater.end();
            return new PacketsFromServer(networkMessage, ByteBuffer.wrap(output, 0, lengthAfter).order(ByteOrder.LITTLE_ENDIAN), true);
        }

        ByteBuffer packetsBuffer = networkMessage.dataBuffer();
        int fillBytes = packetsBuffer.get(); // skip fill bytes
        packetsBuffer.limit( packetsBuffer.limit() - fillBytes);

        return new PacketsFromServer(networkMessage, packetsBuffer, false);
    }


    // ignore this path for now

//        byte opCode = networkMessage.getByte();
//        switch (opCode) {
//            case 0x1E:// ping
//            case 0x1D: // ping
//                return;
//
//            case (byte) 0xEE: // resource balance
//                byte resourceType = networkMessage.getByte();
//                long amount = networkMessage.getInt64();
//
//                System.out.printf("ResourceType=%d, amount=%d\n", resourceType, amount);
//                break;
//            case (byte) 0xAA: // channel message
//                networkMessage.getInt32(); // something
//                String author = networkMessage.getString(); // author
//                networkMessage.skipBytes(9); // no idea
//                String text = networkMessage.getString(); // text
//
//                System.out.printf("channel message author=%s, text=%s\n", author, text);
//
//        }
    //System.out.println(byteBufferToHex(networkMessage.accessPayload()));

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
