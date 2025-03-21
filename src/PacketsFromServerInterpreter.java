import networkmessage.PacketsFromServer;

import java.nio.ByteBuffer;

public class PacketsFromServerInterpreter {

    private final Game game;

    public PacketsFromServerInterpreter(Game game) {
        this.game = game;
    }

    public void process(PacketsFromServer packetsFromServer) {
        bruteForceSearchStatsPacket(packetsFromServer);
    }

    private void bruteForceSearchStatsPacket(PacketsFromServer packetsFromServer) {


        // brute force find stats packet
        int charstatsSize = 50; // dummy val
        for (int i = 0; i < packetsFromServer.getPacketsData().remaining() - 1 /* OpCode */ - charstatsSize; i++) {
            try {
                ByteBuffer dataBuffer = packetsFromServer.getPacketsData();
                dataBuffer.get(); // skip fill
                dataBuffer.position(dataBuffer.position() + i);

                byte opCode = dataBuffer.get();
                if (opCode != (byte) 0xA0) {
                    continue;
                }

                int hp = dataBuffer.getInt();
                int maxHp = dataBuffer.getInt();
                int cap = dataBuffer.getInt();
                long exp = dataBuffer.getLong();


                short level = dataBuffer.getShort();
                byte percent = dataBuffer.get();

                short clientExpDisplay = dataBuffer.getShort();
                short lowLevelBonusDysplay = dataBuffer.getShort();
                short storeExpBonus = dataBuffer.getShort();
                short staminaBonus = dataBuffer.getShort();


                int mana = dataBuffer.getInt();
                int maxMana = dataBuffer.getInt();

                int soul = dataBuffer.get() & 0xFF;
                short staminaMinutes = dataBuffer.getShort();
                short baseSpeed = dataBuffer.getShort();

                short condition = dataBuffer.getShort();

                short offlineTrainingTime = dataBuffer.getShort();

                short offlineTrainingSeconds = dataBuffer.getShort();

                byte expBoostInStore = dataBuffer.get();


                if (hp <= maxHp&& hp >= 0 && maxHp > 0
                        && exp >= 0
                        && mana <= maxMana
                && expBoostInStore == 1
                && baseSpeed > 0
                && soul > 0 && soul <= 200) {
                    // potentially thats it
                    System.out.println("stats packet found, hp = %d, maxHp = %d, mana = %d, maxMana = %d, cap = %d, level = %d, percent = %d, exp =%d, seq = %d, pos = %d"
                            .formatted(hp, maxHp, mana, maxMana, cap, level, percent, exp, packetsFromServer.getMessage().getSequence(), i));

                    game.setHp(hp);
                    game.setMaxHp(maxHp);
                    game.setMana(mana);
                    game.setMaxMana(maxMana);

                    //break; // - don't break - try to match another xA0 packet in this message
                }
            } catch (Exception e) {

            }
        }
    }
}
