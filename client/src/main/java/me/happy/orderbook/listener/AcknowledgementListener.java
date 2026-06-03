package me.happy.orderbook.listener;

import me.happy.orderbook.packet.impl.OrderAcknowledgementPacket;
import me.happy.orderbook.packet.impl.OrderFilledPacket;
import me.happy.orderbook.packet.impl.SnapshotResponsePacket;
import me.happy.orderbook.packet.listener.PacketHandler;

public class AcknowledgementListener {

    @PacketHandler
    public void acknowledgementReceived(OrderAcknowledgementPacket packet) {
        System.out.printf("Client ID: %d, Server ID: %d\n", packet.getClientOrderId(), packet.getServerOrderId());
    }

    @PacketHandler
    public void snapshotReceived(SnapshotResponsePacket packet) {
        System.out.printf("Snapshot for Ticker %d at Seq %d (Depth %d)%n", packet.getTicker(), packet.getSequence(), packet.getDepth());

        packet.getBids().forEach((key, value) -> System.out.printf("  Bid: Price %d | Qty %d%n", key, value));
        packet.getAsks().forEach((key, value) -> System.out.printf("  Ask: Price %d | Qty %d%n", key, value));
    }

    @PacketHandler
    public void orderFilled(OrderFilledPacket packet) {
        System.out.println(packet.getSide().name() + " Order for ticker " + packet.getTickerId() + " filled! $" + packet.getPrice() + " @ " + packet.getQuantity());
    }
}
