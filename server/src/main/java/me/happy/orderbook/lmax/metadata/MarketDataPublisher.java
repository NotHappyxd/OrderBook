package me.happy.orderbook.lmax.metadata;

import com.lmax.disruptor.RingBuffer;
import me.happy.orderbook.order.Side;

public class MarketDataPublisher {

    private final RingBuffer<MarketDataEvent> ringBuffer;

    public MarketDataPublisher(RingBuffer<MarketDataEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void publishLevelUpdate(long ticker, long sequence, Side side, int price, int totalQuantity) {
        long seq = ringBuffer.next();

        try {
            MarketDataEvent event = ringBuffer.get(seq);
            event.setTicker(ticker);
            event.setSequence(sequence);
            event.setSide(side);
            event.setPrice(price);
            event.setTotalQuantity(totalQuantity);
        } finally {
            ringBuffer.publish(seq);
        }
    }
}