package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.happy.orderbook.TickerUtils;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;
import me.happy.orderbook.protocol.Protocol;

@PacketId(Protocol.SNAPSHOT_REQUEST)
@AllArgsConstructor
@NoArgsConstructor
public class SnapshotRequestPacket extends Packet {

    private String ticker;

    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(TickerUtils.packString(ticker));
    }

    @Override
    public void read(ByteBuf buf) {

    }
}
