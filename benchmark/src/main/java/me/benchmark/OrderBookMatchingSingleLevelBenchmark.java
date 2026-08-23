package me.benchmark;

import me.happy.orderbook.order.Side;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

// Test matching speed

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class OrderBookMatchingSingleLevelBenchmark {

    private static final int BATCH = 2_000;
    private static final int RESTING_PRICE = 100;

    @Param({"5000", "50000"})
    public int queueDepth;

    private BenchmarkFixture fixture;
    private long nextOrderId;

    @Setup(Level.Trial)
    public void setupTrial() {
        if (queueDepth < BATCH) {
            throw new IllegalStateException("queueDepth must be >= " + BATCH + " or the batch would run out of resting liquidity");
        }

        fixture = new BenchmarkFixture(1L, 1 << 14, 1 << 18);
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        fixture.resetBook();
        nextOrderId = 0;

        for (int i = 0; i < queueDepth; i++) {
            fixture.orderBook.process(fixture.newOrder(nextOrderId++, Side.SELL, RESTING_PRICE, 1));
        }
    }

    @Benchmark
    public void matchAgainstDeepQueue(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            fixture.orderBook.process(fixture.newOrder(nextOrderId++, Side.BUY, RESTING_PRICE, 1));
        }

        bh.consume(fixture.orderBook);
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() {
        fixture.shutdown();
    }
}
