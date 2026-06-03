package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;

@PacketId(0x03)
@NoArgsConstructor
@Getter
public class OrderFilledPacket extends Packet {

    private long tickerId;
    private long orderId;
    private long takerId;
    private int price;
    private int quantity;
    private Side side;

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public void read(ByteBuf buf) {
        this.tickerId = buf.readLong();
        this.orderId = buf.readLong();
        this.takerId = buf.readLong();
        this.price = buf.readInt();
        this.quantity = buf.readInt();
        this.side = buf.readByte() == 0x01 ? Side.BUY : Side.SELL;
    }
}
