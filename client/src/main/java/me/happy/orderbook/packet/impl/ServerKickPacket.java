package me.happy.orderbook.packet.impl;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.happy.orderbook.protocol.ProtocolError;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketId;
import me.happy.orderbook.protocol.Protocol;

@PacketId(Protocol.SERVER_KICK)
@NoArgsConstructor
@Getter
public class ServerKickPacket extends Packet {

    private ProtocolError kickReason;

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public void read(ByteBuf buf) {
        this.kickReason = ProtocolError.fromCode(buf.readInt());
    }
}
