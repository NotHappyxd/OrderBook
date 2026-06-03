package me.happy.orderbook.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;

@Getter
public abstract class Packet {

    public abstract void write(ByteBuf buf);
    public abstract void read(ByteBuf buf);

    public int getId() {
        PacketId packetId = getClass().getAnnotation(PacketId.class);

        return packetId.value();
    }
}
