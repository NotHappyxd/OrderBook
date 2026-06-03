package me.happy.orderbook.lmax.handler;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import me.happy.orderbook.engine.OrderBook;
import me.happy.orderbook.order.OrderSnapshot;
import me.happy.orderbook.lmax.OrderAllocator;
import me.happy.orderbook.lmax.events.OrderEvent;
import me.happy.orderbook.order.Order;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Getter
public class OrderEventHandler implements EventHandler<OrderEvent> {

    private final OrderAllocator orderAllocator;
    private final int shardId;
    private final int shardCount;
    private long sequence = 0;
    private final Map<Long, OrderBook> orderBookMap = new HashMap<>();

    public OrderEventHandler(int shardId, int shardCount) {
        this.shardId = shardId;
        this.shardCount = shardCount;
        this.orderAllocator = new OrderAllocator(1024);
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        long start = System.nanoTime();
        if (event.isSnapshot()) {
            processSnapshot(event, sequence);
        } else {
            processOrder(event);
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

        order.setSide(event.getSide());
        order.setId((++this.sequence * shardCount) + shardId);
        order.setQuantity(event.getQuantity());
        order.setPrice(event.getPrice());

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
        event.getChannel().write(byteBuf);

        orderBook.process(order);
    }
}
