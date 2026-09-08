package me.happy.orderbook.lmax.journal;

import me.happy.orderbook.lmax.order.OrderEvent;
import me.happy.orderbook.lmax.order.OrderEventCommand;
import me.happy.orderbook.order.Side;
import me.happy.orderbook.processor.OrderEventProcessor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.CRC32C;

public class JournalReplayer {

    private final int recordLength;
    private final OrderEventProcessor processor;
    private final OrderEvent replayEvent = new OrderEvent();
    private final byte[] recordBytes = new byte[Journal.PAYLOAD_LENGTH];
    private final ByteBuffer recordBuffer = ByteBuffer.wrap(recordBytes);
    private final CRC32C checksum = new CRC32C();

    public JournalReplayer(int recordLength, OrderEventProcessor processor) {
        this.recordLength = recordLength;
        this.processor = processor;
    }

    public void replay(Path journalFile, long afterSequence) throws IOException {
        try (FileChannel channel = FileChannel.open(
                journalFile,
                StandardOpenOption.READ)) {
            Journal.verifyHeader(channel, journalFile);
            channel.position(Journal.HEADER_LENGTH);
            ByteBuffer buffer = ByteBuffer.allocateDirect(64 * 1024);

            while (channel.read(buffer) > 0) {
                buffer.flip();

                while (buffer.remaining() >= recordLength) {
                    processRecord(buffer, afterSequence);
                }

                buffer.compact();
            }
        }
    }

    private void processRecord(ByteBuffer buffer, long afterSequence) throws IOException {
        buffer.get(recordBytes);
        int expectedChecksum = buffer.getInt();

        checksum.reset();
        checksum.update(recordBytes, 0, Journal.PAYLOAD_LENGTH);

        if ((int) checksum.getValue() != expectedChecksum) {
            throw new IOException("Journal record checksum mismatch");
        }

        recordBuffer.clear();
        short commandId = recordBuffer.getShort();
        long sequence = recordBuffer.getLong();
        long ticker = recordBuffer.getLong();
        long orderId = recordBuffer.getLong();
        long secret = recordBuffer.getLong();

        short side = recordBuffer.getShort();
        int price = recordBuffer.getInt();
        boolean marketOrder = recordBuffer.get() == 1;
        int quantity = recordBuffer.getInt();
        boolean kill = recordBuffer.get() == 1;

        OrderEventCommand command = OrderEventCommand.fromId(commandId);

        if (sequence <= afterSequence) {
            return;
        }

        replayEvent.setCommand(command);
        replayEvent.setTicker(ticker);
        replayEvent.setOrderId(orderId);
        replayEvent.setClientRequestId(0);
        replayEvent.setSecret(secret);
        replayEvent.setChannel(null);
        replayEvent.setSide(null);
        replayEvent.setMarketPrice(marketOrder);
        replayEvent.setPrice(0);
        replayEvent.setQuantity(0);
        replayEvent.setKill(false);

        if (command == OrderEventCommand.NEW) {
            replayEvent.setSide(Side.fromOrdinal(side));
            replayEvent.setKill(kill);
        }

        if (command != OrderEventCommand.CANCEL) {
            replayEvent.setPrice(price);
            replayEvent.setQuantity(quantity);
        }

        processor.process(replayEvent, sequence, false);
    }
}
