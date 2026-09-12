package me.happy.orderbook.lmax;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Getter;
import me.happy.orderbook.lmax.metadata.MarketDataRegistry;
import me.happy.orderbook.lmax.metadata.PublicFeedEvent;
import me.happy.orderbook.lmax.metadata.PublicFeedHandler;
import me.happy.orderbook.lmax.metadata.PublicFeedPublisher;
import me.happy.orderbook.lmax.order.OrderPublisher;
import me.happy.orderbook.lmax.outbound.OutboundEvent;
import me.happy.orderbook.lmax.outbound.OutboundEventHandler;
import me.happy.orderbook.lmax.outbound.OutboundPublisher;
import me.happy.orderbook.server.NamedThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Getter
public class Exchange {

    private static Exchange INSTANCE;
    private final int shardCount;
    private final ExchangeShard[] shards;
    private final OutboundPublisher outboundPublisher;
    private final MarketDataRegistry marketDataRegistry;
    private final PublicFeedPublisher publicFeedPublisher;
    private final ScheduledExecutorService checkpointScheduler;

    public Exchange(int shardCount) {
        INSTANCE = this;
        WaitStrategy strategy = new TimeoutBlockingWaitStrategy(10, TimeUnit.MILLISECONDS);
        this.shardCount = shardCount;
        int bufferSize = 16 * 1024;
        this.marketDataRegistry = new MarketDataRegistry();
        this.shards = new ExchangeShard[shardCount];

        Disruptor<PublicFeedEvent> publicFeedDisruptor = new Disruptor<>(PublicFeedEvent::new, bufferSize, new NamedThreadFactory("trade"),
                ProducerType.MULTI, strategy);
        PublicFeedHandler publicFeedHandler = new PublicFeedHandler(marketDataRegistry);
        publicFeedDisruptor.handleEventsWith(publicFeedHandler);
        this.publicFeedPublisher = new PublicFeedPublisher(publicFeedDisruptor.start());

        Disruptor<OutboundEvent> outboundEventDisruptor = new Disruptor<>(OutboundEvent::new, bufferSize, new NamedThreadFactory("outbound"),
                ProducerType.MULTI, strategy);
        outboundEventDisruptor.handleEventsWith(new OutboundEventHandler());
        this.outboundPublisher = new OutboundPublisher(outboundEventDisruptor.start());

        this.checkpointScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("checkpoint-timer"));

        for (int i = 0; i < shardCount; i++) {
            try {
                int finalI = i;
                shards[i] = new ExchangeShard(i, shardCount, bufferSize, strategy);

                checkpointScheduler.scheduleAtFixedRate(
                        () -> shards[finalI].getOrderPublisher().processCheckpoint(), 60, 60, TimeUnit.SECONDS
                );

                checkpointScheduler.scheduleAtFixedRate(
                        () -> shards[finalI].getOrderPublisher().processOSWrite(), 10, 10, TimeUnit.MILLISECONDS
                );

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("Started all shards");
    }

    public OrderPublisher getPublisher(long tickerId) {
        int shard = Math.floorMod(tickerId, shardCount);

        return shards[shard].getOrderPublisher();
    }

    public static Exchange getInstance() {
        return INSTANCE;
    }
}
