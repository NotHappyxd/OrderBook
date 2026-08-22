package me.benchmark;

import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.PriceLevel;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class PriceLevelFifoBenchmark {

    private static final int BATCH = 10_000;

    @Param({"1", "1000"})
    public int steadyDepth;

    private PriceLevel priceLevel;
    private Order[] pool;
    private int poolIndex;

    @Setup(Level.Invocation)
    public void setupInvocation() {
        priceLevel = new PriceLevel();
        pool = new Order[BATCH + steadyDepth + 16];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = new Order();
            pool[i].setQuantity(1);
        }
        poolIndex = 0;

        for (int i = 0; i < steadyDepth; i++) {
            priceLevel.addOrder(pool[poolIndex++]);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void churnFifo(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            priceLevel.addOrder(pool[poolIndex++]);
            priceLevel.removeOrder(priceLevel.getHead());
        }
        bh.consume(priceLevel);
    }
}