package me.happy.orderbook.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.order.Side;

import java.util.List;

public class OrderDecoder extends ReplayingDecoder<State> {
    private Exchange exchange;
    private byte operation;

    public OrderDecoder(Exchange exchange) {
        this.exchange = exchange;
        super(State.READ_OPERATION); // Start by looking for the type
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        switch (state()) {
            case READ_OPERATION -> {
                operation = byteBuf.readByte();
                checkpoint(State.READ_PAYLOAD);
            }

            case READ_PAYLOAD -> {
                if (operation == 0x01) { // Market Order
                    long tickerId = byteBuf.readLong();
                    byte orderType = byteBuf.readByte();
                    int price = byteBuf.readInt();
                    int quantity = byteBuf.readInt();

                    exchange.process(tickerId, orderType == 0x01 ? Side.BUY : Side.SELL, price, quantity, channelHandlerContext.channel());
                }else if (operation == 0x02) {
                    // TODO: Implement snapshot logic
                    long tickerId =  byteBuf.readLong();

                    exchange.processSnapshot(tickerId, channelHandlerContext.channel());
                }

                checkpoint(State.READ_OPERATION);
                break;
            }
        }
    }
}

enum State {
    READ_OPERATION,
    READ_PAYLOAD
}
