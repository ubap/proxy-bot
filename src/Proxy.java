import networkmessage.Game;
import networkmessage.Message;
import networkmessage.MessageFactory;
import networkmessage.PacketsFromServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Proxy {

    ArrayBlockingQueue<Message> queueToServer = new ArrayBlockingQueue<>(100);

    Game game = new Game();
    MessageFactory messageFactory = new MessageFactory(game);

    public Proxy(Socket socketToClient, Socket socketToServer) {
        MessageInterpreter messageInterpreter = new MessageInterpreter(queueToServer, game);
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
                    queueToServer.add(messageFromClient.duplicate());
                    //socketToServer.getOutputStream().write(messageFromClient.duplicate().getBackingArray(), 0, messageFromClient.messageLength());

                    if (initialPacket) {
                        messageInterpreter.initialFromClient(messageFromClient);
                    }
                    else {
                        messageInterpreter.fromClient(messageFromClient);
                    }

                    initialPacket = false;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        PacketsFromServerInterpreter packetsFromServerInterpreter = new PacketsFromServerInterpreter(game);
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
                    Message messageToSend = queueToServer.poll(1, TimeUnit.MINUTES);
                    messageToSend.setSequence(seq.getAndIncrement());

                    socketToServer.getOutputStream().write(messageToSend.getBackingArray(), 0, messageToSend.messageLength());

                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // bot
        new Thread(() -> {
            while (true) {
                if (game.getMana() > 400) {
                    Message exuraIco = messageFactory.say("exura ico");
                    queueToServer.add(exuraIco);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }).start();

        serverWriteThread.start();
        clientThread.start();
        serverThread.start();
    }

}
