package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;

@PacketId(0x11)
@NoArgsConstructor
@Getter
public class OrderStatusResponsePacket extends Packet {

    private boolean found;
    private long clientRequestId;
    private long orderId;
    private long tickerId;
    private int price;
    private int quantity; // remaining resting quantity - only meaningful if found
    private Side side;

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public void read(ByteBuf buf) {
        this.found = buf.readByte() == 0x01;
        this.clientRequestId = buf.readLong();
        this.orderId = buf.readLong();
        this.tickerId = buf.readLong();
        this.price = buf.readInt();
        this.quantity = buf.readInt();

        byte side = buf.readByte();
        this.side = side == 0x01 ? Side.BUY : side == 0x02 ? Side.SELL : null;
    }
}