package me.benchmark;

import me.happy.orderbook.lmax.AllocatorPool;
import me.happy.orderbook.order.Order;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class AllocatorPoolBenchmark {

    private AllocatorPool<Order> pool;

    @Setup(Level.Trial)
    public void setup() {
        this.pool = new AllocatorPool<>(1024, Order::new);
    }

    @Benchmark
    public void pooledBorrowRelease(Blackhole bh) {
        Order order = pool.borrow();
        order.reset();
        order.setId(1L);
        order.setQuantity(10);
        bh.consume(order);
        pool.release(order);
    }

    @Benchmark
    public void plainAllocation(Blackhole bh) {
        Order order = new Order();
        order.setId(1L);
        order.setQuantity(10);
        bh.consume(order);
    }
}
