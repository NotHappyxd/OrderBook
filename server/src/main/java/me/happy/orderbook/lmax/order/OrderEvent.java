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

    private boolean marketPrice = false;
    private int price;
    private int quantity;

    private boolean kill = false;

    public void copyFrom(OrderEvent other) {
        this.command = other.command;

        this.ticker = other.ticker;
        this.orderId = other.orderId;
        this.clientRequestId = other.clientRequestId;
        this.secret = other.secret;

        this.channel = other.channel;
        this.side = other.side;

        this.marketPrice = other.marketPrice;
        this.price = other.price;
        this.quantity = other.quantity;

        this.kill = other.kill;
    }

    @Override
    public String toString() {
        return "OrderEvent{" +
                "command=" + command +
                ", ticker=" + ticker +
                ", orderId=" + orderId +
                ", clientRequestId=" + clientRequestId +
                ", secret=" + secret +
                ", channel=" + channel +
                ", side=" + side +
                ", price=" + price +
                ", quantity=" + quantity +
                ", kill=" + kill +
                '}';
    }
}
