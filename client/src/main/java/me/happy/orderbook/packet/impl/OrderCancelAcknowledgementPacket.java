package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;
import me.happy.orderbook.protocol.Protocol;

@PacketId(Protocol.ORDER_CANCEL_ACKNOWLEDGEMENT)
@NoArgsConstructor
@Getter
public class OrderCancelAcknowledgementPacket extends Packet {

    private boolean success;
    private long clientRequestId;
    private long orderId;

    @Override
    public void write(ByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeLong(clientRequestId);
        buf.writeLong(orderId);
    }

    @Override
    public void read(ByteBuf buf) {
        this.success = buf.readBoolean();
        this.clientRequestId = buf.readLong();
        this.orderId = buf.readLong();
    }
}
