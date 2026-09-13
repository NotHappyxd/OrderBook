package me.happy.orderbook.lmax.order;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import lombok.Getter;
import me.happy.orderbook.processor.OrderEventProcessor;

@Getter
public class OrderEventHandler implements EventHandler<OrderEvent> {

    private final OrderEventProcessor processor;

    public OrderEventHandler(OrderEventProcessor processor) {
        this.processor = processor;
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
