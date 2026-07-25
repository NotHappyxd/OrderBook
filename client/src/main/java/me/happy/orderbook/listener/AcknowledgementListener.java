package me.happy.orderbook.listener;

import me.happy.orderbook.Client;
import me.happy.orderbook.TickerUtils;
import me.happy.orderbook.packet.impl.*;
import me.happy.orderbook.packet.listener.PacketHandler;

public class AcknowledgementListener {

    @PacketHandler
    public void acknowledgementReceived(OrderAcknowledgementPacket packet) {
        System.out.printf("Client ID: %d, Server ID: %d, Secret: %d\n", packet.getClientOrderId(), packet.getServerOrderId(), packet.getSecret());

        if (packet.getClientOrderId() == 3) {
            Client.getInstance().getServerConnection().writePacket(new OrderModifyPacket(
                    TickerUtils.packString("asd"), packet.getServerOrderId(), packet.getSecret(), 100, 2, 1
            ));
            //Client.getInstance().getServerConnection().writePacket(new OrderCancelPacket(packet.getServerOrderId(),
            //        TickerUtils.packString("asd"), 11, packet.getSecret()));
        }
    }

    @PacketHandler
    public void acknowledgeModify(OrderModifyAcknowledgePacket packet) {
        System.out.println(packet.toString());
    }
    @PacketHandler
    public void snapshotReceived(SnapshotResponsePacket packet) {
        System.out.printf("Snapshot for Ticker %d at Seq %d (Depth %d)%n", packet.getTicker(), packet.getSequence(), packet.getDepth());

        packet.getBids().forEach((key, value) -> System.out.printf("  Bid: Price %d | Qty %d%n", key, value));
        packet.getAsks().forEach((key, value) -> System.out.printf("  Ask: Price %d | Qty %d%n", key, value));
    }

    @PacketHandler
    public void orderFilled(OrderFilledPacket packet) {
        System.out.printf(
                "Your %s order %d on ticker %d filled %d @ $%d (remaining: %d)%n",
                packet.getSide(), packet.getOrderId(), packet.getTickerId(),
                packet.getQuantity(), packet.getPrice(), packet.getRemainingQuantity()
        );
    }

    @PacketHandler
    public void onKick(ServerKickPacket packet) {
        System.out.println(packet.getKickReason());
    }
}