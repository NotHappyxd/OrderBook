package me.happy.orderbook;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketManager;

import java.util.Optional;

public class ExchangeClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final PacketManager packetManager;

    public ExchangeClientHandler(PacketManager packetManager) {
        this.packetManager = packetManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Channel Active! Connected to server.");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        byte operation = in.getByte(in.readerIndex());

        Optional<Packet> packet = packetManager.getPacket(operation);
        
        if (packet.isEmpty()) {
            System.out.println("No packet found for operation " + operation);
            return;
        }

        in.readByte();
        packet.get().read(in);
        
        packetManager.publishPacket(packet.get());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Disconnected from server.");
        System.exit(12);
    }
}
