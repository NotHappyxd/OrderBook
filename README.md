# Order Book Engine

A high-performance, low-latency **Java Order Book Engine** built using the LMAX Disruptor pattern for high-throughput event processing.

The engine uses lock-free concurrency techniques and object pooling to minimize GC pressure and provide predictable latency characteristics.

Orders are received asynchronously through Netty (see the client implementation for an example) and routed to a dedicated shard where they are processed by a single-threaded matching engine.

## Features

* Limit order support *(Market Orders are not implemented yet)*
* Fill-or-Kill (FOK) orders
* Price-time priority matching engine
* Order cancellation
* Event-driven processing architecture
* Low-latency design using LMAX Disruptor
* Lock-free concurrency model
* Object pooling to reduce allocations
* Deterministic order execution
* Snapshot and journal-based recovery

## Architecture Overview

The system is built around an event-driven architecture using the LMAX Disruptor.

Unlike traditional queue-based designs, events are published directly into a preallocated ring buffer and consumed by dedicated consumers. This avoids lock contention and reduces the overhead associated with thread coordination.

The engine follows a **Multiple Producer Single Consumer (MPSC)** model:

```
              +----------------+
              | Netty Gateway  |
              +-------+--------+
                      |
                      v
             +------------------+
             | Order Router     |
             +--------+---------+
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
      +-------+   +-------+   +-------+
      |Shard 1|   |Shard 2|   |Shard N|
      +---+---+   +---+---+   +---+---+
          |           |           |
          v           v           v
      Matching    Matching    Matching
       Engine      Engine      Engine
```

## Sharding Model

To maintain deterministic behavior while supporting multiple order books as well as distributing load, orders are distributed across shards.

Each shard owns a subset of instruments and contains its own:

* Matching engine
* Order book state
* Event processing loop
* Recovery state

Orders are routed to a shard based on their ticker ID. Since each shard is single-threaded, matching operations require no locks and execution order remains deterministic.

## Matching Engine

The matching engine processes orders using **price-time priority**:

1. Orders are matched against the best available price.
2. Orders at the same price level are executed in arrival order.
3. Remaining quantity is added to the order book.

Example:

```
Incoming Buy Order:
Price:    100
Quantity: 50

Existing Sell Orders:
100 x 20
101 x 40

Execution:
20 units filled at 100
30 units remain and rest on the book
```

## Order Book Structure

Each shard maintains independent bid and ask books.

```
ASKS
101 -> Order 4, Order 5
100 -> Order 2

----------------

BIDS
99  -> Order 1
98  -> Order 3
```

Price levels are maintained according to execution priority:

* Lowest ask prices execute first
* Highest bid prices execute first
* Earlier orders at the same price execute first

## Order Lifecycle

```
             Submitted
                 |
                 v
              Matching
                 |
     +-----------+-----------+
     |           |           |
     v           v           v
   Filled   Partially   Resting
            Filled     On Book
                         |
                         v
                     Cancelled
```

## LMAX Disruptor Integration
The LMAX Disruptor allows for lock-free, high-throughput ingress of data and processing.

Processing flow:

1. Netty receives an incoming order request.
2. The request is converted into an internal event.
3. The event is published to the Disruptor ring buffer.
4. The appropriate shard consumes the event.
5. The matching engine updates the order book state.
6. Sends an acknowledgement and order secret to both parties, publishes the book deltas to subscribers.

## Persistence and Recovery

To maintain consistency across restarts, each shard periodically creates a snapshot of its current state.

Between snapshots, all state-changing events are written to a journal.

Recovery process:

1. Load the latest shard snapshot.
2. Replay journaled events after the snapshot.
3. Restore the order book to its previous state.

This approach provides fast recovery while maintaining deterministic state reconstruction.

## Design Goals

The engine is designed around:

* Low latency
* High throughput
* Deterministic matching
* Minimal garbage collection overhead
* Efficient memory usage
* Clear separation between networking, routing, and matching

## Technology Stack

* Java
* LMAX Disruptor
* Netty
* Object pooling
* Lock-free concurrency primitives

## Future Improvements

Potential additions:

* Market order support
* Performance benchmarks
* Circuit breakers
* Improve cold-start times
