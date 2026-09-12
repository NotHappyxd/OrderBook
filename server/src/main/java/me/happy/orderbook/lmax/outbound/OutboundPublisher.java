package me.happy.orderbook.lmax.outbound;

import com.lmax.disruptor.RingBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import me.happy.orderbook.order.OrderSnapshot;
import me.happy.orderbook.order.Side;

public class OutboundPublisher {

    private final RingBuffer<OutboundEvent> ringBuffer;

    public OutboundPublisher(RingBuffer<OutboundEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void publish(Channel channel, ByteBuf byteBuf) {
        long sequence = ringBuffer.next();

        try {
            OutboundEvent event = ringBuffer.get(sequence);
            event.setType(OutboundEvent.Type.BYTE_BUF);
            event.setChannel(channel);
            event.setByteBuf(byteBuf);
            event.setOrderSnapshot(null);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void publish(Channel channel, OrderSnapshot orderSnapshot) {
        long sequence = ringBuffer.next();

        try {
            OutboundEvent event = ringBuffer.get(sequence);
            event.setType(OutboundEvent.Type.SNAPSHOT);
            event.setChannel(channel);
            event.setByteBuf(null);
            event.setOrderSnapshot(orderSnapshot);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void publishExecutionReport(Channel channel, long ticker, long orderId, int price,
                                       int filledQuantity, int remainingQuantity, Side side) {
        long sequence = ringBuffer.next();

        try {
            OutboundEvent event = ringBuffer.get(sequence);
            event.setType(OutboundEvent.Type.EXECUTION_REPORT);
            event.setChannel(channel);
            event.setByteBuf(null);
            event.setOrderSnapshot(null);
            event.setTicker(ticker);
            event.setOrderId(orderId);
            event.setPrice(price);
            event.setFilledQuantity(filledQuantity);
            event.setRemainingQuantity(remainingQuantity);
            event.setSide(side);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
