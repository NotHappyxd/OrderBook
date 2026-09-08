package me.happy.orderbook.lmax.metadata;

import lombok.Data;
import me.happy.orderbook.order.Side;

@Data
public class PublicFeedEvent {

    private PublicFeedEventType type;
    private long ticker;
    private long marketDataSequence;
    private Side side;
    private int price;
    private int tradedQuantity;
    private int totalQuantity;

    public enum PublicFeedEventType {
        LEVEL_DATA,
        TRADE_PRINT
    }
}
