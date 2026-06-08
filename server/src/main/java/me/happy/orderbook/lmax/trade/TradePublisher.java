package me.happy.orderbook.lmax.trade;

import com.lmax.disruptor.RingBuffer;
import io.netty.channel.Channel;
import lombok.Getter;
import me.happy.orderbook.lmax.order.OrderEvent;
import me.happy.orderbook.lmax.order.OrderEventCommand;
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
        ringBuffer.publishEvent((event, sequence) -> {
            event.setTickerId(tickerId);
            event.setSequence(sequence);
            event.setOrderId(orderId);
            event.setTakerId(takerId);
            event.setPrice(price);
            event.setQuantity(quantity);
            event.setTakerSide(takerSide);
        });
    }
}
