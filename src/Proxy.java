import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Proxy {


    public Proxy(Socket socketToClient, Socket socketToServer) {
        PacketInterpreter packetInterpreter = new PacketInterpreter();
        Thread clientThread = new Thread(() -> {
            byte[] readBuffer = new byte[1024 * 1024];
            try (InputStream input = socketToClient.getInputStream()) {
                // the intro from client is "IglaOts" - it's most likely a custom packet for IglaOts - but I did not verify it.
                byte[] introFromClient = input.readNBytes(8);
                System.out.println("Received intro packet from client: " + new String(introFromClient));
                socketToServer.getOutputStream().write(introFromClient);


                boolean initialPacket = true;
                while (true) {
                    // 2 byte payloadBlocks
                    // 4 byte checksum
                    input.readNBytes(readBuffer, 0, 6);
                    ByteBuffer header = ByteBuffer.wrap(readBuffer).order(ByteOrder.LITTLE_ENDIAN);
                    int payloadBlocks = header.getShort();

                    input.readNBytes(readBuffer, 6, payloadBlocks * 8);

                    socketToServer.getOutputStream().write(readBuffer, 0, payloadBlocks * 8 + 6);



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

        clientThread.start();
        serverThread.start();
    }

}
