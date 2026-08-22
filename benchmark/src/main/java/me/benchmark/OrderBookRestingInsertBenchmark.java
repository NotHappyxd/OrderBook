package me.benchmark;

import me.happy.orderbook.order.Side;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

// Test insertion speed/throughput

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class OrderBookRestingInsertBenchmark {

    private static final int BATCH = 2_000;
    private static final int BASE_PRICE = 1_000_000;

    @Param({"0", "10000", "100000"})
    public int preloadDepth;

    private BenchmarkFixture fixture;
    private long nextOrderId;
    private int nextAskPrice;

    @Setup(Level.Trial)
    public void setupTrial() {
        fixture = new BenchmarkFixture(1L, 1 << 14, 1 << 12);
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        fixture.resetBook();
        nextOrderId = 0;
        nextAskPrice = BASE_PRICE;

        for (int i = 0; i < preloadDepth; i++) {
            fixture.orderBook.process(fixture.newOrder(nextOrderId++, Side.SELL, nextAskPrice++, 10));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void insertRestingOrders(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            fixture.orderBook.process(fixture.newOrder(nextOrderId++, Side.SELL, nextAskPrice++, 10));
        }
        bh.consume(fixture.orderBook);
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() {
        fixture.shutdown();
    }
}
