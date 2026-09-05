package me.happy.orderbook.order;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    private long id;
    private long secret;

    private Side side;

    private boolean marketPrice = false;
    private int price;
    private int quantity;

    private boolean kill;

    private Order next;
    private Order previous;
    private PriceLevel priceLevel;

    private transient Channel channel;

    public void reset() {
        this.id = 0;
        this.side = null;
        this.marketPrice = false;
        this.price = 0;
        this.quantity = 0;
        this.kill = false;

        this.next = null;
        this.previous = null;
        this.priceLevel = null;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", side=" + side +
                ", price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}
