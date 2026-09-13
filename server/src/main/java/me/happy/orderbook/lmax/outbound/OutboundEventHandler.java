package me.happy.orderbook.lmax.outbound;

import com.lmax.disruptor.EventHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.protocol.Protocol;

import java.util.HashSet;
import java.util.Set;

public class OutboundEventHandler implements EventHandler<OutboundEvent> {

    private final Set<Channel> dirtyChannels = new HashSet<>();

    @Override
    public void onEvent(OutboundEvent event, long sequence, boolean endOfBatch) {
        Channel channel = event.getChannel();
        boolean written = false;

        try {
            if (channel == null || !channel.isActive()) {
                releaseUnwrittenBuffer(event);
            } else if (!channel.isWritable()) {
                releaseUnwrittenBuffer(event);
                // Private responses cannot be dropped safely. Closing bounds the queued bytes and
                // lets the client reconnect/rebind instead of allowing an unbounded backlog.
                channel.close();
            } else {
                switch (event.getType()) {
                    case BYTE_BUF -> channel.write(event.getByteBuf(), channel.voidPromise());
                    case SNAPSHOT -> channel.write(event.getOrderSnapshot(), channel.voidPromise());
                    case EXECUTION_REPORT -> channel.write(encodeExecutionReport(channel, event), channel.voidPromise());
                }
                written = true;
            }
        } finally {
            event.setByteBuf(null);
            event.setOrderSnapshot(null);
            event.setChannel(null);
        }

        if (written) {
            this.dirtyChannels.add(channel);
        }

        if (endOfBatch) {
            this.dirtyChannels.forEach(Channel::flush);
            this.dirtyChannels.clear();
        }
    }

    private void releaseUnwrittenBuffer(OutboundEvent event) {
        if (event.getType() == OutboundEvent.Type.BYTE_BUF) {
            ReferenceCountUtil.safeRelease(event.getByteBuf());
        }
    }

    private ByteBuf encodeExecutionReport(Channel channel, OutboundEvent event) {
        ByteBuf buffer = channel.alloc().buffer(1 + Protocol.EXECUTION_REPORT_LENGTH);

        buffer.writeByte(Protocol.EXECUTION_REPORT);
        buffer.writeLong(event.getTicker());
        buffer.writeLong(event.getOrderId());
        buffer.writeInt(event.getPrice());
        buffer.writeInt(event.getFilledQuantity());
        buffer.writeInt(event.getRemainingQuantity());
        buffer.writeByte(event.getSide() == Side.BUY ? Protocol.BUY : Protocol.SELL);

        return buffer;
    }
}
