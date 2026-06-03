package me.happy.orderbook.packet.listener;

import me.happy.orderbook.packet.Packet;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.*;

public class PacketBus {

    private final Map<Class<? extends Packet>, List<PacketInvoker>> invokerMap = new HashMap<>();

    public void register(Object listener) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        Arrays.stream(listener.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PacketHandler.class))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> Packet.class.isAssignableFrom(method.getParameters()[0].getType()))
                .forEach(method -> {
                    method.setAccessible(true);

                    try {
                        MethodHandle handle = lookup.unreflect(method)
                                .bindTo(listener);

                        Class<? extends Packet> packetType = (Class<? extends Packet>) method.getParameters()[0].getType();

                        invokerMap.putIfAbsent(packetType, new ArrayList<>());
                        invokerMap.get(packetType).add(new MethodHandlePacketInvoker(handle));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public void post(Packet packet) {
        List<PacketInvoker> invokers = invokerMap.get(packet.getClass());

        if (invokers == null) {
            return;
        }

        for (PacketInvoker invoker : invokers) {
            invoker.invoke(packet);
        }
    }
}
