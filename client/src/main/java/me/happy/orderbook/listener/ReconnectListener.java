package me.happy.orderbook.listener;

import me.happy.orderbook.packet.impl.OrderStatusResponsePacket;
import me.happy.orderbook.packet.impl.RebindAcknowledgePacket;
import me.happy.orderbook.packet.listener.PacketHandler;

public class ReconnectListener {

    @PacketHandler
    public void onRebindAck(RebindAcknowledgePacket packet) {
        if (packet.isSuccess()) {
            System.out.printf("Rebound order %d to this connection.%n", packet.getOrderId());
        } else {
            System.out.printf("Failed to rebind order %d - wrong secret, or it's no longer resting.%n", packet.getOrderId());
        }
    }

    @PacketHandler
    public void onOrderStatus(OrderStatusResponsePacket packet) {
        if (!packet.isFound()) {
            System.out.printf("Order %d is no longer resting (fully filled or cancelled).%n", packet.getOrderId());
            return;
        }

        System.out.printf(
                "Order %d on ticker %d: %s, %d remaining @ $%d%n",
                packet.getOrderId(), packet.getTickerId(), packet.getSide(), packet.getQuantity(), packet.getPrice()
        );
    }
}