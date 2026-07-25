package me.happy.orderbook.listener;

import me.happy.orderbook.packet.impl.MarketDataDeltaPacket;
import me.happy.orderbook.packet.impl.TradePrintPacket;
import me.happy.orderbook.packet.listener.PacketHandler;

public class MarketDataListener {

    @PacketHandler
    public void onLevelUpdate(MarketDataDeltaPacket packet) {
        System.out.printf(
                "Ticker %d Seq %d | %s %d -> qty %d%s%n",
                packet.getTickerId(), packet.getSequence(), packet.getSide(),
                packet.getPrice(), packet.getTotalQuantity(),
                packet.getTotalQuantity() == 0 ? " (level removed)" : ""
        );
    }

    @PacketHandler
    public void onTradePrint(TradePrintPacket packet) {
        System.out.printf(
                "Ticker %d Seq %d | TRADE %d @ $%d (aggressor: %s)%n",
                packet.getTickerId(), packet.getSequence(),
                packet.getQuantity(), packet.getPrice(), packet.getAggressorSide()
        );
    }
}