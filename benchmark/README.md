## Benchmark Results

**Environment:** JDK 26.0.2 (OpenJDK 64-Bit Server VM) · JMH 1.37 · 1 fork · warmup 3×1s · measurement 5×1s (2s for mixed workload)

**OS:** Arch Linux

**CPU:** Framework 13 with AMD Ryzen AI 5 340

**Ram:** 32 GB (2x16) DDR5-5600


### Matching engine throughput
These benchmarks are the theoretical maximums. They exclude networking (ingress/egress) and focus on the performance of the Orderbook rather than wire/messaging.

`OrderBookCancelBenchmark`, `OrderBookMatchingSingleLevelBenchmark`, and `OrderBookMatchingManyLevelsBenchmark` report total time for a **2,000-op batch** (`avgt`, not per-op) — the "Per-op" column below is that score divided by 2,000. `OrderBookRestingInsertBenchmark` and `PriceLevelFifoBenchmark` use `@OperationsPerInvocation`, so JMH already reports per-op directly.

| Benchmark | Params | Score ± Error | Units | Per-op |
|---|---|---|---|---|
| `OrderBookRestingInsertBenchmark.insertRestingOrders` | preloadDepth=0 | 152.387 ± 4.615 | ns/op | — |
| `OrderBookRestingInsertBenchmark.insertRestingOrders` | preloadDepth=10000 | 160.550 ± 5.187 | ns/op | — |
| `OrderBookRestingInsertBenchmark.insertRestingOrders` | preloadDepth=100000 | 177.590 ± 7.191 | ns/op | — |
| `OrderBookCancelBenchmark.cancelRestingOrders` | bookDepth=5000 | 285.325 ± 13.120 | µs/op (batch) | 142.7 ns |
| `OrderBookCancelBenchmark.cancelRestingOrders` | bookDepth=50000 | 343.359 ± 12.596 | µs/op (batch) | 171.7 ns |
| `OrderBookMatchingSingleLevelBenchmark.matchAgainstDeepQueue` | queueDepth=5000 | 467.798 ± 14.843 | µs/op (batch) | 233.9 ns |
| `OrderBookMatchingSingleLevelBenchmark.matchAgainstDeepQueue` | queueDepth=50000 | 481.483 ± 24.727 | µs/op (batch) | 240.7 ns |
| `OrderBookMatchingManyLevelsBenchmark.matchAndDrainLevels` | bookDepth=5000 | 568.003 ± 8.299 | µs/op (batch) | 284.0 ns |
| `OrderBookMatchingManyLevelsBenchmark.matchAndDrainLevels` | bookDepth=50000 | 679.537 ± 15.231 | µs/op (batch) | 339.8 ns |
| `PriceLevelFifoBenchmark.churnFifo` | steadyDepth=1 | 3.313 ± 0.051 | ns/op | — |
| `PriceLevelFifoBenchmark.churnFifo` | steadyDepth=1000 | 3.493 ± 0.031 | ns/op | — |
| `OrderBookMixedWorkloadBenchmark.mixedTraffic` | — | 4,325,892.59 ± 93,790.37 | ops/s | — |

### Object pool vs. plain allocation

Object pooling has more latency than plain allocation, however it has more predictable performances. Pooling results in less allocations and lot less calls to the garbage collector, further improving predictability.
| Benchmark | Latency (ns/op) | Alloc rate | Alloc/op | GC count | GC time |
|---|---|---|---|---|---|
| `AllocatorPoolBenchmark.plainAllocation` | 2.196 ± 0.043 | 27,806.306 ± 531.021 MB/s | 64.000 ± 0.001 B/op | 918 | 439 ms |
| `AllocatorPoolBenchmark.pooledBorrowRelease` | 2.524 ± 0.007 | 0.007 ± 0.001 MB/s | ≈10⁻⁵ B/op | ≈0 | ≈0 ms |
