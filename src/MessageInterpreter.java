import encryption.RSADecode;
import encryption.XTEA;
import networkmessage.Game;
import networkmessage.Message;
import networkmessage.PacketsFromServer;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class MessageInterpreter {
    ArrayBlockingQueue<Message> queueToServer;

    private final Game game;

    MessageInterpreter(ArrayBlockingQueue<Message> queueToServer, Game game) {
        this.queueToServer = queueToServer;
        this.game = game;
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
        int[] expandedKey = XTEA.expandKey(xteaKey);
        game.setXteaKey(expandedKey);

        int gamemaster = message.getByte(); //gamemaster flag

        String sessionTokenBase64 = message.getString();
        String charName = message.getString();
        message.skipBytes(4); // timestamp
        message.skipBytes(1); // randNumber


        System.out.println("initial packet from client parsed. CharName = " + charName);
    }

    public void fromClient(Message networkMessage) {
        XTEA.decrypt(networkMessage.dataBuffer(), game.getXteaKey());
        byte fill = networkMessage.getByte();

        byte opCode = networkMessage.getByte();
        if (opCode == (byte) 0x96) { // say
            short speakClass = networkMessage.getByte(); // speak class
            if (speakClass == 1 /*say*/) {

                String text = networkMessage.getString();
                System.out.println(text);
                System.out.println(byteBufferToHex(networkMessage.dataBuffer().position(0)));
                if (text.equals("hej")) {
                    XTEA.encrypt(networkMessage.dataBuffer(), game.getXteaKey());
                    queueToServer.add(networkMessage);
                }
            }
        }
    }

    public PacketsFromServer fromServer(Message networkMessage, boolean xtea) {
        if (xtea) {
            XTEA.decrypt(networkMessage.dataBuffer(), game.getXteaKey());
        }
        byte fillBytes = networkMessage.getByte();

        if (networkMessage.isZlibCompressed()) {
            ByteBuffer byteBuffer = networkMessage.dataBuffer();
            byteBuffer.get(); // skip fill
            // big question ?! - do we want to decompress fill bytes? I assume not.
            int newLimit = byteBuffer.limit() - fillBytes;
            // System.out.println("old limit: %d, new limit: %d".formatted(byteBuffer.limit(), newLimit));
            byteBuffer.limit( newLimit); // limit the length by fill bytes
            
            ByteBuffer inflatedData = Zlib.inflate(byteBuffer);

            return new PacketsFromServer(networkMessage, inflatedData, true);
        }

        ByteBuffer packetsBuffer = networkMessage.dataBuffer();
        packetsBuffer.get(); // skip fill bytes
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
            String separator = "";
            while (true) {
                sb.append(separator);
                sb.append(String.format("0x%02X ", bytes.get())); // %02X ensures 2-digit uppercase hex
                separator = ", ";
            }
        } catch (BufferUnderflowException e) {

        }
        return sb.toString().trim(); // Remove last space
    }


}
