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

    public void publishTrade(long tickerId, long sequence, int price, int quantity, Side takerSide) {
        long seq = ringBuffer.next();

        try {
            TradeEvent event = ringBuffer.get(seq);
            event.setTickerId(tickerId);
            event.setSequence(sequence);
            event.setPrice(price);
            event.setQuantity(quantity);
            event.setTakerSide(takerSide);
        }finally {
            ringBuffer.publish(seq);
        }
    }
}