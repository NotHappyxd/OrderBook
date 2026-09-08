package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;
import me.happy.orderbook.protocol.Protocol;

@PacketId(Protocol.ORDER_ACKNOWLEDGEMENT)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OrderAcknowledgementPacket extends Packet {

    private long clientOrderId;
    private long serverOrderId;
    private long secret;

    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(clientOrderId);
        buf.writeLong(serverOrderId);
        buf.writeLong(secret);
    }

    @Override
    public void read(ByteBuf buf) {
        this.clientOrderId = buf.readLong();
        this.serverOrderId = buf.readLong();
        this.secret = buf.readLong();
    }
}
