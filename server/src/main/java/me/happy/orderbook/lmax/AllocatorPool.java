package me.happy.orderbook.lmax;

import me.happy.orderbook.order.Order;

import java.util.ArrayDeque;
import java.util.function.Supplier;

public class AllocatorPool<T> {

    private final ArrayDeque<T> pool;
    private final Supplier<T> supplier;

    public AllocatorPool(int initialCapacity, Supplier<T> supplier) {
        this.pool = new ArrayDeque<>(initialCapacity);
        this.supplier = supplier;

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
        pool.offerFirst(t);
    }
}
