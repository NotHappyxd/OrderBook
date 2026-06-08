package me.happy.orderbook.lmax.order;

import io.netty.channel.Channel;
import lombok.Data;
import me.happy.orderbook.order.Side;

@Data
public class OrderEvent {

    private OrderEventCommand command;

    private long ticker;
    private long orderId;
    private long clientRequestId;
    private long secret;

    private Channel channel;
    private Side side;

    private int price;
    private int quantity;

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
