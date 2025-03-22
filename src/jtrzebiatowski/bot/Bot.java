package jtrzebiatowski.bot;

import jtrzebiatowski.Proxy;
import jtrzebiatowski.game.GameState;
import jtrzebiatowski.networkmessage.Message;
import jtrzebiatowski.networkmessage.MessageFactory;

import java.util.concurrent.ArrayBlockingQueue;


public class Bot {

    private final GameState gameState;
    private final MessageFactory messageFactory;
    private final ArrayBlockingQueue<Message> queueToServer;

    public Bot(Proxy proxy) {
        this.gameState = proxy.getGameState();
        this.messageFactory = proxy.getMessageFactory();
        this.queueToServer = proxy.getQueueToServer();

        spellHeal(1200, 40, "exura ico");
    }

    private void spellHeal(int hpBelow, int manaRequired, String spell) {

        new Thread(() -> {
            while (true) {
                try {
                    if (gameState.getHp() < hpBelow && gameState.getMana() >= manaRequired) {
                        Message exuraIco = messageFactory.say(spell);
                        queueToServer.add(exuraIco);
                        Thread.sleep(500);
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }).start();
    }
}
