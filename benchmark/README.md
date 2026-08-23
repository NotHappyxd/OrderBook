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
| `OrderBookRestingInsertBenchmark.insertRestingOrders` | preloadDepth=0 | 183.844 ± 12.252 | ns/op | — |
| `OrderBookRestingInsertBenchmark.insertRestingOrders` | preloadDepth=10000 | 180.657 ± 9.664 | ns/op | — |
| `OrderBookRestingInsertBenchmark.insertRestingOrders` | preloadDepth=100000 | 209.171 ± 26.424 | ns/op | — |
| `OrderBookCancelBenchmark.cancelRestingOrders` | bookDepth=5000 | 340.009 ± 26.534 | µs/op (batch) | 170.0 ns |
| `OrderBookCancelBenchmark.cancelRestingOrders` | bookDepth=50000 | 412.984 ± 32.647 | µs/op (batch) | 206.5 ns |
| `OrderBookMatchingSingleLevelBenchmark.matchAgainstDeepQueue` | queueDepth=5000 | 598.308 ± 61.816 | µs/op (batch) | 299.2 ns |
| `OrderBookMatchingSingleLevelBenchmark.matchAgainstDeepQueue` | queueDepth=50000 | 624.865 ± 54.592 | µs/op (batch) | 312.4 ns |
| `OrderBookMatchingManyLevelsBenchmark.matchAndDrainLevels` | bookDepth=5000 | 697.667 ± 71.276 | µs/op (batch) | 348.8 ns |
| `OrderBookMatchingManyLevelsBenchmark.matchAndDrainLevels` | bookDepth=50000 | 878.408 ± 46.726 | µs/op (batch) | 439.2 ns |
| `PriceLevelFifoBenchmark.churnFifo` | steadyDepth=1 | 4.709 ± 0.252 | ns/op | — |
| `PriceLevelFifoBenchmark.churnFifo` | steadyDepth=1000 | 4.532 ± 0.178 | ns/op | — |
| `OrderBookMixedWorkloadBenchmark.mixedTraffic` | — | 3,173,757.77 ± 239,460.52 | ops/s | — |

### Object pool vs. plain allocation

Object pooling has more latency than plain allocation, however it has more predictable performances. Pooling results in less allocations and lot less calls to the garbage collector, further improving predictability.
| Benchmark | Latency (ns/op) | Alloc rate | Alloc/op | GC count | GC time |
|---|---|---|---|---|---|
| `AllocatorPoolBenchmark.plainAllocation` | 2.093 ± 0.156 | 29,166.904 ± 2,125.569 MB/s | 64.000 ± 0.001 B/op | 107 | 50 ms |
| `AllocatorPoolBenchmark.pooledBorrowRelease` | 2.512 ± 0.027 | 0.007 ± 0.001 MB/s | ≈10⁻⁵ B/op | ≈0 | ≈0 ms |
