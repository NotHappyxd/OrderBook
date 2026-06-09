package me.happy.orderbook.engine;

import lombok.Getter;
import me.happy.orderbook.lmax.AllocatorPool;
import me.happy.orderbook.lmax.trade.TradePublisher;
import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.OrderSnapshot;
import me.happy.orderbook.order.PriceLevel;
import me.happy.orderbook.order.Side;

import java.util.*;
import java.util.function.BiPredicate;

@Getter
public class OrderBook {

    private final TreeMap<Integer, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, PriceLevel> asks = new TreeMap<>();
    private final Map<Long, Order> orderMap = new HashMap<>();

    private final TradePublisher tradePublisher;
    private final AllocatorPool<Order> orderAllocator;
    private final AllocatorPool<PriceLevel> priceLevelAllocator;
    private final long ticker;

    public OrderBook(TradePublisher tradePublisher, AllocatorPool<Order> orderAllocator, long ticker) {
        this.tradePublisher = tradePublisher;
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

        if (order.getQuantity() > 0) {
            addToBook(order);
        }
    }

    public void addToBook(Order order) {
        getBook(order).computeIfAbsent(order.getPrice(), _ -> borrowPriceLevel())
                .addOrder(order);

        orderMap.put(order.getId(), order);
    }

    public void matchBuy(Order order) {
        match(order, asks, (bestPrice, incomingPrice) -> bestPrice <= incomingPrice);
    }

    public void matchSell(Order order) {
        match(order, bids, (bestPrice, incomingPrice) -> bestPrice >= incomingPrice);
    }

    private void match(Order incomingOrder, TreeMap<Integer, PriceLevel> book, BiPredicate<Integer, Integer> priceCrosses) {
        while (incomingOrder.getQuantity() > 0 && !book.isEmpty()) {
            Map.Entry<Integer, PriceLevel> bestEntry = book.firstEntry();
            int bestPrice = bestEntry.getKey();

            if (!priceCrosses.test(bestPrice, incomingOrder.getPrice())) {
                break;
            }

            PriceLevel priceLevel = bestEntry.getValue();
            matchPriceLevel(incomingOrder, priceLevel, bestPrice);

            if (priceLevel.getHead() == null) {
                priceLevelAllocator.release(priceLevel);
                book.remove(bestPrice);
            }
        }
    }

    private void matchPriceLevel(Order incomingOrder, PriceLevel priceLevel, int bestPrice) {
        Order topOrder = priceLevel.getHead();

        while (topOrder != null && incomingOrder.getQuantity() > 0) {
            int traded = Math.min(topOrder.getQuantity(), incomingOrder.getQuantity());

            topOrder.setQuantity(topOrder.getQuantity() - traded);
            incomingOrder.setQuantity(incomingOrder.getQuantity() - traded);
            priceLevel.setTotalQuantity(priceLevel.getTotalQuantity() - traded);

            if (topOrder.getQuantity() == 0) {
                priceLevel.removeOrder(topOrder);
                orderMap.remove(topOrder.getId());
                orderAllocator.release(topOrder);
            }

            this.tradePublisher.publishFill(ticker, incomingOrder.getId(), topOrder.getId(), bestPrice, traded, incomingOrder.getSide());

            topOrder = topOrder.getNext();
        }
    }

    public void fillSnapshot(OrderSnapshot snapshot, int depth) {
        int i = 0;

        var iterator = asks.entrySet().iterator();

        while (iterator.hasNext() && i < depth) {
            var entry = iterator.next();

            snapshot.getAsks()[i] = entry.getKey();
            snapshot.getAsksQuantities()[i] = entry.getValue().getTotalQuantity();
            i++;
        }

        i = 0;
        iterator = bids.entrySet().iterator();
        while (iterator.hasNext() && i < depth) {
            var entry = iterator.next();

            snapshot.getBids()[i] = entry.getKey();
            snapshot.getBidsQuantities()[i] = entry.getValue().getTotalQuantity();
            i++;
        }
    }

    public boolean cancelOrder(long orderId) {
        Order order = orderMap.get(orderId);

        if (order == null) return false;

        getBook(order).get(order.getPrice()).removeOrder(order);

        orderMap.remove(orderId);
        orderAllocator.release(order);

        return true;
    }

    private PriceLevel borrowPriceLevel() {
        PriceLevel priceLevel = priceLevelAllocator.borrow();
        priceLevel.reset();

        return priceLevel;
    }

    public TreeMap<Integer, PriceLevel> getBook(Order order) {
        return order.getSide() == Side.BUY ? bids : asks;
    }
}
