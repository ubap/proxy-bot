import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ServerHandler {


    int port = 7172;
    String serverAddress = "146.59.53.55"; // Adres IP serwera



    public byte[] connect(byte[] initialData) {
        try (Socket socket = new Socket(serverAddress, port);
             OutputStream output = socket.getOutputStream()) {

            output.write(initialData);

            byte[] readBytes = socket.getInputStream().readNBytes(14);

            return readBytes;
        } catch (IOException e) {
            System.err.println("Błąd połączenia: " + e.getMessage());
        }
        return null;
    }
}
