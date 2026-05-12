package me.happy.orderbook.lmax;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import lombok.Getter;
import me.happy.orderbook.engine.OrderBook;
import me.happy.orderbook.order.Order;

import java.util.HashMap;
import java.util.Map;

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
        Order order = orderAllocator.getOrder();

        order.setSide(event.getSide());
        order.setId((++this.sequence * shardCount) + shardId);
        order.setQuantity(event.getQuantity());
        order.setPrice(event.getPrice());

        OrderBook orderBook = orderBookMap.get(event.getTicker());

        if (orderBook == null) {
            orderBook = new OrderBook(this.orderAllocator);
            this.orderBookMap.put(event.getTicker(), orderBook);
        }

        orderBook.process(order);
    }

    @Override
    public void setSequenceCallback(Sequence sequenceCallback) {
        EventHandler.super.setSequenceCallback(sequenceCallback);
    }
}
