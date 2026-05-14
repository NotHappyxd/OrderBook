package me.happy.orderbook.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import me.happy.orderbook.engine.OrderSnapshot;

public class OrderSnapshotEncoder extends MessageToByteEncoder<OrderSnapshot> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, OrderSnapshot snapshot, ByteBuf byteBuf) throws Exception {
        // Long (Ticker), Byte (Length), (Bid Price, Bid Amount), (Ask Price, Ask Amount)
        byteBuf.writeLong(snapshot.getTicker());
        byteBuf.writeLong(snapshot.getSequenceId());
        byteBuf.writeByte(5); // Depth of orders

        for (int i = 0; i < 5; i++) {
            byteBuf.writeInt(snapshot.getBids()[i]);
            byteBuf.writeInt(snapshot.getBidsQuantities()[i]);
        }

        for (int i = 0; i < 5; i++) {
            byteBuf.writeInt(snapshot.getAsks()[i]);
            byteBuf.writeInt(snapshot.getAsksQuantities()[i]);
        }
    }
}
