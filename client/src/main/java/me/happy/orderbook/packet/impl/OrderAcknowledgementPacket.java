package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;

@PacketId(0x07)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OrderAcknowledgementPacket extends Packet {

    private long clientOrderId;
    private long serverOrderId;

    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(clientOrderId);
        buf.writeLong(serverOrderId);
    }

    @Override
    public void read(ByteBuf buf) {
        System.out.println(buf.capacity());
        System.out.println(buf.readerIndex());
        this.clientOrderId = buf.readLong();
        this.serverOrderId = buf.readLong();
    }
}
