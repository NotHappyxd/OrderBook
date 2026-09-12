package me.happy.orderbook.lmax.outbound;

import com.lmax.disruptor.RingBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import me.happy.orderbook.order.OrderSnapshot;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.protocol.Protocol;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OutboundEventHandlerTest {

    @Test
    public void encodesExecutionReportsOnTheOutboundThread() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        OutboundEvent event = new OutboundEvent();
        event.setType(OutboundEvent.Type.EXECUTION_REPORT);
        event.setChannel(channel);
        event.setTicker(42L);
        event.setOrderId(7L);
        event.setPrice(101);
        event.setFilledQuantity(3);
        event.setRemainingQuantity(4);
        event.setSide(Side.BUY);

        new OutboundEventHandler().onEvent(event, 0, true);

        ByteBuf encoded = channel.readOutbound();
        try {
            assertEquals(Protocol.EXECUTION_REPORT, encoded.readByte());
            assertEquals(42L, encoded.readLong());
            assertEquals(7L, encoded.readLong());
            assertEquals(101, encoded.readInt());
            assertEquals(3, encoded.readInt());
            assertEquals(4, encoded.readInt());
            assertEquals(Protocol.BUY, encoded.readByte());
        } finally {
            encoded.release();
            channel.finishAndReleaseAll();
        }

        assertNull(event.getChannel());
        assertNull(event.getByteBuf());
        assertNull(event.getOrderSnapshot());
    }

    @Test
    public void publishingSnapshotClearsThePreviousBufferReference() {
        RingBuffer<OutboundEvent> ringBuffer = RingBuffer.createSingleProducer(OutboundEvent::new, 2);
        OutboundPublisher publisher = new OutboundPublisher(ringBuffer);
        ByteBuf staleBuffer = Unpooled.buffer();
        ringBuffer.get(0).setByteBuf(staleBuffer);

        publisher.publish(null, new OrderSnapshot(42L, 7L));

        OutboundEvent event = ringBuffer.get(0);
        assertEquals(OutboundEvent.Type.SNAPSHOT, event.getType());
        assertNull(event.getByteBuf());
        staleBuffer.release();
    }
}
