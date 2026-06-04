package me.happy.orderbook.engine;

import lombok.Getter;
import me.happy.orderbook.lmax.AllocatorPool;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.OrderSnapshot;
import me.happy.orderbook.order.Side;

import java.util.Comparator;
import java.util.Deque;
import java.util.TreeMap;

@Getter
public class OrderBook {

    private final TreeMap<Integer, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, PriceLevel> asks = new TreeMap<>();
    private final AllocatorPool<Order> orderAllocator;
    private final AllocatorPool<PriceLevel> priceLevelAllocator;
    private final long ticker;

    public OrderBook(AllocatorPool<Order> orderAllocator, long ticker) {
        this.orderAllocator = orderAllocator;
        this.priceLevelAllocator = new AllocatorPool<>(1024, PriceLevel::new);
        this.ticker = ticker;
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
            bids.computeIfAbsent(order.getPrice(), _ -> {
                        PriceLevel priceLevel = priceLevelAllocator.borrow();
                        priceLevel.getOrders().clear();
                        priceLevel.setTotalQuantity(0);

                        return priceLevel;
                    })
                    .addOrder(order);
        } else {
            asks.computeIfAbsent(order.getPrice(), _ -> {
                        PriceLevel priceLevel = priceLevelAllocator.borrow();
                        priceLevel.getOrders().clear();
                        priceLevel.setTotalQuantity(0);

                        return priceLevel;
                    })
                    .addOrder(order);
        }
    }

    public void matchBuy(Order order) {
        while (order.getQuantity() > 0 && !asks.isEmpty()) {
            int bestPrice = asks.firstKey();

            if (bestPrice > order.getPrice()) break;

            PriceLevel priceLevel = asks.get(bestPrice);
            Deque<Order> sellOrders = priceLevel.getOrders();

            while (!sellOrders.isEmpty() && order.getQuantity() > 0) {
                Order sellOrder = sellOrders.peekFirst();

                int traded = Math.min(sellOrder.getQuantity(), order.getQuantity());

                sellOrder.setQuantity(sellOrder.getQuantity() - traded);
                order.setQuantity(order.getQuantity() - traded);
                priceLevel.setTotalQuantity(priceLevel.getTotalQuantity() - traded);

                if (sellOrder.getQuantity() == 0) {
                    sellOrders.pollFirst();
                    orderAllocator.release(sellOrder);
                }

                Exchange.getInstance().publishFill(ticker, order.getId(), sellOrder.getId(), bestPrice, traded, sellOrder.getSide());
            }

            if (sellOrders.isEmpty()) {
                priceLevelAllocator.release(priceLevel);
                asks.remove(bestPrice);
            }
        }
    }

    public void matchSell(Order order) {
        while (order.getQuantity() > 0 && !bids.isEmpty()) {
            int bestBuyPrice = bids.firstKey();

            if (bestBuyPrice < order.getPrice()) break;

            PriceLevel priceLevel = bids.get(bestBuyPrice);
            Deque<Order> buyOrders = priceLevel.getOrders();

            while (!buyOrders.isEmpty() && order.getQuantity() > 0) {
                Order buyOrder = buyOrders.peekFirst();

                int traded = Math.min(buyOrder.getQuantity(), order.getQuantity());

                buyOrder.setQuantity(buyOrder.getQuantity() - traded);
                order.setQuantity(order.getQuantity() - traded);
                priceLevel.setTotalQuantity(priceLevel.getTotalQuantity() - traded);

                if (buyOrder.getQuantity() == 0) {
                    buyOrders.pollFirst();
                    orderAllocator.release(buyOrder);
                }

                Exchange.getInstance().publishFill(ticker, order.getId(), buyOrder.getId(), bestBuyPrice, traded, buyOrder.getSide());
            }

            if (buyOrders.isEmpty()) {
                priceLevelAllocator.release(priceLevel);
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
