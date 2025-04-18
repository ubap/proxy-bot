package jtrzebiatowski.game;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class GameState {

    private int[] xteaExpandedKey;
    private final CountDownLatch xteaSet = new CountDownLatch(1);

    public void setXteaKey(int[] xteaExpandedKey) {
        this.xteaExpandedKey = Arrays.copyOf(xteaExpandedKey, xteaExpandedKey.length);
        xteaSet.countDown();
    }

    public int[] getXteaKey() {
        try {
            xteaSet.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return xteaExpandedKey;
    }

    private int playerId;
    private int maxHp;
    private int hp;
    private int maxMana;
    private int mana;

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public int getMana() {
        return mana;
    }
}
