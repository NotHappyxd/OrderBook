package me.happy.orderbook.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;
import me.happy.orderbook.Constants;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.server.handlers.CompleteOrderDecoder;
import me.happy.orderbook.server.handlers.LoginHandler;
import me.happy.orderbook.server.handlers.ReplayingOrderDecoder;
import me.happy.orderbook.server.handlers.OrderSnapshotEncoder;

import java.util.concurrent.TimeUnit;

public class ExchangeServer {

    private final int port;

    public ExchangeServer(int port) {
        this.port = port;
    }

    public void startServer(Exchange exchange) throws Exception {
        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new LengthFieldPrepender(Constants.FIELD_LENGTH));
                            socketChannel.pipeline().addLast(new LoginHandler(exchange));
                            socketChannel.pipeline().addLast(new ReadTimeoutHandler(5, TimeUnit.MINUTES));
                            socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(Constants.MAX_MESSAGE_LENGTH, 0, Constants.FIELD_LENGTH, 0, Constants.FIELD_LENGTH));
                            socketChannel.pipeline().addLast(new CompleteOrderDecoder(exchange));
                            socketChannel.pipeline().addLast(new OrderSnapshotEncoder());
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            System.out.println("Exchange Server started on port " + port);
            ChannelFuture future = bootstrap.bind(port).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
