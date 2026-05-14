package me.happy.orderbook.engine;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class OrderSnapshot {
    private final long ticker;
    private long sequenceId;
    private int[] asks = new int[5];
    private int[] bids = new int[5];
    private int[] asksQuantities = new int[5];
    private int[] bidsQuantities = new int[5];
}
