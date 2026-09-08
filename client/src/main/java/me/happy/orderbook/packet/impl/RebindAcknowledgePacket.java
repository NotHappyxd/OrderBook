package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;
import me.happy.orderbook.protocol.Protocol;

@PacketId(Protocol.ORDER_REBIND_ACKNOWLEDGEMENT)
@NoArgsConstructor
@Getter
public class RebindAcknowledgePacket extends Packet {

    private boolean success;
    private long clientRequestId;
    private long orderId;

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public void read(ByteBuf buf) {
        this.success = buf.readByte() == 0x01;
        this.clientRequestId = buf.readLong();
        this.orderId = buf.readLong();
    }
}
