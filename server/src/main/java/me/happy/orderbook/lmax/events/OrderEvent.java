package me.happy.orderbook.lmax.events;

import io.netty.channel.Channel;
import lombok.Data;
import me.happy.orderbook.order.Side;

@Data
public class OrderEvent {

    private long ticker;
    private boolean snapshot = false;
    private Channel channel;
    private Side side;
    private int price;
    private int quantity;
    private long clientRequestId;

    @Override
    public String toString() {
        return "OrderEvent{" +
                "ticker='" + ticker + '\'' +
                ", side=" + side +
                ", price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}
