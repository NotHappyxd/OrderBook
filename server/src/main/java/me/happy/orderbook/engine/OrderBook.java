package me.happy.orderbook.engine;

import lombok.Getter;
import me.happy.orderbook.lmax.AllocatorPool;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.OrderSnapshot;
import me.happy.orderbook.order.PriceLevel;
import me.happy.orderbook.order.Side;

import java.util.*;

@Getter
public class OrderBook {

    private final TreeMap<Integer, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, PriceLevel> asks = new TreeMap<>();
    private final Map<Long, Order> orderMap = new HashMap<>();
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
                        priceLevel.reset();

                        return priceLevel;
                    })
                    .addOrder(order);
        } else {
            asks.computeIfAbsent(order.getPrice(), _ -> {
                        PriceLevel priceLevel = priceLevelAllocator.borrow();
                        priceLevel.reset();

                        return priceLevel;
                    })
                    .addOrder(order);
        }

        orderMap.put(order.getId(), order);
    }

    public void matchBuy(Order order) {
        while (order.getQuantity() > 0 && !asks.isEmpty()) {
            int bestPrice = asks.firstKey();

            if (bestPrice > order.getPrice()) break;

            PriceLevel priceLevel = asks.get(bestPrice);
            Order sellOrder = priceLevel.getHead();

            while (sellOrder != null && order.getQuantity() > 0) {
                int traded = Math.min(sellOrder.getQuantity(), order.getQuantity());

                sellOrder.setQuantity(sellOrder.getQuantity() - traded);
                order.setQuantity(order.getQuantity() - traded);
                priceLevel.setTotalQuantity(priceLevel.getTotalQuantity() - traded);

                if (sellOrder.getQuantity() == 0) {
                    priceLevel.removeOrder(order);
                    orderMap.remove(sellOrder.getId());
                    orderAllocator.release(sellOrder);
                }

                Exchange.getInstance().publishFill(ticker, order.getId(), sellOrder.getId(), bestPrice, traded, sellOrder.getSide());

                sellOrder = sellOrder.getNext();
            }

            if (priceLevel.getHead() == null) {
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
            Order buyOrder = priceLevel.getHead();

            while (buyOrder != null && order.getQuantity() > 0) {
                int traded = Math.min(buyOrder.getQuantity(), order.getQuantity());

                buyOrder.setQuantity(buyOrder.getQuantity() - traded);
                order.setQuantity(order.getQuantity() - traded);
                priceLevel.setTotalQuantity(priceLevel.getTotalQuantity() - traded);

                if (buyOrder.getQuantity() == 0) {
                    priceLevel.removeOrder(order);
                    orderMap.remove(buyOrder.getId());
                    orderAllocator.release(buyOrder);
                }

                Exchange.getInstance().publishFill(ticker, order.getId(), buyOrder.getId(), bestBuyPrice, traded, buyOrder.getSide());

                buyOrder = buyOrder.getNext();
            }

            if (priceLevel.getHead() == null) {
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

    public boolean cancelOrder(long orderId) {
        Order order = orderMap.get(orderId);

        System.out.println("Cannot find order " + orderId);
        System.out.println(orderMap);
        if (order == null) return false;

        if (order.getSide() == Side.BUY) {
            bids.get(order.getPrice()).removeOrder(order);
        }else {
            asks.get(order.getPrice()).removeOrder(order);
        }

        orderAllocator.release(order);

        return true;
    }
}
