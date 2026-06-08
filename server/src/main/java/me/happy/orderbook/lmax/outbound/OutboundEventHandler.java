package me.happy.orderbook.lmax.outbound;

import com.lmax.disruptor.EventHandler;
import io.netty.channel.Channel;

import java.util.HashSet;
import java.util.Set;

public class OutboundEventHandler implements EventHandler<OutboundEvent> {

    private final Set<Channel> dirtyChannels = new HashSet<>();

    @Override
    public void onEvent(OutboundEvent event, long sequence, boolean endOfBatch) throws Exception {
        Channel channel = event.getChannel();

        channel.write(event.getOrderSnapshot() == null ? event.getByteBuf() : event.getOrderSnapshot());

        this.dirtyChannels.add(channel);

        if (endOfBatch) {
            this.dirtyChannels.forEach(Channel::flush);
            this.dirtyChannels.clear();
        }
    }
}
