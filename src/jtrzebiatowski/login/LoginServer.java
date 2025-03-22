package jtrzebiatowski.login;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Responsible for hosting a proxy jtrzebiatowski.login server that replaces the jtrzebiatowski.game world ip on the fly to point to the proxy.
 */
public class LoginServer {

    public static void run() {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(80), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Define a context that serves files from the current directory
        server.createContext("/", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                System.out.println("not post, abort");
                exchange.sendResponseHeaders(400, 0);
                exchange.close();
            }

            // Read the request body as a string
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Received JSON: " + requestBody);


            try {
                // Create an HttpClient
                HttpClient client = HttpClient.newHttpClient();

                // Define the request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://54.37.137.74/login.php")) // Example API
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .build();

                // Send the request and get the response
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String responseText = response.body();
                responseText = responseText.replaceAll("146.59.53.55", "192.168.1.93");
                // Print response body
                System.out.println("Response: " + responseText);
                exchange.sendResponseHeaders(200, responseText.length());

                // Write response body
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseText.getBytes());
                }
                // Close exchange
                exchange.close();
                return;


            } catch (Exception e) {
                e.printStackTrace();
            }


            System.out.println("unexpected");

            exchange.sendResponseHeaders(400, 0);

            exchange.close();
        });

        // Start the server
        server.start();
        System.out.println("Server started on port 80");
    }
}
