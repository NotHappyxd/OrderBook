package me.happy.orderbook;

import lombok.Getter;
import me.happy.orderbook.listener.AcknowledgementListener;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.packet.PacketManager;
import me.happy.orderbook.packet.impl.*;

import java.nio.channels.Channel;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class Client {

    private static Client INSTANCE;
    private final PacketManager packetManager;
    private final ServerConnection serverConnection;

    public Client() {
        INSTANCE = this;

        this.packetManager = new PacketManager();
        this.packetManager.registerPackets(MarketOrderPacket.class, SnapshotRequestPacket.class, SnapshotResponsePacket.class, OrderAcknowledgementPacket.class, OrderFilledPacket.class);
        this.packetManager.registerListeners(new AcknowledgementListener());

        this.serverConnection = new ServerConnection(this);
        this.serverConnection.connect();
    }

    public static void main(String[] args) throws Exception {
        new Client();

        getInstance().serverConnection.writePacket(new MarketOrderPacket("asd", Side.BUY, 1, 1, 3));
        getInstance().serverConnection.writePacket(new MarketOrderPacket("asd", Side.SELL, 1, 1, 4));
        getInstance().serverConnection.writePacket(new SnapshotRequestPacket("asd"));
    }

    public static Client getInstance() {
        return INSTANCE;
    }
}

