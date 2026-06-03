package me.happy.orderbook;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import me.happy.orderbook.packet.Packet;
import me.happy.orderbook.packet.PacketManager;

import java.util.Optional;

public class ExchangeClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final PacketManager packetManager;

    public ExchangeClientHandler(PacketManager packetManager) {
        this.packetManager = packetManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("channel active");

        /*
        System.out.println("Connected to Exchange!");
        // 1. Manually construct a Market Order (Type 0x01)
        ByteBuf order = ctx.alloc().buffer();
        order.writeByte(0x01);      // Operation: Market Order
        order.writeLong(12345L);    // Ticker ID
        order.writeByte(0x01);      // Side: BUY
        order.writeInt(150);        // Price
        order.writeInt(100);        // Quantity
        order.writeLong(1);         // Client-side Order Request Id

        ctx.write(order);

        ByteBuf snapshotReq = ctx.alloc().buffer();
        snapshotReq.writeByte(0x02); // Operation: Snapshot Request
        snapshotReq.writeLong(12345L);
        ctx.write(snapshotReq);

        ctx.flush();*/

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        System.out.println("Received message from server! Bytes: " + in.readableBytes());

        byte operation = in.getByte(in.readerIndex());

        Optional<Packet> packet = packetManager.getPacket(operation);
        
        if (packet.isEmpty()) {
            System.out.println("No packet found for operation " + operation);
            return;
        }

        in.readByte();
        packet.get().read(in);
        
        packetManager.publishPacket(packet.get());
        
        /*
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
        }*/
    }
}
