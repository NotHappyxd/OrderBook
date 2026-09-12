package me.happy.orderbook.processor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import me.happy.orderbook.checkpoint.Checkpoint;
import me.happy.orderbook.engine.OrderBook;
import me.happy.orderbook.lmax.AllocatorPool;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.lmax.order.OrderEvent;
import me.happy.orderbook.lmax.order.OrderPublisher;
import me.happy.orderbook.order.Order;
import me.happy.orderbook.order.OrderSnapshot;
import me.happy.orderbook.order.PriceLevel;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.protocol.Protocol;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public class OrderEventProcessor {

    private final AllocatorPool<Order> orderAllocator;
    private final Map<Long, OrderBook> orderBookMap = new HashMap<>();

    @Setter
    private Path checkpointPath;
    @Setter
    private OrderPublisher orderPublisher;

    private long lastMutatingSequence = 0;
    private long lastCheckpointedSequence = -1;

    public OrderEventProcessor() {
        this.orderAllocator = new AllocatorPool<>(1024, Order::new);
    }

    public void process(OrderEvent event, long sequence, boolean endOfBatch) {
        switch (event.getCommand()) {
            case NEW -> {
                processOrder(event);
                lastMutatingSequence = sequence;
            }
            case SNAPSHOT -> processSnapshot(event, event.getClientRequestId());
            case MODIFY -> {
                processModification(event, sequence);
                lastMutatingSequence = sequence;
            }
            case CANCEL -> {
                cancelOrder(event);
                lastMutatingSequence = sequence;
            }
            case REBIND -> processRebind(event);
            case STATUS -> processStatus(event);
            case CHECKPOINT -> processCheckpoint(sequence);
            case JOURNAL_FORCE, CHECKPOINT_COMPLETE -> {
                // Journal control events are handled upstream by JournalHandler.
            }
        }
    }

    private void processStatus(OrderEvent event) {
        OrderBook orderBook = orderBookMap.get(event.getTicker());
        Order order = null;

        if (orderBook != null) {
            Order candidate = orderBook.getOrderMap().get(event.getOrderId());

            if (candidate != null && candidate.getSecret() == event.getSecret()) {
                order = candidate;
            }
        }

        Order finalOrder = order;
        boolean found = finalOrder != null;

        sendBuffer(event.getChannel(), Protocol.ORDER_STATUS_RESPONSE_LENGTH, Protocol.ORDER_STATUS_RESPONSE, byteBuf -> {
            byteBuf.writeByte(found ? 1 : 0);
            byteBuf.writeLong(event.getClientRequestId());
            byteBuf.writeLong(event.getOrderId());
            byteBuf.writeLong(event.getTicker());
            byteBuf.writeInt(found ? finalOrder.getPrice() : 0);
            byteBuf.writeInt(found ? finalOrder.getQuantity() : 0);
            byteBuf.writeByte(found ? (finalOrder.getSide() == Side.BUY ? Protocol.BUY : Protocol.SELL) : 0);
        });
    }

    private void processRebind(OrderEvent event) {
        OrderBook orderBook = orderBookMap.get(event.getTicker());
        boolean changed = false;

        if (orderBook != null) {
            Order order = orderBook.getOrderMap().get(event.getOrderId());

            if (order != null && order.getSecret() == event.getSecret()) {
                order.setChannel(event.getChannel());
                changed = true;
            }
        }

        boolean finalChanged = changed;
        sendBuffer(event.getChannel(), Protocol.ORDER_REBIND_ACKNOWLEDGEMENT_LENGTH, Protocol.ORDER_REBIND_ACKNOWLEDGEMENT, byteBuf -> {
            byteBuf.writeBoolean(finalChanged);
            byteBuf.writeLong(event.getClientRequestId());
            byteBuf.writeLong(event.getOrderId());
        });
    }

    private void processModification(OrderEvent event, long sequence) {
        OrderBook orderBook = orderBookMap.get(event.getTicker());

        if (orderBook == null) return;

        Order order = orderBook.getOrderMap().get(event.getOrderId());

        if (order == null || order.getSecret() != event.getSecret()) return;
        if (event.getQuantity() <= 0 || event.getPrice() <= 0) return; // Stop negative numbers

        PriceLevel priceLevel = orderBook.getBook(order).get(order.getPrice());

        boolean quantityChanged = event.getQuantity() != order.getQuantity();
        boolean priceChanged = event.getPrice() != order.getPrice();

        if (order.getQuantity() > event.getQuantity() && !priceChanged) { // User gives up liquidity, keep in place.
            int diff = event.getQuantity() - order.getQuantity();
            priceLevel.setTotalQuantity(priceLevel.getTotalQuantity() + diff);
            order.setQuantity(order.getQuantity() + diff);

            orderBook.publishLevelUpdate(order.getSide(), order.getPrice(), priceLevel.getTotalQuantity());
        } else if (order.getQuantity() < event.getQuantity() || priceChanged) {
            priceLevel.removeOrder(order);

            orderBook.reconcileLevel(order.getSide(), order.getPrice());
            if (priceChanged)
                order.setPrice(event.getPrice());

            if (quantityChanged)
                order.setQuantity(event.getQuantity());

            orderBook.addToBook(order);
        }

        sendBuffer(event.getChannel(), Protocol.ORDER_MODIFY_ACKNOWLEDGEMENT_LENGTH, Protocol.ORDER_MODIFY_ACKNOWLEDGEMENT, byteBuf -> {
            byteBuf.writeLong(event.getTicker());
            byteBuf.writeLong(order.getId());
            byteBuf.writeLong(event.getClientRequestId());
            byteBuf.writeInt(order.getQuantity());
            byteBuf.writeInt(order.getPrice());
        });
    }

    private void processSnapshot(OrderEvent event, long requestId) {
        OrderBook orderBook = orderBookMap.get(event.getTicker());

        OrderSnapshot snapshot = new OrderSnapshot(event.getTicker(), requestId);

        if (orderBook != null) {
            orderBook.fillSnapshot(snapshot, 5);
            snapshot.setSequenceId(orderBook.getMarketDataSequence());
        }

        Exchange.getInstance().getOutboundPublisher().publish(event.getChannel(), snapshot);
    }

    private void processOrder(OrderEvent event) {
        Order order = orderAllocator.borrow();
        order.reset();

        order.setSide(event.getSide());
        order.setId(event.getOrderId());
        order.setQuantity(event.getQuantity());
        order.setMarketPrice(event.isMarketPrice());
        order.setPrice(event.getPrice());
        order.setSecret(event.getSecret());
        order.setKill(event.isKill());
        order.setChannel(event.getChannel());

        OrderBook orderBook = orderBookMap.get(event.getTicker());

        if (orderBook == null) {
            orderBook = new OrderBook(Exchange.getInstance().getPublicFeedPublisher(), Exchange.getInstance().getOutboundPublisher(), this.orderAllocator, event.getTicker());
            this.orderBookMap.put(event.getTicker(), orderBook);
        }

        // Acknowledge
        sendBuffer(event.getChannel(), Protocol.ORDER_ACKNOWLEDGEMENT_LENGTH, Protocol.ORDER_ACKNOWLEDGEMENT, byteBuf -> {
            byteBuf.writeLong(event.getClientRequestId());
            byteBuf.writeLong(order.getId());
            byteBuf.writeLong(event.getSecret());
        });

        orderBook.process(order);
    }

    private void cancelOrder(OrderEvent event) {
        OrderBook orderBook = orderBookMap.get(event.getTicker());

        boolean cancelled = false;

        if (orderBook != null) {
            Order order = orderBook.getOrderMap().get(event.getOrderId());

            if (order != null && order.getSecret() == event.getSecret()) {
                cancelled = orderBook.cancelOrder(event.getOrderId());
            }
        }

        boolean finalCancelled = cancelled;
        sendBuffer(event.getChannel(), Protocol.ORDER_CANCEL_ACKNOWLEDGEMENT_LENGTH, Protocol.ORDER_CANCEL_ACKNOWLEDGEMENT, byteBuf -> {
            byteBuf.writeBoolean(finalCancelled);
            byteBuf.writeLong(event.getClientRequestId());
            byteBuf.writeLong(event.getOrderId());
        });
    }

    private void processCheckpoint(long sequence) {
        if (checkpointPath == null || orderPublisher == null) return;

        if (lastMutatingSequence == lastCheckpointedSequence) {
            orderPublisher.processCheckpointComplete();
            return;
        }

        try {
            Checkpoint.write(checkpointPath, sequence, orderPublisher.getSequence(), orderBookMap);
            lastCheckpointedSequence = lastMutatingSequence;
            orderPublisher.processCheckpointComplete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreFromCheckpoint(Checkpoint.CheckpointData data) {
        if (data == null) return;

        for (Checkpoint.TickerState tickerState : data.tickers()) {
            OrderBook orderBook = new OrderBook(
                    Exchange.getInstance().getPublicFeedPublisher(),
                    Exchange.getInstance().getOutboundPublisher(),
                    orderAllocator, tickerState.tickerId()
            );

            for (Checkpoint.OrderRecord order : tickerState.orders()) {
                Order restoredOrder = new Order(order.orderId(), order.secret(),
                        order.side(), order.marketPrice(), order.price(), order.quantity(), false,
                        null, null, null, null);
                orderBook.addToBook(restoredOrder);
            }

            orderBook.setMarketDataSequence(tickerState.marketDataSequence());
            orderBookMap.put(tickerState.tickerId(), orderBook);
        }
    }

    private void sendBuffer(Channel channel, int payloadSize, int operation, Consumer<ByteBuf> payload) {
        if (channel == null) return;
        ByteBuf byteBuf = channel.alloc().buffer(payloadSize + 1); // Size excludes operation
        byteBuf.writeByte(operation);

        payload.accept(byteBuf);

        Exchange.getInstance().getOutboundPublisher().publish(channel, byteBuf);
    }

    public OrderBook getOrderBook(long ticker) {
        return orderBookMap.get(ticker);
    }
}
