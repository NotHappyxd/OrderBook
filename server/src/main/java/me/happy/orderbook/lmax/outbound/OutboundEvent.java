package me.happy.orderbook.lmax.outbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.Data;
import me.happy.orderbook.order.OrderSnapshot;
import me.happy.orderbook.order.Side;

@Data
public class OutboundEvent {

    private Type type;
    private Channel channel;
    private ByteBuf byteBuf;
    private OrderSnapshot orderSnapshot;

    private long ticker;
    private long orderId;
    private Side side;
    private int price;
    private int filledQuantity;
    private int remainingQuantity;

    public enum Type {
        BYTE_BUF,
        SNAPSHOT,
        EXECUTION_REPORT
    }
}
