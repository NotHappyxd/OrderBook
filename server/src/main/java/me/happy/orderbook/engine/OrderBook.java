package me.happy.orderbook.engine;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import me.happy.orderbook.lmax.AllocatorPool;
import me.happy.orderbook.lmax.metadata.MarketDataPublisher;
import me.happy.orderbook.lmax.outbound.OutboundPublisher;
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
    private final MarketDataPublisher marketDataPublisher;
    private final OutboundPublisher outboundPublisher;
    private final AllocatorPool<Order> orderAllocator;
    private final AllocatorPool<PriceLevel> priceLevelAllocator;
    private final long ticker;

    @Setter
    private long marketDataSequence = 0;

    public OrderBook(TradePublisher tradePublisher, MarketDataPublisher marketDataPublisher, OutboundPublisher outboundPublisher, AllocatorPool<Order> orderAllocator, long ticker) {
        this.tradePublisher = tradePublisher;
        this.marketDataPublisher = marketDataPublisher;
        this.outboundPublisher = outboundPublisher;
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

        if (order.getQuantity() > 0 && !order.isKill()) {
            addToBook(order);
        }
    }

    public void addToBook(Order order) {
        PriceLevel priceLevel = getBook(order).computeIfAbsent(order.getPrice(), _ -> borrowPriceLevel());
        priceLevel.addOrder(order);

        orderMap.put(order.getId(), order);

        publishLevelUpdate(order.getSide(), order.getPrice(), priceLevel.getTotalQuantity());
    }

    public void matchBuy(Order order) {
        match(order, asks, Side.SELL, (bestPrice, incomingPrice) -> bestPrice <= incomingPrice);
    }

    public void matchSell(Order order) {
        match(order, bids, Side.BUY, (bestPrice, incomingPrice) -> bestPrice >= incomingPrice);
    }

    private void match(Order incomingOrder, TreeMap<Integer, PriceLevel> book, Side bookSide, BiPredicate<Integer, Integer> priceCrosses) {
        while (incomingOrder.getQuantity() > 0 && !book.isEmpty()) {
            Map.Entry<Integer, PriceLevel> bestEntry = book.firstEntry();
            int bestPrice = bestEntry.getKey();

            if (!priceCrosses.test(bestPrice, incomingOrder.getPrice())) {
                break;
            }

            PriceLevel priceLevel = bestEntry.getValue();
            matchPriceLevel(incomingOrder, priceLevel, bestPrice);

            boolean levelEmptied = priceLevel.getHead() == null;

            if (levelEmptied) {
                priceLevelAllocator.release(priceLevel);
                book.remove(bestPrice);
            }

            publishLevelUpdate(bookSide, bestPrice, levelEmptied ? 0 : priceLevel.getTotalQuantity());
        }
    }

    private void matchPriceLevel(Order incomingOrder, PriceLevel priceLevel, int bestPrice) {
        Order topOrder = priceLevel.getHead();

        while (topOrder != null && incomingOrder.getQuantity() > 0) {
            int traded = Math.min(topOrder.getQuantity(), incomingOrder.getQuantity());

            topOrder.setQuantity(topOrder.getQuantity() - traded);
            incomingOrder.setQuantity(incomingOrder.getQuantity() - traded);
            priceLevel.setTotalQuantity(priceLevel.getTotalQuantity() - traded);

            Order fullyFilledTopOrder = null;

            if (topOrder.getQuantity() == 0) {
                priceLevel.removeOrder(topOrder);
                orderMap.remove(topOrder.getId());
                fullyFilledTopOrder = topOrder; // release to the pool after we're done reading it below
            }

            sendExecutionReport(incomingOrder.getChannel(), incomingOrder, bestPrice, traded);
            sendExecutionReport(topOrder.getChannel(), topOrder, bestPrice, traded);

            this.tradePublisher.publishTrade(ticker, ++marketDataSequence, bestPrice, traded, incomingOrder.getSide());

            topOrder = topOrder.getNext();

            if (fullyFilledTopOrder != null) {
                orderAllocator.release(fullyFilledTopOrder);
            }
        }
    }

    private void sendExecutionReport(Channel channel, Order order, int price, int quantity) {
        if (channel == null) return;

        ByteBuf buf = channel.alloc().buffer(30);

        buf.writeByte(0x03); // private execution report
        buf.writeLong(ticker);
        buf.writeLong(order.getId());
        buf.writeInt(price);
        buf.writeInt(quantity); // quantity filled in this specific match
        buf.writeInt(order.getQuantity()); // this order's remaining resting quantity
        buf.writeByte(order.getSide() == Side.BUY ? 1 : 2);

        outboundPublisher.publish(channel, buf);
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

        TreeMap<Integer, PriceLevel> book = getBook(order);
        PriceLevel priceLevel = book.get(order.getPrice());
        priceLevel.removeOrder(order);

        boolean levelEmptied = priceLevel.getHead() == null;

        if (levelEmptied) {
            priceLevelAllocator.release(priceLevel);
            book.remove(order.getPrice());
        }

        publishLevelUpdate(order.getSide(), order.getPrice(), levelEmptied ? 0 : priceLevel.getTotalQuantity());

        orderMap.remove(orderId);
        orderAllocator.release(order);

        return true;
    }

    public void publishLevelUpdate(Side side, int price, int totalQuantity) {
        marketDataPublisher.publishLevelUpdate(ticker, ++marketDataSequence, side, price, totalQuantity);
    }

    public void reconcileLevel(Side side, int price) {
        TreeMap<Integer, PriceLevel> book = side == Side.BUY ? bids : asks;
        PriceLevel priceLevel = book.get(price);

        if (priceLevel == null) return;

        if (priceLevel.getHead() == null) {
            priceLevelAllocator.release(priceLevel);
            book.remove(price);
            publishLevelUpdate(side, price, 0);
        } else {
            publishLevelUpdate(side, price, priceLevel.getTotalQuantity());
        }
    }

    private PriceLevel borrowPriceLevel() {
        PriceLevel priceLevel = priceLevelAllocator.borrow();
        priceLevel.reset();

        return priceLevel;
    }

    public TreeMap<Integer, PriceLevel> getBook(Order order) {
        return order.getSide() == Side.BUY ? bids : asks;
    }

    public int totalQuantityAt(Side side, int price) {
        TreeMap<Integer, PriceLevel> book = side == Side.BUY ? bids : asks;
        PriceLevel priceLevel = book.get(price);

        return priceLevel == null ? 0 : priceLevel.getTotalQuantity();
    }
}