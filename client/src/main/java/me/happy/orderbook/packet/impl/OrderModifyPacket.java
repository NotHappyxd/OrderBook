package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;

@PacketId(0x08)
@AllArgsConstructor
@NoArgsConstructor
public class OrderModifyPacket extends Packet {

    private long ticker;
    private long orderId;
    private long secret;
    private long clientSideRequestId;
    private int quantity;
    private int price;

    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(ticker);
        buf.writeLong(orderId);
        buf.writeLong(secret);
        buf.writeLong(clientSideRequestId);
        buf.writeInt(quantity);
        buf.writeInt(price);
    }

    @Override
    public void read(ByteBuf buf) {

    }
}
