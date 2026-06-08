package me.happy.orderbook.lmax.outbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.Data;
import me.happy.orderbook.order.OrderSnapshot;

@Data
public class OutboundEvent {

    private Channel channel;
    private ByteBuf byteBuf;
    private OrderSnapshot orderSnapshot;
}
