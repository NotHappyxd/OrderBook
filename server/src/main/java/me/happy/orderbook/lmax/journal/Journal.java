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
import java.util.zip.CRC32C;

public class Journal implements Closeable {

    public static final int MAGIC = 0x4A4E4C33;
    public static final int VERSION = 3;
    public static final int HEADER_LENGTH = Integer.BYTES + Integer.BYTES;
    public static final int PAYLOAD_LENGTH = Short.BYTES + Long.BYTES + Long.BYTES + Long.BYTES
            + Long.BYTES + Short.BYTES + Integer.BYTES + 1 + Integer.BYTES + 1;
    public static final int LENGTH = PAYLOAD_LENGTH + Integer.BYTES;
    private static final int BUFFER_SIZE = 256 * 1024;
    private FileChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final byte[] recordBytes = new byte[PAYLOAD_LENGTH];
    private final ByteBuffer recordBuffer = ByteBuffer.wrap(recordBytes);
    private final CRC32C checksum = new CRC32C();
    private boolean hasUnforcedWrites;
    private final Path path;
    @Getter
    private final Path pendingPath;

    public Journal(Path path) throws Exception {
        this.path = path;
        this.pendingPath = path.resolveSibling(path.getFileName() + ".pending");
        this.channel = open(this.path);
        initializeOrVerifyHeader(this.channel, this.path);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                forceToStorage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public void append(OrderEvent event, long sequence) throws IOException {
        if (event.getCommand() == OrderEventCommand.SNAPSHOT
                || event.getCommand() == OrderEventCommand.REBIND
                || event.getCommand() == OrderEventCommand.STATUS
                || event.getCommand() == OrderEventCommand.CHECKPOINT
                || event.getCommand() == OrderEventCommand.JOURNAL_FORCE
                || event.getCommand() == OrderEventCommand.CHECKPOINT_COMPLETE) return;

        if (buffer.remaining() < LENGTH) {
            flush();
        }

        recordBuffer.clear();
        recordBuffer.putShort((short) event.getCommand().getId());
        recordBuffer.putLong(sequence);
        recordBuffer.putLong(event.getTicker());
        recordBuffer.putLong(event.getOrderId());
        recordBuffer.putLong(event.getSecret());

        int side = event.getCommand() == OrderEventCommand.NEW ? event.getSide().ordinal() : 0;
        recordBuffer.putShort((short) side);

        int price = event.getCommand() == OrderEventCommand.CANCEL ? 0 : event.getPrice();
        int quantity = event.getCommand() == OrderEventCommand.CANCEL ? 0 : event.getQuantity();
        recordBuffer.putInt(price);
        recordBuffer.put(event.isMarketPrice() ? (byte) 1 : 0);
        recordBuffer.putInt(quantity);
        recordBuffer.put(event.isKill() ? (byte) 1 : 0);

        checksum.reset();
        checksum.update(recordBytes, 0, PAYLOAD_LENGTH);
        buffer.put(recordBytes);
        buffer.putInt((int) checksum.getValue());
    }

    public boolean flush() throws IOException {
        if (buffer.position() == 0) {
            return false;
        }

        buffer.flip();

        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }

        buffer.clear();
        hasUnforcedWrites = true;
        return true;
    }

    public boolean forceToStorage() throws IOException {
        flush();

        if (!hasUnforcedWrites) {
            return false;
        }

        channel.force(false);
        hasUnforcedWrites = false;
        return true;
    }

    public void rotate() throws IOException {
        forceToStorage();
        this.channel.close();

        Files.move(this.path, this.pendingPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);

        Path freshTemp = this.path.resolveSibling(this.path.getFileName() + ".fresh");
        Files.deleteIfExists(freshTemp);

        try (FileChannel freshChannel = FileChannel.open(freshTemp, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            writeHeader(freshChannel);
            freshChannel.force(true);
        }

        Files.move(freshTemp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        this.channel = open(path);
    }

    private FileChannel open(Path path) throws IOException {
        return FileChannel.open(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }

    public static void verifyHeader(FileChannel channel, Path path) throws IOException {
        if (channel.size() < HEADER_LENGTH) {
            throw new IOException("Journal " + path + " is missing a complete header");
        }

        ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH);
        while (header.hasRemaining()) {
            int bytesRead = channel.read(header, header.position());

            if (bytesRead <= 0) {
                throw new IOException("Could not read journal header from " + path);
            }
        }

        header.flip();
        int magic = header.getInt();
        int version = header.getInt();

        if (magic != MAGIC || version != VERSION) {
            throw new IOException("Unsupported journal format in " + path + "; expected JNL3");
        }
    }

    private void initializeOrVerifyHeader(FileChannel channel, Path path) throws IOException {
        if (channel.size() == 0) {
            writeHeader(channel);
            return;
        }

        verifyHeader(channel, path);
    }

    private void writeHeader(FileChannel channel) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH);
        header.putInt(MAGIC);
        header.putInt(VERSION);
        header.flip();

        while (header.hasRemaining()) {
            channel.write(header);
        }
    }

    public boolean hasPendingRotation() {
        return Files.exists(this.pendingPath);
    }

    public void markCheckpointComplete() throws IOException {
        Files.deleteIfExists(this.pendingPath);
    }

    @Override
    public void close() throws IOException {
        forceToStorage();
        channel.close();
    }

}
