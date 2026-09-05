package me.benchmark;

import me.happy.orderbook.order.Side;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayDeque;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(3)
public class OrderBookMixedWorkloadBenchmark {

    private static final int BATCH = 2_000;
    private static final int BASE_PRICE = 10_000;
    private static final int PRICE_SPREAD = 200;

    private BenchmarkFixture fixture;
    private SplittableRandom random;
    private long nextOrderId;
    private final ArrayDeque<Long> restingIds = new ArrayDeque<>();
    private static final int MAX_TRACKED_RESTING_IDS = 2_000_000;

    @Setup(Level.Trial)
    public void setupTrial() {
        fixture = new BenchmarkFixture(1L, 1 << 14, 1 << 23);
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        fixture.resetBook();
        restingIds.clear();
        nextOrderId = 0;
        random = new SplittableRandom(42);

        for (int i = 0; i < 20_000; i++) {
            Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
            int price = side == Side.BUY ? BASE_PRICE - random.nextInt(PRICE_SPREAD) : BASE_PRICE + random.nextInt(PRICE_SPREAD);
            long id = nextOrderId++;
            boolean marketOrder = random.nextInt(1, 10) < 2;
            fixture.orderBook.process(fixture.newOrder(id, side, marketOrder, price, 10));

            if (restingIds.size() < MAX_TRACKED_RESTING_IDS)
                restingIds.addLast(id);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void mixedTraffic(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            int roll = random.nextInt(100);

            if (roll < 70) {
                Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
                int price = side == Side.BUY
                        ? BASE_PRICE - PRICE_SPREAD - random.nextInt(PRICE_SPREAD)
                        : BASE_PRICE + PRICE_SPREAD + random.nextInt(PRICE_SPREAD);
                long id = nextOrderId++;
                boolean marketOrder = random.nextInt(1, 10) < 2;

                fixture.orderBook.process(fixture.newOrder(id, side, marketOrder, price, 10));
                if (restingIds.size() < MAX_TRACKED_RESTING_IDS) restingIds.addLast(id);
            } else if (roll < 90) {
                Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
                int price = side == Side.BUY ? BASE_PRICE + PRICE_SPREAD : BASE_PRICE - PRICE_SPREAD;
                fixture.orderBook.process(fixture.newOrder(nextOrderId++, side, price, 5));
            } else {
                Long id = restingIds.pollFirst();
                if (id != null) {
                    fixture.orderBook.cancelOrder(id);
                }
            }
        }
        bh.consume(fixture.orderBook);
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() {
        fixture.shutdown();
    }
}