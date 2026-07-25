package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;

@PacketId(0x0D)
@NoArgsConstructor
@Getter
public class TradePrintPacket extends Packet {

    private long tickerId;
    private long sequence;
    private int price;
    private int quantity;
    private Side aggressorSide;

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public void read(ByteBuf buf) {
        this.tickerId = buf.readLong();
        this.sequence = buf.readLong();
        this.price = buf.readInt();
        this.quantity = buf.readInt();
        this.aggressorSide = buf.readByte() == 0x01 ? Side.BUY : Side.SELL;
    }
}