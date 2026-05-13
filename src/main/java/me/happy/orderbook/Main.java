package me.happy.orderbook;

import lombok.SneakyThrows;
import me.happy.orderbook.engine.OrderBook;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.lmax.OrderEventHandler;
import me.happy.orderbook.order.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        Exchange exchange = new Exchange(4);

        long start = System.currentTimeMillis();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Thread thread = new Thread(() -> {
                int orders = 0;
                while (++orders <= 10000) {
                    exchange.process(packString("APPL"), ThreadLocalRandom.current().nextBoolean() ? Side.BUY : Side.SELL, ThreadLocalRandom.current().nextInt(0, 100), ThreadLocalRandom.current().nextInt(0, 100));
                }
            });

            threads.add(thread);
        }

        threads.forEach(Thread::start);

        for (Thread thread : threads) {
            thread.join();
        }

        Thread.sleep(1000L);
        for (OrderEventHandler handler : exchange.getHandlers()) {
            for (Map.Entry<Long, OrderBook> entry : handler.getOrderBookMap().entrySet()) {
                System.out.println(unpack(entry.getKey()) + " " + entry.getValue().getBids());
            }
        }

        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms");
    }

    public static String unpack(long value) {
        StringBuilder result = new StringBuilder(8);

        for (int i = 7; i >= 0; i--) {
            char code = (char) ((value >> (i * 8)) & 0xFF);

            if (code != 0)
                result.append(code);
        }

        return result.toString();
    }

    public static long packString(String s) {
        if (s.length() > 8) {
            throw new IllegalArgumentException("Max 8 chars");
        }

        long value = 0;

        for (char c : s.toCharArray()) {
            value <<= 8;
            value |= c;
        }

        return value;
    }
}
