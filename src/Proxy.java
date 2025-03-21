import networkmessage.Message;
import networkmessage.PacketsFromServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Proxy {

    ArrayBlockingQueue<byte[]> queueToServer = new ArrayBlockingQueue<>(100);

    public Proxy(Socket socketToClient, Socket socketToServer) {
        MessageInterpreter messageInterpreter = new MessageInterpreter(queueToServer);
        Thread clientThread = new Thread(() -> {
            try (InputStream input = socketToClient.getInputStream()) {
                // the intro from client is "IglaOts" - it's most likely a custom packet for IglaOts - but I did not verify it.
                byte[] introFromClient = input.readNBytes(8);
                if (!new String(introFromClient, 0, 8).equals("IglaOTS\n")) {
                    throw new RuntimeException("First packet from client different than expected");
                }
                // skip queue because this is not a valid "Message", queue not needed yet.
                socketToServer.getOutputStream().write(introFromClient);


                boolean initialPacket = true;
                while (true) {
                    Message message = Message.readFromStream(input);

                    byte[] dataToSend = Arrays.copyOfRange(message.getBackingArray(), 0, message.messageLength());
                    queueToServer.add(dataToSend);


                    if (initialPacket) {
                        messageInterpreter.initialFromClient(message);
                    }
//                    else {
//                        packetInterpreter.fromClient(networkMessage);
//                    }

                    initialPacket = false;
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Thread serverThread = new Thread(() -> {
            try (InputStream input = socketToServer.getInputStream()) {
                while (true) {
                    Message message = Message.readFromStream(input);
                    socketToClient.getOutputStream().write(message.getBackingArray(), 0, message.messageLength());
                    PacketsFromServer packetsFromServer = messageInterpreter.fromServer(message);
                    if (packetsFromServer == null) {
                        continue;
                    }

                    int charstatsSize = 50; // dummy val
                    for (int i = 0; i < packetsFromServer.getPacketsData().remaining() - 1 /* OpCode */ - charstatsSize; i++) {
                        try {
                            ByteBuffer dataBuffer = packetsFromServer.getPacketsData();
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

                            if (hp <= maxHp && hp >= 0 && maxHp > 0 && exp >= 0 && mana <= maxMana) {
                                // potentially thats it
                                System.out.println("stats packet found, hp = %d, maxHp = %d, mana = %d, maxMana = %d, cap = %d, exp =%d, seq = %d, pos = %d"
                                        .formatted(hp, maxHp, mana, maxMana, cap, exp, packetsFromServer.getMessage().getSequence(), i));

                                //break; // - don't break - try to match another xA0 packet in this message
                            }
                        } catch (Exception e) {

                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        AtomicInteger seq = new AtomicInteger();
        Thread serverWriteThread = new Thread(() -> {
            try {
                while (true) {
                    byte[] item = queueToServer.poll(1, TimeUnit.MINUTES);
                    ByteBuffer buffer = ByteBuffer.wrap(item).order(ByteOrder.LITTLE_ENDIAN);
                    buffer.putInt(2, seq.getAndIncrement());

                    // System.out.println(PacketInterpreter.bytesToHex(item));

                    socketToServer.getOutputStream().write(item);

                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        serverWriteThread.start();
        clientThread.start();
        serverThread.start();
    }

}
