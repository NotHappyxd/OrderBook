package me.happy.orderbook.lmax.trade;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.happy.orderbook.order.Side;

public class TradeEventHandler implements EventHandler<TradeEvent> {
    private final ChannelGroup allClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void onEvent(TradeEvent event, long sequence, boolean endOfBatch) throws Exception {
        writeTrade(event);

        if (endOfBatch) {
            flush();
        }
    }

    @Override
    public void setSequenceCallback(Sequence sequenceCallback) {
        EventHandler.super.setSequenceCallback(sequenceCallback);
    }

    public void writeTrade(TradeEvent event) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(34);
        buf.writeByte(0x03);
        buf.writeLong(event.getTickerId());
        buf.writeLong(event.getOrderId());
        buf.writeLong(event.getTakerId());
        buf.writeInt(event.getPrice());
        buf.writeInt(event.getQuantity());
        buf.writeByte(event.getTakerSide() == Side.BUY ? 1 : 2);

        allClients.write(buf);
    }

    public void flush() {
        // flush() sends everything to the OS/Socket at once
        allClients.flush();
    }

    public void addClient(Channel ch) {
        allClients.add(ch);
    }
}
