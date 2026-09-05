package me.happy.orderbook.lmax;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Getter;
import me.happy.orderbook.checkpoint.Checkpoint;
import me.happy.orderbook.lmax.journal.Journal;
import me.happy.orderbook.lmax.journal.JournalHandler;
import me.happy.orderbook.lmax.journal.JournalReplayer;
import me.happy.orderbook.lmax.metadata.MarketDataEvent;
import me.happy.orderbook.lmax.metadata.MarketDataEventHandler;
import me.happy.orderbook.lmax.metadata.MarketDataPublisher;
import me.happy.orderbook.lmax.metadata.MarketDataRegistry;
import me.happy.orderbook.lmax.order.OrderEvent;
import me.happy.orderbook.lmax.order.OrderPublisher;
import me.happy.orderbook.lmax.trade.TradeEvent;
import me.happy.orderbook.lmax.order.OrderEventHandler;
import me.happy.orderbook.lmax.trade.TradeEventHandler;
import me.happy.orderbook.lmax.outbound.OutboundEvent;
import me.happy.orderbook.lmax.outbound.OutboundEventHandler;
import me.happy.orderbook.lmax.outbound.OutboundPublisher;
import me.happy.orderbook.lmax.trade.TradePublisher;
import me.happy.orderbook.processor.OrderEventProcessor;
import me.happy.orderbook.server.NamedThreadFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Getter
public class Exchange {

    private static Exchange INSTANCE;
    private final int shardCount;
    private final OrderPublisher[] publishers;
    private final OrderEventHandler[] handlers;
    private final TradePublisher tradePublisher;
    private final OutboundPublisher outboundPublisher;
    private final MarketDataRegistry marketDataRegistry;
    private final MarketDataPublisher marketDataPublisher;
    private final ScheduledExecutorService checkpointScheduler;

    public Exchange(int shardCount) {
        INSTANCE = this;
        WaitStrategy strategy = new TimeoutBlockingWaitStrategy(10, TimeUnit.MILLISECONDS);
        this.shardCount = shardCount;
        this.publishers = new OrderPublisher[shardCount];
        this.handlers = new OrderEventHandler[shardCount];
        int bufferSize = 16 * 1024;
        this.marketDataRegistry = new MarketDataRegistry();

        Disruptor<TradeEvent> tradeEventDisruptor = new Disruptor<>(TradeEvent::new, bufferSize, new NamedThreadFactory("trade"),
                ProducerType.MULTI, strategy);
        TradeEventHandler tradeEventHandler = new TradeEventHandler(marketDataRegistry);
        tradeEventDisruptor.handleEventsWith(tradeEventHandler);
        this.tradePublisher = new TradePublisher(tradeEventHandler, tradeEventDisruptor.start());

        Disruptor<OutboundEvent> outboundEventDisruptor = new Disruptor<>(OutboundEvent::new, bufferSize, new NamedThreadFactory("outbound"),
                ProducerType.MULTI, strategy);
        outboundEventDisruptor.handleEventsWith(new OutboundEventHandler());
        this.outboundPublisher = new OutboundPublisher(outboundEventDisruptor.start());

        Disruptor<MarketDataEvent> marketDataDisruptor = new Disruptor<>(MarketDataEvent::new, bufferSize, new NamedThreadFactory("marketdata"),
                ProducerType.MULTI, strategy);
        marketDataDisruptor.handleEventsWith(new MarketDataEventHandler(marketDataRegistry));
        this.marketDataPublisher = new MarketDataPublisher(marketDataDisruptor.start());

        this.checkpointScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("checkpoint-timer"));

        for (int i = 0; i < shardCount; i++) {
            try {
                Path path = Paths.get("logs", "shard-" + i + ".journal");
                Path checkpointPath = Paths.get("logs", "shard-" + i + ".checkpoint");

                Journal journal = new Journal(path);
                JournalHandler journalHandler = new JournalHandler(journal);

                Disruptor<OrderEvent> disruptor = new Disruptor<>(OrderEvent::new, bufferSize, new NamedThreadFactory("orderbook"),
                        ProducerType.MULTI, strategy);
                this.handlers[i] = new OrderEventHandler();
                disruptor.handleEventsWith(journalHandler)
                        .then(this.handlers[i]);

                publishers[i] = new OrderPublisher(disruptor.start(), i, shardCount);

                OrderEventProcessor processor = handlers[i].getProcessor();
                processor.setJournal(journal);
                processor.setCheckpointPath(checkpointPath);
                processor.setOrderPublisher(publishers[i]);

                Checkpoint.CheckpointData checkpointData = Checkpoint.load(checkpointPath);
                processor.restoreFromCheckpoint(checkpointData);

                if (checkpointData != null) {
                    publishers[i].setSequence(checkpointData.orderIdSequence());
                }

                JournalReplayer journalReplayer = new JournalReplayer(Journal.LENGTH, handlers[i].getProcessor());

                if (journal.hasPendingRotation()) {
                    journalReplayer.replay(journal.getPendingPath());
                }

                journalReplayer.replay(path);

                checkpointScheduler.scheduleAtFixedRate(
                        publishers[i]::processCheckpoint, 60, 60, TimeUnit.SECONDS
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public OrderPublisher getPublisher(long tickerId) {
        int shard = Math.toIntExact(Math.abs(tickerId % shardCount));

        return publishers[shard];
    }

    public static Exchange getInstance() {
        return INSTANCE;
    }
}