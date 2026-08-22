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
public class OrderBookCancelBenchmark {
    private static final int BATCH = 2_000;
    private static final int BASE_PRICE = 100;

    @Param({"5000", "50000"})
    public int bookDepth;

    private BenchmarkFixture fixture;

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

        for (int i = 0; i < bookDepth; i++) {
            fixture.orderBook.process(fixture.newOrder(i, Side.SELL, BASE_PRICE + i, 10));
        }
    }

    @Benchmark
    public void cancelRestingOrders(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            bh.consume(fixture.orderBook.cancelOrder(i));
        }
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() {
        fixture.shutdown();
    }
}
