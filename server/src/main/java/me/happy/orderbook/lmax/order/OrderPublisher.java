package me.happy.orderbook.lmax.order;

import com.lmax.disruptor.RingBuffer;
import io.netty.channel.Channel;
import me.happy.orderbook.order.Side;

public class OrderPublisher {

    private final RingBuffer<OrderEvent> ringBuffer;

    public OrderPublisher(RingBuffer<OrderEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void process(long ticker, Side side, int price, int quantity, long clientRequestId, boolean kill, Channel channel) {
        ringBuffer.publishEvent((event, sequence) -> {
            event.setCommand(OrderEventCommand.NEW);
            event.setTicker(ticker);
            event.setChannel(channel);
            event.setSide(side);
            event.setPrice(price);
            event.setQuantity(quantity);
            event.setClientRequestId(clientRequestId);
            event.setKill(kill);
        });
    }

    public void processModification(long ticker, long orderId, long secret, long clientSideRequestId, int quantity, int price, Channel channel) {
        ringBuffer.publishEvent((event, sequence) -> {
            event.setCommand(OrderEventCommand.MODIFY);
            event.setTicker(ticker);
            event.setOrderId(orderId);
            event.setSecret(secret);
            event.setClientRequestId(clientSideRequestId);
            event.setQuantity(quantity);
            event.setPrice(price);
            event.setChannel(channel);
        });
    }
    public void processSnapshot(long ticker, Channel channel) {
        ringBuffer.publishEvent((event, sequence) -> {
            event.setCommand(OrderEventCommand.SNAPSHOT);
            event.setTicker(ticker);
            event.setChannel(channel);
        });
    }

    public void processCancel(long orderId, long ticker, long secret, long clientRequestId, Channel channel) {
        ringBuffer.publishEvent((event, sequence) -> {
            event.setCommand(OrderEventCommand.CANCEL);
            event.setOrderId(orderId);
            event.setClientRequestId(clientRequestId);
            event.setTicker(ticker);
            event.setSecret(secret);

            event.setChannel(channel);
        });
    }
}
