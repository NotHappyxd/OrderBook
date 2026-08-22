package me.benchmark;

import me.happy.orderbook.order.Side;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class OrderBookMatchingManyLevelsBenchmark {
    private static final int BATCH = 2_000;
    private static final int BASE_PRICE = 100;

    @Param({"5000", "50000"})
    public int bookDepth;

    private BenchmarkFixture fixture;
    private long nextOrderId;

    @Setup(Level.Trial)
    public void setupTrial() {
        if (bookDepth < BATCH) {
            throw new IllegalStateException("bookDepth must be >= " + BATCH + " or the batch would run out of levels to drain");
        }
        fixture = new BenchmarkFixture(1L, 1 << 14, 1 << 18);
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        fixture.resetBook();
        nextOrderId = 0;

        for (int i = 0; i < bookDepth; i++) {
            fixture.orderBook.process(fixture.newOrder(nextOrderId++, Side.SELL, BASE_PRICE + i, 1));
        }
    }

    @Benchmark
    public void matchAndDrainLevels(Blackhole bh) {
        int marketablePrice = BASE_PRICE + bookDepth;

        for (int i = 0; i < BATCH; i++) {
            fixture.orderBook.process(fixture.newOrder(nextOrderId++, Side.BUY, marketablePrice, 1));
        }
        bh.consume(fixture.orderBook);
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() {
        fixture.shutdown();
    }
}
