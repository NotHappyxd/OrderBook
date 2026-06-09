package me.happy.orderbook.lmax.journal;

import com.lmax.disruptor.EventHandler;
import me.happy.orderbook.lmax.order.OrderEvent;

public class JournalHandler implements EventHandler<OrderEvent> {

    private final Journal journal;

    public JournalHandler(Journal journal) {
        this.journal = journal;
    }

    @Override
    public void onEvent(OrderEvent event, long l, boolean endOfBatch) throws Exception {
        journal.append(event);

        if (endOfBatch) {
            journal.force();
        }
    }
}
