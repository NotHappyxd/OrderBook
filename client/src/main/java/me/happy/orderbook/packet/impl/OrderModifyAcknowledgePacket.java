package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;

@PacketId(0x09)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderModifyAcknowledgePacket extends Packet {

    private long ticker;
    private long orderId;
    private long clientSideRequestId;
    private int quantity;
    private int price;

    @Override
    public void write(ByteBuf buf) {
    }

    @Override
    public void read(ByteBuf buf) {
        this.ticker = buf.readLong();
        this.orderId = buf.readLong();
        this.clientSideRequestId = buf.readLong();
        this.quantity = buf.readInt();
        this.price = buf.readInt();
    }
}
