package me.happy.orderbook;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.util.concurrent.TimeUnit;

public class Client {

    static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();

        short fieldLength = 2;
        short maxMessageLength = 512; // Messages should never exceed 512 bytes (in theory)

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(maxMessageLength, 0, fieldLength, 0, fieldLength));
                            ch.pipeline().addLast(new ExchangeClientHandler());
                        }
                    });

            ChannelFuture f = b.connect("127.0.0.1", 8080).sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class ExchangeClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Connected to Exchange! Sending a Market Order...");

        // 1. Manually construct a Market Order (Type 0x01)
        ByteBuf order = ctx.alloc().buffer();
        order.writeByte(0x01);      // Operation: Market Order
        order.writeLong(12345L);    // Ticker ID
        order.writeByte(0x01);      // Side: BUY
        order.writeInt(150);        // Price
        order.writeInt(100);        // Quantity
        order.writeLong(1);            // Client-side Order Request Id

        ctx.write(order);

        ByteBuf snapshotReq = ctx.alloc().buffer();
        snapshotReq.writeByte(0x02); // Operation: Snapshot Request
        snapshotReq.writeLong(12345L);
        ctx.write(snapshotReq);

        ctx.flush();

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        System.out.println("Received message from server! Bytes: " + in.readableBytes());

        byte operation = in.getByte(in.readerIndex());

        if (operation == 7) {
            in.readByte();

            System.out.println("Order Acknowledgement Received for order (Client ID: " + in.readLong() + ", Server ID: " + in.readLong() + ")");
        } else if (operation == 6) {
            in.readByte();

            long ticker = in.readLong();
            long sequence = in.readLong();
            int depth = in.readByte();

            System.out.printf("Snapshot for Ticker %d at Seq %d (Depth %d)%n", ticker, sequence, depth);

            // Read Bids
            for (int i = 0; i < depth; i++) {
                System.out.printf("  Bid: Price %d | Qty %d%n", in.readInt(), in.readInt());
            }
            // Read Asks
            for (int i = 0; i < depth; i++) {
                System.out.printf("  Ask: Price %d | Qty %d%n", in.readInt(), in.readInt());
            }
        } else if (operation == 3) {
            in.readByte();

            long tickerId = in.readLong();
            long orderId = in.readLong();
            long takerId = in.readLong();
            int price = in.readInt();
            int quantity = in.readInt();
            byte side = in.readByte();

            System.out.println((side == 0x01 ? "BUY" : "SELL") + " Order for ticker " + tickerId + " filled! $" + price + " @ " + quantity);
        }
    }
}
