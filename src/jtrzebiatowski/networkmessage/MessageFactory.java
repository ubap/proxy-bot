package jtrzebiatowski.networkmessage;

import jtrzebiatowski.encryption.XTEA;
import jtrzebiatowski.game.GameState;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static jtrzebiatowski.networkmessage.Message.HEADER_LENGTH;

public class MessageFactory {

    GameState gameState;

    public MessageFactory(GameState gameState) {
        this.gameState = gameState;
    }

    public jtrzebiatowski.networkmessage.Message say(String text) {
        byte opCode = (byte) 0x96;
        byte typeSay = 1;
        byte[] exampleData = {0x02, 0x00, 0x2D, 0x00, 0x00, 0x00,
                0x02, opCode, typeSay, 0x09, 0x00, 0x75, 0x74, 0x61, 0x6E, 0x69, 0x20, 0x68, 0x75, 0x72, 0x35, 0x05};

        ByteBuffer dataBuffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        dataBuffer.position(7); // skip header and fill bytes

        dataBuffer.put(opCode);
        dataBuffer.put(typeSay);

        short len = (short) text.length();
        dataBuffer.putShort(len);
        dataBuffer.put(text.getBytes(StandardCharsets.UTF_8), 0, len);

        // advance the buffer to the desired length
        int fillBytes = 8 - (dataBuffer.position() - HEADER_LENGTH)% 8;
        dataBuffer.position(dataBuffer.position() + fillBytes);

        // write fill bytes
        dataBuffer.put(6, (byte) fillBytes);

        // write length
        byte length = (byte) ((dataBuffer.position() - HEADER_LENGTH) / 8 );
        dataBuffer.putShort(0, length);

        jtrzebiatowski.networkmessage.Message message = new jtrzebiatowski.networkmessage.Message(dataBuffer.array(), dataBuffer.position());

        XTEA.encrypt(message.dataBuffer(), gameState.getXteaKey());
        return message;
    }
}
