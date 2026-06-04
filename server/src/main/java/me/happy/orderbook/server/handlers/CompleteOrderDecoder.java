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

        ByteBuf packetBuffer = byteBuf.readRetainedSlice(size);

        try {
            parseOperation(operation, packetBuffer, channelHandlerContext);
            System.out.printf("Received operation: %d Size: %d\n", operation, size);
        }catch (ArrayIndexOutOfBoundsException e) {
            System.out.printf("Invalid operation: %d Size: %d. Kicking client!\n", operation, size);
            kickClient(channelHandlerContext, 1);
        }
    }

    private void parseOperation(byte operation, ByteBuf byteBuf, ChannelHandlerContext context) throws ArrayIndexOutOfBoundsException {
        switch (operation) {
            case 0x01: {
                long tickerId = safeReadLong(byteBuf);
                byte orderType = safeReadByte(byteBuf);
                int price = safeReadInt(byteBuf);
                int quantity = safeReadInt(byteBuf);
                long clientRequestId = safeReadLong(byteBuf);

                if (isInvalidLength(byteBuf, context)) return;

                exchange.process(tickerId, orderType == 0x01 ? Side.BUY : Side.SELL, price, quantity, clientRequestId, context.channel());

                break;
            }

            case 0x02: {
                long tickerId = safeReadLong(byteBuf);

                if (isInvalidLength(byteBuf, context)) return;

                exchange.processSnapshot(tickerId, context.channel());
                break;
            }
        }
    }

    private boolean isInvalidLength(ByteBuf byteBuf, ChannelHandlerContext context) {
        if (byteBuf.readableBytes() > 0) {
            System.out.println("Received too many bytes. Kicking client!");
            kickClient(context, 2);

            return true;
        }

        return false;
    }

    private void kickClient(ChannelHandlerContext context, int errorCode) {
        ByteBuf byteBuf = context.channel().alloc().buffer(2);

        byteBuf.writeByte(0x04);
        byteBuf.writeInt(errorCode);
        context.channel().writeAndFlush(byteBuf);
        context.channel().close();
    }

    private byte safeReadByte(ByteBuf byteBuf) throws ArrayIndexOutOfBoundsException {
        if (byteBuf.readableBytes() < 1) {
            throw new ArrayIndexOutOfBoundsException("Not enough bytes in buffer for byte");
        }

        return byteBuf.readByte();
    }

    private int safeReadInt(ByteBuf byteBuf) throws ArrayIndexOutOfBoundsException {
        if (byteBuf.readableBytes() < 4) {
            throw new ArrayIndexOutOfBoundsException("Not enough bytes in buffer for int");
        }

        return byteBuf.readInt();
    }

    private long safeReadLong(ByteBuf byteBuf) throws ArrayIndexOutOfBoundsException {
        if (byteBuf.readableBytes() < 8) {
            throw new ArrayIndexOutOfBoundsException("Not enough bytes in buffer for long");
        }

        return byteBuf.readLong();
    }
}
