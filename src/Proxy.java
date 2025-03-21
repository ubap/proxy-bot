import networkmessage.Message;

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
        PacketInterpreter packetInterpreter = new PacketInterpreter(queueToServer);
        Thread clientThread = new Thread(() -> {
            ByteBuffer readBuffer = ByteBuffer.allocateDirect(1024 * 1024);
            try (InputStream input = socketToClient.getInputStream()) {
                // the intro from client is "IglaOts" - it's most likely a custom packet for IglaOts - but I did not verify it.
                byte[] introFromClient = input.readNBytes(8);
                System.out.println("Received intro packet from client: " + new String(introFromClient));

                // skip quwuw bcus this doesmt  seq
                socketToServer.getOutputStream().write(introFromClient);


                // draft here
                /*
               

                read header
                read network message
                send network message to the server (immediately, to avoid lag)


                process network message



                 */
                // draft


                boolean initialPacket = true;
                while (true) {
                    Message message = Message.readFromStream(input);

                    byte[] dataToSend = Arrays.copyOfRange(message.getBackingArray(), 0, message.messageLength());
                    queueToServer.add(dataToSend);


                    if (initialPacket) {
                        packetInterpreter.initialFromClient(message);
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
            byte[] readBuffer = new byte[1024 * 1024];

            try (InputStream input = socketToServer.getInputStream()) {

                while (true) {
                    // 2 byte length
                    // 4 byte checksum
                    input.readNBytes(readBuffer, 0, 6);
                    ByteBuffer header = ByteBuffer.wrap(readBuffer).order(ByteOrder.LITTLE_ENDIAN);
                    int payloadBlocks = header.getShort();
                    input.readNBytes(readBuffer, 6, payloadBlocks * 8);

                    socketToClient.getOutputStream().write(readBuffer, 0, payloadBlocks * 8 + 6);

                    NetworkMessage networkMessage = new NetworkMessage(header, readBuffer);
                    packetInterpreter.fromServer(networkMessage);
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
