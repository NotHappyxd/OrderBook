package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;

@PacketId(0x04)
@NoArgsConstructor
@Getter
public class ServerKickPacket extends Packet {

    private KickReason kickReason;

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public void read(ByteBuf buf) {
        this.kickReason = KickReason.values()[buf.readInt() - 1];
    }

    enum KickReason {
        TOO_FEW_BYTES, TOO_MANY_BYTES
    }
}
