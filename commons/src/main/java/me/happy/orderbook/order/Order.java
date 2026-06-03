package me.happy.orderbook.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    private long id;
    private Side side;
    private int price;
    private int quantity;

    public void reset() {
        this.id = 0;
        this.side = null;
        this.price = 0;
        this.quantity = 0;
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
