package me.happy.orderbook.lmax.order;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import lombok.Getter;
import me.happy.orderbook.processor.OrderEventProcessor;

@Getter
public class OrderEventHandler implements EventHandler<OrderEvent> {

    private final OrderEventProcessor processor;

    public OrderEventHandler(int shardId, int shardCount) {
        this.processor = new OrderEventProcessor(shardId, shardCount);
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        this.processor.process(event, sequence, endOfBatch);
    }

    @Override
    public void setSequenceCallback(Sequence sequenceCallback) {
        EventHandler.super.setSequenceCallback(sequenceCallback);
    }
}
