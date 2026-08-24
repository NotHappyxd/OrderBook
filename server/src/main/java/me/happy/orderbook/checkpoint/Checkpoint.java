package me.happy.orderbook.checkpoint;

import me.happy.orderbook.engine.OrderBook;
import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.PriceLevel;
import me.happy.orderbook.order.Side;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Checkpoint {

    private static final int MAGIC = 0x31DEC8BF;

    public record OrderRecord(Side side, long orderId, long secret, boolean marketPrice, int price, int quantity) {
    }

    public record TickerState(long tickerId, long marketDataSequence, List<OrderRecord> orders) {
    }

    public record CheckpointData(long watermarkSequence, long orderIdSequence, List<TickerState> tickers) {
    }

    public static void write(Path path, long watermarkSequence, long orderIdSequence, Map<Long, OrderBook> orderBookMap) throws IOException {
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp.toFile());
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fos))) {

            out.writeInt(MAGIC);
            out.writeLong(watermarkSequence);
            out.writeLong(orderIdSequence);
            out.writeInt(orderBookMap.size());

            for (Map.Entry<Long, OrderBook> entry : orderBookMap.entrySet()) {
                writeTicker(out, entry.getKey(), entry.getValue());
            }

            out.flush();
            fos.getFD().sync();
        }

        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private static void writeTicker(DataOutputStream out, long tickerId, OrderBook book) throws IOException {
        out.writeLong(tickerId);
        out.writeLong(book.getMarketDataSequence());

        List<Order> orders = new ArrayList<>();
        collectSide(book.getBids(), orders);
        collectSide(book.getAsks(), orders);

        out.writeInt(orders.size());

        for (Order order : orders) {
            out.writeByte(order.getSide() == Side.BUY ? 1 : 2);
            out.writeLong(order.getId());
            out.writeLong(order.getSecret());
            out.writeBoolean(order.isMarketPrice());
            out.writeInt(order.getPrice());
            out.writeInt(order.getQuantity());
        }
    }

    private static void collectSide(TreeMap<Integer, PriceLevel> side, List<Order> orders) {
        for (PriceLevel level : side.values()) {
            Order order = level.getHead();

            while (order != null) {
                orders.add(order);
                order = order.getNext();
            }
        }
    }

    public static CheckpointData load(Path path) throws IOException {
        if (!Files.exists(path)) return null;

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(path.toFile())))) {
            int magic = input.readInt();

            if (magic != MAGIC) {
                throw new IOException("Checkpoint file " + path + " has unrecognized magic - refusing to load a possibly corrupt file");
            }

            long watermarkSequence = input.readLong();
            long orderIdSequence = input.readLong();
            int orderBookSize = input.readInt();

            List<TickerState> tickers = new ArrayList<>(orderBookSize);

            for (int i = 0; i < orderBookSize; i++) {
                tickers.add(readTicker(input));
            }

            return new CheckpointData(watermarkSequence, orderIdSequence, tickers);
        }
    }

    private static TickerState readTicker(DataInputStream input) throws IOException {
        long tickerId = input.readLong();
        long marketDataSequence = input.readLong();

        int size = input.readInt();

        List<OrderRecord> records = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            Side side = input.readByte() == 1 ? Side.BUY : Side.SELL;
            long id = input.readLong();
            long secret = input.readLong();
            boolean marketPrice = input.readBoolean();
            int price = input.readInt();
            int quantity = input.readInt();

            records.add(new OrderRecord(side, id, secret, marketPrice, price, quantity));
        }

        return new TickerState(tickerId, marketDataSequence, records);
    }
}
