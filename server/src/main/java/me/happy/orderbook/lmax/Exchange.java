package me.happy.orderbook.lmax;

import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import lombok.Getter;
import me.happy.orderbook.lmax.metadata.MarketDataRegistry;
import me.happy.orderbook.lmax.metadata.PublicFeedPublisher;
import me.happy.orderbook.lmax.order.OrderPublisher;
import me.happy.orderbook.lmax.outbound.OutboundPublisher;
import me.happy.orderbook.server.NamedThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


@Getter
public class Exchange {

    private static Exchange INSTANCE;
    private final int shardCount;
    private final ExchangeShard[] shards;
    private final MarketDataRegistry marketDataRegistry;
    private final ScheduledExecutorService checkpointScheduler;

    public Exchange(int shardCount) {
        INSTANCE = this;
        Supplier<WaitStrategy> waitStrategyFactory =
                () -> new TimeoutBlockingWaitStrategy(10, TimeUnit.MILLISECONDS);
        this.shardCount = shardCount;
        int bufferSize = 16 * 1024;
        this.marketDataRegistry = new MarketDataRegistry();
        this.shards = new ExchangeShard[shardCount];

        this.checkpointScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("checkpoint-timer"));

        for (int i = 0; i < shardCount; i++) {
            try {
                int finalI = i;
                shards[i] = new ExchangeShard(i, shardCount, bufferSize,
                        waitStrategyFactory, marketDataRegistry);

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
        return shardFor(tickerId).getOrderPublisher();
    }

    public OutboundPublisher getOutboundPublisher(long tickerId) {
        return shardFor(tickerId).getOutboundPublisher();
    }

    public PublicFeedPublisher getPublicFeedPublisher(long tickerId) {
        return shardFor(tickerId).getPublicFeedPublisher();
    }

    private ExchangeShard shardFor(long tickerId) {
        return shards[Math.floorMod(tickerId, shardCount)];
    }

    public static Exchange getInstance() {
        return INSTANCE;
    }
}
