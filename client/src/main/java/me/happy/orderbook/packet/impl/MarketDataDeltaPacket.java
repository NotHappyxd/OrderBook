package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;
import me.happy.orderbook.protocol.Protocol;

@PacketId(Protocol.MARKET_DATA_DELTA)
@NoArgsConstructor
@Getter
public class MarketDataDeltaPacket extends Packet {

    private long tickerId;
    private long sequence;
    private Side side;
    private int price;
    private int totalQuantity; // 0 means this price level no longer exists

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public void read(ByteBuf buf) {
        this.tickerId = buf.readLong();
        this.sequence = buf.readLong();
        this.side = buf.readByte() == Protocol.BUY ? Side.BUY : Side.SELL;
        this.price = buf.readInt();
        this.totalQuantity = buf.readInt();
    }
}
