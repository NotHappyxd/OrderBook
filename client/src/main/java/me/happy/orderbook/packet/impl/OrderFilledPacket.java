package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;
import me.happy.orderbook.protocol.Protocol;

@PacketId(Protocol.EXECUTION_REPORT)
@NoArgsConstructor
@Getter
public class OrderFilledPacket extends Packet {

    private long tickerId;
    private long orderId;
    private int price;
    private int quantity;          // filled in this specific match
    private int remainingQuantity; // this order's remaining resting quantity, 0 if fully filled
    private Side side;

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public void read(ByteBuf buf) {
        this.tickerId = buf.readLong();
        this.orderId = buf.readLong();
        this.price = buf.readInt();
        this.quantity = buf.readInt();
        this.remainingQuantity = buf.readInt();
        this.side = buf.readByte() == Protocol.BUY ? Side.BUY : Side.SELL;
    }
}
