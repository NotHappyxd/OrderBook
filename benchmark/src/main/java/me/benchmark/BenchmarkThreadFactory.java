package me.benchmark;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

// Use daemon threads so that we don't have to wait for each Disruptor consumer thread to die
public class BenchmarkThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger counter = new AtomicInteger();

    public BenchmarkThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, prefix + "-" + counter.getAndIncrement());
        return thread;
    }
}
