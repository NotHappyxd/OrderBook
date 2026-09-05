package me.benchmark;

import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import me.happy.orderbook.engine.OrderBook;
import me.happy.orderbook.lmax.AllocatorPool;
import me.happy.orderbook.lmax.metadata.MarketDataEvent;
import me.happy.orderbook.lmax.metadata.MarketDataEventHandler;
import me.happy.orderbook.lmax.metadata.MarketDataPublisher;
import me.happy.orderbook.lmax.metadata.MarketDataRegistry;
import me.happy.orderbook.lmax.outbound.OutboundEvent;
import me.happy.orderbook.lmax.outbound.OutboundEventHandler;
import me.happy.orderbook.lmax.outbound.OutboundPublisher;
import me.happy.orderbook.lmax.trade.TradeEvent;
import me.happy.orderbook.lmax.trade.TradeEventHandler;
import me.happy.orderbook.lmax.trade.TradePublisher;
import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.Side;

public class BenchmarkFixture {

    public final OrderBook orderBook;
    public final AllocatorPool<Order> orderAllocator;

    private final Disruptor<TradeEvent> tradeDisruptor;
    private final Disruptor<MarketDataEvent> marketDataDisruptor;
    private final Disruptor<OutboundEvent> outboundDisruptor;

    public BenchmarkFixture(long ticker, int ringBufferSize, int orderPoolSize) {
        MarketDataRegistry registry = new MarketDataRegistry();

        this.tradeDisruptor = new Disruptor<>(TradeEvent::new, ringBufferSize, new BenchmarkThreadFactory("bench-trade"), ProducerType.MULTI, new YieldingWaitStrategy());
        TradeEventHandler tradeEventHandler = new TradeEventHandler(registry);
        tradeDisruptor.handleEventsWith(tradeEventHandler);
        TradePublisher tradePublisher = new TradePublisher(tradeEventHandler, tradeDisruptor.start());

        this.marketDataDisruptor = new Disruptor<>(MarketDataEvent::new, ringBufferSize, new BenchmarkThreadFactory("bench-marketdata"), ProducerType.MULTI, new YieldingWaitStrategy());
        marketDataDisruptor.handleEventsWith(new MarketDataEventHandler(registry));
        MarketDataPublisher marketDataPublisher = new MarketDataPublisher(marketDataDisruptor.start());

        this.outboundDisruptor = new Disruptor<>(OutboundEvent::new, ringBufferSize, new BenchmarkThreadFactory("bench-outbound"), ProducerType.MULTI, new YieldingWaitStrategy());
        outboundDisruptor.handleEventsWith(new OutboundEventHandler());
        OutboundPublisher outboundPublisher = new OutboundPublisher(outboundDisruptor.start());

        this.orderAllocator = new AllocatorPool<>(orderPoolSize, Order::new);
        this.orderBook = new OrderBook(tradePublisher, marketDataPublisher, outboundPublisher, orderAllocator, ticker);
    }

    public Order newOrder(long id, Side side, int price, int quantity) {
        return newOrder(id, side, false, price, quantity);
    }

    public Order newOrder(long id, Side side, boolean marketOrder, int price, int quantity) {
        Order order = orderAllocator.borrow();
        order.reset();
        order.setId(id);
        order.setSide(side);
        order.setMarketPrice(marketOrder);
        order.setPrice(price);
        order.setQuantity(quantity);
        order.setChannel(null);
        return order;
    }

    public void resetBook() {
        orderBook.getBids().clear();
        orderBook.getAsks().clear();
        orderBook.getOrderMap().clear();
        orderBook.setMarketDataSequence(0);
    }

    public void shutdown() {
        tradeDisruptor.shutdown();
        marketDataDisruptor.shutdown();
        outboundDisruptor.shutdown();
    }
}
