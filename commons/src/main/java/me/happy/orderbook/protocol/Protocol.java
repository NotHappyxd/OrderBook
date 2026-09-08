package me.happy.orderbook.protocol;

public final class Protocol {

    public static final int ORDER_ENTRY = 0x01;
    public static final int SNAPSHOT_REQUEST = 0x02;
    public static final int EXECUTION_REPORT = 0x03;
    public static final int SERVER_KICK = 0x04;
    public static final int ORDER_CANCEL = 0x05;
    public static final int SNAPSHOT_RESPONSE = 0x06;
    public static final int ORDER_ACKNOWLEDGEMENT = 0x07;
    public static final int ORDER_MODIFY = 0x08;
    public static final int ORDER_MODIFY_ACKNOWLEDGEMENT = 0x09;
    public static final int MARKET_DATA_SUBSCRIBE = 0x0A;
    public static final int MARKET_DATA_UNSUBSCRIBE = 0x0B;
    public static final int MARKET_DATA_DELTA = 0x0C;
    public static final int TRADE_PRINT = 0x0D;
    public static final int ORDER_REBIND = 0x0E;
    public static final int ORDER_STATUS_REQUEST = 0x0F;
    public static final int ORDER_REBIND_ACKNOWLEDGEMENT = 0x10;
    public static final int ORDER_STATUS_RESPONSE = 0x11;

    public static final byte BUY = 0x01;
    public static final byte SELL = 0x02;

    public static final int ORDER_ENTRY_LENGTH = Long.BYTES + 2 + (Integer.BYTES * 2) + Long.BYTES + 1;
    public static final int SNAPSHOT_REQUEST_LENGTH = Long.BYTES;
    public static final int ORDER_CANCEL_LENGTH = Long.BYTES * 4;
    public static final int ORDER_MODIFY_LENGTH = (Long.BYTES * 4) + (Integer.BYTES * 2);
    public static final int MARKET_DATA_SUBSCRIPTION_LENGTH = Long.BYTES;
    public static final int ORDER_REBIND_LENGTH = Long.BYTES * 4;
    public static final int ORDER_STATUS_REQUEST_LENGTH = Long.BYTES * 4;

    public static final int EXECUTION_REPORT_LENGTH = (Long.BYTES * 2) + (Integer.BYTES * 3) + 1;
    public static final int MARKET_DATA_DELTA_LENGTH = (Long.BYTES * 2) + (Integer.BYTES * 2) + 1;
    public static final int TRADE_PRINT_LENGTH = (Long.BYTES * 2) + (Integer.BYTES * 2) + 1;
    public static final int SNAPSHOT_RESPONSE_HEADER_LENGTH = (Long.BYTES * 2) + 1;
    public static final int SNAPSHOT_LEVEL_LENGTH = Integer.BYTES * 4;
    public static final int ORDER_ACKNOWLEDGEMENT_LENGTH = Long.BYTES * 3;
    public static final int ORDER_MODIFY_ACKNOWLEDGEMENT_LENGTH = (Long.BYTES * 3) + (Integer.BYTES * 2);
    public static final int ORDER_REBIND_ACKNOWLEDGEMENT_LENGTH = 1 + (Long.BYTES * 2);
    public static final int ORDER_STATUS_RESPONSE_LENGTH = 2 + (Long.BYTES * 3) + (Integer.BYTES * 2);
    public static final int SERVER_KICK_LENGTH = Integer.BYTES;

    private Protocol() {
    }
}
