package me.happy.orderbook.lmax;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import io.netty.channel.Channel;
import lombok.Getter;
import me.happy.orderbook.lmax.events.OrderEvent;
import me.happy.orderbook.lmax.events.OrderEventCommand;
import me.happy.orderbook.lmax.events.TradeEvent;
import me.happy.orderbook.lmax.handler.OrderEventHandler;
import me.happy.orderbook.lmax.handler.TradeEventHandler;
import me.happy.orderbook.order.Side;


@Getter
public class Exchange {
    private final int shardCount;
    private final Disruptor<OrderEvent>[] disruptors;
    private final OrderEventHandler[] handlers;
    private final Disruptor<TradeEvent> tradeEventDisruptor;
    private final TradeEventHandler tradeEventHandler;
    private static Exchange INSTANCE;

    public Exchange(int shardCount) {
        INSTANCE = this;
        this.shardCount = shardCount;
        this.disruptors = new Disruptor[shardCount];
        this.handlers = new OrderEventHandler[shardCount];
        int bufferSize = 1024;

        this.tradeEventHandler = new TradeEventHandler();
        this.tradeEventDisruptor = new Disruptor<>(TradeEvent::new, bufferSize, DaemonThreadFactory.INSTANCE);

        this.tradeEventDisruptor.handleEventsWith(tradeEventHandler);
        this.tradeEventDisruptor.start();

        for (int i = 0; i < shardCount; i++) {
            this.disruptors[i] = new Disruptor<>(OrderEvent::new, bufferSize, DaemonThreadFactory.INSTANCE);
            this.handlers[i] = new OrderEventHandler(i, shardCount);
            this.disruptors[i].handleEventsWith(this.handlers[i]);
            this.disruptors[i].start();
        }
    }

    public void process(long ticker, Side side, int price, int quantity, long clientRequestId, Channel channel) {
        getDisruptor(ticker).getRingBuffer().publishEvent((event, sequence) -> {
            event.setCommand(OrderEventCommand.NEW);
            event.setTicker(ticker);
            event.setChannel(channel);
            event.setSide(side);
            event.setPrice(price);
            event.setQuantity(quantity);
            event.setClientRequestId(clientRequestId);
        });
    }

    public void processModification(long ticker, long orderId, long secret, long clientSideRequestId, int quantity, int price, Channel channel) {
        getDisruptor(ticker).getRingBuffer().publishEvent((event, sequence) -> {
            event.setCommand(OrderEventCommand.MODIFY);
            event.setTicker(ticker);
            event.setOrderId(orderId);
            event.setSecret(secret);
            event.setClientRequestId(clientSideRequestId);
            event.setQuantity(quantity);
            event.setPrice(price);
            event.setChannel(channel);
        });
    }
    public void processSnapshot(long ticker, Channel channel) {
        getDisruptor(ticker).getRingBuffer().publishEvent((event, sequence) -> {
            event.setCommand(OrderEventCommand.SNAPSHOT);
            event.setTicker(ticker);
            event.setChannel(channel);
        });
    }

    public void processCancel(long orderId, long ticker, long secret, long clientRequestId, Channel channel) {
        getDisruptor(ticker).getRingBuffer().publishEvent((event, sequence) -> {
            event.setCommand(OrderEventCommand.CANCEL);
            event.setOrderId(orderId);
            event.setClientRequestId(clientRequestId);
            event.setTicker(ticker);
            event.setSecret(secret);

            event.setChannel(channel);
        });
    }

    public void publishFill(long tickerId, long orderId, long takerId, int price, int quantity, Side takerSide) {
        tradeEventDisruptor.getRingBuffer().publishEvent((event, sequence) -> {
            event.setTickerId(tickerId);
            event.setSequence(sequence);
            event.setOrderId(orderId);
            event.setTakerId(takerId);
            event.setPrice(price);
            event.setQuantity(quantity);
            event.setTakerSide(takerSide);
        });
    }

    private Disruptor<OrderEvent> getDisruptor(long tickerId) {
        int shard = Math.toIntExact(Math.abs(tickerId % shardCount));

        return disruptors[shard];
    }

    public static Exchange getInstance() {
        return INSTANCE;
    }
}