package me.happy.orderbook.lmax.metadata;

import lombok.Data;
import me.happy.orderbook.order.Side;

@Data
public class MarketDataEvent {

    private long ticker;
    private long sequence;
    private Side side;
    private int price;
    private int totalQuantity;

    public void copyFrom(MarketDataEvent event) {
        this.ticker = event.ticker;
        this.sequence = event.sequence;
        this.side = event.side;
        this.price = event.price;
        this.totalQuantity = event.totalQuantity;
    }
}
