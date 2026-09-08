package me.happy.orderbook.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import me.happy.orderbook.protocol.Protocol;
import me.happy.orderbook.protocol.ProtocolError;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class CompleteOrderDecoderTest {

    @Test
    public void rejectsUnknownOperation() {
        assertKick(Unpooled.buffer().writeByte(0x7F), ProtocolError.UNKNOWN_OPERATION);
    }

    @Test
    public void rejectsTruncatedOrderEntry() {
        assertKick(Unpooled.buffer().writeByte(Protocol.ORDER_ENTRY), ProtocolError.TOO_FEW_BYTES);
    }

    @Test
    public void rejectsOversizedSnapshotRequest() {
        ByteBuf frame = Unpooled.buffer(1 + Protocol.SNAPSHOT_REQUEST_LENGTH + 1);
        frame.writeByte(Protocol.SNAPSHOT_REQUEST);
        frame.writeLong(42L);
        frame.writeByte(0);

        assertKick(frame, ProtocolError.TOO_MANY_BYTES);
    }

    @Test
    public void rejectsInvalidOrderSideBeforePublishing() {
        ByteBuf frame = orderEntryFrame();
        frame.setByte(frame.readerIndex() + 1 + Long.BYTES, 3);

        assertKick(frame, ProtocolError.INVALID_FIELD);
    }

    @Test
    public void rejectsInvalidBooleanBeforePublishing() {
        ByteBuf frame = orderEntryFrame();
        frame.setByte(frame.readerIndex() + 1 + Long.BYTES + 1, 2);

        assertKick(frame, ProtocolError.INVALID_FIELD);
    }

    private static ByteBuf orderEntryFrame() {
        ByteBuf frame = Unpooled.buffer(1 + Protocol.ORDER_ENTRY_LENGTH);
        frame.writeByte(Protocol.ORDER_ENTRY);
        frame.writeLong(42L);
        frame.writeByte(Protocol.BUY);
        frame.writeByte(0);
        frame.writeInt(100);
        frame.writeInt(10);
        frame.writeLong(7L);
        frame.writeByte(0);
        return frame;
    }

    private static void assertKick(ByteBuf inbound, ProtocolError expectedError) {
        EmbeddedChannel channel = new EmbeddedChannel(new CompleteOrderDecoder(null));
        try {
            channel.writeInbound(inbound);

            ByteBuf response = channel.readOutbound();
            assertNotNull(response);
            try {
                assertEquals(Protocol.SERVER_KICK, response.readByte());
                assertEquals(expectedError.code(), response.readInt());
                assertFalse(response.isReadable());
            } finally {
                response.release();
            }
            assertFalse(channel.isOpen());
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
