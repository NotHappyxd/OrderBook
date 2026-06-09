package me.happy.orderbook.lmax.trade;

import com.lmax.disruptor.RingBuffer;
import lombok.Getter;
import me.happy.orderbook.order.Side;

public class TradePublisher {

    @Getter
    private final TradeEventHandler tradeEventHandler;
    private final RingBuffer<TradeEvent> ringBuffer;

    public TradePublisher(TradeEventHandler tradeEventHandler, RingBuffer<TradeEvent> ringBuffer) {
        this.tradeEventHandler = tradeEventHandler;
        this.ringBuffer = ringBuffer;
    }

    public void publishFill(long tickerId, long orderId, long takerId, int price, int quantity, Side takerSide) {
        long sequence = ringBuffer.next();

        try {
            TradeEvent event = ringBuffer.get(sequence);
            event.setTickerId(tickerId);
            event.setSequence(sequence);
            event.setOrderId(orderId);
            event.setTakerId(takerId);
            event.setPrice(price);
            event.setQuantity(quantity);
            event.setTakerSide(takerSide);
        }finally {
            ringBuffer.publish(sequence);
        }
    }
}
