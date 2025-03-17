import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class Main {
// eHvgWfQpR5gsGvN!

    public static void main(String[] args) throws IOException {
        LoginServer.run();


        int port = 7172;
        String serverAddress = "146.59.53.55"; // Adres IP serwera

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socketToClient = serverSocket.accept();
                System.out.println("New client connected, connecting to server");
                Socket socketToServer = new Socket(serverAddress, port);
                System.out.println("Connected to server");

                //new ClientHandler(socket).start();

                new Proxy(socketToClient, socketToServer);


            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }


    }

    static class ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedInputStream input = new BufferedInputStream(socket.getInputStream())) {

                // the intro from client is "IglaOts" - it's most likely a custom packet for IglaOts - but I did not verify it.
                byte[] introFromClient = input.readNBytes(8);
                System.out.println("Received intro packet from client: " + new String(introFromClient));


                byte[] bytesToRespond = new ServerHandler().connect(introFromClient);
                socket.getOutputStream().write(bytesToRespond);



                byte[] readBuffer = new byte[1024 * 1024];

                // 2 byte payloadBlocks
                // 4 byte checksum
                input.readNBytes(readBuffer, 0, 6);
                int payloadBlocks = (readBuffer[1] << 8) | (readBuffer[0] & 0xFF);

                input.readNBytes(readBuffer, 6, payloadBlocks * 8);
                NetworkMessage networkMessage = new NetworkMessage(ByteBuffer.wrap(readBuffer, 0,6).order(ByteOrder.LITTLE_ENDIAN), readBuffer);




            } catch (IOException ex) {
                System.out.println("Server error: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}