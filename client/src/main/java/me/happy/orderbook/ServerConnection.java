package me.happy.orderbook;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.happy.orderbook.packet.Packet;

@Getter
@RequiredArgsConstructor
public class ServerConnection {

    private EventLoopGroup group;
    private Channel channel;
    private final Client client;

    public void connect() {
        this.group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new LengthFieldPrepender(Constants.FIELD_LENGTH));
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Constants.MAX_MESSAGE_LENGTH, 0, Constants.FIELD_LENGTH, 0, Constants.FIELD_LENGTH));
                            ch.pipeline().addLast(new ExchangeClientHandler(client.getPacketManager()));
                        }
                    });

            ChannelFuture f = b.connect("127.0.0.1", 8080).sync();
            this.channel = f.channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void disconnect() {
        if (channel != null) {
            channel.close();
        }

        if (group != null) {
            group.shutdownGracefully();
        }
    }

    public void writePacket(Packet packet) {
        writePacket(packet, true);
    }

    public void writePacket(Packet packet, boolean flush) {
        ByteBuf byteBuf = channel.alloc().buffer();

        byteBuf.writeByte(packet.getId());
        packet.write(byteBuf);

        this.channel.write(byteBuf);

        if (flush) {
            this.channel.flush();
        }
    }
}