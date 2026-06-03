package me.happy.orderbook.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.RequiredArgsConstructor;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.order.Side;

import java.util.List;

@RequiredArgsConstructor
public class CompleteOrderDecoder extends ByteToMessageDecoder {

    private final Exchange exchange;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        byte operation = byteBuf.readByte();
        int size = byteBuf.readableBytes();

        System.out.printf("Received operation: %d Size: %d\n", operation, size);

        if (operation == 0x01 && size == 25) { // Market Order
            long tickerId = byteBuf.readLong();
            byte orderType = byteBuf.readByte();
            int price = byteBuf.readInt();
            int quantity = byteBuf.readInt();
            long clientRequestId = byteBuf.readLong();

            exchange.process(tickerId, orderType == 0x01 ? Side.BUY : Side.SELL, price, quantity, clientRequestId, channelHandlerContext.channel());
        } else if (operation == 0x02) {
            long tickerId = byteBuf.readLong();

            exchange.processSnapshot(tickerId, channelHandlerContext.channel());
        }

    }
}
