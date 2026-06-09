package me.happy.orderbook.lmax.order;

import lombok.Getter;

public enum OrderEventCommand {
    NEW(0), CANCEL(1), MODIFY(2), SNAPSHOT(3)
    ;

    @Getter
    private int id;

    OrderEventCommand(int id) {
        this.id = id;
    }
}
