package me.happy.orderbook.server;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger counter = new AtomicInteger(0);

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);

        thread.setName(prefix + "-" + counter.getAndIncrement());
        thread.setDaemon(false);
        thread.setPriority(Thread.NORM_PRIORITY);

        return thread;
    }
}
