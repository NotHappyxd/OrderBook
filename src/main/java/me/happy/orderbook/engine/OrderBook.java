package me.happy.orderbook.engine;

import lombok.Getter;
import me.happy.orderbook.lmax.OrderAllocator;
import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.Side;

import java.util.Comparator;
import java.util.Deque;
import java.util.TreeMap;

@Getter
public class OrderBook {

    private final TreeMap<Integer, PriceBucket> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, PriceBucket> asks = new TreeMap<>();
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
            bids.computeIfAbsent(order.getPrice(), _ -> new PriceBucket())
                    .addOrder(order);
        } else {
            asks.computeIfAbsent(order.getPrice(), _ -> new PriceBucket())
                    .addOrder(order);
        }
    }

    public void matchBuy(Order order) {
        while (order.getQuantity() > 0 && !asks.isEmpty()) {
            int bestPrice = asks.firstKey();

            if (bestPrice > order.getPrice()) break;

            PriceBucket priceBucket = asks.get(bestPrice);
            Deque<Order> sellOrders = priceBucket.getOrders();

            while (!sellOrders.isEmpty() && order.getQuantity() > 0) {
                Order sellOrder = sellOrders.peekFirst();

                int traded = Math.min(sellOrder.getQuantity(), order.getQuantity());

                sellOrder.setQuantity(sellOrder.getQuantity() - traded);
                order.setQuantity(order.getQuantity() - traded);
                priceBucket.setTotalQuantity(priceBucket.getTotalQuantity() - traded);

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

            PriceBucket priceBucket = bids.get(bestBuyPrice);
            Deque<Order> buyOrders = priceBucket.getOrders();

            while (!buyOrders.isEmpty() && order.getQuantity() > 0) {
                Order buyOrder = buyOrders.peekFirst();

                int traded = Math.min(buyOrder.getQuantity(), order.getQuantity());

                buyOrder.setQuantity(buyOrder.getQuantity() - traded);
                order.setQuantity(order.getQuantity() - traded);
                priceBucket.setTotalQuantity(priceBucket.getTotalQuantity() - traded);

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

    public void fillSnapshot(OrderSnapshot snapshot, int depth) {
        int i = 0;

        var iterator = asks.descendingMap().entrySet().iterator();

        while (iterator.hasNext() && i < depth) {
            var entry = iterator.next();

            snapshot.getAsks()[i] = entry.getKey();
            snapshot.getAsksQuantities()[i] = entry.getValue().getTotalQuantity();
            i++;
        }

        i = 0;
        iterator = bids.descendingMap().entrySet().iterator();
        while (iterator.hasNext() && i < depth) {
            var entry = iterator.next();

            snapshot.getBids()[i] = entry.getKey();
            snapshot.getBidsQuantities()[i] = entry.getValue().getTotalQuantity();
            i++;
        }

    }
}
