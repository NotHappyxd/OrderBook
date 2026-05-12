package me.happy.orderbook.engine;

import lombok.Getter;
import me.happy.orderbook.lmax.OrderAllocator;
import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.Side;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.TreeMap;

@Getter
public class OrderBook {

    private final TreeMap<Integer, Deque<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, Deque<Order>> asks = new TreeMap<>();
    private final OrderAllocator orderAllocator;

    public OrderBook(OrderAllocator orderAllocator) {
        this.orderAllocator = orderAllocator;
    }

    public void process(Order order) {
        if (order.getSide() == Side.BUY) {
            matchBuy(order);
        } else {
            matchSell(order);
        }

        if (order.getQuantity() > 0)
            addToBook(order);
    }

    public void addToBook(Order order) {
        if (order.getSide() == Side.BUY) {
            bids.computeIfAbsent(order.getPrice(), _ -> new ArrayDeque<>())
                    .addLast(order);
        } else {
            asks.computeIfAbsent(order.getPrice(), _ -> new ArrayDeque<>())
                    .addLast(order);
        }
    }

    public void matchBuy(Order order) {
        while (order.getQuantity() > 0 && !asks.isEmpty()) {
            int bestPrice = asks.firstKey();

            if (bestPrice > order.getPrice()) break;

            Deque<Order> sellOrders = asks.get(bestPrice);

            while (!sellOrders.isEmpty() && order.getQuantity() > 0) {
                Order sellOrder = sellOrders.peekFirst();

                int traded = Math.min(sellOrder.getQuantity(), order.getQuantity());

                sellOrder.setQuantity(sellOrder.getQuantity() - traded);
                order.setQuantity(order.getQuantity() - traded);

                if (sellOrder.getQuantity() == 0) {
                    sellOrders.pollFirst();
                    orderAllocator.release(sellOrder);
                }
            }

            if (sellOrders.isEmpty()) {
                asks.remove(bestPrice);
            }
        }
    }

    public void matchSell(Order order) {
        while (order.getQuantity() > 0 && !bids.isEmpty()) {
            int bestBuyPrice = bids.firstKey();

            if (bestBuyPrice < order.getPrice()) break;

            Deque<Order> buyOrders = bids.get(bestBuyPrice);

            while (!buyOrders.isEmpty() && order.getQuantity() > 0) {
                Order buyOrder = buyOrders.peekFirst();

                int traded = Math.min(buyOrder.getQuantity(), order.getQuantity());

                buyOrder.setQuantity(buyOrder.getQuantity() - traded);
                order.setQuantity(order.getQuantity() - traded);

                if (buyOrder.getQuantity() == 0) {
                    buyOrders.pollFirst();
                    orderAllocator.release(buyOrder);
                }
            }

            if (buyOrders.isEmpty()) {
                bids.remove(bestBuyPrice);
            }
        }
    }
}
