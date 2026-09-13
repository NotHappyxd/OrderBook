package me.happy.orderbook.lmax.metadata;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatcher;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.protocol.Protocol;

import java.util.HashSet;
import java.util.Set;

public class PublicFeedHandler implements EventHandler<PublicFeedEvent> {

    private static final ChannelMatcher WRITABLE_CHANNELS = channel -> {
        if (!channel.isActive()) {
            return false;
        }
        if (!channel.isWritable()) {
            // Public deltas cannot be allowed to queue without a bound. A reconnecting client can
            // obtain a fresh snapshot before subscribing again.
            channel.close();
            return false;
        }
        return true;
    };

    private final MarketDataRegistry registry;
    private final Set<ChannelGroup> dirtyGroups = new HashSet<>();

    public PublicFeedHandler(MarketDataRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onEvent(PublicFeedEvent event, long sequence, boolean endOfBatch) throws Exception {
        ChannelGroup group = registry.groupFor(event.getTicker());

        if (group != null && !group.isEmpty()) {
            switch (event.getType()) {
                case LEVEL_DATA -> processLevelData(event, group);
                case TRADE_PRINT -> processTradePrint(event, group);
            }
        }

        if (endOfBatch && !dirtyGroups.isEmpty()) {
            dirtyGroups.forEach(ChannelGroup::flush);
            dirtyGroups.clear();
        }
    }

    @Override
    public void setSequenceCallback(Sequence sequenceCallback) {
        EventHandler.super.setSequenceCallback(sequenceCallback);
    }

    private void processLevelData(PublicFeedEvent event, ChannelGroup group) {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(1 + Protocol.MARKET_DATA_DELTA_LENGTH);

        byteBuf.writeByte(Protocol.MARKET_DATA_DELTA);
        byteBuf.writeLong(event.getTicker());
        byteBuf.writeLong(event.getMarketDataSequence());
        byteBuf.writeByte(event.getSide() == Side.BUY ? Protocol.BUY : Protocol.SELL);
        byteBuf.writeInt(event.getPrice());
        byteBuf.writeInt(event.getTotalQuantity()); // 0 == level removed

        sendBuffer(group, byteBuf);
    }

    private void processTradePrint(PublicFeedEvent event, ChannelGroup group) {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(1 + Protocol.TRADE_PRINT_LENGTH);

        byteBuf.writeByte(Protocol.TRADE_PRINT);
        byteBuf.writeLong(event.getTicker());
        byteBuf.writeLong(event.getMarketDataSequence());
        byteBuf.writeInt(event.getPrice());
        byteBuf.writeInt(event.getTradedQuantity());
        byteBuf.writeByte(event.getSide() == Side.BUY ? Protocol.BUY : Protocol.SELL);

        sendBuffer(group, byteBuf);
    }

    private void sendBuffer(ChannelGroup group, ByteBuf byteBuf) {
        // Feed writes are fire-and-forget. Avoid allocating a ChannelGroupFuture and one promise
        // per subscriber for every market-data message.
        group.write(byteBuf, WRITABLE_CHANNELS, true);
        dirtyGroups.add(group);
    }
}
