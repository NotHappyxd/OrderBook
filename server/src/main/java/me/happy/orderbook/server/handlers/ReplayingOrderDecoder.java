package me.happy.orderbook.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.order.Side;

import java.util.List;

@Deprecated
public class ReplayingOrderDecoder extends ReplayingDecoder<DecoderState> {
    private final Exchange exchange;
    private byte operation;

    public ReplayingOrderDecoder(Exchange exchange) {
        this.exchange = exchange;
        super(DecoderState.READ_OPERATION); // Start by looking for the type
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        switch (state()) {
            case READ_OPERATION -> {
                operation = byteBuf.readByte();
                System.out.println(operation);
                checkpoint(DecoderState.READ_PAYLOAD);
            }

            case READ_PAYLOAD -> {
                if (operation == 0x01) { // Market Order
                    long tickerId = byteBuf.readLong();
                    byte orderType = byteBuf.readByte();
                    int price = byteBuf.readInt();
                    int quantity = byteBuf.readInt();
                    long clientRequestId = byteBuf.readLong();

                    System.out.println(clientRequestId);
                    exchange.process(tickerId, orderType == 0x01 ? Side.BUY : Side.SELL, price, quantity, clientRequestId, channelHandlerContext.channel());
                } else if (operation == 0x02) {
                    long tickerId = byteBuf.readLong();

                    exchange.processSnapshot(tickerId, channelHandlerContext.channel());
                }

                checkpoint(DecoderState.READ_OPERATION);
                break;
            }
        }
    }
}