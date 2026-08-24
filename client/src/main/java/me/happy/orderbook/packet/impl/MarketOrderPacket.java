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
    private boolean marketPrice = false;
    private int price;
    private int quantity;
    private long clientSideId;
    private boolean kill;

    public MarketOrderPacket(String tickerId, Side side, int price, int quantity, long clientSideId) {
        this.tickerId = tickerId;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.clientSideId = clientSideId;
        this.kill = false;
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(TickerUtils.packString(tickerId));
        buf.writeByte(side == Side.BUY ? 0x01 : 0x02);
        buf.writeBoolean(marketPrice);
        buf.writeInt(price);
        buf.writeInt(quantity);
        buf.writeLong(clientSideId);
        buf.writeBoolean(kill);
    }

    @Override
    public void read(ByteBuf buf) {

    }
}
