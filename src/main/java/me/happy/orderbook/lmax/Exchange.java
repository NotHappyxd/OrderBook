package me.happy.orderbook.lmax;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import io.netty.channel.Channel;
import lombok.Getter;
import me.happy.orderbook.lmax.trade.TradeEvent;
import me.happy.orderbook.lmax.trade.TradeEventHandler;
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

    public void process(long ticker, Side side, int price, int quantity, Channel channel) {
        int shard = Math.toIntExact(Math.abs(ticker % shardCount));

        disruptors[shard].getRingBuffer().publishEvent((event, sequence) -> {
            event.setTicker(ticker);
            event.setChannel(channel);
            event.setSide(side);
            event.setPrice(price);
            event.setQuantity(quantity);
        });
    }

    public void processSnapshot(long ticker, Channel channel) {
        int shard = Math.toIntExact(Math.abs(ticker % shardCount));

        disruptors[shard].getRingBuffer().publishEvent((event, sequence) -> {
            event.setSnapshot(true);
            event.setTicker(ticker);
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

    public static Exchange getInstance() {
        return INSTANCE;
    }
}