package me.happy.orderbook.lmax.journal;

import lombok.Getter;
import me.happy.orderbook.lmax.order.OrderEvent;
import me.happy.orderbook.lmax.order.OrderEventCommand;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public class Journal implements Closeable {

    public static final int LENGTH;
    private FileChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
    private final Path path;
    @Getter
    private final Path pendingPath;

    public Journal(Path path) throws Exception {
        this.path = path;
        this.pendingPath = path.resolveSibling(path.getFileName() + ".pending");
        this.channel = open(this.path);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                force();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public void append(OrderEvent event) throws IOException {
        if (event.getCommand() == OrderEventCommand.SNAPSHOT
                || event.getCommand() == OrderEventCommand.REBIND
                || event.getCommand() == OrderEventCommand.STATUS
                || event.getCommand() == OrderEventCommand.CHECKPOINT) return;

        if (buffer.remaining() < LENGTH) {
            flush();
        }

        buffer.putShort((short) event.getCommand().getId());
        buffer.putLong(event.getTicker());
        buffer.putLong(event.getOrderId());
        buffer.putLong(event.getSecret());

        int side = event.getCommand() == OrderEventCommand.NEW ? event.getSide().ordinal() : 0;
        buffer.putShort((short) side);

        int price = event.getCommand() == OrderEventCommand.CANCEL ? 0 : event.getPrice();
        int quantity = event.getCommand() == OrderEventCommand.CANCEL ? 0 : event.getQuantity();
        buffer.putInt(price);
        buffer.putInt(quantity);
        buffer.put(event.isKill() ? (byte) 1 : 0);
    }

    public void flush() throws IOException {
        buffer.flip();

        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }

        buffer.clear();
    }

    public void force() throws IOException {
        flush();
        channel.force(false);
    }

    public void rotate() throws IOException {
        force();
        this.channel.close();

        Files.move(this.path, this.pendingPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);

        Path freshTemp = this.path.resolveSibling(this.path.getFileName() + ".fresh");
        Files.deleteIfExists(freshTemp);

        try (FileChannel freshChannel = FileChannel.open(freshTemp, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            freshChannel.force(true);
        }

        Files.move(freshTemp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        this.channel = open(path);
    }

    private FileChannel open(Path path) throws IOException {
        return FileChannel.open(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }

    public boolean hasPendingRotation() {
        return Files.exists(this.pendingPath);
    }

    public void markCheckpointComplete() throws IOException {
        Files.deleteIfExists(this.pendingPath);
    }

    @Override
    public void close() throws IOException {
        force();
        channel.close();
    }

    static {
        LENGTH = Short.BYTES + Long.BYTES + Long.BYTES + Long.BYTES
                + Short.BYTES + Integer.BYTES + Integer.BYTES + 1;
    }
}
