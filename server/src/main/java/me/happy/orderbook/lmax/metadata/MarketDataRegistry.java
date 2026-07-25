package me.happy.orderbook.lmax.metadata;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MarketDataRegistry {

    private final Map<Long, ChannelGroup> groupsByTicker = new ConcurrentHashMap<>();

    public void subscribe(long ticker, Channel channel) {
        groupsByTicker
                .computeIfAbsent(ticker, t -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE))
                .add(channel);
    }

    public void unsubscribe(long ticker, Channel channel) {
        ChannelGroup group = groupsByTicker.get(ticker);

        if (group != null) {
            group.remove(channel);
        }
    }

    public ChannelGroup groupFor(long ticker) {
        return groupsByTicker.get(ticker);
    }
}