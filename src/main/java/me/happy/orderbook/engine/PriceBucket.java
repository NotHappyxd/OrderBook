package me.happy.orderbook.engine;

import lombok.Data;
import me.happy.orderbook.order.Order;

import java.util.ArrayDeque;
import java.util.Deque;

@Data
public class PriceBucket {

    private Deque<Order> orders = new ArrayDeque<>();
    private int totalQuantity = 0;

    public void addOrder(Order order) {
        orders.addLast(order);
        totalQuantity += order.getPrice();
    }
}
