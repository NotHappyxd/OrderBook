package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.happy.orderbook.TickerUtils;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;

@AllArgsConstructor
@NoArgsConstructor
@PacketId(0x05)
public class OrderCancelPacket extends Packet {

    private long orderId;
    private long tickerId;
    private long clientSideId;
    private long secret;

    @Override
    public void write(ByteBuf buf) {
        System.out.println("Writing cancel for " + this.clientSideId);
        buf.writeLong(orderId);
        buf.writeLong(tickerId);
        buf.writeLong(clientSideId);
        buf.writeLong(secret);
    }

    @Override
    public void read(ByteBuf buf) {

    }
}
