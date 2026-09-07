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
import java.util.Arrays;

public class JournalReplayer {

    private final int recordLength;
    private final OrderEventProcessor processor;

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

    private void processRecord(ByteBuffer buffer, long afterSequence) {
        short commandId = buffer.getShort();
        long sequence = buffer.getLong();
        long ticker = buffer.getLong();
        long orderId = buffer.getLong();
        long secret = buffer.getLong();

        short side = buffer.getShort();
        int price = buffer.getInt();
        int quantity = buffer.getInt();
        boolean kill = buffer.get() == 1;

        OrderEventCommand command = Arrays.stream(OrderEventCommand.values())
                .filter(cmd -> cmd.getId() == commandId)
                .findFirst()
                .orElse(null);

        if (command == null) throw new RuntimeException("Could not parse OrderEventCommand id " + commandId);

        if (sequence <= afterSequence) {
            return;
        }

        OrderEvent orderEvent = new OrderEvent();

        orderEvent.setCommand(command);
        orderEvent.setTicker(ticker);
        orderEvent.setOrderId(orderId);
        orderEvent.setClientRequestId(orderEvent.getClientRequestId());
        orderEvent.setSecret(secret);

        if (command == OrderEventCommand.NEW) {
            orderEvent.setSide(Side.values()[side]);
            orderEvent.setKill(kill);
        }

        if (command != OrderEventCommand.CANCEL) {
            orderEvent.setPrice(price);
            orderEvent.setQuantity(quantity);
        }

        processor.process(orderEvent, sequence, false);
    }
}
