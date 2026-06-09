package me.happy.orderbook.lmax;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import lombok.Getter;
import me.happy.orderbook.lmax.journal.Journal;
import me.happy.orderbook.lmax.journal.JournalHandler;
import me.happy.orderbook.lmax.journal.JournalReplayer;
import me.happy.orderbook.lmax.order.OrderEvent;
import me.happy.orderbook.lmax.order.OrderPublisher;
import me.happy.orderbook.lmax.trade.TradeEvent;
import me.happy.orderbook.lmax.order.OrderEventHandler;
import me.happy.orderbook.lmax.trade.TradeEventHandler;
import me.happy.orderbook.lmax.outbound.OutboundEvent;
import me.happy.orderbook.lmax.outbound.OutboundEventHandler;
import me.happy.orderbook.lmax.outbound.OutboundPublisher;
import me.happy.orderbook.lmax.trade.TradePublisher;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.server.NamedThreadFactory;

import java.nio.file.Path;
import java.nio.file.Paths;


@Getter
public class Exchange {

    private static Exchange INSTANCE;
    private final int shardCount;
    private final OrderPublisher[] publishers;
    private final OrderEventHandler[] handlers;
    private final TradePublisher tradePublisher;
    private final OutboundPublisher outboundPublisher;

    public Exchange(int shardCount) {
        INSTANCE = this;
        this.shardCount = shardCount;
        this.publishers = new OrderPublisher[shardCount];
        this.handlers = new OrderEventHandler[shardCount];
        int bufferSize = 1024;

        Disruptor<TradeEvent> tradeEventDisruptor = new Disruptor<>(TradeEvent::new, bufferSize, new NamedThreadFactory("trade"));
        TradeEventHandler tradeEventHandler = new TradeEventHandler();
        tradeEventDisruptor.handleEventsWith(tradeEventHandler);
        this.tradePublisher = new TradePublisher(tradeEventHandler, tradeEventDisruptor.start());

        Disruptor<OutboundEvent> outboundEventDisruptor = new Disruptor<>(OutboundEvent::new, bufferSize, new NamedThreadFactory("outbound"));
        outboundEventDisruptor.handleEventsWith(new OutboundEventHandler());
        this.outboundPublisher = new OutboundPublisher(outboundEventDisruptor.start());

        for (int i = 0; i < shardCount; i++) {
            try {
                Path path = Paths.get("logs", "shard-" + i + ".journal");
                Journal journal = new Journal(path);
                JournalHandler journalHandler = new JournalHandler(journal);

                Disruptor<OrderEvent> disruptor = new Disruptor<>(OrderEvent::new, bufferSize, new NamedThreadFactory("orderbook"));
                this.handlers[i] = new OrderEventHandler();
                disruptor.handleEventsWith(journalHandler)
                        .then(this.handlers[i]);

                publishers[i] = new OrderPublisher(disruptor.start(), i, shardCount);

                JournalReplayer journalReplayer = new JournalReplayer(Journal.LENGTH, handlers[i].getProcessor());
                journalReplayer.replay(path);
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