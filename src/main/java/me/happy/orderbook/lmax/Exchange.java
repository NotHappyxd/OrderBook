package me.happy.orderbook.lmax;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import io.netty.channel.Channel;
import lombok.Getter;
import me.happy.orderbook.order.Side;


@Getter
public class Exchange {
    private final int shardCount;
    private final Disruptor<OrderEvent>[] disruptors;
    private final OrderEventHandler[] handlers;

    public Exchange(int shardCount) {
        this.shardCount = shardCount;
        this.disruptors = new Disruptor[shardCount];
        this.handlers = new OrderEventHandler[shardCount];
        int bufferSize = 1024;

        for (int i = 0; i < shardCount; i++) {
            this.disruptors[i] = new Disruptor<>(OrderEvent::new, bufferSize, DaemonThreadFactory.INSTANCE);
            this.handlers[i] = new OrderEventHandler(i, shardCount);
            this.disruptors[i].handleEventsWith(this.handlers[i]);
            this.disruptors[i].start();
        }
    }

    public void process(long ticker, Side side, int price, int quantity) {
        int shard = Math.toIntExact(Math.abs(ticker % shardCount));

        disruptors[shard].getRingBuffer().publishEvent((event, sequence) -> {
            event.setTicker(ticker);
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
}