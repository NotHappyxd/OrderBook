package me.happy.orderbook.lmax.order;

import lombok.Getter;

public enum OrderEventCommand {
    NEW(0), CANCEL(1), MODIFY(2), SNAPSHOT(3), REBIND(4), STATUS(5), CHECKPOINT(6),
    JOURNAL_FORCE(7), CHECKPOINT_COMPLETE(8)
    ;

    private static final OrderEventCommand[] BY_ID = new OrderEventCommand[values().length];

    @Getter
    private final int id;

    OrderEventCommand(int id) {
        this.id = id;
    }

    static {
        for (OrderEventCommand command : values()) {
            BY_ID[command.id] = command;
        }
    }

    public static OrderEventCommand fromId(short id) {
        if (id < 0 || id >= BY_ID.length || BY_ID[id] == null) {
            throw new IllegalArgumentException("Unknown order event command id " + id);
        }

        return BY_ID[id];
    }
}
