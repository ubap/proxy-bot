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

    public Message say(String text) {
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

        writeFillBytes(dataBuffer);
        writeLength(dataBuffer);

        Message message = new Message(dataBuffer.array(), dataBuffer.position());

        XTEA.encrypt(message.dataBuffer(), gameState.getXteaKey());
        return message;
    }

    private void writeFillBytes(ByteBuffer dataBuffer) {
        // advance the buffer to the desired length
        int fillBytes = 8 - (dataBuffer.position() - HEADER_LENGTH) % 8;
        dataBuffer.position(dataBuffer.position() + fillBytes);

        // write fill bytes
        dataBuffer.put(6, (byte) fillBytes);
    }

    private void writeLength(ByteBuffer dataBuffer) {
        // write length
        byte length = (byte) ((dataBuffer.position() - HEADER_LENGTH) / 8);
        dataBuffer.putShort(0, length);
    }

    public Message useItemOnYourself(short itemId) {
        byte opCode = (byte) 0x84;
        byte[] example = {0x02, 0x00, 0x0D, 0x00, 0x00, 0x00, 0x02,
                opCode, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x0C, 0x01, 0x00, 0x23, 0x65, 0x00, 0x10, 0x06, 0x1A};

        ByteBuffer dataBuffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        dataBuffer.position(7); // skip header and fill bytes

        dataBuffer.put(opCode);

        dataBuffer.putShort((short) 0xFFFF); // frompos X
        dataBuffer.putShort((short) 0); // frompos y
        dataBuffer.put((byte) 0); // frompos z

        dataBuffer.putShort(itemId);
        dataBuffer.put((byte) 0); // stackpos ?
        dataBuffer.putInt(gameState.getPlayerId());

        writeFillBytes(dataBuffer);
        writeLength(dataBuffer);

        Message message = new Message(dataBuffer.array(), dataBuffer.position());

        XTEA.encrypt(message.dataBuffer(), gameState.getXteaKey());
        return message;
    }

}
