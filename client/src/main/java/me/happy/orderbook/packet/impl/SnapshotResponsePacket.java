package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;

import java.util.HashMap;
import java.util.Map;

@PacketId(0x06)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SnapshotResponsePacket extends Packet {

    private long ticker;
    private long sequence;
    private int depth;
    private Map<Integer, Integer> bids = new HashMap<>();
    private Map<Integer, Integer> asks = new HashMap<>();

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public void read(ByteBuf buf) {
        this.ticker = buf.readLong();
        this.sequence = buf.readLong();
        this.depth = buf.readByte();

        for (int i = 0; i < this.depth; i++) {
            bids.put(buf.readInt(), buf.readInt());
        }

        for (int i = 0; i < this.depth; i++) {
            asks.put(buf.readInt(), buf.readInt());
        }
    }
}
