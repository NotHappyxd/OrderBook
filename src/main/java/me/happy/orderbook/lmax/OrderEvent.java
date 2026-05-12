package me.happy.orderbook.lmax;

import lombok.Data;
import me.happy.orderbook.order.Side;

@Data
public class OrderEvent {

    private long ticker;
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
