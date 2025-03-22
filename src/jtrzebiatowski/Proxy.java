package jtrzebiatowski;

import jtrzebiatowski.bot.Bot;
import jtrzebiatowski.game.GameState;
import jtrzebiatowski.networkmessage.Message;
import jtrzebiatowski.networkmessage.MessageFactory;
import jtrzebiatowski.networkmessage.PacketsFromServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Proxy {

    ArrayBlockingQueue<Message> queueToServer = new ArrayBlockingQueue<>(100);

    GameState gameState = new GameState();
    MessageFactory messageFactory = new MessageFactory(gameState);

    public Proxy(Socket socketToClient, Socket socketToServer) {
        MessageInterpreter messageInterpreter = new MessageInterpreter(queueToServer, gameState);
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

        PacketsFromServerInterpreter packetsFromServerInterpreter = new PacketsFromServerInterpreter(gameState);
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

        serverWriteThread.start();
        clientThread.start();
        serverThread.start();

        Bot bot = new Bot(this);
    }


    public GameState getGameState() {
        return gameState;
    }

    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    public ArrayBlockingQueue<Message> getQueueToServer() {
        return queueToServer;
    }
}
