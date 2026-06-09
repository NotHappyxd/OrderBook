package me.happy.orderbook.processor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import me.happy.orderbook.engine.OrderBook;
import me.happy.orderbook.lmax.AllocatorPool;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.lmax.order.OrderEvent;
import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.OrderSnapshot;
import me.happy.orderbook.order.PriceLevel;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class OrderEventProcessor {

    private final AllocatorPool<Order> orderAllocator;
    private final Map<Long, OrderBook> orderBookMap = new HashMap<>();

    public OrderEventProcessor() {
        this.orderAllocator = new AllocatorPool<>(1024, Order::new);
    }

    public void process(OrderEvent event, long sequence, boolean endOfBatch) {
        long start = System.nanoTime();

        switch (event.getCommand()) {
            case NEW -> processOrder(event);
            case SNAPSHOT -> processSnapshot(event, sequence);
            case MODIFY -> processModification(event, sequence);
            case CANCEL -> cancelOrder(event);
        }

        long elapsed = System.nanoTime() - start;

        System.out.println("Order event took " + elapsed + " ns to complete. (" + TimeUnit.NANOSECONDS.toMillis(elapsed) + " ms)");
    }

    private void processModification(OrderEvent event, long sequence) {
        OrderBook orderBook = orderBookMap.get(event.getTicker());

        if (orderBook == null) return;

        Order order = orderBook.getOrderMap().get(event.getOrderId());

        if (order == null || order.getSecret() != event.getSecret()) return;
        if (event.getQuantity() <= 0 || event.getPrice() <= 0) return; // Stop negative numbers

        PriceLevel priceLevel = orderBook.getBook(order).get(order.getPrice());

        boolean quantityChanged = event.getQuantity() != order.getQuantity();
        boolean priceChanged = event.getPrice() != order.getPrice();

        if (order.getQuantity() > event.getQuantity() && !priceChanged) { // User gives up liquidity, keep in place.
            int diff = event.getQuantity() - order.getQuantity();
            priceLevel.setTotalQuantity(priceLevel.getTotalQuantity() + diff);
            order.setQuantity(order.getQuantity());
        }else if (order.getQuantity() < event.getQuantity() || priceChanged) {
            priceLevel.removeOrder(order);
            priceLevel.setTotalQuantity(priceLevel.getTotalQuantity() - order.getQuantity());

            if (priceChanged)
                order.setPrice(event.getPrice());

            if (quantityChanged)
                order.setQuantity(event.getQuantity());

            orderBook.addToBook(order);
        }

        sendBuffer(event.getChannel(), 32, 0x09, byteBuf -> {
            byteBuf.writeLong(event.getTicker());
            byteBuf.writeLong(order.getId());
            byteBuf.writeLong(event.getClientRequestId());
            byteBuf.writeInt(order.getQuantity());
            byteBuf.writeInt(order.getPrice());
        });
    }

    private void processSnapshot(OrderEvent event, long sequence) {
        OrderBook orderBook = orderBookMap.get(event.getTicker());

        OrderSnapshot snapshot = new OrderSnapshot(event.getTicker());
        snapshot.setSequenceId(sequence);

        if (orderBook != null) {
            orderBook.fillSnapshot(snapshot, 5);
        }

        Exchange.getInstance().getOutboundPublisher().publish(event.getChannel(), snapshot);
    }

    private void processOrder(OrderEvent event) {
        Order order = orderAllocator.borrow();
        order.reset();

        order.setSide(event.getSide());
        order.setId(event.getOrderId());
        order.setQuantity(event.getQuantity());
        order.setPrice(event.getPrice());
        order.setSecret(event.getSecret());
        order.setKill(event.isKill());

        OrderBook orderBook = orderBookMap.get(event.getTicker());

        if (orderBook == null) {
            orderBook = new OrderBook(Exchange.getInstance().getTradePublisher(), this.orderAllocator, event.getTicker());
            this.orderBookMap.put(event.getTicker(), orderBook);
        }

        // Acknowledge
        sendBuffer(event.getChannel(), 32, 0x07, byteBuf -> {
            byteBuf.writeLong(event.getClientRequestId());
            byteBuf.writeLong(order.getId());
            byteBuf.writeLong(event.getSecret());
        });

        orderBook.process(order);
    }

    private void cancelOrder(OrderEvent event) {
        OrderBook orderBook = orderBookMap.get(event.getTicker());

        if (orderBook == null) return;

        Order order = orderBook.getOrderMap().get(event.getOrderId());

        if (order == null || order.getSecret() != event.getSecret()) return;

        boolean succeeded = orderBookMap.get(event.getTicker()).cancelOrder(event.getOrderId());

        // send ack
        if (succeeded) {
            sendBuffer(event.getChannel(), 24, 0x07, byteBuf -> {
                byteBuf.writeByte(0x07);
                byteBuf.writeLong(event.getClientRequestId());
                byteBuf.writeLong(event.getOrderId());
                byteBuf.writeLong(event.getSecret());
            });
        }
    }

    private void sendBuffer(Channel channel, int payloadSize, int operation, Consumer<ByteBuf> payload) {
        if (channel == null) return;
        ByteBuf byteBuf = channel.alloc().buffer(payloadSize + 1); // Size excludes operation
        byteBuf.writeByte(operation);

        payload.accept(byteBuf);

        Exchange.getInstance().getOutboundPublisher().publish(channel, byteBuf);
    }

    public OrderBook getOrderBook(long ticker) {
        return orderBookMap.get(ticker);
    }
}
