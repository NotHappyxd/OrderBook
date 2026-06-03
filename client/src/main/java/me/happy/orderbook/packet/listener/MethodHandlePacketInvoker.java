package me.happy.orderbook.packet.listener;

import lombok.RequiredArgsConstructor;
import me.happy.orderbook.packet.Packet;

import java.lang.invoke.MethodHandle;

@RequiredArgsConstructor
public class MethodHandlePacketInvoker implements PacketInvoker {

    private final MethodHandle methodHandle;

    @Override
    public void invoke(Packet packet) {
        try {
            methodHandle.invoke(packet);
        } catch (Throwable e) {
            throw new RuntimeException("Packet invoker failed", e);
        }
    }
}
