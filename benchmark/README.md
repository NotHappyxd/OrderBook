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
| `OrderBookRestingInsertBenchmark.insertRestingOrders`         | preloadDepth=0      | 150.314 ± 5.496          | ns/op         | —        |
| `OrderBookRestingInsertBenchmark.insertRestingOrders`         | preloadDepth=10000  | 164.413 ± 5.500          | ns/op         | —        |
| `OrderBookRestingInsertBenchmark.insertRestingOrders`         | preloadDepth=100000 | 168.765 ± 4.266          | ns/op         | —        |
| `OrderBookCancelBenchmark.cancelRestingOrders`                | bookDepth=5000      | 274.237 ± 9.702          | µs/op (batch) | 137.1 ns |
| `OrderBookCancelBenchmark.cancelRestingOrders`                | bookDepth=50000     | 334.363 ± 8.762          | µs/op (batch) | 167.2 ns |
| `OrderBookMatchingSingleLevelBenchmark.matchAgainstDeepQueue` | queueDepth=5000     | 461.220 ± 12.067         | µs/op (batch) | 230.6 ns |
| `OrderBookMatchingSingleLevelBenchmark.matchAgainstDeepQueue` | queueDepth=50000    | 508.631 ± 21.802         | µs/op (batch) | 254.3 ns |
| `OrderBookMatchingManyLevelsBenchmark.matchAndDrainLevels`    | bookDepth=5000      | 575.131 ± 10.047         | µs/op (batch) | 287.6 ns |
| `OrderBookMatchingManyLevelsBenchmark.matchAndDrainLevels`    | bookDepth=50000     | 692.567 ± 18.992         | µs/op (batch) | 346.3 ns |
| `PriceLevelFifoBenchmark.churnFifo`                           | steadyDepth=1       | 3.461 ± 0.080            | ns/op         | —        |
| `PriceLevelFifoBenchmark.churnFifo`                           | steadyDepth=1000    | 3.468 ± 0.030            | ns/op         | —        |
| `OrderBookMixedWorkloadBenchmark.mixedTraffic`                | —                   | 3,610,464.66 ± 140,421.18 | ops/s         | —        |

### Object pool vs. plain allocation

Object pooling has more latency than plain allocation, however it has more predictable performances. Pooling results in less allocations and lot less calls to the garbage collector, further improving predictability.

| Benchmark                                    | Latency (ns/op) | Alloc rate                | Alloc/op            | GC count | GC time |
| --------------------------------------------- | --------------- | ------------------------- | ------------------- | -------- | ------- |
| `AllocatorPoolBenchmark.plainAllocation`     | 2.091 ± 0.013   | 27,806.306 ± 531.021 MB/s | 64.000 ± 0.001 B/op | 918      | 439 ms  |
| `AllocatorPoolBenchmark.pooledBorrowRelease` | 2.081 ± 0.004   | 0.007 ± 0.001 MB/s        | ≈10⁻⁵ B/op          | ≈0       | ≈0 ms   |
