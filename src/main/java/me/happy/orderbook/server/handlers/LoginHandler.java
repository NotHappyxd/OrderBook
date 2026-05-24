package me.happy.orderbook.server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import me.happy.orderbook.lmax.Exchange;

@RequiredArgsConstructor
public class LoginHandler extends ChannelInboundHandlerAdapter {

    private final Exchange exchange;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        exchange.getTradeEventHandler().addClient(ctx.channel());
        ctx.fireChannelActive();
    }
}
