import encryption.RSADecode;
import encryption.XTEA;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class PacketInterpreter {
    ArrayBlockingQueue<byte[]> queueToServer;

    int[] expandedKey;

    PacketInterpreter(ArrayBlockingQueue<byte[]> queueToServer) {
        this.queueToServer = queueToServer;
    }

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

    public void fromServer(NetworkMessage networkMessage) {
        if (expandedKey == null) {
          return;
        }

        int seq = networkMessage.header().getInt(2);
        boolean zlibCompressed = (seq >>> 31) == 1;
        seq = seq & (0xFFFFFFFF >>> 1);
        // System.out.println("zlib="+zlib);


        XTEA.decrypt(networkMessage.dataBuffer(), expandedKey);
        byte fill = networkMessage.getByte(); // how many fill bytes are there at the end
        networkMessage.setFillBytes(fill);


        if (zlibCompressed) {

            Inflater inflater = new Inflater(true);

            ByteBuffer byteBuffer = networkMessage.dataBuffer();
            byteBuffer.get(); // skip fill
            byteBuffer.mark();

            int lengthBefore = byteBuffer.remaining();
            ByteBuffer buffer = ByteBuffer.wrap(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());

            inflater.setInput(buffer);

            byte[] output = new byte[1024*1024]; // Adjust size if needed
            int lengthAfter = 0;
            try {
                lengthAfter = inflater.inflate(output);
            } catch (DataFormatException e) {
                e.printStackTrace();
            }

            byteBuffer.limit(byteBuffer.limit() + lengthAfter - lengthBefore);
            byteBuffer.put(output, 0, lengthAfter);
            byteBuffer.reset();

            networkMessage.setFillBytes(0);
            networkMessage.setLimit(byteBuffer.limit() + lengthAfter - lengthBefore);


            inflater.end();
        }




        // it will be some effort to parse everything
        // brute force try to parse update stats packet - maybe it is somewhere.

        int charstatsSize = 50; // dummy val
        for (int i = 0; i <  networkMessage.dataBuffer().remaining() - 1 /* OpCode */- charstatsSize; i++) {
            try {
                ByteBuffer dataBuffer = networkMessage.dataBuffer();
                dataBuffer.get(); // skip fill
                dataBuffer.position(dataBuffer.position() + i);

                byte opCode = dataBuffer.get();
                if (opCode != (byte) 0xA0) {
                    continue;
                }

                int hp = dataBuffer.getInt();
                int maxHp = dataBuffer.getInt();
                int cap = dataBuffer.getInt();
                long exp = dataBuffer.getLong();

                short level = dataBuffer.getShort();
                byte percent = dataBuffer.get();

                short clientExpDisplay = dataBuffer.getShort();
                short lowLevelBonusDysplay = dataBuffer.getShort();
                short storeExpBonus = dataBuffer.getShort();
                short staminaBonus = dataBuffer.getShort();

                int mana = dataBuffer.getInt();
                int maxMana = dataBuffer.getInt();

                if (hp <= maxHp && hp >= 0 && maxHp > 0 && exp >= 0 && mana <=  maxMana) {
                    // potentially thats it
                    System.out.println("stats packet found, hp = %d, maxHp = %d, mana = %d, maxMana = %d, cap = %d, exp =%d, seq = %d, pos = %d"
                            .formatted(hp, maxHp, mana, maxMana, cap, exp, seq, i));

                    //break; // - don't break - try to match another xA0 packet in this message
                }
            } catch (Exception e) {

            }

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
