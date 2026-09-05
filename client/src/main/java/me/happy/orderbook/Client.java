package me.happy.orderbook;

import lombok.Getter;
import me.happy.orderbook.listener.AcknowledgementListener;
import me.happy.orderbook.listener.MarketDataListener;
import me.happy.orderbook.listener.ReconnectListener;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.packet.PacketManager;
import me.happy.orderbook.packet.impl.*;

@Getter
public class Client {

    private static Client INSTANCE;
    private final PacketManager packetManager;
    private final ServerConnection serverConnection;

    public Client() {
        INSTANCE = this;

        this.packetManager = new PacketManager();
        this.packetManager.registerPackets(MarketOrderPacket.class, SnapshotRequestPacket.class,
                SnapshotResponsePacket.class, OrderAcknowledgementPacket.class, OrderFilledPacket.class,
                ServerKickPacket.class, OrderModifyPacket.class, OrderModifyAcknowledgePacket.class,
                SubscribeMarketDataPacket.class, UnsubscribeMarketDataPacket.class, MarketDataDeltaPacket.class,
                TradePrintPacket.class, RebindOrderPacket.class, RebindAcknowledgePacket.class,
                OrderStatusRequestPacket.class, OrderStatusResponsePacket.class
        );
        this.packetManager.registerListeners(new AcknowledgementListener(), new MarketDataListener(), new ReconnectListener());

        this.serverConnection = new ServerConnection(this);
        this.serverConnection.connect();
    }

    public static void main(String[] args) throws Exception {
        new Client();

        int clientRequestId = 2;

        getInstance().serverConnection.writePacket(new SubscribeMarketDataPacket("asd"));
        for (int i = 0; i < 1_000_000; i++) {
            getInstance().serverConnection.writePacket(new MarketOrderPacket("asd", Side.BUY, 1, 1, ++clientRequestId));
        }
        //getInstance().serverConnection.writePacket(new MarketOrderPacket("asd", Side.SELL, true, 1, 1, ++clientRequestId, false));
        Thread.sleep(1000L);
        getInstance().serverConnection.writePacket(new SnapshotRequestPacket("asd"));

        // After a reconnect, you'd instead do something like:
        //   getInstance().serverConnection.writePacket(new RebindOrderPacket("asd", savedOrderId, savedSecret, ++clientRequestId));
        //   getInstance().serverConnection.writePacket(new OrderStatusRequestPacket("asd", savedOrderId, savedSecret, ++clientRequestId));
        // using the orderId/secret you saved from that order's OrderAcknowledgementPacket.
    }

    public static Client getInstance() {
        return INSTANCE;
    }
}