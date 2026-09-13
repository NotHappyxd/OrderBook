package me.happy.orderbook.lmax.metadata;

import io.netty.channel.embedded.EmbeddedChannel;
import me.happy.orderbook.order.Side;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class PublicFeedHandlerTest {

    @Test
    public void disconnectsNonWritableSubscribersInsteadOfQueuingMoreData() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel() {
            @Override
            public boolean isWritable() {
                return false;
            }
        };
        MarketDataRegistry registry = new MarketDataRegistry();
        registry.subscribe(42L, channel);

        PublicFeedEvent event = new PublicFeedEvent();
        event.setType(PublicFeedEvent.PublicFeedEventType.LEVEL_DATA);
        event.setTicker(42L);
        event.setMarketDataSequence(1L);
        event.setSide(Side.BUY);
        event.setPrice(100);
        event.setTotalQuantity(10);

        new PublicFeedHandler(registry).onEvent(event, 0, true);

        assertFalse(channel.isOpen());
    }
}
