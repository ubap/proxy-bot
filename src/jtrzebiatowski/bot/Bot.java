package jtrzebiatowski.bot;

import jtrzebiatowski.Proxy;
import jtrzebiatowski.game.GameState;
import jtrzebiatowski.networkmessage.Message;
import jtrzebiatowski.networkmessage.MessageFactory;

import java.util.concurrent.ArrayBlockingQueue;

import static jtrzebiatowski.bot.ItemIds.GREAT_HEALTH_POTION;
import static jtrzebiatowski.bot.ItemIds.MANA_POTION;


public class Bot {

    private final GameState gameState;
    private final MessageFactory messageFactory;
    private final ArrayBlockingQueue<Message> queueToServer;

    public Bot(Proxy proxy) {
        this.gameState = proxy.getGameState();
        this.messageFactory = proxy.getMessageFactory();
        this.queueToServer = proxy.getQueueToServer();
    }

    public void start() {
        spellHeal(1300, 40, "exura ico");
        potions(450, 1000);
    }

    private void potions(int manaBelow, int hpBelow) {
        new Thread(() -> {
            while (true) {
                try {
                    if (gameState.getHp() < hpBelow) {
                        Message mp = messageFactory.useItemOnYourself(GREAT_HEALTH_POTION);
                        queueToServer.add(mp);
                        Thread.sleep(500);
                        continue;
                    }

                    if (gameState.getMana() < manaBelow) {
                        Message mp = messageFactory.useItemOnYourself(MANA_POTION);
                        queueToServer.add(mp);
                        Thread.sleep(500);

                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }).start();
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
