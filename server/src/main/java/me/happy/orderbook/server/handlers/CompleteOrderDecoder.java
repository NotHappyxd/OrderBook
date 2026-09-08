package me.happy.orderbook.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.protocol.ProtocolError;

@RequiredArgsConstructor
public class CompleteOrderDecoder extends SimpleChannelInboundHandler<ByteBuf> {

    private static final byte NEW_ORDER = 0x01;
    private static final byte SNAPSHOT_REQUEST = 0x02;
    private static final byte CANCEL_ORDER = 0x05;
    private static final byte MODIFY_ORDER = 0x08;
    private static final byte SUBSCRIBE_MARKET_DATA = 0x0A;
    private static final byte UNSUBSCRIBE_MARKET_DATA = 0x0B;
    private static final byte REBIND_ORDER = 0x0E;
    private static final byte ORDER_STATUS = 0x0F;
    private static final byte SERVER_KICK = 0x04;

    private static final int NEW_ORDER_LENGTH = Long.BYTES + 2 + (Integer.BYTES * 2) + Long.BYTES + 1;
    private static final int SNAPSHOT_REQUEST_LENGTH = Long.BYTES;
    private static final int CANCEL_ORDER_LENGTH = Long.BYTES * 4;
    private static final int MODIFY_ORDER_LENGTH = (Long.BYTES * 4) + (Integer.BYTES * 2);
    private static final int SUBSCRIPTION_LENGTH = Long.BYTES;
    private static final int REBIND_ORDER_LENGTH = Long.BYTES * 4;
    private static final int ORDER_STATUS_LENGTH = Long.BYTES * 4;

    private final Exchange exchange;

    @Override
    protected void channelRead0(ChannelHandlerContext context, ByteBuf buffer) {
        if (!buffer.isReadable()) {
            kickClient(context, ProtocolError.TOO_FEW_BYTES);
            return;
        }

        byte operation = buffer.readByte();
        int expectedLength = payloadLength(operation);

        if (expectedLength < 0) {
            kickClient(context, ProtocolError.UNKNOWN_OPERATION);
            return;
        }

        if (buffer.readableBytes() < expectedLength) {
            kickClient(context, ProtocolError.TOO_FEW_BYTES);
            return;
        }

        if (buffer.readableBytes() > expectedLength) {
            kickClient(context, ProtocolError.TOO_MANY_BYTES);
            return;
        }

        try {
            dispatch(operation, buffer, context);
        } catch (IllegalArgumentException exception) {
            kickClient(context, ProtocolError.INVALID_FIELD);
        }
    }

    private void dispatch(byte operation, ByteBuf buffer, ChannelHandlerContext context) {
        switch (operation) {
            case NEW_ORDER -> {
                long tickerId = buffer.readLong();
                Side side = readSide(buffer);
                boolean marketPrice = readBoolean(buffer);
                int price = buffer.readInt();
                int quantity = buffer.readInt();
                long clientRequestId = buffer.readLong();
                boolean kill = readBoolean(buffer);

                exchange.getPublisher(tickerId).process(
                        tickerId, side, marketPrice, price, quantity, clientRequestId, kill, context.channel());
            }
            case SNAPSHOT_REQUEST -> {
                long tickerId = buffer.readLong();
                exchange.getPublisher(tickerId).processSnapshot(tickerId, context.channel());
            }
            case CANCEL_ORDER -> {
                long orderId = buffer.readLong();
                long tickerId = buffer.readLong();
                long clientRequestId = buffer.readLong();
                long secret = buffer.readLong();

                exchange.getPublisher(tickerId).processCancel(orderId, tickerId, secret, clientRequestId, context.channel());
            }
            case MODIFY_ORDER -> {
                long tickerId = buffer.readLong();
                long orderId = buffer.readLong();
                long secret = buffer.readLong();
                long clientRequestId = buffer.readLong();
                int quantity = buffer.readInt();
                int price = buffer.readInt();

                exchange.getPublisher(tickerId).processModification(
                        tickerId, orderId, secret, clientRequestId, quantity, price, context.channel());
            }
            case SUBSCRIBE_MARKET_DATA -> exchange.getMarketDataRegistry().subscribe(buffer.readLong(), context.channel());
            case UNSUBSCRIBE_MARKET_DATA -> exchange.getMarketDataRegistry().unsubscribe(buffer.readLong(), context.channel());
            case REBIND_ORDER -> {
                long tickerId = buffer.readLong();
                long orderId = buffer.readLong();
                long secret = buffer.readLong();
                long clientRequestId = buffer.readLong();

                exchange.getPublisher(tickerId).processRebind(tickerId, orderId, secret, clientRequestId, context.channel());
            }
            case ORDER_STATUS -> {
                long tickerId = buffer.readLong();
                long orderId = buffer.readLong();
                long secret = buffer.readLong();
                long clientRequestId = buffer.readLong();

                exchange.getPublisher(tickerId).processStatusQuery(tickerId, orderId, secret, clientRequestId, context.channel());
            }
            default -> throw new IllegalStateException("Validated operation was not dispatched: " + operation);
        }
    }

    private static int payloadLength(byte operation) {
        return switch (operation) {
            case NEW_ORDER -> NEW_ORDER_LENGTH;
            case SNAPSHOT_REQUEST -> SNAPSHOT_REQUEST_LENGTH;
            case CANCEL_ORDER -> CANCEL_ORDER_LENGTH;
            case MODIFY_ORDER -> MODIFY_ORDER_LENGTH;
            case SUBSCRIBE_MARKET_DATA, UNSUBSCRIBE_MARKET_DATA -> SUBSCRIPTION_LENGTH;
            case REBIND_ORDER -> REBIND_ORDER_LENGTH;
            case ORDER_STATUS -> ORDER_STATUS_LENGTH;
            default -> -1;
        };
    }

    private static Side readSide(ByteBuf buffer) {
        return switch (buffer.readByte()) {
            case 0x01 -> Side.BUY;
            case 0x02 -> Side.SELL;
            default -> throw new IllegalArgumentException("Invalid order side");
        };
    }

    private static boolean readBoolean(ByteBuf buffer) {
        return switch (buffer.readByte()) {
            case 0 -> false;
            case 1 -> true;
            default -> throw new IllegalArgumentException("Invalid boolean value");
        };
    }

    private static void kickClient(ChannelHandlerContext context, ProtocolError error) {
        ByteBuf buffer = context.channel().alloc().buffer(1 + Integer.BYTES);
        buffer.writeByte(SERVER_KICK);
        buffer.writeInt(error.code());
        context.channel().writeAndFlush(buffer).addListener(ChannelFutureListener.CLOSE);
    }
}
