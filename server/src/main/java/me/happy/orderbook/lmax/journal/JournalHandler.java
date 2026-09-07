package me.happy.orderbook.lmax.journal;

import com.lmax.disruptor.EventHandler;
import me.happy.orderbook.lmax.order.OrderEvent;
import me.happy.orderbook.lmax.order.OrderEventCommand;

public class JournalHandler implements EventHandler<OrderEvent> {

    private final Journal journal;

    public JournalHandler(Journal journal) {
        this.journal = journal;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) throws Exception {

        switch (event.getCommand()) {
            case JOURNAL_FORCE -> journal.forceToStorage();
            case CHECKPOINT -> {
                journal.rotate();
            }
            case CHECKPOINT_COMPLETE -> journal.markCheckpointComplete();
            default -> {
                journal.append(event);

                if (endOfBatch) {
                    journal.flush();
                }
            }
        }
    }

}
