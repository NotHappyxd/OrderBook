package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;
import me.happy.orderbook.protocol.Protocol;

@PacketId(Protocol.ORDER_STATUS_RESPONSE)
@NoArgsConstructor
@Getter
public class OrderStatusResponsePacket extends Packet {

    private boolean found;
    private long clientRequestId;
    private long orderId;
    private long tickerId;
    private int price;
    private int quantity; // remaining resting quantity - only meaningful if found
    private Side side;

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public void read(ByteBuf buf) {
        this.found = buf.readByte() == 0x01;
        this.clientRequestId = buf.readLong();
        this.orderId = buf.readLong();
        this.tickerId = buf.readLong();
        this.price = buf.readInt();
        this.quantity = buf.readInt();

        byte side = buf.readByte();
        this.side = side == Protocol.BUY ? Side.BUY : side == Protocol.SELL ? Side.SELL : null;
    }
}
