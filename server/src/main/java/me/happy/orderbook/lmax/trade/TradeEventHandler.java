package me.happy.orderbook.lmax.trade;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.happy.orderbook.lmax.metadata.MarketDataRegistry;
import me.happy.orderbook.order.Side;

import java.util.HashSet;
import java.util.Set;

public class TradeEventHandler implements EventHandler<TradeEvent> {
    private final MarketDataRegistry registry;
    private final Set<ChannelGroup> dirtyGroups = new HashSet<>();

    public TradeEventHandler(MarketDataRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onEvent(TradeEvent event, long sequence, boolean endOfBatch) throws Exception {
        ChannelGroup group = registry.groupFor(event.getTickerId());

        if (group != null && !group.isEmpty()) {
            group.write(encodeTrade(event));
            dirtyGroups.add(group);
        }

        if (endOfBatch) {
            dirtyGroups.forEach(ChannelGroup::flush);
            dirtyGroups.clear();
        }
    }

    @Override
    public void setSequenceCallback(Sequence sequenceCallback) {
        EventHandler.super.setSequenceCallback(sequenceCallback);
    }

    public ByteBuf encodeTrade(TradeEvent event) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(26);
        buf.writeByte(0x0D);
        buf.writeLong(event.getTickerId());
        buf.writeLong(event.getSequence());
        buf.writeInt(event.getPrice());
        buf.writeInt(event.getQuantity());
        buf.writeByte(event.getTakerSide() == Side.BUY ? 1 : 2);

        return buf;
    }
}
