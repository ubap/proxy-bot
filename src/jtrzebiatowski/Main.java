package jtrzebiatowski;

import jtrzebiatowski.login.LoginServer;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;


public class Main {
// eHvgWfQpR5gsGvN!

    public static void main(String[] args) throws IOException {
        LoginServer.run();

        int port = 7172;
        String serverAddress = "146.59.53.55"; // Adres IP serwera

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                try {
                    Socket socketToClient = serverSocket.accept();
                    System.out.println("New client connected, connecting to server");
                    Socket socketToServer = new Socket(serverAddress, port);
                    System.out.println("Connected to server");


                    new Proxy(socketToClient, socketToServer);

                } catch (ConnectException connectException) {
                    connectException.printStackTrace();
                }

            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}