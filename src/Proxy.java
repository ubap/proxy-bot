import java.io.BufferedInputStream;
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
            byte[] readBuffer = new byte[1024 * 1024];
            try (InputStream input = socketToClient.getInputStream()) {
                // the intro from client is "IglaOts" - it's most likely a custom packet for IglaOts - but I did not verify it.
                byte[] introFromClient = input.readNBytes(8);
                System.out.println("Received intro packet from client: " + new String(introFromClient));

                // skip quwuw bcus this doesmt  seq
                socketToServer.getOutputStream().write(introFromClient);


                boolean initialPacket = true;
                while (true) {
                    // 2 byte payloadBlocks
                    // 4 byte checksum
                    input.readNBytes(readBuffer, 0, 6);
                    ByteBuffer header = ByteBuffer.wrap(readBuffer).order(ByteOrder.LITTLE_ENDIAN);
                    int payloadBlocks = header.getShort();

                    input.readNBytes(readBuffer, 6, payloadBlocks * 8);


                    byte[] dataToSend = Arrays.copyOfRange(readBuffer, 0, payloadBlocks * 8 + 6);
                    queueToServer.add(dataToSend);

                    // socketToServer.getOutputStream().write(readBuffer, 0, payloadBlocks * 8 + 6);



                    NetworkMessage networkMessage = new NetworkMessage(header, readBuffer);
                    if (initialPacket) {
                        packetInterpreter.initialFromClient(networkMessage);
                    } else {
                        packetInterpreter.fromClient(networkMessage);
                    }

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
                    int length = (readBuffer[1] << 8) | (readBuffer[0] & 0xFF);

                    input.readNBytes(readBuffer, 6, length * 8);

                    socketToClient.getOutputStream().write(readBuffer, 0, length * 8 + 6);
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
