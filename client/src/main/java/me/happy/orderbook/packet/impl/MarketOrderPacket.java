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
@PacketId(0x01)
public class MarketOrderPacket extends Packet {

    private String tickerId;
    private Side side;
    private int price;
    private int quantity;
    private long clientSideId;

    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(TickerUtils.packString(tickerId));
        buf.writeByte(side == Side.BUY ? 0x01 : 0x02);
        buf.writeInt(price);
        buf.writeInt(quantity);
        buf.writeLong(clientSideId);
    }

    @Override
    public void read(ByteBuf buf) {

    }
}
