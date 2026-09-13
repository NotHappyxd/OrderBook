package me.happy.orderbook.lmax;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class AllocatorPoolTest {

    @Test
    public void startsLazyAndDoesNotRetainMoreThanItsConfiguredMaximum() {
        AtomicInteger allocations = new AtomicInteger();
        AllocatorPool<Object> pool = new AllocatorPool<>(0, 2, () -> {
            allocations.incrementAndGet();
            return new Object();
        });

        assertEquals(0, pool.retainedSize());

        Object first = pool.borrow();
        Object second = pool.borrow();
        Object third = pool.borrow();
        assertEquals(3, allocations.get());

        pool.release(first);
        pool.release(second);
        pool.release(third);

        assertEquals(2, pool.retainedSize());
        assertEquals(2, pool.maxRetained());
    }
}
