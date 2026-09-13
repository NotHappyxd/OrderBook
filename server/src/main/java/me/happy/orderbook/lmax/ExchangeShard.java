package me.happy.orderbook.lmax;

import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Getter;
import me.happy.orderbook.checkpoint.Checkpoint;
import me.happy.orderbook.lmax.journal.Journal;
import me.happy.orderbook.lmax.journal.JournalHandler;
import me.happy.orderbook.lmax.journal.JournalReplayer;
import me.happy.orderbook.lmax.order.OrderEvent;
import me.happy.orderbook.lmax.order.OrderEventHandler;
import me.happy.orderbook.lmax.order.OrderPublisher;
import me.happy.orderbook.processor.OrderEventProcessor;
import me.happy.orderbook.server.NamedThreadFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

@Getter
public class ExchangeShard {

    private final int shardId;
    private Journal journal;
    private OrderEventHandler orderEventHandler;
    private OrderPublisher orderPublisher;

    public ExchangeShard(int shardId, int shardCount, int bufferSize,
                         Supplier<WaitStrategy> waitStrategyFactory) {
        this.shardId = shardId;

        try {
            Path path = Paths.get("logs", "shard-" + shardId + ".journal");
            Path checkpointPath = Paths.get("logs", "shard-" + shardId + ".checkpoint");

            this.journal = new Journal(path);

            Disruptor<OrderEvent> disruptor = new Disruptor<>(OrderEvent::new, bufferSize,
                    new NamedThreadFactory("orderbook"), ProducerType.MULTI,
                    waitStrategyFactory.get());
            this.orderEventHandler = new OrderEventHandler();

            JournalHandler journalHandler = new JournalHandler(journal);

            disruptor.handleEventsWith(journalHandler)
                    .then(this.orderEventHandler);

            this.orderPublisher = new OrderPublisher(disruptor.start(), shardId, shardCount);

            OrderEventProcessor processor = this.orderEventHandler.getProcessor();
            processor.setCheckpointPath(checkpointPath);
            processor.setOrderPublisher(this.orderPublisher);

            Checkpoint.CheckpointData checkpointData = Checkpoint.load(checkpointPath);
            processor.restoreFromCheckpoint(checkpointData);

            if (checkpointData != null) {
                this.orderPublisher.setSequence(checkpointData.orderIdSequence());
            }

            JournalReplayer journalReplayer = new JournalReplayer(Journal.LENGTH, this.orderEventHandler.getProcessor());
            long checkpointSequence = checkpointData == null ? -1 : checkpointData.watermarkSequence();

            if (journal.hasPendingRotation()) {
                journalReplayer.replay(journal.getPendingPath(), checkpointSequence);
            }

            journalReplayer.replay(path, checkpointSequence);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
