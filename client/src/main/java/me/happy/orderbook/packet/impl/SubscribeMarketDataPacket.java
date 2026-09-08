package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.happy.orderbook.TickerUtils;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;
import me.happy.orderbook.protocol.Protocol;

@PacketId(Protocol.MARKET_DATA_SUBSCRIBE)
@AllArgsConstructor
@NoArgsConstructor
public class SubscribeMarketDataPacket extends Packet {

    private String ticker;

    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(TickerUtils.packString(ticker));
    }

    @Override
    public void read(ByteBuf buf) {

    }
}
