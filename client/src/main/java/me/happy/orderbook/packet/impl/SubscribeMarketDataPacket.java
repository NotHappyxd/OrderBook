package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.happy.orderbook.TickerUtils;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;

@PacketId(0x0A)
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