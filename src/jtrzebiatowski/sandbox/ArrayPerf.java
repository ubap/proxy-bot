package jtrzebiatowski.sandbox;

import java.util.ArrayList;
import java.util.List;

public class ArrayPerf {

    public static void main(String[] args) {


        allocatePackets(100_000);

        int packets = 10_000;
        long begin = System.nanoTime();
        List<byte[]> bytes = allocatePackets(packets);

        long elapsed = System.nanoTime() - begin;
        System.out.println("took: %d ns, %d ns per packet".formatted(elapsed, elapsed / packets));
    }

    public static  List<byte[]> allocatePackets(int n) {
        List<byte[]> allocatedPackets = new ArrayList<>();
        for (int i = 0 ; i < n; i++) {

            byte[] buffer = new byte[5000];
            allocatedPackets.add(buffer);

        }
        return allocatedPackets;
    }
}
