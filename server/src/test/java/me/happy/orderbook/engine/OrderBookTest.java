package me.happy.orderbook.engine;

import com.lmax.disruptor.RingBuffer;
import me.happy.orderbook.lmax.AllocatorPool;
import me.happy.orderbook.lmax.metadata.PublicFeedEvent;
import me.happy.orderbook.lmax.metadata.PublicFeedPublisher;
import me.happy.orderbook.lmax.outbound.OutboundEvent;
import me.happy.orderbook.lmax.outbound.OutboundPublisher;
import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.OrderSnapshot;
import me.happy.orderbook.order.PriceLevel;
import me.happy.orderbook.order.Side;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class OrderBookTest {

    private static final long TICKER = 42L;

    private OrderBook book;

    @Before
    public void setUp() {
        RingBuffer<PublicFeedEvent> publicFeedEventRingBuffer = RingBuffer.createSingleProducer(PublicFeedEvent::new, 16);
        RingBuffer<OutboundEvent> outboundEvents = RingBuffer.createSingleProducer(OutboundEvent::new, 16);

        book = new OrderBook(
                new PublicFeedPublisher(publicFeedEventRingBuffer),
                new OutboundPublisher(outboundEvents),
                new AllocatorPool<>(16, Order::new),
                TICKER
        );
    }

    @Test
    public void keepsNonCrossingOrdersAtTheBestPrice() {
        Order lowerBid = limitOrder(1L, Side.BUY, 99, 4);
        Order bestBid = limitOrder(2L, Side.BUY, 101, 6);

        book.process(lowerBid);
        book.process(bestBid);

        assertEquals(101, book.getBids().firstKey().intValue());
        assertEquals(99, book.getBids().lastKey().intValue());
        assertEquals(6, book.totalQuantityAt(Side.BUY, 101));
        assertEquals(4, book.totalQuantityAt(Side.BUY, 99));
        assertSame(bestBid, book.getOrderMap().get(2L));
        assertSame(lowerBid, book.getOrderMap().get(1L));
    }

    @Test
    public void matchesTheOldestOrderFirstAtTheSamePrice() {
        Order firstSell = limitOrder(1L, Side.SELL, 100, 5);
        Order secondSell = limitOrder(2L, Side.SELL, 100, 7);

        book.process(firstSell);
        book.process(secondSell);
        book.process(limitOrder(3L, Side.BUY, 100, 8));

        PriceLevel remainingLevel = book.getAsks().get(100);
        assertEquals(4, remainingLevel.getTotalQuantity());
        assertSame(secondSell, remainingLevel.getHead());
        assertSame(secondSell, remainingLevel.getTail());
        assertEquals(4, secondSell.getQuantity());
        assertFalse(book.getOrderMap().containsKey(1L));
        assertTrue(book.getOrderMap().containsKey(2L));
        assertFalse(book.getOrderMap().containsKey(3L));
    }

    @Test
    public void drainsMultipleOrdersAtOnePriceWithOneLevelUpdate() {
        book.process(limitOrder(1L, Side.SELL, 100, 5));
        book.process(limitOrder(2L, Side.SELL, 100, 7));

        Order incomingBuy = limitOrder(3L, Side.BUY, 100, 12);
        book.process(incomingBuy);

        assertTrue(book.getAsks().isEmpty());
        assertTrue(book.getOrderMap().isEmpty());
        assertSame(incomingBuy, book.getOrderAllocator().borrow());
        // Two adds, two trade prints, and one level-removal delta.
        assertEquals(5, book.getMarketDataSequence());
    }

    @Test
    public void doesNotMatchOrdersThatDoNotCross() {
        Order sell = limitOrder(1L, Side.SELL, 100, 5);
        Order buy = limitOrder(2L, Side.BUY, 99, 5);

        book.process(sell);
        book.process(buy);

        assertEquals(5, book.totalQuantityAt(Side.SELL, 100));
        assertEquals(5, book.totalQuantityAt(Side.BUY, 99));
        assertSame(sell, book.getOrderMap().get(1L));
        assertSame(buy, book.getOrderMap().get(2L));
    }

    @Test
    public void cancellingOneOrderPreservesTheRemainingLevelQuantity() {
        book.process(limitOrder(1L, Side.BUY, 100, 4));
        book.process(limitOrder(2L, Side.BUY, 100, 6));

        assertTrue(book.cancelOrder(1L));

        PriceLevel remainingLevel = book.getBids().get(100);
        assertEquals(6, remainingLevel.getTotalQuantity());
        assertEquals(6, book.totalQuantityAt(Side.BUY, 100));
        assertSame(book.getOrderMap().get(2L), remainingLevel.getHead());
        assertNull(remainingLevel.getHead().getPrevious());
    }

    @Test
    public void cancellingAnUnknownOrderDoesNotChangeTheBook() {
        book.process(limitOrder(1L, Side.BUY, 100, 4));

        assertFalse(book.cancelOrder(999L));
        assertEquals(4, book.totalQuantityAt(Side.BUY, 100));
        assertEquals(1, book.getOrderMap().size());
    }

    @Test
    public void cancellingTheLastOrderRemovesItsPriceLevel() {
        book.process(limitOrder(1L, Side.SELL, 100, 4));

        assertTrue(book.cancelOrder(1L));

        assertFalse(book.getAsks().containsKey(100));
        assertTrue(book.getOrderMap().isEmpty());
        assertEquals(0, book.totalQuantityAt(Side.SELL, 100));
    }

    @Test
    public void killOrderDoesNotRestItsUnfilledQuantity() {
        Order killOrder = new Order(1L, 10L, Side.BUY, false, 100, 4, true,
                null, null, null, null);

        book.process(killOrder);

        assertTrue(book.getBids().isEmpty());
        assertTrue(book.getOrderMap().isEmpty());
        assertSame(killOrder, book.getOrderAllocator().borrow());
    }

    @Test
    public void snapshotUsesBestPriceFirstAndRespectsTheRequestedDepth() {
        book.process(limitOrder(1L, Side.BUY, 99, 1));
        book.process(limitOrder(2L, Side.BUY, 101, 2));
        book.process(limitOrder(3L, Side.BUY, 100, 3));
        book.process(limitOrder(4L, Side.SELL, 104, 4));
        book.process(limitOrder(5L, Side.SELL, 102, 5));
        book.process(limitOrder(6L, Side.SELL, 103, 6));

        OrderSnapshot snapshot = new OrderSnapshot(TICKER, 1);
        book.fillSnapshot(snapshot, 2);

        assertEquals(101, snapshot.getBids()[0]);
        assertEquals(2, snapshot.getBidsQuantities()[0]);
        assertEquals(100, snapshot.getBids()[1]);
        assertEquals(3, snapshot.getBidsQuantities()[1]);
        assertEquals(102, snapshot.getAsks()[0]);
        assertEquals(5, snapshot.getAsksQuantities()[0]);
        assertEquals(103, snapshot.getAsks()[1]);
        assertEquals(6, snapshot.getAsksQuantities()[1]);
    }

    private static Order limitOrder(long id, Side side, int price, int quantity) {
        return new Order(id, id * 10, side, false, price, quantity, false,
                null, null, null, null);
    }
}
