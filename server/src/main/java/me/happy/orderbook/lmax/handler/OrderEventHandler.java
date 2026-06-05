package me.happy.orderbook.lmax.handler;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import me.happy.orderbook.engine.OrderBook;
import me.happy.orderbook.lmax.AllocatorPool;
import me.happy.orderbook.order.OrderSnapshot;
import me.happy.orderbook.lmax.events.OrderEvent;
import me.happy.orderbook.order.Order;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Getter
public class OrderEventHandler implements EventHandler<OrderEvent> {

    private final AllocatorPool<Order> orderAllocator;
    private final int shardId;
    private final int shardCount;
    private long sequence = 0;
    private final Map<Long, OrderBook> orderBookMap = new HashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public OrderEventHandler(int shardId, int shardCount) {
        this.shardId = shardId;
        this.shardCount = shardCount;
        this.orderAllocator = new AllocatorPool<>(1024, Order::new);
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        long start = System.nanoTime();

        switch (event.getCommand()) {
            case NEW -> processOrder(event);
            case SNAPSHOT -> processSnapshot(event, sequence);
            case CANCEL -> cancelOrder(event);
        }

        if (endOfBatch) {
            event.getChannel().flush();
        }

        long elapsed = System.nanoTime() - start;

        System.out.println("Order event took " + elapsed + " ns to complete. (" + TimeUnit.NANOSECONDS.toMillis(elapsed) + " ms)");
    }

    @Override
    public void setSequenceCallback(Sequence sequenceCallback) {
        EventHandler.super.setSequenceCallback(sequenceCallback);
    }

    private void processSnapshot(OrderEvent event, long sequence) {
        OrderBook orderBook = orderBookMap.get(event.getTicker());
        OrderSnapshot snapshot = new OrderSnapshot(event.getTicker());
        snapshot.setSequenceId(sequence);

        if (orderBook != null) {
            orderBook.fillSnapshot(snapshot, 5);
        }

        event.getChannel().write(snapshot);
    }

    private void processOrder(OrderEvent event) {
        Order order = orderAllocator.borrow();
        order.reset();

        long secret = secureRandom.nextLong();

        order.setSide(event.getSide());
        order.setId((++this.sequence * shardCount) + shardId);
        order.setQuantity(event.getQuantity());
        order.setPrice(event.getPrice());
        order.setSecret(secret);

        OrderBook orderBook = orderBookMap.get(event.getTicker());

        if (orderBook == null) {
            orderBook = new OrderBook(this.orderAllocator, event.getTicker());
            this.orderBookMap.put(event.getTicker(), orderBook);
        }

        // Acknowledge
        ByteBuf byteBuf = event.getChannel().alloc().buffer(33);
        byteBuf.writeByte(0x07);
        System.out.println(event.getClientRequestId());
        byteBuf.writeLong(event.getClientRequestId());
        byteBuf.writeLong(order.getId());
        byteBuf.writeLong(secret);
        event.getChannel().write(byteBuf);

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
            ByteBuf byteBuf = event.getChannel().alloc().buffer();
            byteBuf.writeByte(0x07);
            byteBuf.writeLong(event.getClientRequestId());
            byteBuf.writeLong(event.getOrderId());
            byteBuf.writeLong(event.getSecret());

            event.getChannel().write(byteBuf);
        }
    }
}
