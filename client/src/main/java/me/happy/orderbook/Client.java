package me.happy.orderbook;

import lombok.Getter;
import me.happy.orderbook.listener.AcknowledgementListener;
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
                ServerKickPacket.class, OrderModifyPacket.class, OrderModifyAcknowledgePacket.class
        );
        this.packetManager.registerListeners(new AcknowledgementListener());

        this.serverConnection = new ServerConnection(this);
        this.serverConnection.connect();
    }

    public static void main(String[] args) throws Exception {
        new Client();

        int clientRequestId = 2;

        getInstance().serverConnection.writePacket(new MarketOrderPacket("asd", Side.BUY, 1, 1, ++clientRequestId));
        getInstance().serverConnection.writePacket(new MarketOrderPacket("asd", Side.BUY, 2, 1, ++clientRequestId));
        getInstance().serverConnection.writePacket(new MarketOrderPacket("asd", Side.BUY, 3, 1, ++clientRequestId));
        getInstance().serverConnection.writePacket(new MarketOrderPacket("asd", Side.SELL, 5, 1, ++clientRequestId));
        getInstance().serverConnection.writePacket(new MarketOrderPacket("asd", Side.SELL, 4, 1, ++clientRequestId));
        getInstance().serverConnection.writePacket(new MarketOrderPacket("asd", Side.SELL, 3, 1, ++clientRequestId));
        Thread.sleep(1000L);
        getInstance().serverConnection.writePacket(new SnapshotRequestPacket("asd"));
    }

    public static Client getInstance() {
        return INSTANCE;
    }
}

