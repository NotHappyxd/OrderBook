package me.happy.orderbook.lmax.outbound;

import com.lmax.disruptor.RingBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import me.happy.orderbook.order.OrderSnapshot;

public class OutboundPublisher {

    private final RingBuffer<OutboundEvent> ringBuffer;

    public OutboundPublisher(RingBuffer<OutboundEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void publish(Channel channel, ByteBuf byteBuf) {
        long sequence = ringBuffer.next();

        OutboundEvent event = ringBuffer.get(sequence);
        event.setChannel(channel);
        event.setByteBuf(byteBuf);
        event.setOrderSnapshot(null);

        ringBuffer.publish(sequence);
    }

    public void publish(Channel channel, OrderSnapshot orderSnapshot) {
        long sequence = ringBuffer.next();

        OutboundEvent event = ringBuffer.get(sequence);
        event.setChannel(channel);
        event.setOrderSnapshot(orderSnapshot);

        ringBuffer.publish(sequence);
    }
}
