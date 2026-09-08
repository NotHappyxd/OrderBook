package me.happy.orderbook.lmax.metadata;

import com.lmax.disruptor.RingBuffer;
import me.happy.orderbook.order.Side;

public class PublicFeedPublisher {

    private final RingBuffer<PublicFeedEvent> ringBuffer;

    public PublicFeedPublisher(RingBuffer<PublicFeedEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void publishDelta(long ticker, long marketDataSequence, Side side, int price, int totalQuantity) {
        publishFeedEvent(PublicFeedEvent.PublicFeedEventType.LEVEL_DATA, ticker, marketDataSequence, side, price, 0, totalQuantity);
    }

    public void publishTrade(long ticker, long marketDataSequence, Side side, int price, int tradedQuantity) {
        publishFeedEvent(PublicFeedEvent.PublicFeedEventType.TRADE_PRINT, ticker, marketDataSequence, side, price, tradedQuantity, 0);
    }

    private void publishFeedEvent(PublicFeedEvent.PublicFeedEventType type, long ticker, long marketDataSequence, Side side, int price, int tradedQuantity, int totalQuantity) {
        long seq = ringBuffer.next();

        try {
            PublicFeedEvent event = ringBuffer.get(seq);
            event.setType(type);
            event.setTicker(ticker);
            event.setMarketDataSequence(marketDataSequence);
            event.setSide(side);
            event.setPrice(price);
            event.setTradedQuantity(tradedQuantity);
            event.setTotalQuantity(totalQuantity);
        } finally {
            ringBuffer.publish(seq);
        }
    }
}
