package me.happy.orderbook.lmax;

import me.happy.orderbook.order.Order;

import java.util.ArrayDeque;
import java.util.function.Supplier;

public class AllocatorPool<T> {

    private final ArrayDeque<T> pool;
    private final Supplier<T> supplier;
    private final int maxRetained;

    public AllocatorPool(int initialCapacity, Supplier<T> supplier) {
        this(initialCapacity, Integer.MAX_VALUE, supplier);
    }

    public AllocatorPool(int initialCapacity, int maxRetained, Supplier<T> supplier) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be >= 0");
        }
        if (maxRetained < initialCapacity) {
            throw new IllegalArgumentException("maxRetained must be >= initialCapacity");
        }

        this.pool = new ArrayDeque<>(initialCapacity);
        this.supplier = supplier;
        this.maxRetained = maxRetained;

        for (int i = 0; i < initialCapacity; i++) {
            pool.add(supplier.get());
        }
    }

    public T borrow() {
        T t = pool.pollFirst();

        if (t == null) {
            return supplier.get();
        }

        return t;
    }

    public void release(T t) {
        if (pool.size() < maxRetained) {
            pool.offerFirst(t);
        }
    }

    public int retainedSize() {
        return pool.size();
    }

    public int maxRetained() {
        return maxRetained;
    }
}
