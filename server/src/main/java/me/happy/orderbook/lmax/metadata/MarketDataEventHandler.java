package me.happy.orderbook.lmax.metadata;

import com.lmax.disruptor.EventHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.group.ChannelGroup;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.protocol.Protocol;

import java.util.HashSet;
import java.util.Set;

public class MarketDataEventHandler implements EventHandler<MarketDataEvent> {

    private final MarketDataRegistry registry;
    private final Set<ChannelGroup> dirtyGroups = new HashSet<>();

    public MarketDataEventHandler(MarketDataRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onEvent(MarketDataEvent event, long sequence, boolean endOfBatch) throws Exception {
        ChannelGroup group = registry.groupFor(event.getTicker());

        if (group != null && !group.isEmpty()) {
            ByteBuf byteBuf = encode(event);

            group.write(byteBuf);
            dirtyGroups.add(group);
        }

        if (endOfBatch) {
            dirtyGroups.forEach(ChannelGroup::flush);
            dirtyGroups.clear();
        }
    }

    private ByteBuf encode(MarketDataEvent event) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(1 + Protocol.MARKET_DATA_DELTA_LENGTH);

        buf.writeByte(Protocol.MARKET_DATA_DELTA);
        buf.writeLong(event.getTicker());
        buf.writeLong(event.getSequence());
        buf.writeByte(event.getSide() == Side.BUY ? Protocol.BUY : Protocol.SELL);
        buf.writeInt(event.getPrice());
        buf.writeInt(event.getTotalQuantity()); // 0 == level removed

        return buf;
    }
}
