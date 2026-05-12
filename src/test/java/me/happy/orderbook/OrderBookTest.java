package me.happy.orderbook;

import junit.framework.TestCase;
import me.happy.orderbook.engine.OrderBook;
import me.happy.orderbook.lmax.OrderAllocator;
import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.Side;

public class OrderBookTest extends TestCase {

    public OrderBookTest() {
        super("Order Book Test");
    }

    public void testOrderBook() {
        OrderBook orderbook = new OrderBook(new OrderAllocator(50));

        // Test if added properly
        orderbook.process(new Order(1, Side.BUY, 100, 100));
        assertEquals(1, orderbook.getBids().size());
        orderbook.process(new Order(1, Side.SELL, 101, 100));
        assertEquals(1, orderbook.getAsks().size());

        // Test Matching
        orderbook.process(new Order(1, Side.SELL, 100, 50));
        assertEquals(1, orderbook.getBids().size());
        assertEquals(50, orderbook.getBids().firstEntry().getValue().getFirst().getQuantity());

        orderbook.process(new Order(1, Side.BUY, 101, 50));
        assertEquals(1, orderbook.getAsks().size());
        assertEquals(50, orderbook.getAsks().firstEntry().getValue().getFirst().getQuantity());

        // Test Multiple Price Levels
        orderbook.process(new Order(1, Side.BUY, 105, 100));
        assertEquals(2, orderbook.getBids().size());

        orderbook.process(new Order(1, Side.SELL, 105, 100));
        assertEquals(1, orderbook.getAsks().size());

    }
}
