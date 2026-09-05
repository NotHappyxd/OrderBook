## Benchmark Results

**Environment:** JDK 26.0.2 (OpenJDK 64-Bit Server VM) · JMH 1.37 · 3 forks · warmup 5×2s · measurement 10×1s (5x2s for mixed workload)

**OS:** Arch Linux

**CPU:** Framework 13 with AMD Ryzen AI 5 340

**Ram:** 32 GB (2x16) DDR5-5600

### Matching engine throughput

These benchmarks are the theoretical maximums. They exclude networking (ingress/egress) and focus on the performance of the Orderbook rather than wire/messaging.

`OrderBookCancelBenchmark`, `OrderBookMatchingSingleLevelBenchmark`, and `OrderBookMatchingManyLevelsBenchmark` report total time for a **2,000-op batch** (`avgt`, not per-op) — the "Per-op" column below is that score divided by 2,000. `OrderBookRestingInsertBenchmark` and `PriceLevelFifoBenchmark` use `@OperationsPerInvocation`, so JMH already reports per-op directly.

| Benchmark                                                     | Params              | Score ± Error            | Units         | Per-op   |
| ------------------------------------------------------------- | ------------------- | ------------------------ | ------------- | -------- |
| `OrderBookRestingInsertBenchmark.insertRestingOrders`         | preloadDepth=0      | 97.392 ± 8.195           | ns/op         | —        |
| `OrderBookRestingInsertBenchmark.insertRestingOrders`         | preloadDepth=10000  | 130.479 ± 6.004          | ns/op         | —        |
| `OrderBookRestingInsertBenchmark.insertRestingOrders`         | preloadDepth=100000 | 127.361 ± 16.600         | ns/op         | —        |
| `OrderBookCancelBenchmark.cancelRestingOrders`                | bookDepth=5000      | 139.004 ± 9.731          | µs/op (batch) | 69.5 ns  |
| `OrderBookCancelBenchmark.cancelRestingOrders`                | bookDepth=50000     | 187.362 ± 17.821         | µs/op (batch) | 93.7 ns  |
| `OrderBookMatchingSingleLevelBenchmark.matchAgainstDeepQueue` | queueDepth=5000     | 258.181 ± 24.603         | µs/op (batch) | 129.1 ns |
| `OrderBookMatchingSingleLevelBenchmark.matchAgainstDeepQueue` | queueDepth=50000    | 279.054 ± 22.202         | µs/op (batch) | 139.5 ns |
| `OrderBookMatchingManyLevelsBenchmark.matchAndDrainLevels`    | bookDepth=5000      | 286.618 ± 19.541         | µs/op (batch) | 143.3 ns |
| `OrderBookMatchingManyLevelsBenchmark.matchAndDrainLevels`    | bookDepth=50000     | 340.138 ± 15.565         | µs/op (batch) | 170.1 ns |
| `PriceLevelFifoBenchmark.churnFifo`                           | steadyDepth=1       | 3.432 ± 0.088            | ns/op         | —        |
| `PriceLevelFifoBenchmark.churnFifo`                           | steadyDepth=1000    | 3.530 ± 0.049            | ns/op         | —        |
| `OrderBookMixedWorkloadBenchmark.mixedTraffic`                | —                   | 7,900,690.42 ± 742,884.17 | ops/s         | —        |

### Object pool vs. plain allocation

Object pooling has more latency than plain allocation, however it has more predictable performances. Pooling results in less allocations and lot less calls to the garbage collector, further improving predictability.

| Benchmark                                    | Latency (ns/op) | Alloc rate                | Alloc/op            | GC count | GC time |
| --------------------------------------------- | --------------- | ------------------------- | ------------------- | -------- | ------- |
| `AllocatorPoolBenchmark.plainAllocation`     | 2.063 ± 0.017   | 27,806.306 ± 531.021 MB/s | 64.000 ± 0.001 B/op | 918      | 439 ms  |
| `AllocatorPoolBenchmark.pooledBorrowRelease` | 2.081 ± 0.007   | 0.007 ± 0.001 MB/s        | ≈10⁻⁵ B/op          | ≈0       | ≈0 ms   |
