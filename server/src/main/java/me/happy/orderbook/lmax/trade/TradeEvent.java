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

}
