package me.happy.orderbook.order;

public enum Side {
    BUY, SELL;

    private static final Side[] VALUES = values();

    public static Side fromOrdinal(short ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) {
            throw new IllegalArgumentException("Unknown side ordinal " + ordinal);
        }

        return VALUES[ordinal];
    }
}
