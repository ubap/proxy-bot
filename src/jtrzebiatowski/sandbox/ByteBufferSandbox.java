package jtrzebiatowski.sandbox;

import java.nio.ByteBuffer;

public class ByteBufferSandbox {

    public static void main(String[] args) {


        byte[] backingArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        ByteBuffer buffer = ByteBuffer.wrap(backingArray, 2, 2);

        ByteBuffer byteBuffer = buffer.duplicate();


        System.out.println("stop");
    }
}
