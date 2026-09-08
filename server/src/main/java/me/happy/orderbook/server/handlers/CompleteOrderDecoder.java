package me.happy.orderbook.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.protocol.ProtocolError;
import me.happy.orderbook.protocol.Protocol;

@RequiredArgsConstructor
public class CompleteOrderDecoder extends SimpleChannelInboundHandler<ByteBuf> {

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
            case Protocol.ORDER_ENTRY -> {
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
            case Protocol.SNAPSHOT_REQUEST -> {
                long tickerId = buffer.readLong();
                long requestId = buffer.readLong();
                exchange.getPublisher(tickerId).processSnapshot(tickerId, requestId, context.channel());
            }
            case Protocol.ORDER_CANCEL -> {
                long orderId = buffer.readLong();
                long tickerId = buffer.readLong();
                long clientRequestId = buffer.readLong();
                long secret = buffer.readLong();

                exchange.getPublisher(tickerId).processCancel(orderId, tickerId, secret, clientRequestId, context.channel());
            }
            case Protocol.ORDER_MODIFY -> {
                long tickerId = buffer.readLong();
                long orderId = buffer.readLong();
                long secret = buffer.readLong();
                long clientRequestId = buffer.readLong();
                int quantity = buffer.readInt();
                int price = buffer.readInt();

                exchange.getPublisher(tickerId).processModification(
                        tickerId, orderId, secret, clientRequestId, quantity, price, context.channel());
            }
            case Protocol.MARKET_DATA_SUBSCRIBE -> exchange.getMarketDataRegistry().subscribe(buffer.readLong(), context.channel());
            case Protocol.MARKET_DATA_UNSUBSCRIBE -> exchange.getMarketDataRegistry().unsubscribe(buffer.readLong(), context.channel());
            case Protocol.ORDER_REBIND -> {
                long tickerId = buffer.readLong();
                long orderId = buffer.readLong();
                long secret = buffer.readLong();
                long clientRequestId = buffer.readLong();

                exchange.getPublisher(tickerId).processRebind(tickerId, orderId, secret, clientRequestId, context.channel());
            }
            case Protocol.ORDER_STATUS_REQUEST -> {
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
            case Protocol.ORDER_ENTRY -> Protocol.ORDER_ENTRY_LENGTH;
            case Protocol.SNAPSHOT_REQUEST -> Protocol.SNAPSHOT_REQUEST_LENGTH;
            case Protocol.ORDER_CANCEL -> Protocol.ORDER_CANCEL_LENGTH;
            case Protocol.ORDER_MODIFY -> Protocol.ORDER_MODIFY_LENGTH;
            case Protocol.MARKET_DATA_SUBSCRIBE, Protocol.MARKET_DATA_UNSUBSCRIBE -> Protocol.MARKET_DATA_SUBSCRIPTION_LENGTH;
            case Protocol.ORDER_REBIND -> Protocol.ORDER_REBIND_LENGTH;
            case Protocol.ORDER_STATUS_REQUEST -> Protocol.ORDER_STATUS_REQUEST_LENGTH;
            default -> -1;
        };
    }

    private static Side readSide(ByteBuf buffer) {
        return switch (buffer.readByte()) {
            case Protocol.BUY -> Side.BUY;
            case Protocol.SELL -> Side.SELL;
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
        buffer.writeByte(Protocol.SERVER_KICK);
        buffer.writeInt(error.code());
        context.channel().writeAndFlush(buffer).addListener(ChannelFutureListener.CLOSE);
    }
}
