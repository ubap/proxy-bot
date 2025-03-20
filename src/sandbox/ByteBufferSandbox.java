package sandbox;

import java.nio.ByteBuffer;

public class ByteBufferSandbox {

    public static void main(String[] args) {


        byte[] backingArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        ByteBuffer buffer = ByteBuffer.wrap(backingArray, 2, 2);

        buffer.get();

        buffer.mark();

        byte byteToDobule = buffer.get();
        buffer.reset();
        buffer.put((byte) (byteToDobule*2));
        buffer.reset();

        System.out.println(buffer.get());
    }
}
