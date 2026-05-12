package me.happy.orderbook.lmax;

import me.happy.orderbook.order.Order;

import java.util.ArrayDeque;

public class OrderAllocator {

    private final ArrayDeque<Order> orders;

    public OrderAllocator(int initialCapacity) {
        this.orders = new ArrayDeque<>(initialCapacity);

        for (int i = 0; i < initialCapacity; i++) {
            orders.add(new Order());
        }
    }

    public Order getOrder() {
        Order order = orders.pollFirst();

        if (order == null) {
            return new Order();
        }

        return order;
    }

    public void release(Order order) {
        order.reset();

        orders.offerFirst(order);
    }
}
