package me.happy.orderbook.lmax.outbound;

import com.lmax.disruptor.EventHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.protocol.Protocol;

import java.util.HashSet;
import java.util.Set;

public class OutboundEventHandler implements EventHandler<OutboundEvent> {

    private final Set<Channel> dirtyChannels = new HashSet<>();

    @Override
    public void onEvent(OutboundEvent event, long sequence, boolean endOfBatch) throws Exception {
        Channel channel = event.getChannel();

        try {
            switch (event.getType()) {
                case BYTE_BUF -> channel.write(event.getByteBuf());
                case SNAPSHOT -> channel.write(event.getOrderSnapshot());
                case EXECUTION_REPORT -> channel.write(encodeExecutionReport(channel, event));
            }
        } finally {
            event.setByteBuf(null);
            event.setOrderSnapshot(null);
            event.setChannel(null);
        }

        this.dirtyChannels.add(channel);

        if (endOfBatch) {
            this.dirtyChannels.forEach(Channel::flush);
            this.dirtyChannels.clear();
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
