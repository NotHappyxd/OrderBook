package me.happy.orderbook.packet;

import lombok.Getter;
import me.happy.orderbook.packet.listener.PacketBus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PacketManager {

    private final Map<Integer, MethodHandle> packetMap = new HashMap<>();
    @Getter
    private final PacketBus packetBus = new PacketBus();

    @SafeVarargs
    public final void registerPackets(Class<? extends Packet>... clazz) {
        Arrays.stream(clazz).forEach(this::registerPacket);
    }

    public void registerPacket(Class<? extends Packet> clazz) {
        PacketId packetId = clazz.getAnnotation(PacketId.class);

        if (packetId == null) {
            throw new IllegalArgumentException("Packet class " + clazz.getName() + " must have @PacketId annotation");
        }

        MethodHandles.Lookup lookup = MethodHandles.lookup();

        Optional<Constructor<?>> emptyConstructor = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(constructor -> constructor.getParameterCount() == 0)
                .findFirst();

        if (emptyConstructor.isEmpty()) {
            throw new IllegalArgumentException("Packet class " + clazz.getName() + " must have an empty constructor");
        }

        try {
            MethodHandle constructorHandle = lookup.findConstructor(clazz, MethodType.methodType(void.class));

            packetMap.put(packetId.value(), constructorHandle);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Packet> getPacket(int id) {
        MethodHandle methodHandle = packetMap.get(id);

        if (methodHandle == null) {
            return Optional.empty();
        }

        try {
            return Optional.of((Packet) methodHandle.invoke());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    public MethodHandle getPacketHandleById(int packetId) {
        return packetMap.get(packetId);
    }

    public void registerListeners(Object... listeners) {
        Arrays.stream(listeners).forEach(this::registerListener);
    }

    public void registerListener(Object listener) {
        packetBus.register(listener);
    }

    public void publishPacket(Packet packet) {
        packetBus.post(packet);
    }
}
