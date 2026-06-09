package me.happy.orderbook.lmax.trade;

import lombok.Data;
import me.happy.orderbook.order.Side;

@Data
public class TradeEvent {

    private long tickerId;
    private long sequence;
    private long orderId;
    private long takerId;
    private int price;
    private int quantity;
    private Side takerSide;

    public void copyFrom(TradeEvent tradeEvent) {
        this.tickerId = tradeEvent.getTickerId();
        this.sequence = tradeEvent.getSequence();
        this.orderId = tradeEvent.getOrderId();
        this.takerId = tradeEvent.getTakerId();
        this.price = tradeEvent.getPrice();
        this.quantity = tradeEvent.getQuantity();
        this.takerSide = tradeEvent.getTakerSide();
    }
}
