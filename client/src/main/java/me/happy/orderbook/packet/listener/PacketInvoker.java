package me.happy.orderbook.packet.listener;

import me.happy.orderbook.packet.Packet;

public interface PacketInvoker {

    void invoke(Packet packet);
}
