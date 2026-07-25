package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.happy.orderbook.TickerUtils;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;

@PacketId(0x0F)
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusRequestPacket extends Packet {

    private String ticker;
    private long orderId;
    private long secret;
    private long clientRequestId;

    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(TickerUtils.packString(ticker));
        buf.writeLong(orderId);
        buf.writeLong(secret);
        buf.writeLong(clientRequestId);
    }

    @Override
    public void read(ByteBuf buf) {

    }
}