package me.happy.orderbook.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;
import me.happy.orderbook.lmax.Exchange;
import me.happy.orderbook.server.handlers.LoginHandler;
import me.happy.orderbook.server.handlers.OrderDecoder;
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

        short fieldLength = 2;

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new LengthFieldPrepender(fieldLength));
                            socketChannel.pipeline().addLast(new LoginHandler(exchange));
                            socketChannel.pipeline().addLast(new ReadTimeoutHandler(5, TimeUnit.MINUTES));
                            socketChannel.pipeline().addLast(new OrderDecoder(exchange));
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
