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
                    Message messageFromClient = Message.readFromStream(input);
                    byte[] dataToSend = Arrays.copyOfRange(messageFromClient.getBackingArray(), 0, messageFromClient.messageLength());
                    queueToServer.add(dataToSend);

                    if (initialPacket) {
                        messageInterpreter.initialFromClient(messageFromClient);
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

        PacketsFromServerInterpreter packetsFromServerInterpreter = new PacketsFromServerInterpreter();
        Thread serverThread = new Thread(() -> {
            try (InputStream input = socketToServer.getInputStream()) {
                boolean initial = true;
                while (true) {
                    Message message = Message.readFromStream(input);
                    socketToClient.getOutputStream().write(message.getBackingArray(), 0, message.messageLength());
                    boolean useXtea = !initial;
                    PacketsFromServer packetsFromServer = messageInterpreter.fromServer(message, useXtea);
                    packetsFromServerInterpreter.process(packetsFromServer);
                    initial = false;
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
